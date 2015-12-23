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
package org.wcs.smart.query.ui.importexport;

import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.ui.editor.IQueryEditor;
import org.wcs.smart.util.E3Utils;

/**
 * Handler for the export query button.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class ExportQueryHandler {

	@Execute
	public void execute(EPartService pService, Shell activeShell, @Named(IServiceConstants.ACTIVE_SELECTION) Object thisSelection) {
		Query query = null;
		//find any query editor part; get the parent stack; then get the 
		//active child
		for (MPart p : pService.getParts()){
			Object src = E3Utils.getSourceObject(p);
			if (src instanceof IQueryEditor){
				Object x = p.getParent().getSelectedElement();
				if (x instanceof MPart){
					Object activeq = E3Utils.getSourceObject((MPart)x);
					if (activeq instanceof IQueryEditor){
						query = ((IQueryEditor)activeq).getQueryProxy().getQuery();
						break;
					}
				}
			}
		}
		if (query == null) return;

		ExportQueryWizard wizard = new ExportQueryWizard(query);
		WizardDialog wd = new WizardDialog(activeShell, wizard);
		wd.open();
	}

	public static class ExportQueryHandlerWrapper extends DIHandler<ExportQueryHandler>{
		public ExportQueryHandlerWrapper(){
			super(ExportQueryHandler.class);
		}
	}
}
