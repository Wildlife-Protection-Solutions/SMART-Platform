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
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.hibernate.Session;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.ui.NewPatrolWizardPage;
import org.wcs.smart.plan.PlanHibernateManager;
import org.wcs.smart.plan.model.Plan;
import org.wcs.smart.plan.ui.tree.PlanViewer;

/**
 * Wizard page for the new patrol wizard that collects
 * the plan information associated with the patrol.
 * 
 * @author Emily
 *
 */
public class NewPatrolPlanWizardPage extends NewPatrolWizardPage {

	private static final String NONE_LABEL = "(None)";
	private PlanViewer pv;
	private Plan lastSelection = null;
	
	public NewPatrolPlanWizardPage() {
		super("PlanPage"); //$NON-NLS-1$
	}

	@Override
	public void createControl(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(1, false));

		pv = new PlanViewer(main);
		pv.getViewer().getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		super.setTitle("Patrol Plan");
		super.setMessage(MessageFormat.format(
				"Select the plan associated with this patrol or select {0} if no plan associated with this patrol.", new String[]{NONE_LABEL}));
		super.setControl(main);
	}

	private Plan getSelectedPlan(){
		return (Plan) pv.getSelectedPlan();
	}
	
	@Override
	public boolean updateModel(Patrol p) {
		lastSelection = getSelectedPlan();
		if (lastSelection != null){
			if (lastSelection.getStation() != null){
				p.setStation(lastSelection.getStation());
			}
			if (lastSelection.getTeam() != null){
				p.setTeam(lastSelection.getTeam());
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
		//TODO save link to plan somewhere in the database
		//Patrol = p
		//Plan = lastSelection
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void initModel(Patrol p, Session session) {
		List roots = PlanHibernateManager.getAllRootPlans(session);
		roots.add(0, NONE_LABEL);
		pv.setRootPlans(roots.toArray(new Object[roots.size()]));
		if (lastSelection != null){
			pv.setSelection(lastSelection);
		}else{
			pv.setSelection(NONE_LABEL);
		}
		
	}

}
