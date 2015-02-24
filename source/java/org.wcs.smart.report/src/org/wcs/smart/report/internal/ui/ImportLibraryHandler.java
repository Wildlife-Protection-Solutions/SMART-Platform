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
package org.wcs.smart.report.internal.ui;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import org.apache.commons.io.FileUtils;
import org.eclipse.birt.report.designer.core.model.SessionHandleAdapter;
import org.eclipse.birt.report.designer.ui.lib.explorer.LibraryExplorerView;
import org.eclipse.birt.report.model.api.command.LibraryChangeEvent;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.internal.PartSite;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.report.ReportPlugIn;
import org.wcs.smart.report.internal.Messages;
import org.wcs.smart.report.library.SmartBirtLibrary;
import org.wcs.smart.report.ui.SmartLibraryEditorInput;
import org.wcs.smart.util.ZipUtil;

/**
 * Handler for importing report libraries.
 * 
 * @author egouge
 *
 */
public class ImportLibraryHandler {

	private static final String ERROR_MSG = Messages.ImportLibraryHandler_ErrorImportingLibrary;

	@Execute
	public void execute(final Shell activeShell, EPartService pService){
		FileDialog fd = new FileDialog(activeShell);
		fd.setFilterNames(new String[]{Messages.ImportLibraryHandler_ZipFilterName, Messages.ImportLibraryHandler_AllFilesFilterName});
		fd.setFilterExtensions(new String[]{"*.zip", "*.*"}); //$NON-NLS-1$ //$NON-NLS-2$
		fd.setText(Messages.ImportLibraryHandler_FileLabel);
		fd.setFileName(SmartDB.getCurrentConservationArea().getId() + "_ReportLibrary.zip"); //$NON-NLS-1$
		
		final String importFile = fd.open();
		if (importFile == null) return;
		
		//close existing report library editor
		SmartLibraryEditorInput ri = new SmartLibraryEditorInput(SmartBirtLibrary.getInstance().getLibraryFile());
		
		IEditorPart part = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findEditor(ri);
		if (part != null){
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().closeEditor(part, false);
		}
		
		final IViewPart site = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(LibraryExplorerView.ID);
		//zip up the contents of the rptlibrary
		ProgressMonitorDialog outputDialog = new ProgressMonitorDialog(activeShell);
		try{
			outputDialog.run(true, false, new IRunnableWithProgress() {
			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException,
					InterruptedException {
				File dirToZip = SmartBirtLibrary.getInstance().getLibraryLocation();
				try {
					FileUtils.deleteDirectory(dirToZip);
					ZipUtil.unzipFolder(new File(importFile), dirToZip.getParentFile());
					activeShell.getDisplay().syncExec(new Runnable(){
						@Override
						public void run() {
							MessageDialog.openInformation(activeShell, Messages.ImportLibraryHandler_ImportOk_DialogTitle, Messages.ImportLibraryHandler_ImportOk);		
						}});
					
					//fire library change event
					SessionHandleAdapter.getInstance().getSessionHandle().fireResourceChange(new LibraryChangeEvent(SmartBirtLibrary.getInstance().getLibraryFile().getAbsolutePath()));

					//refresh shared resources programatically; this does not refresh editors
					
					if (site != null){
						((PartSite)site.getSite()).getActionBars().getGlobalActionHandler(ActionFactory.REFRESH.getId( )).run();
					}
				} catch (Exception ex) {
					ReportPlugIn.displayLog(ERROR_MSG + ex.getLocalizedMessage(), ex);
				}	
			}
		});
		}catch (Exception ex){
			ReportPlugIn.displayLog(ERROR_MSG, ex);
		}
	}

	public static class ImportLibraryHandlerWrapper extends DIHandler<ImportLibraryHandler>{
		public ImportLibraryHandlerWrapper(){
			super(ImportLibraryHandler.class);
		}
	}
}
