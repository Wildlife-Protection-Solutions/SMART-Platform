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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.wcs.smart.plan.model.Plan;
import org.wcs.smart.plan.model.PlanTarget;
import org.wcs.smart.plan.ui.newPlanWizard.TargetPropertyPage;
import org.wcs.smart.plan.ui.newPlanWizard.viewer.TargetListViewer;

/**
 * Composite for editing plan targets
 * 
 * @author jeffloun
 * @since 1.0.0
 */
public class PlanTargetComposite extends PlanComposite {

	private TargetListViewer targetTable;
	private List<PlanTarget> targets;

    
	/**
	 * @param parent
	 * @param style
	 */
	public PlanTargetComposite(Composite parent, int style) {
		super(parent, style);
		setMessage("Add plan targets by selecting the \"Add new target\" button. Click the edit button beside an existing target to make changes to it:");
		createControls();
	}

	private void createControls() {
        this.setLayout(new GridLayout(2, false));
        this.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
        
        Label lbl = new Label(this, SWT.NONE);
		lbl.setText("Plan Targets:");
		lbl.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
		
		Composite table = new Composite(this, SWT.NONE);
		table.setLayout(new GridLayout());
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		targetTable  = new TargetListViewer(table);
		
		Composite buttonPnl = new Composite(this, SWT.NONE);
		buttonPnl.setLayout(new GridLayout());
		buttonPnl.setLayoutData(new GridData(SWT.TOP, SWT.FILL, false, false));
		
		Button btnNew = new Button(buttonPnl, SWT.NONE);
		btnNew.setText("Add Target...");
		btnNew.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				TargetPropertyPage dia = new TargetPropertyPage(getShell(), targets, null); 
			    if (dia.open() == Window.CANCEL){
			    	//do nothing
				}else{

					targetTable.updateModel(targets);
					fireInputChangeListeners();
				}
			}
			
		});
		
		
		final Button btnEdit = new Button(buttonPnl, SWT.NONE);
		btnEdit.setText("Edit Target...");
		btnEdit.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				IStructuredSelection sec = (IStructuredSelection)targetTable.getSelection();
		        if (sec.isEmpty()){
		            return;
		        }
		        
		        PlanTarget selected = (PlanTarget)sec.getFirstElement(); 
				TargetPropertyPage dia = new TargetPropertyPage(getShell(), targets, selected); 
			    if (dia.open() == Window.CANCEL){
			    	//do nothing
				}else{
					targetTable.updateModel(targets);
					isDataValid();
					fireInputChangeListeners();
				}
			}
			
		});
		btnEdit.setEnabled(false);
		targetTable.getViewer().addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				btnEdit.setEnabled(!targetTable.getSelection().isEmpty());
				
			}
		});
		
		
		final Button btnDelete = new Button(buttonPnl, SWT.NONE);
		btnDelete.setText("Delete Target");
		btnDelete.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				IStructuredSelection sec = (IStructuredSelection)targetTable.getSelection();
		        if (sec.isEmpty()){
		            return;
		        }
		        
		        PlanTarget selected = (PlanTarget)sec.getFirstElement(); 
		        MessageBox dialog = new MessageBox(getShell(), SWT.ICON_QUESTION | SWT.OK| SWT.CANCEL);
        		dialog.setText("Delete Target: " + selected.getName());
        		dialog.setMessage("Do you really want to delete this Target?");
        		int result = dialog.open();
        		if( result == SWT.OK){
        			//delete the target
        			targets.remove(selected);
        			targetTable.updateModel(targets);
        			fireInputChangeListeners();
        		}
        			
        	}
			
		});
		btnDelete.setEnabled(false);
		targetTable.getViewer().addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				btnDelete.setEnabled(!targetTable.getSelection().isEmpty());
				
			}
		});
		       
	}
	
	@Override
	public boolean updateModel(Plan plan) {
		List<PlanTarget> list = plan.getTargets();
		if(list != null){
			for (PlanTarget pt : plan.getTargets()){
				pt.setPlan(plan);
			}
		}
   		targetTable.updateModel(plan.getTargets());
		return true;
	}

	@Override
	public void initFromModel(Plan plan) {
		targets = plan.getTargets();
		if(targets == null){
			targets = new ArrayList<PlanTarget>();
		}
		targetTable.initValues(targets);
	}

	@Override
	public boolean isDataValid() {
		return true;
	}
	
	public List<PlanTarget> getTargets(){
		return this.targets;
	}
	
	@Override
	public String getTitle() {
		return "Plan Targets";
	}
	
}
