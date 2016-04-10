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
package org.wcs.smart.connect.dataentry;

import java.util.ArrayList;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.hibernate.Session;
import org.wcs.smart.dataentry.dialog.ConfigurableModelEditDialog;
import org.wcs.smart.dataentry.dialog.ConfigurableModelLabelProvider;
import org.wcs.smart.dataentry.dialog.ConfigurableModelTreeContentProvider;
import org.wcs.smart.dataentry.dialog.IConfigurableModelEditorTabContent;

/**
 * Tab with SMART Connect Alerts content for configurable model.
 * 
 * @author elitvin
 * @since 4.0.0
 */
public class ConfigurableModelEditorConnectTab implements IConfigurableModelEditorTabContent {

	private ConfigurableModelEditDialog dialog;

	private TreeViewer modelTreeViewer;
	private TableViewer alertTable;
	private Button btnNew;
	private Button btnEdit;
	private Button btnDelete;
	
	@Override
	public void setDialog(ConfigurableModelEditDialog dialog) {
		this.dialog = dialog;
	}

	@Override
	public String getTabName() {
		return "Alerts - SMART Connect";
	}

	@Override
	public Composite createTabContent(Composite parent) {
		SashForm container = new SashForm(parent, SWT.HORIZONTAL);
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite innerLeft = new Composite(container, SWT.NONE);
		innerLeft.setLayout(new GridLayout());
		
		modelTreeViewer = new TreeViewer(innerLeft, SWT.V_SCROLL | SWT.H_SCROLL| SWT.BORDER);
		modelTreeViewer.setLabelProvider(new ConfigurableModelLabelProvider());
		modelTreeViewer.setContentProvider(new ConfigurableModelTreeContentProvider(false));
		modelTreeViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)modelTreeViewer.getControl().getLayoutData()).widthHint = 100;
		((GridData)modelTreeViewer.getControl().getLayoutData()).heightHint = 100;
		modelTreeViewer.setInput(dialog.getModel());
		modelTreeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				btnNew.setEnabled(modelTreeViewer.getSelection() != null && !modelTreeViewer.getSelection().isEmpty());
			}
		});
		modelTreeViewer.expandToLevel(2);

		btnNew = new Button(innerLeft, SWT.PUSH);
		btnNew.setText("New Alert");
		btnNew.setLayoutData(new GridData(SWT.END, SWT.BOTTOM, false, false));
		btnNew.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				//TODO: logic to add alerts
			}
		});
		
		Composite rightPanel = new Composite(container, SWT.NONE);
		rightPanel.setLayout(new GridLayout(1, false));
		
		alertTable = createAlertsTable(rightPanel);
		alertTable.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				ISelection sel = alertTable.getSelection();
				btnEdit.setEnabled(sel != null && !sel.isEmpty());
				btnDelete.setEnabled(sel != null && !sel.isEmpty());
			}
		});

		Composite buttonPanel = new Composite(rightPanel, SWT.NONE);
		buttonPanel.setLayout(new GridLayout(2, false));
		((GridLayout)buttonPanel.getLayout()).marginHeight = 0;
		((GridLayout)buttonPanel.getLayout()).marginWidth = 0;
		buttonPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		btnEdit = new Button(buttonPanel, SWT.PUSH);
		btnEdit.setText("Edit");
		btnEdit.setLayoutData(new GridData(SWT.BEGINNING, SWT.BOTTOM, false, false));
		btnEdit.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				//TODO: logic to add alerts
			}
		});

		btnDelete = new Button(buttonPanel, SWT.PUSH);
		btnDelete.setText("Delete");
		btnDelete.setLayoutData(new GridData(SWT.BEGINNING, SWT.BOTTOM, false, false));
		btnDelete.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				//TODO: logic to add alerts
			}
		});
		
		container.setWeights(new int[]{40,60});

		modelTreeViewer.refresh();
		
		return container;
	}

	private TableViewer createAlertsTable(Composite parent) {
		TableViewer table = new TableViewer(parent, SWT.FULL_SELECTION | SWT.BORDER);
		table.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		table.setContentProvider(ArrayContentProvider.getInstance());
		table.getTable().setHeaderVisible(true);
		table.getTable().setLinesVisible(true);
		
		TableViewerColumn colAlert = new TableViewerColumn(table, SWT.NONE);
		colAlert.getColumn().setWidth(200);
		colAlert.getColumn().setText("Alert");
		colAlert.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				//TODO: implement
			  	return super.getText(element);
			}

		});
		
		TableViewerColumn colType = new TableViewerColumn(table, SWT.NONE);
		colType.getColumn().setWidth(90);
		colType.getColumn().setText("Type");
		colType.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				//TODO: implement
			  	return super.getText(element);
			}
		});

		TableViewerColumn colImportance = new TableViewerColumn(table, SWT.NONE);
		colImportance.getColumn().setWidth(90);
		colImportance.getColumn().setText("Importance");
		colImportance.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				//TODO: implement
			  	return super.getText(element);
			}
		});
		
		table.setInput(new ArrayList<Object>()); //TODO: implement
		return table;
	}

	@Override
	public void performSave(Session s) {
		// TODO Auto-generated method stub
	}

	@Override
	public int getTabIndex() {
		return 200;
	}

}
