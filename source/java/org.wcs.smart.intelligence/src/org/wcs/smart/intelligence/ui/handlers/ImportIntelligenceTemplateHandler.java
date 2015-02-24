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
package org.wcs.smart.intelligence.ui.handlers;

import java.io.File;
import java.text.MessageFormat;

import org.apache.commons.io.FileUtils;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.intelligence.IntelligencePlugIn;
import org.wcs.smart.intelligence.internal.Messages;
import org.wcs.smart.intelligence.report.IntelligenceReportPerspective;
import org.wcs.smart.intelligence.report.ReportIntelligence;

/**
 * Handler for importing an intelligence template
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class ImportIntelligenceTemplateHandler {

	@Execute
	public void execute(Shell activeShell, EModelService modelService, MWindow activeWindow) {
		if (! MessageDialog.openConfirm(activeShell, Messages.ImportIntelligenceTemplateHandler_ConfirmDialog_Title, Messages.ImportIntelligenceTemplateHandler_ConfirmDialog_Message)){
			return;
		}
		
		FileDialog fd = new FileDialog(activeShell, SWT.OPEN);
		fd.setFilterNames(new String[]{Messages.ImportIntelligenceTemplateHandler_ReportDesignFile, Messages.ImportIntelligenceTemplateHandler_AllFiles});
		fd.setFilterExtensions(new String[]{"*.rptdesign", "*.*"}); //$NON-NLS-1$ //$NON-NLS-2$
		fd.setText(Messages.ImportIntelligenceTemplateHandler_Dialog_Title);
		fd.setFileName(SmartDB.getCurrentConservationArea().getId() + "_intelligence_template.rptdesign"); //$NON-NLS-1$
		fd.setOverwrite(true);
		String exportFile = fd.open();
		if (exportFile == null){
			return;
		}
		
		final File inFile = new File(exportFile);
		if (!inFile.exists()){
			MessageDialog.openError(activeShell, Messages.ImportIntelligenceTemplateHandler_Error, MessageFormat.format(Messages.ImportIntelligenceTemplateHandler_FileNotExist_Error, exportFile));
			return;
		}
		
		try{
			boolean open = ReportIntelligence.closeTemplateEditor();

			File f = new File(IntelligencePlugIn.getDefault().getIntelligenceDirectory(), ReportIntelligence.INTELLIGENCE_TEMPLATE);
			FileUtils.copyFile(inFile, f);

			//re-open
			if (open && 
					modelService.getActivePerspective(activeWindow).getElementId().equals(IntelligenceReportPerspective.ID)){
				ReportIntelligence.editTemplate();
			}

		}catch (Exception ex){
			IntelligencePlugIn.displayLog(Messages.ImportIntelligenceTemplateHandler_Import_Error + ex.getMessage(), ex);
		}
	}

	public static class ImportIntelligenceTemplateHandlerWrapper extends DIHandler<ImportIntelligenceTemplateHandler>{
		public ImportIntelligenceTemplateHandlerWrapper(){
			super(ImportIntelligenceTemplateHandler.class);
		}
	}
}
