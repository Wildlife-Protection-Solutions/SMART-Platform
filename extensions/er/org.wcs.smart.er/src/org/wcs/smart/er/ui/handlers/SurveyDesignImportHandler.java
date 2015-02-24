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
package org.wcs.smart.er.ui.handlers;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.er.ui.SurveyDesignListView;
import org.wcs.smart.er.ui.surveydesign.importing.ImportSurveyDesignWizard;
import org.wcs.smart.observation.ui.ShowFieldDataPerspective;

/**
 * Handler for importing entity types.
 * @author Jeff
 *
 */

public class SurveyDesignImportHandler {

	@Execute
	public void execute(Shell activeShell, IEclipseContext ctx){
		ctx.set(ShowFieldDataPerspective.FOCUS_VIEW, SurveyDesignListView.ID);
		ContextInjectionFactory.invoke(new ShowFieldDataPerspective(), Execute.class, ctx);
		
		ImportSurveyDesignWizard wizard = new ImportSurveyDesignWizard();
		WizardDialog wd = new WizardDialog(activeShell, wizard);
		wd.open();
	}
	
	public static class SurveyDesignImportHandlerWrapper extends DIHandler<SurveyDesignImportHandler>{
		public SurveyDesignImportHandlerWrapper(){
			super(SurveyDesignImportHandler.class);
		}
	}
}