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
package org.wcs.smart.plan.internal.ui.patrol;


import java.text.MessageFormat;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.hibernate.Session;
import org.wcs.smart.ca.Station;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.Team;
import org.wcs.smart.patrol.ui.NewPatrolWizardPage;
import org.wcs.smart.plan.internal.Messages;
import org.wcs.smart.plan.model.PatrolPlan;
import org.wcs.smart.plan.model.Plan;
import org.wcs.smart.plan.ui.LoadPlanJob;
import org.wcs.smart.plan.ui.editor.PlanEditorInput;

/**
 * Wizard page for the new patrol wizard that collects
 * the plan information associated with the patrol.
 * 
 * @author Emily
 *
 */
public class NewPatrolPlanWizardPage extends NewPatrolWizardPage {

	private PatrolPlanComposite ppComp;
	private PlanEditorInput lastSelection = null;
	
	public NewPatrolPlanWizardPage() {
		super("PlanPage"); //$NON-NLS-1$
		
	}

	@Override
	public void createControl(Composite parent) {
		
		ppComp = new PatrolPlanComposite(parent, SWT.NONE);
		ppComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		super.setTitle(Messages.NewPatrolPlanWizardPage_Title);
		super.setMessage(MessageFormat.format(
				Messages.NewPatrolPlanWizardPage_Message, LoadPlanJob.NONE_LABEL));
		super.setControl(ppComp);
	}

	private PlanEditorInput getSelectedPlan(){
		if (ppComp.getViewer().getSelectedPlan() instanceof PlanEditorInput){
			return (PlanEditorInput)ppComp.getViewer().getSelectedPlan();
		}else{
			return null;
		}
	}
	
	@Override
	public boolean updateModel(Patrol p, Session session) {
		lastSelection = getSelectedPlan();
		if (lastSelection != null) {
			session.beginTransaction();
			try {
				Plan plan = (Plan) session.get(Plan.class, lastSelection.getUuid());
				if (plan != null) {
					if (plan.getStation() != null) {
						Station x = plan.getStation();
						x.getName();	//ensure item loaded
						p.setStation(x);
					}
					if (plan.getTeam() != null) {
						Team x = plan.getTeam();
						x.getName();
						p.setTeam(x);
					}
				}
			} finally {
				session.getTransaction().rollback();
			}

		}
		return true;
	}

	/**
	 * Called when the patrol is saved to the database.
	 * 
	 * @param p the patrol saved
	 * @param session current session in open transaction
	 * 
	 * @throws Exception if error occurs while saving 
	 */
	public void save(Patrol p, Session session) throws Exception{
		if (lastSelection != null){
			Plan plan = (Plan)session.getReference(Plan.class, lastSelection.getUuid());
			if (plan == null){
				MessageDialog.openError(getShell(), Messages.NewPatrolPlanWizardPage_ErrorDialog_Title, 
						MessageFormat.format(Messages.NewPatrolPlanWizardPage_ErrorDialog_Message, lastSelection.getName()));				
			}
			
			PatrolPlan pp = new PatrolPlan();
			pp.setPatrol(p);
			pp.setPlan(plan);
			session.persist(pp);
		}
	}
	
	@Override
	public void initModel(Patrol p, Session session) {
		if (lastSelection != null){
			ppComp.setDefaultSelection(lastSelection);
		}else{
			ppComp.setDefaultSelection(LoadPlanJob.NONE_LABEL);
		}
		
	}

	

}
