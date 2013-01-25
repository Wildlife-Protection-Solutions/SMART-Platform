package org.wcs.smart.plan.ui.newPlanWizard;

import org.eclipse.swt.widgets.Composite;
import org.wcs.smart.plan.model.PlanTarget;

public interface ITargetPage {

	public PlanTarget createTarget();
	
	public void initPage(PlanTarget pt);
	
	public void updateTarget(PlanTarget pt);
	
	public String getPageName();
	
	public boolean validate();
	
	public Composite createComponent(Composite parent, int style);
}
