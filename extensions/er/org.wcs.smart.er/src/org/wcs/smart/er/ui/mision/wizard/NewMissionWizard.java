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
package org.wcs.smart.er.ui.mision.wizard;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.IPageChangingListener;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Composite;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.SurveyEventHandler;
import org.wcs.smart.er.SurveyEventHandler.EventType;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.Survey;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.ui.ISurveyListener;
import org.wcs.smart.er.ui.mision.CommentComposite;
import org.wcs.smart.er.ui.mision.DateComposite;
import org.wcs.smart.er.ui.mision.IdComposite;
import org.wcs.smart.er.ui.mision.MissionComposite;
import org.wcs.smart.er.ui.mision.MissionEmployeeComposite;
import org.wcs.smart.er.ui.mision.MissionPropertyValuesComposite;
import org.wcs.smart.er.ui.mision.SurveyComposite;
import org.wcs.smart.er.ui.mision.SurveyDesignComposite;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;

/**
 * Wizard for creating a new mission.
 * 
 * @author Emily
 *
 */
public class NewMissionWizard extends Wizard implements IPageChangingListener{

	private Session session;
	
	private Mission newMission;
	private SurveyDesign parentDesign;
	private Survey parentSurvey;
	
	private boolean canFinish = false;
	
	private List<MissionComposite> localPages;
	
	/**
	 * Creates a new wizard
	 */
	public NewMissionWizard(byte[] parentDesignUuid, byte[] parentSurveyUuid){
		//init design
		this.newMission = new Mission();
		
		session = HibernateManager.openSession();
		
		if (parentDesignUuid != null){
			parentDesign = (SurveyDesign) session.load(SurveyDesign.class, parentDesignUuid);
		}
		if (parentSurveyUuid != null){
			parentSurvey = (Survey) session.load(Survey.class, parentSurveyUuid);
			parentDesign = parentSurvey.getSurveyDesign();
			newMission.setSurvey(parentSurvey);
			newMission.setStartDate(parentSurvey.getStartDate());
			newMission.setEndDate(parentSurvey.getEndDate());
		}
	}
	
	public Mission getNewMission(){
		return this.newMission;
	}
	
	@Override
	public void dispose(){
		super.dispose();
		session.close();
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
		
		if (getStartingPage() instanceof MissionCompositeWizardPage){
			((MissionCompositeWizardPage)getStartingPage()).initPage(newMission, parentDesign, session);
		}
		
		//configure listeners
    	ISurveyListener updateButtons = new ISurveyListener() {
			@Override
			public void compositeModified() {
				getContainer().updateButtons();
			}
		};
		
		for (MissionComposite c : localPages){
    		c.addChangeListener(updateButtons);
    	}
	}
	
	
	@Override
	public boolean performFinish() {
		//update last page
		((MissionCompositeWizardPage)getPages()[getPageCount() - 1]).updateModel(newMission);
		
		session.beginTransaction();
		try{
			session.save(newMission);
			session.getTransaction().commit();
			
			SurveyEventHandler.getInstance().fireEvent(EventType.MISSION_ADDED, newMission);
			return true;
		}catch (Exception ex){
			try{
				session.getTransaction().rollback();
			}catch (Exception ex2){
				EcologicalRecordsPlugIn.log(ex.getMessage(), ex2);
			}
			EcologicalRecordsPlugIn.displayLog(Messages.NewMissionWizard_SaveError + "\n\n" + ex.getMessage(), ex); //$NON-NLS-1$
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
    	
    	setWindowTitle(Messages.NewMissionWizard_WizardTitle);

    	localPages = new ArrayList<MissionComposite>();
    	if (parentDesign == null){
    		
        	List<SurveyDesign> others = session.createCriteria(SurveyDesign.class)
        			.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())) //$NON-NLS-1$
        			.add(Restrictions.eq("state", SurveyDesign.State.ACTIVE)).list(); //$NON-NLS-1$

    		
    		localPages.add(new SurveyDesignComposite(others));
    	}
    	
    	if (parentSurvey != null){
    		newMission.setSurvey(parentSurvey);
    	}
    	
    	localPages.add(new SurveyComposite());
    	localPages.add(new IdComposite());
    	localPages.add(new DateComposite());
    	localPages.add(new MissionEmployeeComposite());
    	localPages.add(new CommentComposite());
    	localPages.add(new MissionPropertyValuesComposite());
    	
		for (MissionComposite m : localPages){
    		super.addPage(new MissionCompositeWizardPage(m));
    	}

    	((WizardDialog) getContainer()).addPageChangingListener(this);
    }


	@Override
	public void handlePageChanging(PageChangingEvent event) {
		//update design with page values
		if (event.getCurrentPage() instanceof MissionCompositeWizardPage){
			((MissionCompositeWizardPage)event.getCurrentPage()).updateModel(newMission);
			
			MissionCompositeWizardPage p = (MissionCompositeWizardPage) event.getCurrentPage();
			if (p.getComposite() instanceof SurveyDesignComposite){
				this.parentDesign = ((SurveyDesignComposite)p.getComposite()).getSurveyDesign();
			}
		}
		
		//init target page
		if (event.getTargetPage() instanceof MissionCompositeWizardPage){
			((MissionCompositeWizardPage)event.getTargetPage()).initPage(newMission, parentDesign, session);
			
			canFinish = ((IWizardPage)event.getTargetPage()).getNextPage() == null;
		}
	}
}
