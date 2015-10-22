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
package org.wcs.smart.er.ui.surveydesign.wizard;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.IPageChangingListener;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Composite;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.SurveyEventHandler;
import org.wcs.smart.er.SurveyEventHandler.EventType;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.ui.ISurveyListener;
import org.wcs.smart.er.ui.surveydesign.ConfigurableModelComposite;
import org.wcs.smart.er.ui.surveydesign.DateComposites;
import org.wcs.smart.er.ui.surveydesign.DescriptionComposite;
import org.wcs.smart.er.ui.surveydesign.DistanceDirectionComposite;
import org.wcs.smart.er.ui.surveydesign.MissionPropertiesComposite;
import org.wcs.smart.er.ui.surveydesign.NameIdComposite;
import org.wcs.smart.er.ui.surveydesign.PropertiesComposite;
import org.wcs.smart.er.ui.surveydesign.SurveyDesignComposite;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;

/**
 * New survey design wizard.
 * 
 * @author Emily
 *
 */
public class NewSurveyDesignWizard extends Wizard implements IPageChangingListener{

	
	private Session session;
	
	private SurveyDesign newDesign;
	private SurveyDesign savedNewDesign;
	
	private boolean canFinish = false;
	
	private SurveyDesignComposite[] comps;
	
	private Object lastPage;
	private TemplateWizardPage templatePage;
		
	/**
	 * Creates a new wizard
	 */
	public NewSurveyDesignWizard(){
		//init design
		this.newDesign = new SurveyDesign();
		this.newDesign.setConservationArea(SmartDB.getCurrentConservationArea());
		this.newDesign.setState(SurveyDesign.State.ACTIVE);
		
		session = HibernateManager.openSession();
	}
	
	@Override
	public void dispose(){
		super.dispose();
		if (session.isOpen()){
			session.close();
		}
	}
	
	public SurveyDesign getNewSurveyDesign(){
		return savedNewDesign;
	}
	
	@Override
	public boolean canFinish(){
		if (canFinish){
			return super.canFinish();
		}
		return false;
	}
	
	@Override
	public void createPageControls(Composite pageContainer) {
		super.createPageControls(pageContainer);
		
		if (getStartingPage() instanceof SurveyCompositeWizardPage){
			((SurveyCompositeWizardPage)getStartingPage()).initPage(newDesign);
		}
		
		//configure listeners
    	ISurveyListener updateButtons = new ISurveyListener() {
			@Override
			public void compositeModified() {
				getContainer().updateButtons();
			}
		};
		
		for (SurveyDesignComposite c : comps){
    		c.addChangeListener(updateButtons);
    	}
	}
	
	
	@Override
	public boolean performFinish() {
		savedNewDesign = null;
        if (lastPage instanceof SurveyCompositeWizardPage) {
            ((SurveyCompositeWizardPage) lastPage).updateModel(newDesign);
        }

        session.beginTransaction();
		try{
			session.save(newDesign);
			
			if (templatePage != null){
				if (templatePage.getClonedSamplingUnits() != null){
					for (SamplingUnit s : templatePage.getClonedSamplingUnits()){
						s.setSurveyDesign(newDesign);
						session.save(s);
					}
				}
			}
			session.getTransaction().commit();
			
			savedNewDesign = newDesign;
			SurveyEventHandler.getInstance().fireEvent(EventType.SURVEY_DESIGN_ADDED, newDesign);
			return true;
		}catch (Exception ex){
			try{
				session.getTransaction().rollback();
			}catch (Exception ex2){
				EcologicalRecordsPlugIn.log(ex.getMessage(), ex2);
			}
			EcologicalRecordsPlugIn.displayLog(Messages.NewSurveyDesignWizard_SaveError + "\n\n" + ex.getMessage(), ex); //$NON-NLS-1$
		}
		return false;
	}

	
	/**
     * The <code>Wizard</code> implementation of this <code>IWizard</code>
     * method does nothing. Subclasses should extend if extra pages need to be
     * added before the wizard opens. New pages should be added by calling
     * <code>addPage</code>.
     */
    @SuppressWarnings("unchecked")
	public void addPages() {
    	
    	setWindowTitle(Messages.NewSurveyDesignWizard_WindowTitle);
    	
    	List<ConfigurableModel> models = session.createCriteria(ConfigurableModel.class)
    			.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())).list(); //$NON-NLS-1$
    	
    	List<SurveyDesign> allOthers = session.createCriteria(SurveyDesign.class).list();
    	
    	List<SurveyDesign> caOthers = new ArrayList<SurveyDesign>();
    	for (SurveyDesign d : allOthers){
    		if (d.getConservationArea().equals(SmartDB.getCurrentConservationArea())){
    			caOthers.add(d);
    		}
    	}
    	
    	if (allOthers.size() > 0){
    		//only add template option if other survey available
    		templatePage = new TemplateWizardPage(allOthers);
    		super.addPage(templatePage);
    	}
    	
    	comps = new SurveyDesignComposite[]{
    			new NameIdComposite(caOthers),
    			new DateComposites(),
    			new DistanceDirectionComposite(),
    			new ConfigurableModelComposite(models),
    			new MissionPropertiesComposite(),
    			new PropertiesComposite(),
    			new DescriptionComposite()
    	};
		for (SurveyDesignComposite c : comps){
    		super.addPage(new SurveyCompositeWizardPage(c));
    	}

    	((WizardDialog) getContainer()).addPageChangingListener(this);
    }


	@Override
	public void handlePageChanging(PageChangingEvent event) {
		//update design with page values
		if (event.getCurrentPage() instanceof SurveyCompositeWizardPage){
			((SurveyCompositeWizardPage)event.getCurrentPage()).updateModel(newDesign);
		}else if (event.getCurrentPage() instanceof TemplateWizardPage){
			((TemplateWizardPage)event.getCurrentPage()).updateModel(newDesign, session);
		}
		
		//init target page
		if (event.getTargetPage() instanceof SurveyCompositeWizardPage){
			((SurveyCompositeWizardPage)event.getTargetPage()).initPage(newDesign);
			
			canFinish =  (event.getTargetPage().equals(getPages()[getPageCount()-1]));
		}
		
        if (event.doit) {
            lastPage = event.getTargetPage();
        }

	}

	public Session getSession() {
		return this.session;
	}
}
