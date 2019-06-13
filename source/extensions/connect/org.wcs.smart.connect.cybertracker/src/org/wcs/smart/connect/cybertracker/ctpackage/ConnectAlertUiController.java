/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.connect.cybertracker.ctpackage;

import java.text.MessageFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.css.swt.dom.WidgetElement;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.common.celleditor.ComboBoxViewerCellEditor;
import org.wcs.smart.common.control.SmartUiUtils;
import org.wcs.smart.connect.api.model.AlertType;
import org.wcs.smart.connect.cybertracker.dataentry.ConnectCmTreeContentProvider;
import org.wcs.smart.connect.cybertracker.dataentry.ConnectCmTreeElement;
import org.wcs.smart.connect.cybertracker.dataentry.ConnectCmTreeLabelProvider;
import org.wcs.smart.connect.cybertracker.internal.Messages;
import org.wcs.smart.connect.cybertracker.model.CtConnectPackageMetadata;
import org.wcs.smart.connect.cybertracker.model.CtPackageAlert;
import org.wcs.smart.cybertracker.export.IPackageUiContribution;
import org.wcs.smart.cybertracker.model.AbstractCtPackage;
import org.wcs.smart.cybertracker.model.ICmProvider;
import org.wcs.smart.cybertracker.model.ICtPackage;
import org.wcs.smart.cybertracker.model.MetadataFieldValue;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.CmAttributeListItem;
import org.wcs.smart.dataentry.model.CmAttributeTreeNode;
import org.wcs.smart.dataentry.model.CmNode;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Ui Controller for adding connect alerts/upload details to package
 * @author Emily
 *
 */
public class ConnectAlertUiController implements IPackageUiContribution{

	private static final String ALERT_LBL_KEY = "ALERT_LBL"; //$NON-NLS-1$

	private ICtPackage ctpackage;
	
	private TableViewer alertList;
	private TreeViewer modelViewer;
	private Composite alertComp;
	private SashForm sash;
	
	private Listener onModified;

	private List<CtPackageAlert> currentAlerts;
	private HashMap<ConfigurableModel, List<CtPackageAlert>> cmAlerts;
	private List<AlertType> types;
	private ConfigurableModel currentModel = null;
	
	@Inject private IEclipseContext context;
	
	public boolean isTab() { 
		return true; 
	}
	
	public String getTabName() { 
		return Messages.ConnectAlertUiController_TabName; 
	}
	
	private void fireEvents() {
		if (onModified != null) onModified.handleEvent(new Event());
	}
	
	@Override
	public Composite createUi(Composite parent, ICtPackage ctpackage, Listener onModified) {
		this.ctpackage = ctpackage;
		this.onModified = onModified;
		
		this.cmAlerts = new HashMap<>();
		this.currentAlerts = new ArrayList<>();
		
		if (ctpackage instanceof ICmProvider) {
			ICmProvider cmprovider = (ICmProvider)ctpackage;
			if (cmprovider.isDataModel()) {
				currentModel = new ConfigurableModel();
			}else {
				currentModel = cmprovider.getConfigurableModel();
			}
		}
		
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout());
		((GridLayout)main.getLayout()).marginWidth = 0;
		((GridLayout)main.getLayout()).marginHeight = 0;
		
		alertComp = new Composite(main, SWT.FLAT);
		alertComp.setLayout(new GridLayout());
		((GridLayout)alertComp.getLayout()).marginWidth = 0;
		((GridLayout)alertComp.getLayout()).marginHeight = 0;
		alertComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Composite header = new Composite(alertComp, SWT.NONE);
		header.setLayout(new GridLayout());
		header.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		WidgetElement.setCSSClass(header, SmartUiUtils.HEADER_CLASS); 
		
		Label l = new Label(header, SWT.NONE);
		l.setText(Messages.ConnectAlertUiController_SectionHeader);

		sash = new SashForm(alertComp, SWT.HORIZONTAL);
		sash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Composite listComp = new Composite(sash, SWT.NONE);
		listComp.setLayout(new GridLayout(2, false));
		((GridLayout)listComp.getLayout()).marginWidth = 0;
		((GridLayout)listComp.getLayout()).marginHeight = 0;

