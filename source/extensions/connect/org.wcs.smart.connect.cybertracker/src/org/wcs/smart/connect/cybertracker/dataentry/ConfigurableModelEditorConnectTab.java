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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
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
import org.wcs.smart.connect.cybertracker.model.ConnectCtProperties;
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
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.util.UuidUtils;

/**
 * Tab with SMART Connect Alerts content for configurable model.
 * 
 * @author elitvin
 * @since 4.0.0
 */
public class ConfigurableModelEditorConnectTab implements IConfigurableModelEditorTabContent {
	
	private static final String LABEL_KEY = "label"; //$NON-NLS-1$

	private static final String UUID_KEY = "uuid"; //$NON-NLS-1$

	private ConfigurableModelEditDialog dialog;

	private TreeViewer modelTreeViewer;
	private TableViewer alertTable;
	private Button btnNew;
	private Button btnEdit;
	private Button btnDelete;
	
	private Text txtPosition;
	private Text txtData;
	
	private ControlDecoration cdData;
	private ControlDecoration cdPosition;
	private ComboViewer cmbPositionType;
	private ControlDecoration cdPositionType;
	
	private List<ConnectAlert> alertsList;
	private List<ConnectAlert> dbAlertsList;
	private List<ConnectAlertType> alertTypeList;
	private HashMap<UUID, ConnectAlertType> typeCache;
	
	private ConnectCtProperties properties;
	
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
		Composite all = new Composite(parent, SWT.BORDER);
		all.setLayout(new GridLayout(1, false));
		all.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		dbAlertsList = loadAlerts(dialog.getModel());
		properties = ConnectCtHibernateManager.getCtProperties(dialog.getModel(), dialog.getSession());
		
		Group g = new Group(all, SWT.NONE);
		g.setText(Messages.ConfigurableModelEditorConnectTab_UploadHeader);
		g.setLayout(new GridLayout(1, false));
		g.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		//controls for position frequence
		Composite positionComp = new Composite(g, SWT.NONE);
		positionComp.setLayout(new GridLayout(6, false));
		positionComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		Button btnPosition = new Button(positionComp, SWT.CHECK);
		
		Label lbl1 = new Label(positionComp, SWT.NONE);
		lbl1.setText(Messages.ConfigurableModelEditorConnectTab_PositionUpdateLabel);
		txtPosition = new Text(positionComp, SWT.BORDER);
		txtPosition.setTextLimit(4);
		
		GridData gd2 = new GridData(SWT.FILL, SWT.FILL, false, false);
		gd2.widthHint = 25;
		txtPosition.setLayoutData(gd2);
		Label lbl2 = new Label(positionComp, SWT.NONE);
		lbl2.setText(Messages.ConfigurableModelEditorConnectTab_PositionUpdateTime);
		cdPosition = createDecoration(lbl2);
		
		
		Label lbl3a = new Label(positionComp, SWT.NONE);
		lbl3a.setText(Messages.ConfigurableModelEditorConnectTab_PositionTypeLabel);
		
