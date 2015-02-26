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

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.report.internal.ui.export.ImportReportWizard;
import org.wcs.smart.ui.ShowPerspectiveHandler;

/**
 * Import report handler pojo
 * @author Emily
 *
 */
public class ImportReportHandler  {

	@Execute
	public void execute(Shell activeShell, MWindow activeWindow){
		
		(new ShowPerspectiveHandler()).execute(ReportViewerPerspective.ID, activeWindow);
		
		ImportReportWizard wizard = new ImportReportWizard();
		WizardDialog dialog = new WizardDialog(activeShell, wizard);
		dialog.open();
	}
	
	public static class ImportReportHandlerWrapper extends DIHandler<ImportReportHandler>{
		public ImportReportHandlerWrapper(){
			super(ImportReportHandler.class);
		}
	}

}
