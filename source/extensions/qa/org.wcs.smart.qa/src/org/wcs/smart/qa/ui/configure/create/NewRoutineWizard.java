/*
 * Copyright (C) 2017 Wildlife Conservation Society
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
package org.wcs.smart.qa.ui.configure.create;

import org.eclipse.jface.dialogs.IPageChangingListener;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.qa.InternalExtensionManager;
import org.wcs.smart.qa.QaPlugIn;
import org.wcs.smart.qa.model.QaRoutine;

/**
 * Wizard for creating a new QA Routine 
 * @author Emily
 *
 */
public class NewRoutineWizard extends Wizard implements IPageChangingListener{

	private TypeNamePage page1 ;
	private ParameterPage page2 ;

    private QaRoutine routine = null;
    
    @Override
	public void addPages() {
    	((WizardDialog) getContainer()).addPageChangingListener(this);

    	page1 = new TypeNamePage();
    	page2 = new ParameterPage();
    	
    	addPage(page1);
    	addPage(page2);
    	
    	setWindowTitle("Create New Quality Assurance Routine");    	
    }

    @Override
	public boolean canFinish(){
		if (!super.canFinish()) return false;
		
		if (getContainer().getCurrentPage() == page2){
			return true;
		}
		return false;
	}
	
 	
	@Override
	public void handlePageChanging(PageChangingEvent event) {
		if (event.getCurrentPage() == page1){
			routine = page1.getRoutine();
		}
		
		if (event.getTargetPage() == page2){
			page2.initPage(routine);
		}
	}


	@Override
	public boolean performFinish() {
		if (routine == null) return false;
		
		page2.updateRoutine(routine);
		
		Session s = HibernateManager.openSession();
		s.beginTransaction();
		try{
			s.save(routine);
			s.getTransaction().commit();
		}catch (Exception ex){
			s.getTransaction().rollback();
			QaPlugIn.displayLog("Unable to save QA Routine: " + ex.getMessage(), ex);
			return false;
		}finally{
			s.close();
		}
		InternalExtensionManager.INSTANCE.clearAutoRoutines();
		return true;
	}
}