		cmbPositionType = new ComboViewer(positionComp, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbPositionType.setLabelProvider(new LabelProvider(){
			@Override
			public String getText(Object element) {
				if (element instanceof UUID) {
					ConnectAlertType alerttype = typeCache.get((UUID)element);
					if (alerttype == null){
						return ((UUID)element).toString() + Messages.ConfigurableModelEditorConnectTab_RefreshForLabels;
					}else{
						return alerttype.getLabel();
					}
				}
			  	return super.getText(element);
			}
		});
		
		cmbPositionType.setContentProvider(ArrayContentProvider.getInstance());
		ArrayList<UUID> types = new ArrayList<UUID>();
		if (typeCache != null) types.addAll(typeCache.keySet());
		cmbPositionType.setInput(types);
		cmbPositionType.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				boolean change = properties.getPingType() == null || !properties.getPingType().equals(((IStructuredSelection)cmbPositionType.getSelection()).getFirstElement());
				IStructuredSelection selection = (IStructuredSelection) cmbPositionType.getSelection();
				if (!selection.isEmpty()){
					Object x = selection.getFirstElement();
					if (x instanceof UUID){
						properties.setPingType((UUID)x);
						cdPositionType.hide();
					}
				}
				if (change){
					dialog.notifyChangesMade();
				}
				
			}
		});
		cdPositionType = createDecoration(cmbPositionType.getControl());
		cdPositionType.hide();
		
		String tooltip = Messages.ConfigurableModelEditorConnectTab_PositionFreqTooltip;
		lbl1.setToolTipText(tooltip);
		lbl2.setToolTipText(tooltip);
		txtPosition.setToolTipText(tooltip);
		btnPosition.setToolTipText(tooltip);
		
		String alertTooltip = Messages.ConfigurableModelEditorConnectTab_PositionTypeTooltip;
		lbl3a.setToolTipText(alertTooltip);
		cmbPositionType.getControl().setToolTipText(alertTooltip);
		
		btnPosition.addSelectionListener(new SelectionAdapter() {	
			@Override
			public void widgetSelected(SelectionEvent e) {
				lbl1.setEnabled(btnPosition.getSelection());
				lbl2.setEnabled(btnPosition.getSelection());
				lbl3a.setEnabled(btnPosition.getSelection());
				txtPosition.setEnabled(btnPosition.getSelection());
				cmbPositionType.getControl().setEnabled(btnPosition.getSelection());
				updatePositionFrequency();
				validatePositionFrequency();
				dialog.notifyChangesMade();
			}
		});

		//intialize controls for position frequency
		if (properties.getPingFrequency() == null || properties.getPingFrequency() == 0){
			btnPosition.setSelection(false);
			txtPosition.setText("0"); //$NON-NLS-1$
		}else{
			btnPosition.setSelection(true);
			txtPosition.setText(properties.getPingFrequency().toString());
		}
		lbl1.setEnabled(btnPosition.getSelection());
		lbl2.setEnabled(btnPosition.getSelection());
		lbl3a.setEnabled(btnPosition.getSelection());
		txtPosition.setEnabled(btnPosition.getSelection());
		cmbPositionType.getControl().setEnabled(btnPosition.getSelection());
		validatePositionFrequency();
		txtPosition.addModifyListener(new ModifyListener() {
			
			@Override
			public void modifyText(ModifyEvent e) {
				validatePositionFrequency();
				dialog.notifyChangesMade();
			}
		});
		
		//create data frequency options
		Composite dataComp = new Composite(g, SWT.NONE);
		dataComp.setLayout(new GridLayout(4, false));
		dataComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		Button btnData = new Button(dataComp, SWT.CHECK);
		
		Label lbl3 = new Label(dataComp, SWT.NONE);
		lbl3.setText(Messages.ConfigurableModelEditorConnectTab_DataUploadLabel);
		txtData = new Text(dataComp, SWT.BORDER);
		txtData.setTextLimit(4);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, false, false);
		gd.widthHint = 25;
		txtData.setLayoutData(gd);
		Label lbl4 = new Label(dataComp, SWT.NONE);
		lbl4.setText(Messages.ConfigurableModelEditorConnectTab_DataUploadTime);
		cdData = createDecoration(lbl4);


		String tooltip2 = Messages.ConfigurableModelEditorConnectTab_DataUploadTooltip;
		lbl3.setToolTipText(tooltip2);
		lbl4.setToolTipText(tooltip2);
		txtData.setToolTipText(tooltip2);
		btnData.setToolTipText(tooltip2);
		btnData.addSelectionListener(new SelectionAdapter() {	
			@Override
			public void widgetSelected(SelectionEvent e) {
				lbl3.setEnabled(btnData.getSelection());
				lbl4.setEnabled(btnData.getSelection());
				txtData.setEnabled(btnData.getSelection());
				updateDataFrequency();
				validateDataFrequency();
				dialog.notifyChangesMade();
			}
		});
		
		//intialize controls for data frequency
		if (properties.getDataFrequency() == null || properties.getDataFrequency() == 0){
			btnData.setSelection(false);
			txtData.setText("0"); //$NON-NLS-1$
		}else{
			btnData.setSelection(true);
			txtData.setText(properties.getDataFrequency().toString());
		}
		lbl3.setEnabled(btnData.getSelection());
		lbl4.setEnabled(btnData.getSelection());
		txtData.setEnabled(btnData.getSelection());
		validateDataFrequency();
		txtData.addModifyListener(new ModifyListener() {
			
			@Override
			public void modifyText(ModifyEvent e) {
				validateDataFrequency();
				dialog.notifyChangesMade();
			}
		});
		
		Group g2 = new Group(all, SWT.NONE);
		g2.setText(Messages.ConfigurableModelEditorConnectTab_AlertConfigTitle);
		g2.setLayout(new GridLayout(1, false));
		g2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