		modelViewer = new TreeViewer(listComp, SWT.BORDER | SWT.V_SCROLL);
		modelViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,true));
		modelViewer.setContentProvider(new ConnectCmTreeContentProvider(false));
		modelViewer.setLabelProvider(new ConnectCmTreeLabelProvider());
		
		ToolBar alertToolbar = new ToolBar(listComp, SWT.FLAT | SWT.VERTICAL);
		alertToolbar.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
		
		ToolItem tiAdd= new ToolItem(alertToolbar, SWT.PUSH);
		tiAdd.setEnabled(false);
		tiAdd.setToolTipText(Messages.ConnectAlertUiController_createtooltip);
		tiAdd.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		tiAdd.addListener(SWT.Selection, e->addAlert());
		
		ToolItem tiDelete = new ToolItem(alertToolbar, SWT.PUSH);
		tiDelete.setEnabled(false);
		tiDelete.setToolTipText(Messages.ConnectAlertUiController_deletetooltip);
		tiDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		tiDelete.addListener(SWT.Selection, e->deleteAlert());
		
		Menu modelMenu = new Menu(modelViewer.getControl());
		
		MenuItem miAdd = new MenuItem(modelMenu, SWT.PUSH);
		miAdd.setText(DialogConstants.ADD_BUTTON_TEXT);
		miAdd.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		miAdd.setEnabled(false);
		miAdd.addListener(SWT.Selection, e->addAlert());
		
		modelViewer.getControl().setMenu(modelMenu);
		modelViewer.addDoubleClickListener(evt->addAlert());
		
		Composite aComp = new Composite(sash, SWT.NONE);
		aComp.setLayout(new GridLayout());
		((GridLayout)aComp.getLayout()).marginWidth = 0;
		((GridLayout)aComp.getLayout()).marginHeight = 0;
		aComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
				
		alertList = new TableViewer(aComp, SWT.BORDER | SWT.FULL_SELECTION);
		alertList.setContentProvider(ArrayContentProvider.getInstance());
		alertList.getTable().setHeaderVisible(true);
		alertList.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		alertList.setInput(currentAlerts);
		

		Menu alertMenu = new Menu(alertList.getControl());
		
		MenuItem miDelete = new MenuItem(alertMenu, SWT.PUSH);
		miDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		miDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		miDelete.setEnabled(false);
		miDelete.addListener(SWT.Selection, e->deleteAlert());
		
		alertList.getControl().setMenu(alertMenu);
				
		TableViewerColumn alertCol = new TableViewerColumn(alertList,  SWT.NONE);
		alertCol.getColumn().setText(Messages.ConnectAlertUiController_TypeColumn);
		alertCol.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				CtPackageAlert pa = (CtPackageAlert)element;
				if (types == null) {
					return MessageFormat.format(Messages.ConnectAlertUiController_TypeNotLoaded, pa.getType().toString());
				}else {
					for (AlertType a : types) {
						if (a.getUuid().equals(pa.getType())) {
							return a.getLabel();
						}
					}
					//not found
					return MessageFormat.format(Messages.ConnectAlertUiController_TypeNotFound, pa.getType().toString());
				}
			}
		});
		alertCol.getColumn().pack();
		alertCol.setEditingSupport(new EditingSupport(alertCol.getViewer()) {
			
			@Override
			protected void setValue(Object element, Object value) {
				if (value == null) return;
				((CtPackageAlert)element).setType(((AlertType)value).getUuid());
				alertList.refresh();
				fireEvents();
			}
			
			@Override
			protected Object getValue(Object element) {
				UUID at = ((CtPackageAlert)element).getType();
				for (AlertType a : types) {
					if (a.getUuid().equals(at)) return a;
				}
				return null;
			}
			
			@Override
			protected CellEditor getCellEditor(Object element) {
				ComboBoxViewerCellEditor bv = new ComboBoxViewerCellEditor((Composite) getViewer().getControl(), SWT.READ_ONLY | SWT.DROP_DOWN);
				bv.setContentProvider(ArrayContentProvider.getInstance());
				bv.setLabelProvider(new LabelProvider() {
					@Override
					public String getText(Object x) {
						return ((AlertType)x).getLabel();
					}
				});
				bv.setInput(types);
				return bv;
			}
			
			@Override
			protected boolean canEdit(Object element) {
				return true;
			}
		});
		
		TableViewerColumn importanceCol = new TableViewerColumn(alertList,  SWT.NONE);
		importanceCol.getColumn().setText(Messages.ConnectAlertUiController_ImportanceColumn);
		importanceCol.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				return String.valueOf( ((CtPackageAlert)element).getLevel().value );
			}
		});
		importanceCol.setEditingSupport(new EditingSupport(importanceCol.getViewer()) {
			
			@Override
			protected void setValue(Object element, Object value) {
				((CtPackageAlert)element).setLevel(((CtPackageAlert.Level)value));
				alertList.refresh();
				fireEvents();
			}
			
			@Override
			protected Object getValue(Object element) {
				return ((CtPackageAlert)element).getLevel();
			}
			
			@Override
			protected CellEditor getCellEditor(Object element) {
				ComboBoxViewerCellEditor bv = new ComboBoxViewerCellEditor((Composite) getViewer().getControl(), SWT.READ_ONLY | SWT.DROP_DOWN);
				bv.setContentProvider(ArrayContentProvider.getInstance());
				bv.setLabelProvider(new LabelProvider() {
					@Override
					public String getText(Object x) {
						return String.valueOf ( ((CtPackageAlert.Level)x).value );
					}
				});
				bv.setInput(CtPackageAlert.Level.values());
				return bv;
			}
			
			@Override
			protected boolean canEdit(Object element) {
				return true;
			}
		});
		importanceCol.getColumn().pack();
		
		TableViewerColumn itemCol = new TableViewerColumn(alertList,  SWT.NONE);
		itemCol.getColumn().setText(Messages.ConnectAlertUiController_ObsColumn);
		itemCol.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				CtPackageAlert alert = ((CtPackageAlert)element);
				
				StringBuilder sb = new StringBuilder();
				if (alert.getCmAttributeListItem()  != null) {
					CmAttributeListItem i = alert.getCmAttributeListItem();
					if (i.getName() != null) {
						sb.append(i.getName());
					}else {
						sb.append(i.getListItem().getName());
					}
					sb.append(" ("); //$NON-NLS-1$
				}
				if (alert.getCmAttributeTreeNode()  != null) {
					CmAttributeTreeNode i = alert.getCmAttributeTreeNode();
					if (i.getName() != null) {
						sb.append(i.getName());
					}else {
						sb.append(i.getDmTreeNode().getName());
					}
					sb.append(" ("); //$NON-NLS-1$
				}

					
				
				if (alert.getCmAttribute() != null) {
					if (alert.getCmAttribute().getName() != null) {
						sb.append(alert.getCmAttribute().getName());
					}else {
						sb.append(alert.getCmAttribute().getAttribute().getName());
					}
					if (alert.getCmAttributeListItem() != null || alert.getCmAttributeTreeNode() != null) {
						sb.append(")"); //$NON-NLS-1$
					}
					sb.append(" -> "); //$NON-NLS-1$
				}
				
				
				CmNode node = alert.getCmNode();
				while(node != null) {
					if (node!= null) {
						if (node.getName() != null) {
							sb.append(node.getName());
						}else if (node.getCategory() != null) {
							sb.append(node.getCategory().getName());
						}
						sb.append(" -> "); //$NON-NLS-1$
					}
					node = node.getParent();
				}
				if (sb.length() < 4) return sb.toString();
				return sb.substring(0, sb.length() - 4);
			}
		});
		itemCol.getColumn().setWidth(200);
		

		modelViewer.addSelectionChangedListener(e->{
			boolean enabled = false;
			Object item = modelViewer.getStructuredSelection().getFirstElement();
			if (item instanceof Category) enabled = true;
			else if (item instanceof AttributeListItem) enabled = true;
			else if (item instanceof AttributeTreeNode) enabled = true;
			else if (item instanceof CmNode) enabled = true;
			else if (item instanceof ConnectCmTreeElement) enabled = true;
			
			tiAdd.setEnabled(enabled);
			miAdd.setEnabled(enabled);
		
		});
		
		alertList.addSelectionChangedListener(e->{
			boolean enabled = !alertList.getStructuredSelection().isEmpty();
			tiDelete.setEnabled(enabled);
			miDelete.setEnabled(enabled);
			
		});
		sash.setWeights(new int[] {2,3});
		initModel();
		
		Link link = new Link(main, SWT.NONE);
		link.setText("<a>" + Messages.ConnectAlertUiController_RefreshLink + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
		link.addListener(SWT.Selection, e->{
			refreshAlertTypes(true);
		});
		
		loadAlerts.schedule();
		refreshAlertTypes(false);
		
		return main;
	}
	
	private void refreshAlertTypes(boolean force) {
		(new LoadAlertTypesJob(context, force) {
			@Override
			public void typesLoaded(List<AlertType> atypes) {
				types = atypes;
				alertList.getControl().getDisplay().asyncExec(()->alertList.refresh());
			}
		}).schedule();
	}

	private void addAlert() {
		if (types == null || types.isEmpty()) {
			MessageDialog.openError(alertList.getControl().getShell(), Messages.ConnectAlertUiController_NoTypesTitle, Messages.ConnectAlertUiController_NoTypesMessage);
			return;
		}
		Object selectedItem = modelViewer.getStructuredSelection().getFirstElement();
		
		boolean isok = false;
		if (selectedItem instanceof Category) isok = true;
		else if (selectedItem instanceof AttributeListItem) isok = true;
		else if (selectedItem instanceof AttributeTreeNode) isok = true;
		else if (selectedItem instanceof CmNode) isok = true;
		else if (selectedItem instanceof ConnectCmTreeElement) isok = true;
		//cannot create an alert for this type
		if (!isok) return;
		
		
		CtPackageAlert alert = new CtPackageAlert();
		
		alert.setLevel(CtPackageAlert.Level.ONE);
		alert.setPackage(ctpackage);
		alert.setType(types.get(0).getUuid());//uuid
		if (selectedItem instanceof CmNode) {
			CmNode node = (CmNode)selectedItem;
			alert.setCmNode(node);
		}else if (selectedItem instanceof ConnectCmTreeElement) {
			ConnectCmTreeElement telement = (ConnectCmTreeElement)selectedItem;
			
			alert.setCmNode(telement.getAttribute().getNode());
			alert.setCmAttribute( telement.getAttribute() );
			if (telement.getElement() instanceof CmAttributeListItem) {
				alert.setCmAttributeListItem( (CmAttributeListItem)telement.getElement() );
			}else if (telement.getElement() instanceof CmAttributeTreeNode) {
				alert.setCmAttributeTreeNode( (CmAttributeTreeNode)telement.getElement() );
			}
		}
		
		currentAlerts.add(alert);
		alertList.refresh();
		
		for (int i = 0; i < alertList.getTable().getColumnCount(); i ++) {
			int x = alertList.getTable().getColumn(i).getWidth();
			alertList.getTable().getColumn(i).pack();
			int x2 = alertList.getTable().getColumn(i).getWidth();
			if (x > x2) alertList.getTable().getColumn(i).setWidth(x);
		}
		
		fireEvents();
	}

	private void deleteAlert() {
		IStructuredSelection s = alertList.getStructuredSelection();
		for (Iterator<?> iterator = s.iterator(); iterator.hasNext();) {
			CtPackageAlert alert = (CtPackageAlert) iterator.next();
			currentAlerts.remove(alert);
		}
		alertList.refresh();
		fireEvents();
	}
	
	private void initModel() {

		if (currentModel == null || currentModel.getUuid() == null) {
			//data model does not support alergs
			alertList.getControl().setEnabled(false);
			modelViewer.getControl().setEnabled(false);
		
			if (alertComp.getData(ALERT_LBL_KEY) == null) {
				Label warning = new Label(alertComp, SWT.WRAP);
				warning.setBackground(warning.getDisplay().getSystemColor( SWT.COLOR_TRANSPARENT));
				warning.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				((GridData)warning.getLayoutData()).widthHint = 200;
				warning.setText(Messages.ConnectAlertUiController_AlertsNotSupported);
				warning.moveAbove(sash);
				
				alertComp.setData(ALERT_LBL_KEY, warning);
				
				alertComp.layout(true);
			}
		}else {
			alertList.getControl().setEnabled(true);
			modelViewer.getControl().setEnabled(true);
			modelViewer.setInput(DialogConstants.LOADING_TEXT);
			modelViewer.refresh();
			if (alertComp.getData(ALERT_LBL_KEY) != null) {
				((Label)alertComp.getData(ALERT_LBL_KEY)).dispose();
				alertComp.setData(ALERT_LBL_KEY, null);
				alertComp.layout(true);
			}
			
		}
		loadModelJob.schedule();
		
	}
	
	@Override
	public String isValid() {
		
		//update configurable model viewer
		ConfigurableModel cm = context.get(ConfigurableModel.class);
		if (cm != null) {
			//first cache old alerts
			if (currentModel != null && currentModel.equals(cm)) {
				//do nothing; same model
			}else if (currentModel != null && !currentModel.equals(cm)) {
				//different model; clear alerts and reload
				cmAlerts.put(currentModel, new ArrayList<>(currentAlerts));
				currentAlerts.clear();
				if (cmAlerts.containsKey(cm)) {
					currentAlerts.addAll(cmAlerts.get(cm));
				}
				currentModel = cm;
				alertList.refresh();
				initModel();
			}else if (currentModel == null) {
				//not set yet ignore
			}
		}
		
		return null;
	}

	@Override
	public void updatePackage(ICtPackage ctpackage) {
		if (!(ctpackage instanceof AbstractCtPackage)) return;
		
		AbstractCtPackage apackage = (AbstractCtPackage)ctpackage;
		//remove existing metadata values and add new ones
		List<MetadataFieldValue> existing = new ArrayList<>();
		for (MetadataFieldValue vv : apackage.getMetadataValues()) {
			if (vv.getMetadataKey().equals(CtConnectPackageMetadata.Properties.CONNECT_ALERT.name())) {
				existing.add(vv);
			}
		}
		
		List<MetadataFieldValue> newvalues = new ArrayList<>();
		for (CtPackageAlert pa : currentAlerts) {
			MetadataFieldValue v = pa.toMetadataField();
			v.setCtPackage(apackage);
			newvalues.add(v);
		}
		
		apackage.getMetadataValues().removeAll(existing);
		apackage.getMetadataValues().addAll(newvalues);
		
	}
	

	private Job loadAlerts = new Job(Messages.ConnectAlertUiController_LoadingAlerts) {
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			try(Session session = HibernateManager.openSession()){
				currentAlerts.addAll(CtPackageAlert.fromString((AbstractCtPackage)ctpackage, session));
			}
			Display.getDefault().asyncExec(()->{alertList.refresh();});
			return Status.OK_STATUS;
		}
	};
	
	private Job loadModelJob = new Job(Messages.ConnectAlertUiController_LoadingDataModel) {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			if (currentModel.getUuid() == null) {
				//data model
				
				Display.getDefault().syncExec(()->{
					modelViewer.setInput(Messages.ConnectAlertUiController_NotSupported);	
				});

			}else {
				try(Session s = HibernateManager.openSession()){
					currentModel = s.get(ConfigurableModel.class, currentModel.getUuid());
					currentModel.getUuid();
					ArrayDeque<CmNode> nodes = new ArrayDeque<>();
					nodes.addAll(currentModel.getNodes());
					while(!nodes.isEmpty()) {
						CmNode n = nodes.removeFirst();
						if (n.getCategory() != null) n.getCategory().getFullCategoryName();
						if (n.getCmAttributes() != null) {
							for (CmAttribute a : n.getCmAttributes()) {
								a.getAttribute().getName();
								if (a.getCurrentList() != null) {
									for (CmAttributeListItem li : a.getCurrentList()) {
										li.getListItem().getNames().size();
										li.getNames().size();
										li.getImageFile();
										if (li.getListItem().getIcon() != null) {
											li.getListItem().getIcon().getFiles().forEach(f->{
												li.getListItem().getIcon().getIconFile(f.getIconSet()).computeFileLocation(s);
											});
										}
									}
								}
								if (a.getCurrentTree() != null) {
									ArrayDeque<CmAttributeTreeNode> tnodes = new ArrayDeque<>();
									tnodes.addAll(a.getCurrentTree());
									while(!tnodes.isEmpty()) {
										CmAttributeTreeNode nn = tnodes.removeFirst();
										nn.getDmTreeNode().getNames().size();
										nn.getNames().size();
										if (nn.getDmTreeNode().getIcon() != null) {
											
											nn.getDmTreeNode().getIcon().getFiles().forEach(f->{
												nn.getDmTreeNode().getIcon().getIconFile(f.getIconSet()).computeFileLocation(s);
											});
										}
										nn.getImageFile();
										if (nn.getChildren() != null) tnodes.addAll(nn.getChildren());
									}

								}
							}
						}
						if (n.getChildren() != null) nodes.addAll(n.getChildren());
					}
				}
				
				Display.getDefault().syncExec(()->{
					if (modelViewer.getControl().isDisposed()) return;
					modelViewer.setLabelProvider(new ConnectCmTreeLabelProvider());
					modelViewer.setInput(currentModel);
				});

			}
			return Status.OK_STATUS;
		}
		
	};
	
}
