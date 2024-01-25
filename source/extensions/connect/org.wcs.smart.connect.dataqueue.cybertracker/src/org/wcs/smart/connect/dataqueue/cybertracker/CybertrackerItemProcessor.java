/*
 * Copyright (C) 2016 Wildlife Conservation Society
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.wcs.smart.connect.dataqueue.cybertracker;

import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.hibernate.Session;
import org.json.simple.JSONObject;
import org.wcs.smart.common.attachment.AttachmentInterceptor;
import org.wcs.smart.common.control.WarningDialog;
import org.wcs.smart.connect.dataqueue.cybertracker.internal.Messages;
import org.wcs.smart.connect.dataqueue.model.DataQueueItem;
import org.wcs.smart.connect.dataqueue.model.LocalDataQueueItem;
import org.wcs.smart.connect.dataqueue.model.LocalDataQueueItem.Status;
import org.wcs.smart.connect.dataqueue.process.IItemProcessor;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.importer.json.IDesktopJsonProcessor;
import org.wcs.smart.cybertracker.json.CtJsonUtil;
import org.wcs.smart.cybertracker.json.UserCancelledException;
import org.wcs.smart.cybertracker.util.ZLibUtil;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * Processes cybertrakcer json data.
 * 
 * @author Emily
 *
 */
public class CybertrackerItemProcessor implements IItemProcessor, IRunnableWithProgress {

	public static final String CT_TYPE = "JSON_CT"; //$NON-NLS-1$
	public static final String CT_ZIP_TYPE = "JSON_ZLIB_CT"; //$NON-NLS-1$
			
	private String json;
	private ProcessingStatus returnValue;
	private Exception returnException;
	private LocalDataQueueItem litem;
	private ProgressMonitorDialog pmdDialog;
	
	private Object lock = new Object();
	private static volatile List<IDesktopJsonProcessor> jsonProcessors;

	private Listener disposeListener = new Listener() {
		
		@Override
		public void handleEvent(Event event) {
			synchronized (lock) {
				lock.notifyAll();
			}
		}
	};
	
	public CybertrackerItemProcessor() {

	}

	@Override
	public boolean canProcess(String type) {
		if (!MobileProcessingEngine.INSTANCE.getCanSmartMobileDesktopProcessing()) return false;
		return (type.toUpperCase(Locale.ROOT).equals(CT_TYPE) || type.toUpperCase(Locale.ROOT).equals(CT_ZIP_TYPE));
	}
	
	@Override
	public ProcessingStatus process(DataQueueItem item, IProgressMonitor monitor)
			throws Exception {
		litem = (LocalDataQueueItem)item;
		
		json = null;
		returnException = null;
		returnValue = null;
		
		final CloseMsgDialog[] id = {null};
		
		if (item.getType().toUpperCase(Locale.ROOT).equals(CT_ZIP_TYPE)){
			json = ZLibUtil.decompressFile(litem.getFullFilePath());
		}else if (item.getType().toUpperCase(Locale.ROOT).equals(CT_TYPE)){
			try(Reader in = Files.newBufferedReader(litem.getFullFilePath(), StandardCharsets.UTF_8)){
				json = IOUtils.toString(in);
			}
		}

		//ensure all dialogs are closed before proceeding; proceed in pmd so users cannot open dialogs and cause problems
		if (PlatformUI.getWorkbench().getActiveWorkbenchWindow() == null){
			run(new NullProgressMonitor());
		}else{
			final boolean[] wait = new boolean[]{true};
			while (true){
				//wait for all dialog shells to close
				Display.getDefault().syncExec(new Runnable(){
					@Override
					public void run() {
						if (id[0] != null) id[0].close();
						
						Shell main = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
						if ((main.getShells().length == 0) || (main.getShells().length == 1 && main.getShells()[0] == id[0].getShell())){
							wait[0] = false;
						}
						for (Shell s : main.getShells()){
							boolean found = false;
							for (Listener ll : s.getListeners(SWT.Dispose)){
								if (ll == disposeListener){
									found = true;
									break;
								}
							}
							if (!found) s.addListener(SWT.Dispose,disposeListener);
						}
						
						if (!wait[0]){
							pmdDialog = new ProgressMonitorDialog(Display.getDefault().getActiveShell());
							try {
								pmdDialog.run(true, false, CybertrackerItemProcessor.this);
							} catch (InvocationTargetException | InterruptedException e) {
								returnException = e;
							}
						}else{
							if (id[0] == null && PlatformUI.getWorkbench() != null){
								id [0] = new CloseMsgDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());
							}
							id[0].open();
						}
					}
				});
				if (!wait[0]) break;
				synchronized (lock) {
					lock.wait();
				}
			}
		}
		