//		SashForm container = new SashForm(g2, SWT.HORIZONTAL);
//		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));	
		Composite container = new Composite(g2, SWT.NONE);
		container.setLayout(new GridLayout(2, false));
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite innerLeft = new Composite(container, SWT.NONE);
		innerLeft.setLayout(new GridLayout());
		innerLeft.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)innerLeft.getLayout()).marginHeight = 0;
		
		modelTreeViewer = new TreeViewer(innerLeft, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		modelTreeViewer.setLabelProvider(new ConnectCmTreeLabelProvider(dialog.getModel()));
		modelTreeViewer.setContentProvider(new ConnectCmTreeContentProvider(false));
		modelTreeViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	
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
		rightPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)rightPanel.getLayout()).marginHeight = 0;
		
		alertTable = createAlertsTable(rightPanel);
		alertTable.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		alertTable.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				updateEditDeleteButtonState();
			}
		});
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
				alertTable.refresh();
			}
		});
		
		//container.setWeights(new int[]{40,60});

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
		return all;
	}

	protected ControlDecoration createDecoration(Control control){
		ControlDecoration cd = new ControlDecoration(control, SWT.LEFT);
		cd.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		cd.setShowHover(true);
		return cd;
	}
	
	private boolean areFrequencyValid() {
		return !cdData.isVisible() && !cdPosition.isVisible();
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

		updateTypeCache();
		colType.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof ConnectAlert) {
					ConnectAlert a = (ConnectAlert) element;
					ConnectAlertType alerttype = typeCache.get(a.getType());
					if (alerttype == null){
						return a.getTypeInternal() + Messages.ConfigurableModelEditorConnectTab_RefreshForLabels;
					}else{
						return alerttype.getLabel();
					}
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

	@SuppressWarnings("unchecked")
	private synchronized void updateTypeCache(){
		typeCache = new HashMap<UUID, ConnectAlertType>();
		if (alertTypeList == null){
			ArrayList<ConnectAlertType> types = getCachedAlertTypes();
			if (types != null){
				for (ConnectAlertType t : types){
					typeCache.put(t.getUuid(), t);
				}
			}
		}else{
			for (ConnectAlertType t : alertTypeList){
				typeCache.put(t.getUuid(), t);
			}
		}
		
		List<UUID> alertTypes = (List<UUID>) cmbPositionType.getInput();
		alertTypes.clear();
		alertTypes.addAll(typeCache.keySet());
		
		cmbPositionType.refresh();
		cmbPositionType.getControl().getParent().layout(true);
		cdPositionType.hide();
		
		if (typeCache.isEmpty()){
			cdPositionType.setDescriptionText(Messages.ConfigurableModelEditorConnectTab_NoTypesFound);
			cdPositionType.show();
		}else{
			if (properties.getPingType() != null){
				if (!typeCache.containsKey(properties.getPingType())){
					properties.setPingType(null);
					cmbPositionType.setSelection(null);
					cdPositionType.setDescriptionText(Messages.ConfigurableModelEditorConnectTab_TypeNotValid);
					cdPositionType.show();
				}else{
					cmbPositionType.setSelection(new StructuredSelection(properties.getPingType()));
				}
			}else{
				cdPositionType.setDescriptionText(Messages.ConfigurableModelEditorConnectTab_TypeRequired);
				cdPositionType.show();
			}
		}
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
		List<ConnectAlertType> alertTypes = getAlertTypes();
		if (alertTypes == null){
			MessageDialog.openError(dialog.getShell(),Messages.ConfigurableModelEditorConnectTab_NoAlertsTitle, Messages.ConfigurableModelEditorConnectTab_NoAlertsMessage);
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
				List<ConnectAlertType> alertTypes = getAlertTypes();
				if (alertTypes == null){
					MessageDialog.openWarning(dialog.getShell(),Messages.ConfigurableModelEditorConnectTab_NoAlertsTitle, Messages.ConfigurableModelEditorConnectTab_NoAlertsMessage);
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
				for (Iterator<?> i = sel.iterator(); i.hasNext();) {
					ConnectAlert alert = (ConnectAlert) i.next();
					//no actual delete from database, it will be done when 'Save' is pressed
					alertsList.remove(alert);
				}
				alertTable.refresh();
				dialog.notifyChangesMade();
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

	
	@SuppressWarnings("unchecked")
	private List<ConnectAlertType> getAlertTypes() {
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
					}
				}
			};
			
			List<ConnectAlertType> newAlerts = null;
			
			if (cd.open() != Window.CANCEL){
				SmartConnect sc = cd.getConnection();
				try{
					List<AlertType> types = sc.getAlertTypes();
					newAlerts = new ArrayList<ConnectAlertType>();
					for(AlertType at : types){
						newAlerts.add(new ConnectAlertType(at.getUuid(), at.getLabel()));
					}
				}catch (Exception ex){
					ConnectPlugIn.log("Unable to get Alert Types from server:" + ex.getMessage(), ex); //$NON-NLS-1$
				}
			}
			if (newAlerts == null){
				MessageDialog.openWarning(dialog.getShell(), 
						Messages.ConfigurableModelEditorConnectTab_WarningTitle, 
						Messages.ConfigurableModelEditorConnectTab_WarningMessage);
				newAlerts = getCachedAlertTypes();
			}else{
				if (newAlerts  != null){
					JSONArray array = new JSONArray();
					for (ConnectAlertType a : newAlerts){
						JSONObject obj = new JSONObject();
						obj.put(UUID_KEY, a.getUuid().toString());
						obj.put(LABEL_KEY, a.getLabel());
						array.add(obj);
					}
					ConnectPlugIn.getDefault().getPreferenceStore().setValue(getPreferenceKey(), array.toJSONString());
				}
			}
			
			alertTypeList = newAlerts;
		}
		updateTypeCache();
		return alertTypeList;
	}
	
	/**
	 * the preference key for caching alert types - this is Conservation Area specific
	 * @return
	 */
	private String getPreferenceKey(){
		String cauuid = UuidUtils.uuidToString(SmartDB.getCurrentConservationArea().getUuid());
		return ConnectPlugIn.CONNECT_ALERT_TYPE_CACHE_PREF + "." + cauuid; //$NON-NLS-1$
	}
	
	private ArrayList<ConnectAlertType> getCachedAlertTypes(){
		
		String types = ConnectPlugIn.getDefault().getPreferenceStore().getString(getPreferenceKey());
		if (types == null || types.isEmpty()){
			//nothing found - empty list
			return null;
		}else{
			ArrayList<ConnectAlertType> newAlerts = new ArrayList<ConnectAlertType>();
			JSONParser parser = new JSONParser();
			JSONArray array;
			try {
				array = ((JSONArray )parser.parse(types));
				for (Iterator<?> iterator = array.iterator(); iterator.hasNext();) {
					JSONObject object = (JSONObject)iterator.next();
					
					UUID uuid = UUID.fromString((String)object.get(UUID_KEY));
					String label = (String)object.get(LABEL_KEY);
					
					newAlerts.add(new ConnectAlertType(uuid, label));
				}
			} catch (ParseException e) {
				ConnectPlugIn.log("Error parsing alert types from preference store.", e); //$NON-NLS-1$
			}
			return newAlerts;
		}
		
	}
	@Override
	public String validate() {
		if (!areFrequencyValid()) {
			return Messages.ConfigurableModelEditorConnectTab_InvalidConfig;
		}
		if (cmbPositionType.getControl().isEnabled() && properties.getPingType() == null){
			return Messages.ConfigurableModelEditorConnectTab_TypeMustBeSelected; 
		}
		return null;
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

		s.saveOrUpdate(properties);
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

	private void updateDataFrequency(){
		properties.setDataFrequency(0);
		if (txtData.isEnabled()){
			try{
				int x = Integer.parseInt(txtData.getText());
				if (x >= 0){
					properties.setDataFrequency(x);
				}
			}catch (Exception ex){
				
			}
		}
	}
	
	private void updatePositionFrequency(){
		properties.setPingFrequency(0);
		if (txtPosition.isEnabled()){
			try{
				int x = Integer.parseInt(txtPosition.getText());
				if (x >= 0){
					properties.setPingFrequency(x);
				}
			}catch (Exception ex){
				
			}
		}
	}
	
	private void validateDataFrequency() {
		if (!txtData.isEnabled()){
			cdData.hide();
			return;
		}
		try{
			Integer i = Integer.parseInt(txtData.getText());
			if (i <= 0 || i > ConnectCtProperties.FREQUENCY_MAX_VALUE){
				cdData.setDescriptionText(MessageFormat.format(Messages.ConfigurableModelEditorConnectTab_InvalidInteger, ConnectCtProperties.FREQUENCY_MAX_VALUE));
				cdData.show();
			}else{
				updateDataFrequency();
				cdData.hide();
			}
		}catch (Exception ex){
			cdData.show();
			cdData.setDescriptionText(Messages.ConfigurableModelEditorConnectTab_InvalidInteger2);
			
		}
	}
	
	private void validatePositionFrequency(){
		if (!txtPosition.isEnabled()){
			cdPosition.hide();
			return;
		}
		try{
			Integer i = Integer.parseInt(txtPosition.getText());
			if (i <= 0 || i > ConnectCtProperties.FREQUENCY_MAX_VALUE){
				cdPosition.setDescriptionText(MessageFormat.format(Messages.ConfigurableModelEditorConnectTab_InvalidInteger, ConnectCtProperties.FREQUENCY_MAX_VALUE));
				cdPosition.show();
			}else{
				cdPosition.hide();
				updatePositionFrequency();
			}
		}catch (Exception ex){
			cdPosition.show();
			cdPosition.setDescriptionText(Messages.ConfigurableModelEditorConnectTab_InvalidInteger2);
			
		}
	}
}
