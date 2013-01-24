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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.codehaus.groovy.tools.shell.Shell;
import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.jface.databinding.viewers.ObservableListContentProvider;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.hibernate.Session;
import org.wcs.smart.plan.model.Plan;
import org.wcs.smart.plan.model.PlanTarget;
import org.wcs.smart.plan.ui.newPlanWizard.viewer.TargetListViewer;



/**
 * Wizard page for collecting the patrol comment
 * @author egouge
 * @since 1.0.0
 */
public class NewPlanWizardPage6 extends NewPlanWizardPage {

	private TargetListViewer targetTable;
	private HashMap<ttColumn, TableViewerColumn> targetTableColumns;
	private Plan plan;
	private TargetPropertyPage dia;
	private NewPlanWizardPage6 thisPage;
	
	private TargetListViewer rows;
		
	protected NewPlanWizardPage6(Plan plan) {
		super("Plan Targets");
		this.plan = plan;
		this.thisPage = this;
	}
	protected enum ttColumn {
		NAME("Target Name", 1), DESC("Target Description", 2);
		protected String guiName;
		protected int weight;

		private ttColumn(String name, int weight) {
			this.guiName = name;
			this.weight = weight;
		}
	}

	
	/**
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(Composite parent) {
		Composite center = new Composite(parent, SWT.NONE);
		center.setLayout(new GridLayout(2, false));
		center.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
		
		Label lbl = new Label(center, SWT.NONE);
		lbl.setText("Plan Targets:");
		lbl.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
		
		

		
		Composite table = new Composite(center, SWT.NONE);
		table.setLayout(new TableColumnLayout());
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		targetTable  = new TargetListViewer(table, plan);
		
   
		Button btnNew = new Button(center, SWT.NONE);
		btnNew.setText("Add Target...");
		btnNew.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				TargetPropertyPage dia = new TargetPropertyPage(thisPage, getShell(), plan, null); 
			    if (dia.open() == Window.CANCEL){
			    	//do nothing
				}else{
					targetTable.updateModel(plan);
				}
			}
			
		});
		
		Button btnEdit = new Button(center, SWT.NONE);
		btnEdit.setText("Edit Selected Target");
		btnEdit.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				IStructuredSelection sec = (IStructuredSelection)targetTable.getSelection();
		        if (sec.isEmpty()){
		            return;
		        }
		        
		        PlanTarget selected = (PlanTarget)sec.getFirstElement(); 
				TargetPropertyPage dia = new TargetPropertyPage(thisPage, getShell(), plan, selected); 
			    if (dia.open() == Window.CANCEL){
			    	//do nothing
				}else{
					targetTable.updateModel(plan);
					validate();
				}
			}
			
		});

		
		setControl(center);
		setMessage("Add all of the plan targets by selecting the \"Add new target\" button. Click the edit button beside an existing target to make change to it:");
		
			
	}
	


	@Override
	public boolean updateModel(Plan p) {
		targetTable.updateModel(plan);
		return true;
	}
	
	@Override
	void initModel(Plan p, Session session) {
		targetTable.updateModel(plan);
	}
	
	public void refreshTargetTable(){
		//targetTable.refresh();
	}
	
	public void validate(){
		((CreatePlanWizard)getWizard()).validate();
	}
	
}