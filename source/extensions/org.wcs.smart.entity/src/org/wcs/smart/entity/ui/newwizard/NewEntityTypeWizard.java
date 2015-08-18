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
package org.wcs.smart.entity.ui.newwizard;

import java.util.ArrayList;
import java.util.Date;

import org.eclipse.jface.dialogs.IPageChangingListener;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Composite;
import org.hibernate.Session;
import org.wcs.smart.ca.datamodel.DataModelManager;
import org.wcs.smart.entity.EntityPlugIn;
import org.wcs.smart.entity.event.EntityEventManager;
import org.wcs.smart.entity.internal.Messages;
import org.wcs.smart.entity.model.EntityAttribute;
import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.entity.model.Status;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;

/**
 * New incident wizard.
 * @author Emily
 *
 */
public class NewEntityTypeWizard extends Wizard implements IPageChangingListener {

	private EntityType newType;
	private Session session;
	
	public NewEntityTypeWizard(){
		super();
		
		setWindowTitle(Messages.NewEntityTypeWizard_WizardTitle);

		newType = new EntityType();
		newType.setConservationArea(SmartDB.getCurrentConservationArea());
		newType.setCreator(SmartDB.getCurrentEmployee());
		newType.setDateCreated(new Date());
		newType.setStatus(Status.ACTIVE);
		newType.setAttributes(new ArrayList<EntityAttribute>());

		session = HibernateManager.openSession();
	}

	public EntityType getNewType(){
		return this.newType;
	}
	
	@Override
	public boolean performFinish() {
		//update the last page
		((NewEntityTypeWizardPage)this.getPages()[this.getPageCount()-1]).updateEntityType(newType);
		
		//save to db
		session.beginTransaction();
		try{
			session.saveOrUpdate(newType.getDmAttribute());
			session.save(newType);
			session.getTransaction().commit();
		}catch(Exception ex){
			session.getTransaction().rollback();
			EntityPlugIn.displayLog(Messages.NewEntityTypeWizard_ErrorSavingEntityType + ex.getMessage(), ex);
			return false;
		}
		
		// fire events
		EntityEventManager.getInstance().fireEvent(EntityEventManager.ENTITY_TYPE_ADDED, newType);
		DataModelManager.INSTANCE.fireChangeListeners();	//we have added a new attribute to the data model
		
		return true;
	}
	
	/**
	 * Closes the active session
	 */
	@Override
	public void dispose() {
		super.dispose();
		if (session != null && session.isOpen()) {
			session.close();
		}
	}

	@Override
	public boolean canFinish(){
		if (!super.canFinish()) return false;
		
		if (getContainer().getCurrentPage() != null && ((NewEntityTypeWizardPage)super.getContainer().getCurrentPage()).canFinish()){
			return true;
		}
		return false;
	}
	
    public void addPages() {
    	((WizardDialog) getContainer()).addPageChangingListener(this);
    	
    	super.addPage(new NewEntityTypeWizardPage(this, new TypeComposite()));
    	super.addPage(new NewEntityTypeWizardPage(this, new NameIdKeyComposite()));
    	super.addPage(new NewEntityTypeWizardPage(this, new AttributeNameField()));
    	
    }
    
	
	@Override
	 public void createPageControls(Composite pageContainer) {
		super.createPageControls(pageContainer);
	
		((NewEntityTypeWizardPage)super.getPages()[0]).initPage(newType, session);
	}
	
	@Override
	public void handlePageChanging(PageChangingEvent event) {
		NewEntityTypeWizardPage page = (NewEntityTypeWizardPage) event.getCurrentPage();
		NewEntityTypeWizardPage next = (NewEntityTypeWizardPage) event.getTargetPage();
		NewEntityTypeWizardPage enext = (NewEntityTypeWizardPage) getNextPage(page);
		if (next == enext){
			//we are moving forward
			String error = page.validate();
			if (error != null){
				//there is an error
				event.doit = false;
				return;
			}
			page.updateEntityType(newType);
			
			//init the new page
			next.initPage(newType, session);
			
		}else{
			//moving backwards so don't do any validation
		}
		next.validate();
		getContainer().updateButtons();
		
		
	}

}
