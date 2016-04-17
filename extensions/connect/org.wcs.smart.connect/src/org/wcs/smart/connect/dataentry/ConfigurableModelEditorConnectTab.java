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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.UuidItem;
import org.wcs.smart.connect.model.ConnectAlert;
import org.wcs.smart.dataentry.dialog.ConfigurableModelEditDialog;
import org.wcs.smart.dataentry.dialog.IConfigurableModelChangeListener;
import org.wcs.smart.dataentry.dialog.IConfigurableModelEditorTabContent;
import org.wcs.smart.dataentry.model.CmAttributeListItem;
import org.wcs.smart.dataentry.model.CmNode;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.hibernate.HibernateManager;

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
	
	private List<ConnectAlert> alertsList;
	private List<ConnectAlert> dbAlertsList;
	private List<String> alertTypeList;
	
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
		modelTreeViewer.setLabelProvider(new ConnectCmTreeLabelProvider(dialog.getModel()));
		modelTreeViewer.setContentProvider(new ConnectCmTreeContentProvider(false));
		modelTreeViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)modelTreeViewer.getControl().getLayoutData()).widthHint = 100;
		((GridData)modelTreeViewer.getControl().getLayoutData()).heightHint = 100;
		modelTreeViewer.setInput(dialog.getModel());
		modelTreeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				updateNewButtonState();
			}
		});
		modelTreeViewer.expandToLevel(2);

		btnNew = new Button(innerLeft, SWT.PUSH);
		btnNew.setText("New Alert");
		btnNew.setLayoutData(new GridData(SWT.END, SWT.BOTTOM, false, false));
		btnNew.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleNewAlert();
			}
		});
		
		Composite rightPanel = new Composite(container, SWT.NONE);
		rightPanel.setLayout(new GridLayout(1, false));
		
		alertTable = createAlertsTable(rightPanel);
		alertTable.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				updateEditDeleteButtonState();
			}
		});
		dbAlertsList = loadAlerts(dialog.getModel());
		alertsList = new ArrayList<ConnectAlert>(dbAlertsList); //this is a copy of db data that can be changes, changes will be persisted on 'Save'
		alertTable.setInput(alertsList);

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
				handleEditAlert();
			}
		});

		btnDelete = new Button(buttonPanel, SWT.PUSH);
		btnDelete.setText("Delete");
		btnDelete.setLayoutData(new GridData(SWT.BEGINNING, SWT.BOTTOM, false, false));
		btnDelete.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleDeleteAlert();
			}
		});
		
		container.setWeights(new int[]{40,60});

		modelTreeViewer.refresh();
		dialog.addModelChangedListener(new IConfigurableModelChangeListener() {
			@Override
			public void notifyChangesMade() {
				modelTreeViewer.refresh();
				//TODO: delete alerts if alert item was removed from tree
			}
		});
		
		updateNewButtonState();
		updateEditDeleteButtonState();
		return container;
	}

	private TableViewer createAlertsTable(Composite parent) {
		TableViewer table = new TableViewer(parent, SWT.BORDER | SWT.VIRTUAL | SWT.FULL_SELECTION | SWT.MULTI);
		table.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		table.setContentProvider(ArrayContentProvider.getInstance());
		table.getTable().setHeaderVisible(true);
		table.getTable().setLinesVisible(true);
		table.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				handleEditAlert();
			}
		});
		
		TableViewerColumn colAlert = new TableViewerColumn(table, SWT.NONE);
		colAlert.getColumn().setWidth(200);
		colAlert.getColumn().setText("Alert");
		colAlert.setLabelProvider(new ConnectAlertSourceLabelProvider(dialog.getModel()));		
		TableViewerColumn colType = new TableViewerColumn(table, SWT.NONE);
		colType.getColumn().setWidth(90);
		colType.getColumn().setText("Type");
		colType.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof ConnectAlert) {
					ConnectAlert a = (ConnectAlert) element;
					return a.getType();
				}
			  	return super.getText(element);
			}
		});

		TableViewerColumn colImportance = new TableViewerColumn(table, SWT.NONE);
		colImportance.getColumn().setWidth(90);
		colImportance.getColumn().setText("Importance");
		colImportance.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof ConnectAlert) {
					ConnectAlert a = (ConnectAlert) element;
					return String.valueOf(a.getLevel());
				}
			  	return super.getText(element);
			}
		});
		
		return table;
	}

	private void updateNewButtonState() {
		boolean canCreate = false;
		IStructuredSelection sel = (IStructuredSelection) modelTreeViewer.getSelection();
		if (sel != null && !sel.isEmpty()) {
			Object obj = sel.getFirstElement();
			canCreate = obj instanceof CmNode || obj instanceof CmAttributeListItem || obj instanceof ConnectCmTreeElement;
		}
		btnNew.setEnabled(canCreate);
	}

	private void updateEditDeleteButtonState() {
		ISelection sel = alertTable.getSelection();
		btnEdit.setEnabled(sel != null && !sel.isEmpty());
		btnDelete.setEnabled(sel != null && !sel.isEmpty());
	}

	private ConnectAlert createAlertFromSelection() {
		IStructuredSelection sel = (IStructuredSelection) modelTreeViewer.getSelection();
		if (sel != null && !sel.isEmpty()) {
			ConnectAlert alert = new ConnectAlert();
			Object obj = sel.getFirstElement();
			if (obj instanceof ConnectCmTreeElement) {
				ConnectCmTreeElement el = (ConnectCmTreeElement) obj;
				alert.setAttrubute(el.getAttribute());
				alert.setAlertItem(el.getElement());
			} else if (obj instanceof UuidItem) {
				UuidItem item = (UuidItem) obj; //must be a category object
				alert.setAlertItem(item);
			} else {
				//NOTE: should never be here
				SmartPlugIn.log("Unexpected object was selected", null); //$NON-NLS-1$
				return null;
			}
			alert.setModel(dialog.getModel());
			return alert;
		}
		return null;
	}
	
	private void handleNewAlert() {
		ConnectAlert alert = createAlertFromSelection();
		if (alert == null) {
			return;
		}
		AlertEditDialog d = new AlertEditDialog(dialog.getShell(), true, alert, getAlertTypes());
		if (d.open() == Window.OK) {
			alertsList.add(alert);
			alertTable.refresh();
		}
	}

	private void handleEditAlert() {
		IStructuredSelection sel = (IStructuredSelection) alertTable.getSelection();
		if (sel != null && !sel.isEmpty()) {
			Object obj = sel.getFirstElement();
			if (obj instanceof ConnectAlert) {
				ConnectAlert alert = (ConnectAlert) obj;
				AlertEditDialog d = new AlertEditDialog(dialog.getShell(), false, alert, getAlertTypes());
				if (d.open() == Window.OK) {
					alertTable.refresh(true);
				}
			}
		}		
	}
	
	private void handleDeleteAlert() {
		IStructuredSelection sel = (IStructuredSelection) alertTable.getSelection();
		if (sel != null && !sel.isEmpty()) {
			if (MessageDialog.openQuestion(dialog.getShell(), "Confirm Delete", "Do you really want to delete selected alerts?")) {
//				int size = sel.size();
//				int count = 0;
				for (Iterator<?> i = sel.iterator(); i.hasNext();) {
					ConnectAlert alert = (ConnectAlert) i.next();
					//no actual delete from database, it will be done when 'Save' is pressed
					alertsList.remove(alert);
//					count++;
				}
				alertTable.refresh();
//				MessageDialog.openInformation(dialog.getShell(), "Alert delete", MessageFormat.format("{0} out of {1} alert(s) deleted.", count, size));
			}
		}
	}

	private List<ConnectAlert> loadAlerts(final ConfigurableModel cm) {
		final List<ConnectAlert> resultList = new ArrayList<ConnectAlert>();
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(dialog.getShell());
		try {
			pmd.run(true, false, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					monitor.beginTask("Loading SMART Connect Alerts", 1);
					Session s = HibernateManager.openSession();
					s.beginTransaction();
					try {
						@SuppressWarnings("unchecked")
						List<ConnectAlert> items = s.createCriteria(ConnectAlert.class)
								.add(Restrictions.eq("model", cm)).list();  //$NON-NLS-1$
						resultList.addAll(items);
					} catch (Exception ex) {
						SmartPlugIn.displayLog("Error occurs while loading SMART Connect Alerts.", ex);
					} finally {
						s.getTransaction().rollback();
						s.close();
					}
				}
			});
		} catch (Exception e) {
			SmartPlugIn.displayLog("Error occurs while loading SMART Connect Alerts.", e);
			return Collections.emptyList();
		}
		return resultList;
	}
	
	private List<String> getAlertTypes() {
		if (alertTypeList == null) {
			//TODO: need real list
			alertTypeList = (List<String>) Arrays.asList("One", "Two", "Three");
		}
		return alertTypeList;
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
