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
package org.wcs.smart.plan.ui.handlers;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.plan.SmartPlanPlugIn;
import org.wcs.smart.plan.internal.Messages;
import org.wcs.smart.plan.report.ReportPlan;

/**
 * Handler for exporting a plan template
 * @author Emily
 *
 */
public class ExportTemplateHandler  {

	@Execute
	public void execute(Shell activeShell){
		
		FileDialog fd = new FileDialog(activeShell, SWT.SAVE);
		fd.setFilterNames(new String[]{Messages.ExportTemplateHandler_ReportDesignFileName, Messages.ExportTemplateHandler_AllFiles});
		fd.setFilterExtensions(new String[]{"*.rptdesign", "*.*"}); //$NON-NLS-1$ //$NON-NLS-2$
		fd.setText(Messages.ExportTemplateHandler_ExportDialogTitle);
		fd.setFileName(SmartDB.getCurrentConservationArea().getId() + "_plantemplate.rptdesign"); //$NON-NLS-1$
		fd.setOverwrite(true);
		String exportFile = fd.open();
		if (exportFile == null){
			return;
		}
		final File outFile = new File(exportFile);
		//zip up the contents of the rptlibrary
		ProgressMonitorDialog outputDialog = new ProgressMonitorDialog(activeShell);
		try{
			outputDialog.run(true, false, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException,
					InterruptedException {
					try {
						FileUtils.copyInputStreamToFile(ReportPlan.getPlanTemplate(), outFile);
					} catch (Exception ex) {
						SmartPlanPlugIn.displayLog(Messages.ExportTemplateHandler_ErrorExportingTemplate + ex.getMessage(), ex);
					}	
				}
			});
		}catch (Exception ex){
			SmartPlanPlugIn.displayLog(Messages.ExportTemplateHandler_ErrorExportingTemplate + ex.getMessage(), ex);
		}
	}

	public static class ExportTemplateHandlerWrapper extends DIHandler<ExportTemplateHandler>{
		public ExportTemplateHandlerWrapper(){
			super(ExportTemplateHandler.class);
		}
	}
}

