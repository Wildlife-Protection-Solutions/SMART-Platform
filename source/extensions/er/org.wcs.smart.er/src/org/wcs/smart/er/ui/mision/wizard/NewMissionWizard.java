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

import java.sql.Time;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.eclipse.jface.dialogs.IPageChangingListener;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Composite;
import org.hibernate.Session;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.SurveyEventHandler;
import org.wcs.smart.er.SurveyEventHandler.EventType;
import org.wcs.smart.er.hibernate.SurveyDesignFilter;
import org.wcs.smart.er.hibernate.SurveyHibernateManager;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.MissionDay;
import org.wcs.smart.er.model.MissionTrack;
import org.wcs.smart.er.model.Survey;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.model.SurveyDesign.State;
import org.wcs.smart.er.model.SurveyWaypoint;
import org.wcs.smart.er.ui.ISurveyListener;
import org.wcs.smart.er.ui.mision.CommentComposite;
import org.wcs.smart.er.ui.mision.DateComposite;
import org.wcs.smart.er.ui.mision.IdComposite;
import org.wcs.smart.er.ui.mision.MissionComposite;
import org.wcs.smart.er.ui.mision.MissionEmployeeComposite;
import org.wcs.smart.er.ui.mision.MissionPropertyValuesComposite;
import org.wcs.smart.er.ui.mision.SurveyComposite;
import org.wcs.smart.er.ui.mision.SurveyDesignComposite;
import org.wcs.smart.er.ui.surveydesign.editor.SurveyDesignEditorInput;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.util.SmartUtils;

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
	
	private Object lastPage;
	
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
		
		IWizardPage start = getStartingPage();
		
		if (getStartingPage() instanceof MissionCompositeWizardPage){
			((MissionCompositeWizardPage)getStartingPage()).initPage(newMission, parentDesign, session);
		}
		if (getPages()[0] != start){
			if (getPages()[0] instanceof MissionCompositeWizardPage){
				((MissionCompositeWizardPage)getPages()[0]).initPage(newMission, parentDesign, session);
			}
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
	
	
	private Time createTime(int hours, int minute, int second){
		Calendar cForProcessing = Calendar.getInstance();
		cForProcessing.setTimeInMillis(0);
		cForProcessing.set(Calendar.HOUR_OF_DAY, hours);
		cForProcessing.set(Calendar.MINUTE, minute);
		cForProcessing.set(Calendar.SECOND, second);
		cForProcessing.set(Calendar.MILLISECOND, 0);
		return new Time(cForProcessing.getTime().getTime());
	}
	
	@Override
	public boolean performFinish() {
		//update last page
        if (lastPage instanceof MissionCompositeWizardPage) {
            ((MissionCompositeWizardPage) lastPage).updateModel(newMission);
        }
		
		session.beginTransaction();
		try{
			
			//create days
			Calendar calStart = SmartUtils.convertDate(newMission.getStartDate());
			calStart.set(Calendar.HOUR, 0);
			calStart.set(Calendar.MINUTE, 0);
			calStart.set(Calendar.SECOND, 0);
			calStart.set(Calendar.MILLISECOND, 0);
			
			Calendar calEnd = SmartUtils.convertDate(newMission.getEndDate());
			newMission.setMissionDays(new ArrayList<MissionDay>());
			while (calStart.before(calEnd) || calStart.equals(calEnd)) {
				MissionDay md = new MissionDay();
				md.setDate(SmartUtils.getDatePart(calStart.getTime(), false));
				md.setStartTime(createTime(0, 0, 0));
				md.setEndTime(createTime(23, 59, 59));
				md.setRestMinutes(0);
				md.setTracks(new ArrayList<MissionTrack>());
				md.setWaypoints(new ArrayList<SurveyWaypoint>());
				md.setMission(newMission);
				newMission.getMissionDays().add(md);
				
				calStart.add(Calendar.DAY_OF_MONTH, 1);
			}
			
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
	public void addPages() {
    	
    	setWindowTitle(Messages.NewMissionWizard_WizardTitle);

    	localPages = new ArrayList<MissionComposite>();
    	if (parentDesign == null){
    		//get active surveys
    		SurveyDesignFilter f = new SurveyDesignFilter();
    		f.setSurveyStates(new State[]{State.ACTIVE});
    		List<SurveyDesignEditorInput> others = SurveyHibernateManager.getInstance().getSurveyDesignEditorInputs(session, f);
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
	public IWizardPage getStartingPage() {
		if (parentDesign == null){
			return super.getStartingPage();
		}
		if (parentSurvey == null){
			return super.getStartingPage();
		}
		if (getPageCount() == 0) {
			return null;
		}
		return getPages()[1];
	}

	@Override
	public void handlePageChanging(PageChangingEvent event) {
		//session may be closed by new survey design wizard
		if (!session.isOpen()){
			session = HibernateManager.openSession();
		}
		
		//update design with page values
		if (event.getCurrentPage() instanceof MissionCompositeWizardPage){
			((MissionCompositeWizardPage)event.getCurrentPage()).updateModel(newMission);
			MissionCompositeWizardPage p = (MissionCompositeWizardPage) event.getCurrentPage();
			if (p.getComposite() instanceof SurveyDesignComposite){
				this.parentDesign = ((SurveyDesignComposite)p.getComposite()).getSurveyDesign(session);
			}
		}
		
		//init target page
		if (event.getTargetPage() instanceof MissionCompositeWizardPage){
			((MissionCompositeWizardPage)event.getTargetPage()).initPage(newMission, parentDesign, session);
			
			canFinish = ((IWizardPage)event.getTargetPage()).getNextPage() == null;
		}
		
        if (event.doit) {
            lastPage = event.getTargetPage();
        }
		
	}
}
