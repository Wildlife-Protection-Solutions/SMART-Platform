/*
 * Copyright (C) 2012 Wildlife Conservation Society
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
package org.wcs.smart.connect.ui.server;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;

import javax.inject.Named;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.EPartService.PartState;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.internal.ErrorEditorPart;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.SmartConnect;
import org.wcs.smart.connect.server.replication.DownloadChangeLogEngine;
import org.wcs.smart.util.E3Utils;

/**
 * Download change log handler.
 * 
 * @author Emily
 *
 */
public class DownloadChangeLogHandler {

	
	
	@Execute
	public void execute(@Named(IServiceConstants.ACTIVE_SHELL) Shell activeShell, 
			final EPartService pService, IEventBroker events) {
		DownloadChangeLogDialog dialog = new DownloadChangeLogDialog(activeShell);
		if (dialog.open() == Window.OK){
			downloadChangeLog(activeShell, pService, dialog.getConnection(), events);
		}
	}

	private void downloadChangeLog(Shell activeShell, final EPartService pService, 
			final SmartConnect connect, final IEventBroker events){
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(activeShell);
		try {
			pmd.run(true, false, new IRunnableWithProgress() {
				@Override
				public void run(final IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {
					monitor.beginTask("Download and Apply Changelog", 2);
					DownloadChangeLogEngine engine = new DownloadChangeLogEngine(connect);
					try{
						if (engine.downloadInstall(pService, new SubProgressMonitor(monitor, 1))){
							//refresh ui
							refreshUi(pService, events, monitor);
						}
					}catch (Exception ex){
						ConnectPlugIn.displayLog(ex.getMessage(), ex);
					}
					monitor.done();
				}
			});
		} catch (InvocationTargetException | InterruptedException e) {
			ConnectPlugIn.displayLog(e.getMessage(), e);
		}
	}

	private void refreshUi(final EPartService pService,
			final IEventBroker events,
			final IProgressMonitor monitor) {
		Display.getDefault().syncExec(new Runnable(){

			@Override
			public void run() {
				monitor.subTask("Refreshing UI");
				MessageDialog.openInformation(Display.getDefault().getActiveShell(), "Download Complete", "Download complete.");
				
				events.post(SmartPlugIn.E4_DATABASE_CHANGED_EVENT, null);
				
				//find all editors, close and reopen
				Collection<MPart> parts = pService.getParts();
				for (MPart part : parts){
					if (E3Utils.isCompatibilityEditor(part)){
						pService.hidePart(part, false);
						
						PartState state = PartState.ACTIVATE;
						if (pService.isPartVisible(part)){
							state = PartState.VISIBLE;
						}
						try{
							pService.showPart(part, state);
							Object e3part = E3Utils.getSourceObject(part);
//							if (e3part instanceof EditorPart && )
							if (e3part instanceof ErrorEditorPart){
//								System.out.println(((EditorPart) e3part).getEditorSite().getId());
								pService.hidePart(part, true);
							}
						}catch (Throwable ex){
							//eat me; likely the input behind the part was removed
							pService.hidePart(part, true);
						}
					}
				}
			}
			
		});
	}
	
	public static class DownloadChangeLogHandlerWrapper extends DIHandler<DownloadChangeLogHandler>{
		public DownloadChangeLogHandlerWrapper() {
			super(DownloadChangeLogHandler.class);
		}
		
	}
}
