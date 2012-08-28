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

import java.util.HashMap;

import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.jface.databinding.viewers.ObservableListContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.hibernate.Session;
import org.wcs.smart.plan.model.Plan;



/**
 * Wizard page for collecting the patrol comment
 * @author egouge
 * @since 1.0.0
 */
public class NewPlanWizardPage6 extends NewPlanWizardPage {

	private TableViewer targetTable;
	private HashMap<ttColumn, TableViewerColumn> targetTableColumns;
	private Plan plan;
	
		
	protected NewPlanWizardPage6(Plan plan) {
		super("Plan Targets");
		
		this.plan = plan;		
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
		
		
		targetTable = new TableViewer(center, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI);
		setupTable();
		
		WritableList inputList = new WritableList();
		targetTable.setInput(inputList);
		
		Button btnNew = new Button(center, SWT.NONE);
		btnNew.setText("Add Target...");
		btnNew.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				TargetPropertyPage dia = new TargetPropertyPage(getShell(), plan, null); //the plan
				dia.open();
			}
			
		});
		
		setControl(center);
		setMessage("Add all of the plan targets by selecting the \"Add new target\" button. Click the edit button beside an existing target to make change to it:");
	
	}
	
	private void setupTable() {
		targetTableColumns = new HashMap<ttColumn, TableViewerColumn>();

		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true,2,1);
		gd.heightHint = targetTable.getTable().computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
		targetTable.getTable().setLayoutData(gd);
		targetTable.getTable().setLinesVisible(true);
		targetTable.getTable().setHeaderVisible(true);
		targetTable.setContentProvider(new ObservableListContentProvider());

		
		for (int i = 0; i < ttColumn.values().length; i++) {
			final ttColumn columntype = ttColumn.values()[i];
		

		
			final TableViewerColumn column = new TableViewerColumn(targetTable,SWT.NONE);
			column.setLabelProvider(new targetTableLabelProvider(columntype));
			column.getColumn().setText(columntype.guiName);
			column.getColumn().setResizable(true);
			column.getColumn().setMoveable(false);


		
			targetTableColumns.put(columntype, column);
		
		}
	
	}


	@Override
	public boolean updateModel(Plan p) {
		return true;
	}
	
	@Override
	void initModel(Plan p, Session session) {

	}
	
	class targetTableLabelProvider extends ColumnLabelProvider {

		private ttColumn column = null;

		public targetTableLabelProvider(ttColumn column) {
			this.column = column;
		}
	}

	public void refreshTargetTable(){
		targetTable.refresh();
	}
	
	
}