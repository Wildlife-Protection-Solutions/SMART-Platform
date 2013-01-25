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

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.hibernate.Session;
import org.wcs.smart.plan.PlanHibernateManager;
import org.wcs.smart.plan.model.Plan;
import org.wcs.smart.plan.ui.tree.PlanViewer;
import org.wcs.smart.util.SmartUtils;

/**
 * Wizard page for selecting a parent plan
 * 
 * @author jeff
 * @author egouge
 * @since 1.0.0
 */
public class NewPlanWizardPage7 extends NewPlanWizardPage implements SelectionListener {

	private DateTime dtStartDate;
	private DateTime dtEndDate;
	private Button btnNoParent;
	private Button btnUseSelected;
	private ControlDecoration cdEndDate;
	
	
	private PlanViewer planTreeViewer;
	private Plan lastSelection = null;
	
	/**
	 * 
	 */
	protected NewPlanWizardPage7() {
		super("Plan Parent");
	}

	
	/**
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(Composite parent) {
		
		Composite center = new Composite(parent, SWT.NONE);
		center.setLayout(new GridLayout(4, false));
		center.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));


		Composite buttonPanel = new Composite(center, SWT.NONE);
		buttonPanel.setLayout(new GridLayout(1, false));
		buttonPanel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 4, 1));
		
		btnNoParent = new Button(buttonPanel, SWT.RADIO);
		btnNoParent.setText("No Parent Plan");
		btnNoParent.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		btnNoParent.setSelection(true);
		
		btnUseSelected = new Button(buttonPanel, SWT.RADIO);
		btnUseSelected.setText("Set the following to be the plan Parent:");
		btnUseSelected.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
				
		Label lbl = new Label(center, SWT.NONE);
		lbl.setText("Only show plans from:");
		lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		((GridData)lbl.getLayoutData()).horizontalIndent = 10;
		
		dtStartDate = new DateTime(center, SWT.BORDER | SWT.DROP_DOWN | SWT.LONG);
		dtStartDate.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		
		lbl = new Label(center, SWT.NONE);
		lbl.setText(" to ");
		lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		dtEndDate = new DateTime(center, SWT.BORDER | SWT.DROP_DOWN | SWT.LONG);
		dtEndDate.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		cdEndDate = new ControlDecoration(lbl, SWT.RIGHT);
		cdEndDate.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_DEC_FIELD_WARNING));
		cdEndDate.hide();
		
		dtEndDate.addSelectionListener(this);
		dtStartDate.addSelectionListener(this);
		
		planTreeViewer = new PlanViewer(center);
		planTreeViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 4 , 1));
		
		
		btnNoParent.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateVisibility();
			}
			
		});
		
		btnUseSelected.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateVisibility();
			}
			
		});
		updateVisibility();
		setControl(center);
		setMessage("A parent plan allows you to group patrol plans into team, station and conservation area plans." +
				"Create the conservation area plan first, then select it in this window when creating each each patrol, " +
				"station or team plan you want to include in the Conservation area plan. Use the same method to create station or team plans.");
	
	}

	private void updateVisibility(){
		boolean areVisible = !btnNoParent.getSelection();
		dtEndDate.setEnabled(areVisible);
		dtStartDate.setEnabled(areVisible);
		planTreeViewer.getControl().setEnabled(areVisible);
	}

	@Override
	public boolean updateModel(Plan p) {
		if(!btnNoParent.getSelection()){
			try{
				Plan tmp = planTreeViewer.getSelectedPlan();
				p.setParent(tmp);
			}catch (Exception e) {
			// nothing to update if those are null
			}
		}else{
			p.setParent(null);
		}
		return true;
	}
	
	@Override
	void initModel(Plan p, Session session) {
		((CreatePlanWizard)getWizard()).setSeenAll(true);
		
		List<Plan> roots = PlanHibernateManager.getAllRootPlans(session);
		planTreeViewer.setRootPlans(roots.toArray(new Object[roots.size()]));
		lastSelection = p.getParent();
		if (lastSelection != null){
			planTreeViewer.setSelection(lastSelection);
			btnNoParent.setSelection(false);
			btnUseSelected.setSelection(true);
		}else{
			btnNoParent.setSelection(true);
			btnUseSelected.setSelection(false);
		}
		updateVisibility();
		
		try{
			planTreeViewer.setSelection(p.getTemplatePlan().getParent());
		}catch (Exception e) {
			// means there was no default, OK
		}
	}
	
	/**
	 * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
	 */
	@Override
	public void widgetSelected(SelectionEvent e) {
		if (SmartUtils.getDate(dtStartDate).after(SmartUtils.getDate(dtEndDate))){
			cdEndDate.setDescriptionText("End date must be after the start date.");
			cdEndDate.show();
		}else{
			cdEndDate.hide();
		}
		fireChangeListeners();
	}
	
	/**
	 * @see org.eclipse.swt.events.SelectionListener#widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent)
	 */
	@Override
	public void widgetDefaultSelected(SelectionEvent e) {
	}
	
	
}