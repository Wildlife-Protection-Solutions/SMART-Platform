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

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.handlers.HandlerUtil;
import org.hibernate.Session;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.entity.internal.Messages;
import org.wcs.smart.entity.ui.newwizard.NewEntityTypeWizard;
import org.wcs.smart.entity.ui.typelist.EntityTypeListView;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.ui.FieldDataPerspective;

/**
 * Create new entity type handler
 * @author Emily
 *
 */
public class NewEntityHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		//check if data model is configured before continuing
		Session s = HibernateManager.openSession();
		try{
			DataModel dm = HibernateManager.loadDataModel(SmartDB.getCurrentConservationArea(), s);
			if (dm == null || dm.getCategories().size() == 0){
				MessageDialog.openInformation(HandlerUtil.getActiveShell(event), Messages.NewEntityHandler_DialogTitle, Messages.NewEntityHandler_DataModelNotInitialized);
				return null;
			}
		}finally{
			s.close();
		}
		
		WizardDialog newEntityDialog = new WizardDialog(HandlerUtil.getActiveShell(event), new NewEntityTypeWizard());
		newEntityDialog.open();
		
		FieldDataPerspective.openPerspective(EntityTypeListView.ID);
		return null;
	}

}
