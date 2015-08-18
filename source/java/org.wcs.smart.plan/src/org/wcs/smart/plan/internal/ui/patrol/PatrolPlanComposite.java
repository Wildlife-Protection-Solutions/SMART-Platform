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

import java.util.UUID;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Link;
import org.wcs.smart.plan.filter.PlanFilter;
import org.wcs.smart.plan.internal.Messages;
import org.wcs.smart.plan.model.Plan;
import org.wcs.smart.plan.ui.IPlanFilterItem;
import org.wcs.smart.plan.ui.LoadPlanJob;
import org.wcs.smart.plan.ui.PlanFilterDialog;
import org.wcs.smart.plan.ui.editor.PlanEditorInput;
import org.wcs.smart.plan.ui.panel.PlanComposite;
import org.wcs.smart.plan.ui.tree.PlanViewer;

/**
 * Composite for selecting a plan for a given patrol.
 * '
 * @author Emily
 *
 */
public class PatrolPlanComposite extends PlanComposite implements IPlanFilterItem  {
	public static final String TITLE = Messages.PatrolPlanComposite_Title;
	
	private PlanFilter currentFilter; 
	private PlanViewer pv;
	
	private LoadPlanJob updateJob;
	
	public PatrolPlanComposite(Composite parent, int style) {
		super(parent, style);
		setMessage(Messages.PatrolPlanComposite_Message);
		currentFilter = new PlanFilter();
		createControl();
	}
	
	private void createControl(){
		 this.setLayout(new GridLayout(1, false));
	     this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Link lnk = new Link(this, SWT.NONE);
		lnk.setText(Messages.PatrolPlanComposite_Filter_Link);
		lnk.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				PlanFilterDialog d = new PlanFilterDialog(getShell(), PatrolPlanComposite.this);
				d.open();
			}
		});
		pv = new PlanViewer(this);
		pv.getViewer().getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		updateJob = new LoadPlanJob(pv, currentFilter, true);
		updateJob.schedule();
		
		
	}

	public void setDefaultSelection(Object defaultSelection){
		updateJob.setDefaultSelection(defaultSelection);
	}
	
	public PlanViewer getViewer(){
		return this.pv;
	}
	
	public UUID getSelection(){
		Object sel = pv.getSelectedPlan();
		if (sel instanceof PlanEditorInput){
			return ((PlanEditorInput) sel).getUuid();
		}else if (sel instanceof Plan){
			return ((Plan) sel).getUuid();
		}
		return null;
	}
	
	@Override
	protected boolean updateModelInternal(Plan plan) {
		//nothing to do
		return true;
	}

	@Override
	public void initFromModel(Plan plan) {
		Object defaultSelection = null;
		if (plan != null){
			defaultSelection = new PlanEditorInput(plan.getUuid(), plan.getLabel(), plan.getType());
		}else{
			defaultSelection = LoadPlanJob.NONE_LABEL;
		}
		
		final Object initSelection = defaultSelection;
		updateJob.setDefaultSelection(defaultSelection);
		pv.getViewer().addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				if (pv.getSelectedPlan() == null || !pv.getSelectedPlan().equals(initSelection)){
					fireInputChangeListeners();
				}
				
			}
		});
		
	}

	@Override
	public String getTitle() {
		return TITLE;
	}

	@Override
	public void updateContent() {
		updateJob.schedule();
	}

	@Override
	public PlanFilter getPlanFilter() {
		return this.currentFilter;
	}
}
