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
package org.wcs.smart.plan.ui.panel;

import java.util.List;

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.plan.PlanHibernateManager;
import org.wcs.smart.plan.model.Plan;
import org.wcs.smart.plan.ui.tree.PlanViewer;

/**
 * Composite for editing plan targets
 * 
 * @author jeffloun
 * @since 1.0.0
 */
public class PlanParentIdComposite extends PlanComposite {

	private DateTime dtStartDate;
	private DateTime dtEndDate;
	private Button btnNoParent;
	private Button btnUseSelected;
	private ControlDecoration cdEndDate;
	
	private PlanViewer planTreeViewer;

    
	/**
	 * @param parent
	 * @param style
	 */
	public PlanParentIdComposite(Composite parent, int style) {
		super(parent, style);
		setMessage("Select the plan's parent:");

		createControls();
	}

	private void createControls() {
        this.setLayout(new GridLayout(4, false));
        this.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, true));
        
        Composite buttonPanel = new Composite(this, SWT.NONE);
		buttonPanel.setLayout(new GridLayout(1, false));
		buttonPanel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 4, 1));
		
		btnNoParent = new Button(buttonPanel, SWT.RADIO);
		btnNoParent.setText("No Parent Plan");
		btnNoParent.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		btnNoParent.setSelection(true);
		
		btnUseSelected = new Button(buttonPanel, SWT.RADIO);
		btnUseSelected.setText("Set the following to be the plan Parent:");
		btnUseSelected.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
				
		Label lbl = new Label(this, SWT.NONE);
		lbl.setText("Only show plans from:");
		lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		((GridData)lbl.getLayoutData()).horizontalIndent = 10;
		
		dtStartDate = new DateTime(this, SWT.BORDER | SWT.DROP_DOWN | SWT.LONG);
		dtStartDate.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));
		
		
		
		lbl = new Label(this, SWT.NONE);
		lbl.setText(" to ");
		lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		dtEndDate = new DateTime(this, SWT.BORDER | SWT.DROP_DOWN | SWT.LONG);
		dtEndDate.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));
		
		cdEndDate = new ControlDecoration(lbl, SWT.RIGHT);
		cdEndDate.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_DEC_FIELD_WARNING));
		cdEndDate.hide();
		
		planTreeViewer = new PlanViewer(this);
		planTreeViewer.getViewer().getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 4 , 1));
		
		
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
	}
	
	private void updateVisibility(){
		boolean areVisible = !btnNoParent.getSelection();
		dtEndDate.setEnabled(areVisible);
		dtStartDate.setEnabled(areVisible);
		planTreeViewer.getViewer().getControl().setEnabled(areVisible);
	}

	
	@Override
	public boolean updateModel(Plan plan) {
		if(!btnNoParent.getSelection()){
			try{
				Plan tmp = (Plan) planTreeViewer.getSelectedPlan();
				plan.setParent(tmp);
			}catch (Exception e) {
			// nothing to update if those are null
			}
		}else{
			plan.setParent(null);
		}
		return true;
	}

	@Override
	public void initFromModel(Plan plan) {
		List<Plan> roots = PlanHibernateManager.getAllRootPlans(HibernateManager.openSession());
		planTreeViewer.setRootPlans(roots.toArray(new Object[roots.size()]));
		Plan parent = plan.getParent();
		if (parent != null){
			planTreeViewer.setSelection(parent);
			btnNoParent.setSelection(false);
			btnUseSelected.setSelection(true);
		}else{
			btnNoParent.setSelection(true);
			btnUseSelected.setSelection(false);
		}
		updateVisibility();
	}

	@Override
	public boolean isDataValid() {
		return true;
	}
	
}
