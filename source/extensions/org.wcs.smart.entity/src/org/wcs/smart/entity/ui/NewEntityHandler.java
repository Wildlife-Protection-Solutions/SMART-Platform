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
package org.wcs.smart.entity.ui;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.entity.internal.Messages;
import org.wcs.smart.entity.ui.newwizard.NewEntityTypeWizard;
import org.wcs.smart.entity.ui.typelist.EntityTypeListView;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.ui.ShowFieldDataPerspective;

/**
 * Create new entity type handler
 * @author Emily
 *
 */
public class NewEntityHandler {

	@Execute
	public void execute(Shell activeShell, MWindow activeWindow){
		//check if data model is configured before continuing
		Session s = HibernateManager.openSession();
		try{
			DataModel dm = HibernateManager.loadDataModel(SmartDB.getCurrentConservationArea(), s);
			if (dm == null || dm.getCategories().size() == 0){
				MessageDialog.openInformation(activeShell, Messages.NewEntityHandler_DialogTitle, Messages.NewEntityHandler_DataModelNotInitialized);
			}
		}finally{
			s.close();
		}
		NewEntityTypeWizard wizard = new NewEntityTypeWizard();
		WizardDialog newEntityDialog = new WizardDialog(activeShell, wizard);
		if (newEntityDialog.open() == Window.OK){
			(new ShowFieldDataPerspective()).execute(EntityTypeListView.ID, activeWindow);
			// open in editor
			(new OpenEntityTypeHandler()).openEntityType(wizard.getNewType(), activeWindow);
		}

		
	}

	public static class NewEntityHandlerWrapper extends DIHandler<NewEntityHandler>{
		public NewEntityHandlerWrapper(){
			super(NewEntityHandler.class);
		}
	}
}
