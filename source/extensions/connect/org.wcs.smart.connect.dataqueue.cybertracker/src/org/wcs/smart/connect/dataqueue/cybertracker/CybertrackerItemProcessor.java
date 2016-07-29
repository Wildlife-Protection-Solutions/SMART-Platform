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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.json.simple.JSONObject;
import org.wcs.smart.common.attachment.AttachmentInterceptor;
import org.wcs.smart.common.control.WarningDialog;
import org.wcs.smart.connect.dataqueue.model.DataQueueItem;
import org.wcs.smart.connect.dataqueue.model.DataQueueItem.Type;
import org.wcs.smart.connect.dataqueue.model.LocalDataQueueItem;
import org.wcs.smart.connect.dataqueue.model.LocalDataQueueItem.Status;
import org.wcs.smart.connect.dataqueue.process.IItemProcessor;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.util.ZLibUtil;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * Processes cybertrakcer json data.
 * 
 * @author Emily
 *
 */
public class CybertrackerItemProcessor implements IItemProcessor {

	public CybertrackerItemProcessor() {
	}

	@Override
	public boolean canProcess(Type type) {
		return type == Type.JSON_CT || type == Type.JSON_ZLIB_CT;
	}

	@Override
	public ProcessingStatus process(DataQueueItem item, IProgressMonitor monitor)
			throws Exception {
		LocalDataQueueItem litem = (LocalDataQueueItem)item;
		
		String json = null;
		if (item.getType() == Type.JSON_ZLIB_CT){
			json = ZLibUtil.decompressFile(litem.getFullFilePath().toFile());
		}else if (item.getType() == Type.JSON_CT){
			try(Reader in = Files.newBufferedReader(litem.getFullFilePath(), StandardCharsets.UTF_8)){
				json = IOUtils.toString(in);
			}
		}

		List<JSONObject> features = (new JsonCtParser()).parseFeaturesFromJsonString(json);
		Session session = HibernateManager.openSession(new AttachmentInterceptor());
		try{
			session.beginTransaction();
			
			List<JSONObject> notProc = new ArrayList<JSONObject>();
			notProc.addAll(features);
			StringBuilder statusMsg = new StringBuilder();
			List<IJsonProcessor> processors = getProcessors();
			for (IJsonProcessor p : processors){
				List<JSONObject> processed = p.processJson(features, session);
				notProc.removeAll(processed);
				String msg = p.getStatusMessage();
				if (msg != null){
					statusMsg.append(msg);
				}
			}

			
			if (notProc.size() == features.size()){
				//nothing has been processed perhaps we re-queue this item
				//for later as maybe something else needs to be processed first
				session.getTransaction().rollback();
				ProcessingStatus status = new ProcessingStatus(Status.REQUEUED, "Could not process any of the data, requeueing on server.");
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
						String message = MessageFormat.format("{0} of {1} features where processed. The following {2} features could not be processed.  If you continue this data will NOT be imported into SMART. Do you want to continue?", features.size() - notProc.size(), features.size(), notProc.size() );
						WarningDialog wd = new WarningDialog(Display.getDefault().getActiveShell(), "Warning",message,
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
				ProcessingStatus status = new ProcessingStatus(Status.REQUEUED, "User Cancelled: Item to be rescheduled.  User cancelled because not all data could be processed.");
				return status;
			}
			
			session.getTransaction().commit();
			
			ProcessingStatus status = new ProcessingStatus(Status.COMPLETE, "Complete: " + statusMsg.toString());
			
			
			for (IJsonProcessor p : processors){
				try{
					p.afterSave();
				}catch (Throwable t){
					CyberTrackerPlugIn.displayError("Error", t.getMessage(), t);
				}
			}
			return status;
		}catch (UserCancelledException ex){
			session.getTransaction().rollback();
			ProcessingStatus status = new ProcessingStatus(Status.REQUEUED, "User cancelled, item should be requeue on server." + ex.getMessage());
			return status;

		}catch (Exception ex){
			CyberTrackerPlugIn.log(ex.getMessage(), ex);
			session.getTransaction().rollback();
			
			ProcessingStatus status = new ProcessingStatus(Status.ERROR, "Error processing data: " + ex.getMessage());
			return status;
		}finally{
			session.close();
		}
	}

	private static volatile List<IJsonProcessor> jsonProcessors;
	
	private static List<IJsonProcessor> getProcessors() throws Exception{
		if (jsonProcessors == null){
			synchronized (CybertrackerItemProcessor.class) {
				if (jsonProcessors == null){
					jsonProcessors = new ArrayList<IJsonProcessor>();
					IConfigurationElement[] config = Platform.getExtensionRegistry()
							.getConfigurationElementsFor(IJsonProcessor.EXTENSION_ID);
					
					for (IConfigurationElement e : config) {
						if (e.getName().equalsIgnoreCase("JsonProcessor")){ //$NON-NLS-1$
							IJsonProcessor proc = (IJsonProcessor) e.createExecutableExtension("class"); //$NON-NLS-1$
							jsonProcessors.add(proc);
						}
					}
				}
			}
			
		}
		List<IJsonProcessor> copy = new ArrayList<IJsonProcessor>(jsonProcessors.size());
		for (IJsonProcessor p : jsonProcessors){
			copy.add(p.getClass().newInstance());
		}
		return copy;
	}
}
