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
package org.wcs.smart.connect.cybertracker.dataentry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.dialogs.MessageDialog;
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
import org.eclipse.swt.widgets.Control;
import org.hibernate.Session;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.UuidItem;
import org.wcs.smart.connect.ConnectHibernateManager;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.SmartConnect;
import org.wcs.smart.connect.api.model.AlertType;
import org.wcs.smart.connect.cybertracker.ConnectCtHibernateManager;
import org.wcs.smart.connect.cybertracker.internal.Messages;
import org.wcs.smart.connect.cybertracker.model.ConnectAlert;
import org.wcs.smart.connect.cybertracker.util.AlertLookup;
import org.wcs.smart.connect.cybertracker.util.CmTreeNodesVisitor;
import org.wcs.smart.connect.cybertracker.util.CmTreeNodesVisitor.INodeVisitHandler;
import org.wcs.smart.connect.model.ConnectUser;
import org.wcs.smart.connect.ui.server.ConnectDialog;
import org.wcs.smart.dataentry.dialog.ConfigurableModelEditDialog;
import org.wcs.smart.dataentry.dialog.IConfigurableModelChangeListener;
import org.wcs.smart.dataentry.dialog.IConfigurableModelEditorTabContent;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.CmAttributeListItem;
import org.wcs.smart.dataentry.model.CmNode;
import org.wcs.smart.dataentry.model.ConfigurableModel;

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
	
	private ConnectAlertSourceLabelProvider alertSourceLabelProvider;
	
	@Override
	public void setDialog(ConfigurableModelEditDialog dialog) {
		this.dialog = dialog;
		alertSourceLabelProvider = new ConnectAlertSourceLabelProvider(dialog.getModel());
	}

	@Override
	public String getTabName() {
		return Messages.ConfigurableModelEditorConnectTab_TabName;
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
		modelTreeViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				IStructuredSelection sel = (IStructuredSelection) modelTreeViewer.getSelection();
				if (sel != null && !sel.isEmpty()) {
					Object obj = sel.getFirstElement();
					if (obj instanceof CmNode || obj instanceof CmAttributeListItem || obj instanceof ConnectCmTreeElement){
						handleNewAlert();
					}
				}
			}
		});
		modelTreeViewer.expandToLevel(2);

		btnNew = new Button(innerLeft, SWT.PUSH);
		btnNew.setText(Messages.ConfigurableModelEditorConnectTab_Button_NewAlert);
		btnNew.setLayoutData(new GridData(SWT.END, SWT.BOTTOM, false, false));
		((GridData)btnNew.getLayoutData()).widthHint = 90;
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
		buttonPanel.setLayout(new GridLayout(3, false));
		((GridLayout)buttonPanel.getLayout()).marginHeight = 0;
		((GridLayout)buttonPanel.getLayout()).marginWidth = 0;
		buttonPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		btnEdit = new Button(buttonPanel, SWT.PUSH);
		btnEdit.setText(Messages.ConfigurableModelEditorConnectTab_Button_Edit);
		btnEdit.setLayoutData(new GridData(SWT.BEGINNING, SWT.BOTTOM, false, false));
		((GridData)btnEdit.getLayoutData()).widthHint = 90;
		btnEdit.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleEditAlert();
			}
		});

		btnDelete = new Button(buttonPanel, SWT.PUSH);
		btnDelete.setText(Messages.ConfigurableModelEditorConnectTab_Button_Delete);
		btnDelete.setLayoutData(new GridData(SWT.BEGINNING, SWT.BOTTOM, false, false));
		((GridData)btnDelete.getLayoutData()).widthHint = 90;
		btnDelete.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleDeleteAlert();
			}
		});
		
		Button btnRefresh = new Button(buttonPanel, SWT.PUSH);
		btnRefresh.setText(Messages.ConfigurableModelEditorConnectTab_RefreshTypes);
		btnRefresh.setToolTipText(Messages.ConfigurableModelEditorConnectTab_RefreshToolTip);
		btnRefresh.setLayoutData(new GridData(SWT.RIGHT, SWT.BOTTOM, true, false));
		btnRefresh.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				alertTypeList = null;
				getAlertTypes();
			}
		});
		
		container.setWeights(new int[]{40,60});

		modelTreeViewer.refresh();
		dialog.addModelChangedListener(new IConfigurableModelChangeListener() {
			@Override
			public void notifyChangesMade() {
				modelTreeViewer.refresh();
				cleanupAlerts();
				alertTable.refresh();
			}
		});
		
		updateNewButtonState();
		updateEditDeleteButtonState();
		return container;
	}

	/**
	 * Delete alerts if alert item was removed from configurable model itself
	 */
	protected void cleanupAlerts() {
		Set<ConnectAlert> toDelete = new HashSet<>(alertsList); //initially all alerts
		AlertLookup lookup = new AlertLookup(alertsList);
		
		CmTreeNodesVisitor visitor = new CmTreeNodesVisitor();
		visitor.visit(dialog.getModel(), new INodeVisitHandler() {
			@Override
			public void handle(UuidItem item, CmAttribute attribute) {
				List<ConnectAlert> found = lookup.getAlerts(item, attribute);
				toDelete.removeAll(found);
			}
		});
		//all item that are still mapped to configurable model are now removed from toDelete
		alertsList.removeAll(toDelete);
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
		colAlert.getColumn().setText(Messages.ConfigurableModelEditorConnectTab_Column_Alert);
		colAlert.setLabelProvider(alertSourceLabelProvider);		
		TableViewerColumn colType = new TableViewerColumn(table, SWT.NONE);
		colType.getColumn().setWidth(90);
		colType.getColumn().setText(Messages.ConfigurableModelEditorConnectTab_Column_Type);
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
		colImportance.getColumn().setText(Messages.ConfigurableModelEditorConnectTab_Column_Importance);
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
		List<String> alertTypes = getAlertTypes();
		if (alertTypes == null){
			//TODO: ERROR MESSAGE
			return;
		}
		AlertEditDialog d = new AlertEditDialog(dialog.getShell(), true, alert, alertTypes);
		if (d.open() == Window.OK) {
			alertsList.add(alert);
			alertTable.refresh();
			dialog.notifyChangesMade();
		}
	}

	private void handleEditAlert() {
		IStructuredSelection sel = (IStructuredSelection) alertTable.getSelection();
		if (sel != null && !sel.isEmpty()) {
			Object obj = sel.getFirstElement();
			if (obj instanceof ConnectAlert) {
				ConnectAlert alert = (ConnectAlert) obj;
				List<String> alertTypes = getAlertTypes();
				if (alertTypes == null){
					//TODO: ERROR MESSAGE
					return;
				}
				
				AlertEditDialog d = new AlertEditDialog(dialog.getShell(), false, alert, alertTypes);
				if (d.open() == Window.OK) {
					alertTable.refresh(true);
					dialog.notifyChangesMade();
				}
			}
		}		
	}
	
	private void handleDeleteAlert() {
		IStructuredSelection sel = (IStructuredSelection) alertTable.getSelection();
		if (sel != null && !sel.isEmpty()) {
			if (MessageDialog.openQuestion(dialog.getShell(), Messages.ConfigurableModelEditorConnectTab_ConfirmDeleteDialogTitle, Messages.ConfigurableModelEditorConnectTab_ConfirmDeleteDialogMessage)) {
//				int size = sel.size();
//				int count = 0;
				for (Iterator<?> i = sel.iterator(); i.hasNext();) {
					ConnectAlert alert = (ConnectAlert) i.next();
					//no actual delete from database, it will be done when 'Save' is pressed
					alertsList.remove(alert);
//					count++;
				}
				alertTable.refresh();
				dialog.notifyChangesMade();
//				MessageDialog.openInformation(dialog.getShell(), "Alert delete", MessageFormat.format("{0} out of {1} alert(s) deleted.", count, size));
			}
		}
	}

	private List<ConnectAlert> loadAlerts(final ConfigurableModel cm) {
		final List<ConnectAlert> resultList = new ArrayList<ConnectAlert>();
		if (cm.getUuid() == null) {
			return resultList;
		}
		resultList.addAll(ConnectCtHibernateManager.getConnectAlerts(cm, dialog.getSession(), true));
		return resultList;
	}

	
	private List<String> getAlertTypes() {
		if (alertTypeList == null) {
			ConnectDialog cd = new ConnectDialog(dialog.getShell(), true){
				@Override
				protected Control createDialogArea(Composite parent) {
					setTitle(Messages.ConfigurableModelEditorConnectTab_ConnectDialogTitle);
					getShell().setText(Messages.ConfigurableModelEditorConnectTab_ConnectDialogText);
					setMessage(Messages.ConfigurableModelEditorConnectTab_ConnectDialogMessage);
					return super.createDialogArea(parent);	
				}
				protected void loadDatabaseInformation(){
					Session s = dialog.getSession();
					s.beginTransaction();
					cs = ConnectHibernateManager.getConnectServer(s);
					user = ConnectHibernateManager.getConnectUser(employee, s);			
					s.getTransaction().commit();
				}
				protected void saveUserInfo(final String newName, String newPassword)
						throws Exception {
					Session s = dialog.getSession();
					try{
						s.beginTransaction();
						if (user == null){
							ConnectUser newuser = new ConnectUser();
							newuser.setConnectUsername(newName);
							newuser.setServer(cs);
							newuser.setSmartUser(employee);
							user = newuser;
							s.save(newuser);
						}
						user.setConnectPassword(newPassword);
						s.saveOrUpdate(user);
						s.getTransaction().commit();
					}catch (Exception ex){
						s.getTransaction().rollback();

					}finally{
						s.close();
					}
				}
			};
			
			List<String> newAlerts = null;
			
			if (cd.open() != Window.CANCEL){
				SmartConnect sc = cd.getConnection();
				try{
					List<AlertType> types = sc.getAlertTypes();
					newAlerts = new ArrayList<String>();
					for(AlertType at : types){
						newAlerts.add(at.getLabel());
					}
				}catch (Exception ex){
					ConnectPlugIn.log("Unable to get Alert Types from server:" + ex.getMessage(), ex); //$NON-NLS-1$
				}
			}
			if (newAlerts == null){
				MessageDialog.openWarning(dialog.getShell(), 
						Messages.ConfigurableModelEditorConnectTab_WarningTitle, 
						Messages.ConfigurableModelEditorConnectTab_WarningMessage);
				
				String types = ConnectPlugIn.getDefault().getPreferenceStore().getString(ConnectPlugIn.CONNECT_ALERT_TYPE_CACHE_PREF);
				if (types == null || types.isEmpty()){
					//nothing found - empty list
					newAlerts = new ArrayList<String>();
				}else{
					newAlerts = new ArrayList<String>();
					JSONParser parser = new JSONParser();
					JSONArray array;
					try {
						array = ((JSONArray )parser.parse(types));
						for (Iterator<?> iterator = array.iterator(); iterator.hasNext();) {
							String object = (String) iterator.next();
							newAlerts.add(object);
						}
					} catch (ParseException e) {
						ConnectPlugIn.log("Error parsing alert types from preference store.", e); //$NON-NLS-1$
					}
				}
			}else{
				JSONArray array = new JSONArray();
				array.addAll(newAlerts);
				ConnectPlugIn.getDefault().getPreferenceStore().setValue(ConnectPlugIn.CONNECT_ALERT_TYPE_CACHE_PREF, array.toJSONString());
			}
			
			alertTypeList = newAlerts;
		}
		return alertTypeList;
	}
	
	@Override
	public void performSave(Session s) {
		Set<ConnectAlert> removed = new HashSet<ConnectAlert>(dbAlertsList);
		removed.removeAll(alertsList);
		for (ConnectAlert a : removed) {
			//evict alert and reload from database; without evict it may not be in the database but
			//still the hibernate cache
			s.evict(a);
			Object del = s.get(ConnectAlert.class, a.getUuid());
			if (del != null) {
				//it may be the case that object was removed by constraint in db 
				s.delete(del);
			}
		}
		for (ConnectAlert a : alertsList) {
			s.saveOrUpdate(a);
		}
	}
	
	@Override
	public void postSave() {
		//no need to fetch alerts from database as alertsList must be identical to current database state.
		dbAlertsList = new ArrayList<ConnectAlert>(alertsList); 
	}

	@Override
	public int getTabIndex() {
		return 200;
	}

}
