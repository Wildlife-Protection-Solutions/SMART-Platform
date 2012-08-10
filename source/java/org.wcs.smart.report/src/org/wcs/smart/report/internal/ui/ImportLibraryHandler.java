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
import org.eclipse.birt.report.model.api.command.LibraryChangeEvent;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.handlers.HandlerUtil;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.report.ReportPlugIn;
import org.wcs.smart.report.library.SmartBirtLibrary;
import org.wcs.smart.util.ZipUtil;

/**
 * Handler for importing report libraries.
 * 
 * @author egouge
 *
 */
public class ImportLibraryHandler extends AbstractHandler {

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		FileDialog fd = new FileDialog(HandlerUtil.getActiveShell(event));
		fd.setFilterNames(new String[]{"Zip (*.zip)", "*.*"});
		fd.setFilterExtensions(new String[]{"*.zip", "*.*"});
		fd.setText("Import File");
		fd.setFileName(SmartDB.getCurrentConservationArea().getId() + "_ReportLibrary.zip");
		
		final String importFile = fd.open();
		if (importFile == null){
			return null;
		}
		
		//zip up the contents of the rptlibrary
		ProgressMonitorDialog outputDialog = new ProgressMonitorDialog(HandlerUtil.getActiveShell(event));
		try{
		outputDialog.run(true, false, new IRunnableWithProgress() {
			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException,
					InterruptedException {
				File dirToZip = SmartBirtLibrary.getInstance().getLibraryLocation();
				try {
					FileUtils.deleteDirectory(dirToZip);
					ZipUtil.unzipFolder(new File(importFile), dirToZip.getParentFile());
					Display.getDefault().syncExec(new Runnable(){
						@Override
						public void run() {
							MessageDialog.openInformation(HandlerUtil.getActiveShell(event), "Import", "Library imported successfully");		
						}});
					
					//fire library change event
					SessionHandleAdapter.getInstance().getSessionHandle().fireResourceChange(new LibraryChangeEvent(SmartBirtLibrary.getInstance().getLibraryFile().getAbsolutePath()));
					//TODO: figure out how to refresh shared resources programatically
					
					
					
				} catch (Exception ex) {
					ReportPlugIn.displayLog("Error importing report library: " + ex.getMessage(), ex);
				}	
			}
		});
		}catch (Exception ex){
			ReportPlugIn.displayLog("Error importing report library", ex);
		}
		return null;
	}

}
