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
package org.wcs.smart.er.ui.survey.wizard;

import org.eclipse.jface.dialogs.IPageChangingListener;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Composite;
import org.hibernate.Session;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.SurveyEventHandler;
import org.wcs.smart.er.SurveyEventHandler.EventType;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.Survey;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * New survey wizard.
 * 
 * @author Emily
 *
 */
public class NewSurveyWizard extends Wizard implements IPageChangingListener{

	private Session session;
	
	private Survey newSurvey;
	
	private boolean canFinish = false;
	
	private SurveyDesignPage sdPage;
	private SurveyIdPage idPage;
	private SurveyDatePage datePage;
	
	/**
	 * Creates a new wizard
	 * @param parentDesign parent design; optional
	 * @param surveySibling sibling survey; optional
	 */
	public NewSurveyWizard(byte[] parentDesign, byte[] surveySibling){
		session = HibernateManager.openSession();
	
		newSurvey = new Survey();
		setNeedsProgressMonitor(true);
		
		//try to initialize the design
		if (parentDesign != null){
			SurveyDesign surveyDesign = (SurveyDesign) session.load(SurveyDesign.class, parentDesign);
			newSurvey.setSurveyDesign(surveyDesign);
		}else if (surveySibling != null){
			Survey survey = (Survey) session.load(Survey.class, surveySibling);
			newSurvey.setSurveyDesign(survey.getSurveyDesign());
		}
	}
	
	/**
	 * 
	 * @return the newly create survey 
	 */
	public Survey getNewSurvey(){
		return this.newSurvey;
	}
	
	@Override
	public void dispose(){
		if (session.isOpen()){
			session.close();
		}
		super.dispose();
		
	}
	
	@Override
	public boolean canFinish(){
		if (canFinish){
			return super.canFinish();
		}
		return false;
	}

	
	@Override
	public boolean performFinish() {
		datePage.updateSurvey(newSurvey, session);
		
		if (newSurvey.getSurveyDesign() == null){
			return false; 
		}
		
		session.beginTransaction();
		try{
			session.saveOrUpdate(newSurvey);
			session.getTransaction().commit();
		}catch (Exception ex){
			EcologicalRecordsPlugIn.displayLog(Messages.NewSurveyWizard_Error + "\n\n" + ex.getMessage(), ex); //$NON-NLS-1$
			return false;
		}
		
		session.close();
		
		SurveyEventHandler.getInstance().fireEvent(EventType.SURVEY_ADDED, newSurvey);
		return true;
	}

	
	/**
     * The <code>Wizard</code> implementation of this <code>IWizard</code>
     * method does nothing. Subclasses should extend if extra pages need to be
     * added before the wizard opens. New pages should be added by calling
     * <code>addPage</code>.
     */
	public void addPages() {
    	setWindowTitle(Messages.NewSurveyWizard_WizardTitle);
    	
    	sdPage = new SurveyDesignPage();
    	idPage = new SurveyIdPage();
    	datePage = new SurveyDatePage();
    	
    	super.addPage(sdPage);
    	super.addPage(idPage);
    	super.addPage(datePage);
    	    	
    	((WizardDialog) getContainer()).addPageChangingListener(this);
    }

	@Override
	public void createPageControls(Composite pageContainer) {
		super.createPageControls(pageContainer);
		sdPage.initControls(newSurvey, session);
	}

	@Override
	public void handlePageChanging(PageChangingEvent event) {
		//session may be closed by new survey design wizard
		if (!session.isOpen()){
			session = HibernateManager.openSession();
		}
		boolean update = ((INewSurveyWizardPage)event.getCurrentPage()).updateSurvey(newSurvey, session);
		if (!update){
			event.doit = false;
			return;
		}

		((INewSurveyWizardPage)event.getTargetPage()).initControls(newSurvey, session);
		
		if (event.getTargetPage() == datePage){
			canFinish = true;
		}else{
			canFinish = false;
		}
	}

}
