package org.wcs.smart.plan.internal.ui.patrol;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.hibernate.Session;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.ui.NewPatrolWizardPage;

public class NewPatrolPlanWizardPage extends NewPatrolWizardPage {

	public NewPatrolPlanWizardPage() {
		super("PlanPage");
	}

	@Override
	public void createControl(Composite parent) {
		Label lbl = new Label(parent, SWT.NONE);
		lbl.setText("Select Plan Here");
		super.setControl(lbl);
	}

	@Override
	public boolean updateModel(Patrol p) {
		return true;
	}

	@Override
	public void initModel(Patrol p, Session session) {
	}

}
