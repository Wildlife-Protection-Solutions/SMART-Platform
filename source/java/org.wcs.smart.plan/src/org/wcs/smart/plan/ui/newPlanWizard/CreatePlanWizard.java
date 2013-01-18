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

import org.eclipse.jface.dialogs.IPageChangingListener;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Composite;
import org.hibernate.Session;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.PatrolEventManager;
import org.wcs.smart.patrol.PatrolHibernateManager;
import org.wcs.smart.plan.PlanHibernateManager;
import org.wcs.smart.plan.model.Plan;

/**
 * Wizard to create a new patrol.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class CreatePlanWizard extends Wizard implements IPageChangingListener {

	private boolean completedOK = false;

	private Plan plan= null;
	private Session session = null;

	private boolean canFinish = false;
	private IWizardPage lastPage = null;

	private NewPlanWizardPage1 page1;

	/**
	 * Creates a new wizard.
	 */
	public CreatePlanWizard() {
		setWindowTitle("Create New Plan");

		plan = new Plan();
		
		plan.setConservationArea(SmartDB.getCurrentConservationArea());

		Session mysession = getSession();
		mysession.beginTransaction();
		plan.setId(PlanHibernateManager.generatePlanId(plan, session));
		mysession.getTransaction().rollback();
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
		super.dispose();
		if (session != null && session.isOpen()) {
			session.close();
		}
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
			
			session.update(plan.getConservationArea());
		}
		return session;
	}

	@Override
	public void addPages() {
		((WizardDialog) getContainer()).addPageChangingListener(this);
		page1 = new NewPlanWizardPage1();
		
		super.addPage(page1); //choose a template or not
		super.addPage(new NewPlanWizardPage2b()); //template selector
		super.addPage(new NewPlanWizardPage2()); //choose type
		super.addPage(new NewPlanWizardPage3()); //team/station
		super.addPage(new NewPlanWizardPage4()); // id/name/desc
		super.addPage(new NewPlanWizardPage5()); // dates
		super.addPage(new NewPlanWizardPage6(plan)); //targets
		super.addPage(new NewPlanWizardPage7()); //parent
		
	}
	
	@Override
	 public void createPageControls(Composite pageContainer) {
		 super.createPageControls(pageContainer);
		 Session mysession = getSession();
		 mysession.beginTransaction();
		 page1.initModel(plan, mysession);
		 mysession.getTransaction().rollback();
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
	 * Creates the patrol leg days then saved the patrol to the database.
	 */
	@Override
	public boolean performFinish() {
		if (lastPage instanceof NewPlanWizardPage) {
			((NewPlanWizardPage) lastPage).updateModel(this.plan);
		}


		//TODO: make the following 8 lines work:
		boolean ret = PlanHibernateManager.savePlan(getPlan(),PlanHibernateManager.openSession());
		
		 if (!ret)
		 return false;
		
		 // fire events
		 //PlanEventManager.getInstance().planAdded(getPlan());
		
		// open in editor
		//TODO:open in an editor once we have one. 
/*		PatrolEditorInput input = new PatrolEditorInput(this.patrol.getUuid(),
				this.patrol.getId(), this.patrol.getPatrolType(),
				this.patrol.getStartDate(), this.patrol.getEndDate());

		try {
			PlatformUI.getWorkbench().getActiveWorkbenchWindow()
					.getActivePage().openEditor(input, PatrolEditor.ID);
		} catch (PartInitException e) {
			throw new RuntimeException(e);
		}
*/
		return ret;
	}

	/**
	 * @see org.eclipse.jface.dialogs.IPageChangingListener#handlePageChanging(org.eclipse.jface.dialogs.PageChangingEvent)
	 */
	@Override
	public void handlePageChanging(PageChangingEvent event) {
		if (event.getCurrentPage() instanceof NewPlanWizardPage) {
			if (!((NewPlanWizardPage) event.getCurrentPage())
					.updateModel(plan)) {
				event.doit = false;
				return;
			}
		}
		if (event.getTargetPage() instanceof NewPlanWizardPage) {
			session.beginTransaction();
			((NewPlanWizardPage) event.getTargetPage()).initModel(plan,
					session);
			session.getTransaction().rollback();
		}

		if (event.doit) {
			lastPage = (IWizardPage) event.getTargetPage();
		}
	}

}