		if (returnException != null) throw returnException;
		return returnValue;		
	}

	private static List<IDesktopJsonProcessor> getProcessors() throws Exception{
		if (jsonProcessors == null){
			synchronized (CybertrackerItemProcessor.class) {
				if (jsonProcessors == null){
					ArrayList<IDesktopJsonProcessor> temp = new ArrayList<IDesktopJsonProcessor>();
					IConfigurationElement[] config = Platform.getExtensionRegistry()
							.getConfigurationElementsFor(IDesktopJsonProcessor.EXTENSION_ID);
					
					for (IConfigurationElement e : config) {
						if (e.getName().equalsIgnoreCase("JsonProcessor")){ //$NON-NLS-1$
							IDesktopJsonProcessor proc = (IDesktopJsonProcessor) e.createExecutableExtension("class"); //$NON-NLS-1$
							temp.add(proc);
						}
					}
					jsonProcessors = temp;
				}
			}
			
		}
		List<IDesktopJsonProcessor> copy = new ArrayList<IDesktopJsonProcessor>(jsonProcessors.size());
		for (IDesktopJsonProcessor p : jsonProcessors){
			copy.add(p.getClass().getConstructor().newInstance());
		}
		return copy;
	}

	@Override
	public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		try{
			monitor.setTaskName(MessageFormat.format(Messages.CybertrackerItemProcessor_TaskName, litem.getName()));
			returnValue = run();
		}catch (Exception ex){
			returnException = ex;
		}
		return;
	}
	
	private ProcessingStatus run() throws Exception{
		List<JSONObject> features = CtJsonUtil.parseFeaturesFromJsonString(json);
		try(Session session = HibernateManager.openSession(new AttachmentInterceptor())){
		
			session.beginTransaction();
			try {
				List<JSONObject> notProc = new ArrayList<JSONObject>();
				notProc.addAll(features);
				StringBuilder statusMsg = new StringBuilder();
				List<IDesktopJsonProcessor> processors = getProcessors();
				for (IDesktopJsonProcessor p : processors){
					List<JSONObject> processed = p.processJson(features, session, Locale.getDefault());
					notProc.removeAll(processed);
					String msg = p.getStatusMessage(Locale.getDefault());
					if (msg != null){
						statusMsg.append(msg);
					}
				}
	
				
				if (notProc.size() == features.size()){
					//nothing has been processed perhaps we re-queue this item
					//for later as maybe something else needs to be processed first
					session.getTransaction().rollback();
					ProcessingStatus status = new ProcessingStatus(Status.REQUEUED, Messages.CybertrackerItemProcessor_NoData);
					return status;
				}
				
				final boolean[] cont = new boolean[]{true};			
				if (!notProc.isEmpty()){
					//not all items have been processed
					final List<String> warnings = new ArrayList<String>();
					for (JSONObject o : notProc){
						warnings.add(o.toJSONString());
					}	
					Display.getDefault().syncExec(new Runnable(){
						@Override
						public void run() {
							String message = MessageFormat.format(Messages.CybertrackerItemProcessor_ProcessedMsg, features.size() - notProc.size(), features.size(), notProc.size() );
							WarningDialog wd = new WarningDialog(Display.getDefault().getActiveShell(), Messages.CybertrackerItemProcessor_WarningTitle,message,
									warnings, new String[]{IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL}, 1);
							if (wd.open() == 0){
								cont[0] = true;
							}else{
								cont[0] = false;
							}
						}
						
					});
				}
				if (!cont[0]){
					session.getTransaction().rollback();
					ProcessingStatus status = new ProcessingStatus(Status.REQUEUED, Messages.CybertrackerItemProcessor_CancelledMsg);
					return status;
				}
				
				session.getTransaction().commit();
				
				ProcessingStatus status = new ProcessingStatus(Status.COMPLETE, MessageFormat.format(Messages.CybertrackerItemProcessor_CompleteMsg, statusMsg.toString()));
				
				
				for (IDesktopJsonProcessor p : processors){
					try{
						p.afterSave();
					}catch (Throwable t){
						CyberTrackerPlugIn.displayError(Messages.CybertrackerItemProcessor_ErrorTitle, t.getMessage(), t);
					}
				}
				return status;
			}catch (UserCancelledException ex){
				session.getTransaction().rollback();
				ProcessingStatus status = new ProcessingStatus(Status.REQUEUED, Messages.CybertrackerItemProcessor_Cancelled2 + ex.getMessage());
				return status;
	
			}catch (Exception ex){
				CyberTrackerPlugIn.log(ex.getMessage(), ex);
				session.getTransaction().rollback();
				
				ProcessingStatus status = new ProcessingStatus(Status.ERROR, MessageFormat.format(Messages.CybertrackerItemProcessor_DataProcessingError, ex.getMessage()));
				return status;
			}
		}
	}
}
