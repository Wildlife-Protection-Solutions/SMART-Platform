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
package org.wcs.smart.plan.ui.newPlanWizard;

import java.util.List;

import org.eclipse.jface.dialogs.IPageChangingListener;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Composite;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.PatrolHibernateManager;
import org.wcs.smart.plan.PlanEventManager;
import org.wcs.smart.plan.PlanHibernateManager;
import org.wcs.smart.plan.internal.Messages;
import org.wcs.smart.plan.model.Plan;
import org.wcs.smart.plan.model.PlanTarget;
import org.wcs.smart.plan.ui.handlers.OpenPlanHandler;
import org.wcs.smart.plan.ui.panel.PlanUtil;
import org.wcs.smart.util.SmartUtils;

/**
 * Wizard to create a new plan.
 * 
 * @author Jeff
 * @author Emily
 * @since 1.0.0
 */
public class CreatePlanWizard extends Wizard implements IPageChangingListener {

	private boolean completedOK = false;

	private Plan plan= null;
	private Session session = null;

	private boolean canFinish = false;
	private IWizardPage lastPage = null;

	private UseTemplatePlanWizardPage page1;

	//set once all pages are seen, I don't want to allow the user to "finish" without actually seeing the options to set a parent plan etc.
	//overridden if the user selects a template however.
	private boolean seenAll;

	/**
	 * Creates a new wizard.
	 */
	public CreatePlanWizard() {
		setWindowTitle(Messages.CreatePlanWizard_Title);

		plan = new Plan();		
		plan.setCreator(SmartDB.getCurrentEmployee());
		plan.setConservationArea(SmartDB.getCurrentConservationArea());
		plan.setId(PlanHibernateManager.generatePlanId(plan, getSession()));
		plan.getName();
		plan.getDescription();
		plan.getId();
}

	/*
	 * Check for valid data throughout the whole wizard and set the finish button 
	 */
	public void validate(){
		if(plan.getTemplatePlan() != null && validData()){
			setCanFinish(true);
		}else if (seenAll && validData()){
			setCanFinish(true);
		}else{
			setCanFinish(false);
		}
	}
	
	/* 
	 * just checks for valid data that won't crash the insert
	 */
	public boolean validData(){
		boolean idIsSimple = SmartUtils.isSimpleString(plan.getId(),
				SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX, 32, 2);

		if(plan.getId() == null || !idIsSimple || plan.getType() == null || plan.getStartDate() == null){
			return false;
		}
		
		return PlanUtil.isDatesInParentRange(plan, plan.getParent());
	}

	/**
	 * Sets if the wizard can finish
	 * 
	 * @param canFinish
	 *            if the wizard can finish
	 */
	public void setCanFinish(boolean canFinish) {
		this.canFinish = canFinish;
		getContainer().updateButtons();
	}

	/**
	 * Closes the active session
	 */
	@Override
	public void dispose() {
		
		Session tmp = getSession();
		if (tmp != null && tmp.isOpen()) {
			if(tmp.getTransaction().isActive()){
				tmp.getTransaction().commit();
			}
			tmp.close();
		}
		
		super.dispose();
	}

	@Override
	public boolean canFinish() {
		return super.canFinish() && this.canFinish;
	}

	/**
	 * 
	 * @return the current patrol being created
	 */
	public Plan getPlan() {
		return this.plan;
	}

	/**
	 * Creates a new session and attaches the current conservation area.
	 * 
	 * @return
	 */
	public Session getSession() {
		if (session == null || !session.isOpen()) {
			session = PatrolHibernateManager.openSession();
		}
		return session;
	}

	@Override
	public void addPages() {
		((WizardDialog) getContainer()).addPageChangingListener(this);
		page1 = new UseTemplatePlanWizardPage();
		
		super.addPage(page1); //choose a template or not
		super.addPage(new TemplateSelectPlanWizardPage()); //template selector
		super.addPage(new TypeEmployeePlanWizardPage()); //choose type
		super.addPage(new ParentIdPlanWizardPage()); //parent
		super.addPage(new IdNameDescPlanWizardPage()); //id/name/desc
		super.addPage(new StationTeamPlanWizardPage()); //team/station
		super.addPage(new DatesPlanWizardPage()); //dates
		super.addPage(new TargetsPlanWizardPage()); //targets

		
	}
	
	@Override
	 public void createPageControls(Composite pageContainer) {
		 super.createPageControls(pageContainer);
		 page1.initModel(plan);
	 }

	/**
	 * 
	 * @return true if the wizard completed okay with no errors; false if error
	 *         occured while finishing wizard
	 */
	public boolean isCompletedOk() {
		return completedOK;
	}

	/**
	 * Saves the Plan, then loads it in the view plan perspective.
	 */
	@Override
	public boolean performFinish() {
		if (lastPage instanceof PlanWizardPage) {
			((PlanWizardPage) lastPage).updateModel(this.plan);
		}

		
		if (!PlanUtil.isDatesInParentRange(plan, plan.getParent())) {
			//this validation might fail at this point as we allow to complete parent page
			//having invalid date range
			MessageDialog.openInformation(getShell(),  Messages.PlanParentIdComposite_InfoDialog_Title, Messages.CreatePlanWizard_InvalidDateRange_Message);
			return false;
		}
		
		Plan p = getPlan();
		if(p.getTargets() != null){
			List<PlanTarget> tars = p.getTargets();
			for(PlanTarget x : tars){
				x.setPlan(p);
			}
		}

		boolean saved = false;
		Session s = HibernateManager.openSession();
		try{
			saved = PlanHibernateManager.savePlan(p,s);
		}finally{
			s.close();
		}
		
		 // fire events
		PlanEventManager.getInstance().planAdded(getPlan());
		
		//Open Plan Perspective and the plan you just created.
		(new OpenPlanHandler()).openPlan(p.getUuid());
		return saved;
	}

	/**
	 * @see org.eclipse.jface.dialogs.IPageChangingListener#handlePageChanging(org.eclipse.jface.dialogs.PageChangingEvent)
	 */
	@Override
	public void handlePageChanging(PageChangingEvent event) {
		
		if (event.getCurrentPage() instanceof PlanWizardPage) {
			boolean result = ((PlanWizardPage) event.getCurrentPage()).updateModel(plan);
			if (!result){
				event.doit = false;
				return;
			}
		}
		if (event.getTargetPage() instanceof PlanWizardPage) {
			((PlanWizardPage) event.getTargetPage()).initModel(plan);
		}

		if (event.doit) {
			lastPage = (IWizardPage) event.getTargetPage();
		}
		validate();
	}

	public void setSeenAll(boolean b) {
		this.seenAll = b;
	}

}
