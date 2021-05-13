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
package org.wcs.smart.observation.ui;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.common.attachment.AttachmentInterceptor;
import org.wcs.smart.common.control.WarningDialog;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.ObservationPlugIn;
import org.wcs.smart.observation.WaypointSourceEngine;
import org.wcs.smart.observation.internal.Messages;
import org.wcs.smart.observation.json.IJsonFeatureProcessor;
import org.wcs.smart.observation.json.JsonFileProcessor;
import org.wcs.smart.observation.model.IWaypointSource;
import org.wcs.smart.observation.model.IWaypointSourceProvider;

/**
 * Simple handler for loading GeoJSON observation
 * data.
 * 
 * @author Emily
 * @since 1.0.0
 */
@SuppressWarnings("restriction")
public class ImportJsonDataHandler {

	@Execute
	public void execute(Shell activeShell) {
	
		//get file to process
		FileDialog d = new FileDialog(activeShell);
		String f = d.open();
		if (f == null) return;
		
		Path p = Paths.get(f);
		
		//find processor
		Set<Class<? extends IJsonFeatureProcessor>> processors = new HashSet<>();
		Set<IWaypointSourceProvider> providers = new HashSet<>();
		for (IWaypointSource src : WaypointSourceEngine.INSTANCE.getSupportedSources()) {
			if (src.getJsonFeatureProcessor() != null) {
				processors.add(src.getJsonFeatureProcessor());
				providers.add(WaypointSourceEngine.INSTANCE.findUiProvider(src.getKey()));
			}
		}
		
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(activeShell);
		try {
			pmd.run(true, false, new IRunnableWithProgress() {
				
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					monitor.beginTask(Messages.ImportJsonDataHandler_TaskName, IProgressMonitor.UNKNOWN);
					
					try(Session session = HibernateManager.openSession(new AttachmentInterceptor())){
						
						session.beginTransaction();
						try {
							JsonFileProcessor processor = JsonFileProcessor.create(SmartDB.getCurrentConservationArea(), processors, Locale.getDefault());
							try {
								processor.processData(p, session);
								
								List<String> warnings = processor.getWarnings();
								if (!warnings.isEmpty()) {
										
									int[] r = new int[] {0};
									activeShell.getDisplay().syncExec(()->{
										WarningDialog wd = new WarningDialog(activeShell, Messages.ImportJsonDataHandler_WarningTitle, Messages.ImportJsonDataHandler_WarningMessage,
												warnings, new String[] {Messages.ImportJsonDataHandler_ContinueButton, IDialogConstants.CANCEL_LABEL}, 0);
										r[0] = wd.open();
									});
									if (r[0] != 0) {
										session.getTransaction().rollback();
										return;	
									}
								}
								session.getTransaction().commit();
							
							}finally {
								processor.dispose();
							}
							
							//inform user
							String msg = processor.getMessages().stream().collect(Collectors.joining(" ")); //$NON-NLS-1$
							activeShell.getDisplay().syncExec(()->MessageDialog.openInformation(activeShell, Messages.ImportJsonDataHandler_CompleteTitle, MessageFormat.format(Messages.ImportJsonDataHandler_CompleteMessage, msg)));						

							//post process; fire events
							for (IWaypointSourceProvider provider : providers) {
								for (IJsonFeatureProcessor jsonprocessor : processor.getProcessors()) {
									provider.postProcessJsonData(jsonprocessor);
								}
							}
							
						}catch (Exception ex) {
							ObservationPlugIn.displayLog(ex.getMessage(), ex);
							session.getTransaction().rollback();
						}
						
						session.beginTransaction();
					}
				}
			});
		} catch (Exception e) {
			ObservationPlugIn.displayLog(e.getMessage(), e);
		}
		
	}
	
	public static class ImportJsonDataHandlerWrapper extends DIHandler<ImportJsonDataHandler>{
		public ImportJsonDataHandlerWrapper(){
			super(ImportJsonDataHandler.class);
		}
	}
}
