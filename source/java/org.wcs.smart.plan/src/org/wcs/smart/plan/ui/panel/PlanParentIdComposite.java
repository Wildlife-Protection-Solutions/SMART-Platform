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

import java.util.Arrays;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Link;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.plan.filter.PlanFilter;
import org.wcs.smart.plan.internal.Messages;
import org.wcs.smart.plan.model.Plan;
import org.wcs.smart.plan.ui.IPlanFilterItem;
import org.wcs.smart.plan.ui.LoadPlanJob;
import org.wcs.smart.plan.ui.PlanFilterDialog;
import org.wcs.smart.plan.ui.editor.PlanEditorInput;
import org.wcs.smart.plan.ui.tree.PlanViewer;

/**
 * Composite for editing plan targets
 * 
 * @author jeffloun
 * @since 1.0.0
 */
public class PlanParentIdComposite extends PlanComposite implements IPlanFilterItem {


	private Button btnNoParent;
	private Button btnUseSelected;

	private PlanFilter currentFilter = new PlanFilter();
	private PlanViewer planTreeViewer;
	private LoadPlanJob updateJob;
    private Link filterLink;
    
	/**
	 * @param parent
	 * @param style
	 */
	public PlanParentIdComposite(Composite parent, int style) {
		super(parent, style);
		setMessage(Messages.PlanParentIdComposite_Message);

		createControls();
	}

	private void createControls() {
        this.setLayout(new GridLayout(1, false));
        this.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, true));
        
        Composite buttonPanel = new Composite(this, SWT.NONE);
		buttonPanel.setLayout(new GridLayout(1, false));
		buttonPanel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		
		btnNoParent = new Button(buttonPanel, SWT.RADIO);
		btnNoParent.setText(Messages.PlanParentIdComposite_NoParentPlan_Label);
		btnNoParent.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		btnNoParent.setSelection(true);
		
		btnUseSelected = new Button(buttonPanel, SWT.RADIO);
		btnUseSelected.setText(Messages.PlanParentIdComposite_UseParent_Label);
		btnUseSelected.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
				
		filterLink = new Link(this, SWT.NONE);
		filterLink.setText(Messages.PlanParentIdComposite_Filter_Link_Text);
		filterLink.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		((GridData)filterLink.getLayoutData()).horizontalIndent = 10;
		filterLink.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				PlanFilterDialog d = new PlanFilterDialog(getShell(), PlanParentIdComposite.this);
				d.open();
			}
		});
		
		
		planTreeViewer = new PlanViewer(this);
		planTreeViewer.getViewer().getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 4 , 1));
		planTreeViewer.getViewer().addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				fireChangeListeners();
			}
		});
		
		btnNoParent.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateVisibility();
				fireChangeListeners();
			}
			
		});
		
		btnUseSelected.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateVisibility();
				fireChangeListeners();
			}
			
		});
		updateJob = new LoadPlanJob(planTreeViewer, currentFilter);
		updateJob.schedule();
		updateVisibility();
	}
	
	private void fireChangeListeners(){
		fireDataValidStateListeners();
		fireInputChangeListeners();
	}
	
	private void updateVisibility(){
		boolean areVisible = !btnNoParent.getSelection();
		planTreeViewer.getViewer().getControl().setEnabled(areVisible);
		filterLink.setEnabled(areVisible);
	}

	
	@Override
	public boolean updateModel(Plan plan) {
		if(!btnNoParent.getSelection()){
			if (planTreeViewer.getSelectedPlan() == null){
				MessageDialog.openInformation(getShell(), Messages.PlanParentIdComposite_InfoDialog_Title, Messages.PlanParentIdComposite_InfoDialog_PlanRequired_Message);
				return false;
			}
			
			PlanEditorInput tmp = (PlanEditorInput) planTreeViewer.getSelectedPlan();
			if (Arrays.equals(tmp.getUuid(), plan.getUuid())){
				MessageDialog.openError(getShell(),  Messages.PlanParentIdComposite_InfoDialog_Title, Messages.PlanParentIdComposite_InfoDialog_ReferItself_Message);
				return false;
			}
			Plan parent = null;
			Session session = HibernateManager.openSession();
			try{
				session.beginTransaction();
				parent = (Plan) session.load(Plan.class, tmp.getUuid());
				session.getTransaction().rollback();
			}finally{
				session.close();
			}
			if (parent == null){
				MessageDialog.openInformation(getShell(),  Messages.PlanParentIdComposite_InfoDialog_Title, Messages.PlanParentIdComposite_InfoDialog_PlanNotFound_Message);
				return false;
			}
			plan.setParent(parent);
		}else{
			plan.setParent(null);
		}
		return true;
	}

	@Override
	public void initFromModel(Plan plan) {
		
		Plan parent = plan.getParent();
		if (parent != null){
			updateJob.setDefaultSelection(new PlanEditorInput(parent.getUuid(), null, null));
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

	@Override
	public void updateContent() {
		updateJob.schedule();
	}

	@Override
	public PlanFilter getPlanFilter() {
		return currentFilter;
	}
	
	@Override
	public String getTitle() {
		return Messages.PlanParentIdComposite_Title;
	}
	
}
