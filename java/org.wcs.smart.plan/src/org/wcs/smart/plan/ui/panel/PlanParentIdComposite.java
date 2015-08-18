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
import java.util.UUID;

import org.eclipse.jface.dialogs.IDialogConstants;
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
import org.eclipse.swt.widgets.Display;
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
				//no need to fire events when selection is changed by updateJob (e.g. loading data)
				if (updateJob != null && !updateJob.isUpdatingViewerSelection()) {
					fireInputChangeListeners();
				}
			}
		});
		
		btnNoParent.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateVisibility();
				fireInputChangeListeners();
			}
			
		});
		
		btnUseSelected.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateVisibility();
				fireInputChangeListeners();
			}
			
		});
		updateJob = new LoadPlanJob(planTreeViewer, currentFilter);
		updateJob.schedule();
		updateVisibility();
	}
	
	private void updateVisibility(){
		boolean areVisible = !btnNoParent.getSelection();
		planTreeViewer.getViewer().getControl().setEnabled(areVisible);
		filterLink.setEnabled(areVisible);
	}

	@Override
	protected boolean updateModelInternal(Plan plan) {
		if(!btnNoParent.getSelection()){
			PlanEditorInput tmp = (PlanEditorInput) planTreeViewer.getSelectedPlan();
			//tmp is supposed not to be null as in was checked in validate() method
			if (tmp.getUuid().equals(plan.getUuid())){
				MessageDialog.openError(getShell(),  Messages.PlanParentIdComposite_InfoDialog_Title, Messages.PlanParentIdComposite_InfoDialog_ReferItself_Message);
				return false;
			}
			Plan parent = null;
			Session session = HibernateManager.openSession();
			try{
				session.beginTransaction();
				parent = (Plan) session.load(Plan.class, tmp.getUuid());
				if (parent == null){
					MessageDialog.openInformation(getShell(),  Messages.PlanParentIdComposite_InfoDialog_Title, Messages.PlanParentIdComposite_InfoDialog_PlanNotFound_Message);
					return false;
				}
				parent.getName(); //to actually load data
			}finally{
				session.getTransaction().rollback();
				session.close();
			}
			updatePlan(plan, parent);
		}else{
			plan.setParent(null);
		}
		return true;
	}

	protected boolean updatePlan(Plan plan, Plan parent) {
		if (!PlanUtil.isDatesInParentRange(plan, parent)) {
			if (!updatePlanDates(plan, parent)) {
				return false;
			}
		}
		plan.setParent(parent);
		return true;
	}
	
	private boolean updatePlanDates(Plan plan, Plan parent) {
		MessageDialog dialog = new MessageDialog(Display.getCurrent().getActiveShell(),
				Messages.PlanParentIdComposite_DateWarnDialog_Title,
				null,
				Messages.PlanParentIdComposite_DateWarnDialog_Message,
				MessageDialog.CONFIRM, 
				new String[] { IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL }, 1);
		
		if (dialog.open() == MessageDialog.OK) {
			PlanUtil.fitDatesInParentRange(plan, parent);
			return true;
		}
		return false;
	}

	@Override
	protected void validate() {
		if(!btnNoParent.getSelection()){
			if (planTreeViewer.getSelectedPlan() == null){
				setErrorMessage(Messages.PlanParentIdComposite_InfoDialog_PlanRequired_Message);
				return;
			}
			PlanEditorInput in = (PlanEditorInput) planTreeViewer.getSelectedPlan();
			if (currentPlan.getUuid() != null){
				if (in.getUuid().equals(currentPlan.getUuid())){
					setErrorMessage(Messages.PlanParentIdComposite_SameParentError);
					return;
				}
				//ensure the new parent is not a child of the current plan
				Session s = HibernateManager.openSession();
				s.beginTransaction();
				try{
					s.update(currentPlan);
					//new parent cannot be a child of the current plan
					if (findPlan(currentPlan.getChildren(), in.getUuid())){
						setErrorMessage(Messages.PlanParentIdComposite_ChildParentError);
						return;
					}
				}finally{
					try{
						s.getTransaction().rollback();
					}catch (Exception ex){}
					s.close();
				}
			}
		}
		setErrorMessage(null);
	}

	private boolean findPlan(List<Plan> plans, UUID uuid){
		if (plans == null){
			return false;
		}
		for (Plan p : plans){
			if(p.getUuid().equals(uuid)){
				return true;
			}
		}
		for (Plan p : plans){
			
			if (findPlan(p.getChildren(), uuid)){
				return true;
			}
		}
		return false;
	}
	
	private Plan currentPlan;
	@Override
	public void initFromModel(Plan plan) {
		
		this.currentPlan = plan;
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
