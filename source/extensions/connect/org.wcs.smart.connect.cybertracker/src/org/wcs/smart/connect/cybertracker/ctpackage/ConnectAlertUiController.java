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
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.common.celleditor.ComboBoxViewerCellEditor;
import org.wcs.smart.connect.SmartConnect;
import org.wcs.smart.connect.api.model.AlertType;
import org.wcs.smart.connect.cybertracker.dataentry.ConnectCmTreeContentProvider;
import org.wcs.smart.connect.cybertracker.dataentry.ConnectCmTreeElement;
import org.wcs.smart.connect.cybertracker.dataentry.ConnectCmTreeLabelProvider;
import org.wcs.smart.connect.cybertracker.model.CtPackageAlert;
import org.wcs.smart.connect.ui.server.ConnectDialog;
import org.wcs.smart.cybertracker.export.IPackageUiContribution;
import org.wcs.smart.cybertracker.model.ICmProvider;
import org.wcs.smart.cybertracker.model.ICtPackage;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.CmAttributeListItem;
import org.wcs.smart.dataentry.model.CmAttributeTreeNode;
import org.wcs.smart.dataentry.model.CmNode;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.properties.DataModelContentProvider;
import org.wcs.smart.ui.properties.DataModelLabelProvider;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Ui Controller for adding connect alerts/upload details to package
 * @author Emily
 *
 */
public class ConnectAlertUiController implements IPackageUiContribution{

	private ICtPackage ctpackage;
	
	private TableViewer alertList;
	private TreeViewer modelViewer;
	
	private Listener onModified;
	
	
	private List<CtPackageAlert> currentAlerts;
	private HashMap<ConfigurableModel, List<CtPackageAlert>> cmAlerts;
	
	private List<AlertType> types;
	
	private ConfigurableModel currentModel = null;
	
	@Inject
	private IEclipseContext context;

	private LoadAlertTypesJob loadTypesJob;
	
	
	public boolean isTab() { 
		return true; 
	}
	
	public String getTabName() { 
		return "Alerts"; 
	}
	
	@Override
	public Composite createUi(Composite parent, ICtPackage ctpackage, Listener onModified) {
		this.ctpackage = ctpackage;
		this.onModified = onModified;
		
		this.cmAlerts = new HashMap<>();
		this.currentAlerts = new ArrayList<>();
		
		loadTypesJob = new LoadAlertTypesJob(context) {
			@Override
			public void typesLoaded(List<AlertType> atypes) {
				types = atypes;
				parent.getDisplay().asyncExec(()->alertList.refresh());
			}
		};
		
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout());
		((GridLayout)main.getLayout()).marginWidth = 0;
		((GridLayout)main.getLayout()).marginHeight = 0;
		
		Composite alertComp = new Composite(main, SWT.FLAT);
		alertComp.setLayout(new GridLayout());
		((GridLayout)alertComp.getLayout()).marginWidth = 0;
		((GridLayout)alertComp.getLayout()).marginHeight = 0;
		alertComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Composite header = new Composite(alertComp, SWT.NONE);
		header.setLayout(new GridLayout());
		header.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		WidgetElement.setCSSClass(header, "SMARTSection");
		
		Label l = new Label(header, SWT.NONE);
		l.setText("Alerts");

		SashForm sash = new SashForm(alertComp, SWT.HORIZONTAL);
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
		tiAdd.setToolTipText("create a new alert");
		tiAdd.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		tiAdd.addListener(SWT.Selection, e->addAlert());
		
		ToolItem tiDelete = new ToolItem(alertToolbar, SWT.PUSH);
		tiDelete.setEnabled(false);
		tiDelete.setToolTipText("delete alert");
		tiDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		tiDelete.addListener(SWT.Selection, e->deleteAlert());
		
		Menu modelMenu = new Menu(modelViewer.getControl());
		
		MenuItem miAdd = new MenuItem(modelMenu, SWT.PUSH);
		miAdd.setText(DialogConstants.ADD_BUTTON_TEXT);
		miAdd.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		miAdd.setEnabled(false);
		miAdd.addListener(SWT.Selection, e->addAlert());
		
		modelViewer.getControl().setMenu(modelMenu);
		
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
		alertCol.getColumn().setText("Type");
		alertCol.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				CtPackageAlert pa = (CtPackageAlert)element;
				if (types == null) {
					return MessageFormat.format("<Types Not Loaded> ({0})", pa.getUuid().toString());
				}else {
					for (AlertType a : types) {
						if (a.getUuid().equals(pa.getType())) {
							return a.getLabel();
						}
					}
					//not found
					return MessageFormat.format("<Types Not Found> ({0})", pa.getUuid().toString());
				}
			}
		});
		alertCol.getColumn().pack();
		alertCol.setEditingSupport(new EditingSupport(alertCol.getViewer()) {
			
			@Override
			protected void setValue(Object element, Object value) {
				((CtPackageAlert)element).setType(((AlertType)value).getUuid());
				alertList.refresh();
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
		importanceCol.getColumn().setText("Importance");
		importanceCol.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				return String.valueOf( ((CtPackageAlert)element).getLevel() );
			}
		});
		importanceCol.setEditingSupport(new EditingSupport(importanceCol.getViewer()) {
			
			@Override
			protected void setValue(Object element, Object value) {
				((CtPackageAlert)element).setLevel(((CtPackageAlert.Level)value).value);
				alertList.refresh();
			}
			
			@Override
			protected Object getValue(Object element) {
				
				int v = ((CtPackageAlert)element).getLevel();
				for (CtPackageAlert.Level l : CtPackageAlert.Level.values()) {
					if (l.value == v) {
						return l;
					}
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
		itemCol.getColumn().setText("Alert");
		itemCol.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				return ((CtPackageAlert)element).toString();
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
		link.setText("<a>" + "refresh alert types" + "</a>");
		link.addListener(SWT.Selection, e->{
			loadTypesJob.schedule();
		});
		
		loadTypesJob.schedule();
		
		return main;
	}

	private void addAlert() {
		if (types == null || types.isEmpty()) {
			MessageDialog.openError(alertList.getControl().getShell(), "No Alert Types", "No alert types were loaded from Connect.  Ensure alert types are configured on Connect, then refresh the alert types");
			return;
		}
		Object selectedItem = modelViewer.getStructuredSelection().getFirstElement();
		
		CtPackageAlert alert = new CtPackageAlert();
		
		alert.setLevel(CtPackageAlert.Level.ONE.value);
		alert.setPackage(ctpackage);
		alert.setType(types.get(0).getUuid());//uuid
		if (selectedItem instanceof CmNode) {
			CmNode node = (CmNode)selectedItem;
			alert.setCmNode(node);
		}else if (selectedItem instanceof ConnectCmTreeElement) {
			ConnectCmTreeElement telement = (ConnectCmTreeElement)selectedItem;
			
			alert.setCmNode(telement.getAttribute().getNode());
			alert.setCmAttrubute( telement.getAttribute() );
			alert.setCmAttributeItem( telement.getElement() );
		}
		
		currentAlerts.add(alert);
		alertList.refresh();
		
		for (int i = 0; i < alertList.getTable().getColumnCount(); i ++) {
			int x = alertList.getTable().getColumn(i).getWidth();
			alertList.getTable().getColumn(i).pack();
			int x2 = alertList.getTable().getColumn(i).getWidth();
			if (x > x2) alertList.getTable().getColumn(i).setWidth(x);
		}
	}

	private void deleteAlert() {
		IStructuredSelection s = alertList.getStructuredSelection();
		for (Iterator<Object> iterator = s.iterator(); iterator.hasNext();) {
			CtPackageAlert alert = (CtPackageAlert) iterator.next();
			currentAlerts.remove(alert);
		}
		alertList.refresh();
	}
	
	private void initModel() {
		if (!(ctpackage instanceof ICmProvider)) {
			modelViewer.setInput(null);
			modelViewer.getControl().setEnabled(false);
			alertList.getControl().setEnabled(false);
		}else {
			ICmProvider cmprovider = (ICmProvider)ctpackage;
			
			modelViewer.getControl().setEnabled(true);
			alertList.getControl().setEnabled(true);
			
			if (cmprovider.isDataModel()) {
				modelViewer.setContentProvider(new DataModelContentProvider(false,  true) {
					@Override
					public Object[] getElements(Object inputElement) {
						return getChildren(super.getElements(inputElement) [0]);
					}
					
					@Override
					public Object[] getChildren(Object parentElement) {
						if (parentElement instanceof Category) {
							ArrayList<Object> children = new ArrayList<Object>();
							Category category = ((Category)parentElement);
							//add children attributes
							if (category.getActiveChildren() != null){
								children.addAll(category.getActiveChildren());
							}
							List<CategoryAttribute> all = new ArrayList<>();
							category.getAllCategoryAttribute(all, true);
							for (CategoryAttribute ca : all) {
								children.add(ca);
							}
							return children.toArray();
						}else if (parentElement instanceof CategoryAttribute) {
							Attribute a = ((CategoryAttribute)parentElement).getAttribute();
							if (a.getType() == AttributeType.LIST) {
								return a.getActiveListItems().toArray();
							}else if (a.getType() == AttributeType.TREE) {
								return a.getActiveTreeNodes().toArray();
							}
						}else if (parentElement instanceof AttributeTreeNode) {
							AttributeTreeNode a = (AttributeTreeNode)parentElement;
							return a.getActiveChildren().toArray();
						}
						
						return super.getChildren(parentElement);
					}
					public boolean hasChildren(Object element) {
						if (element instanceof Category) return true;
						if (element instanceof CategoryAttribute ) return true;
						if (element instanceof AttributeTreeNode) return !((AttributeTreeNode)element).getActiveChildren().isEmpty();
						return super.hasChildren(element);
					}
				});
				modelViewer.setLabelProvider(new DataModelLabelProvider());
			}else {
				modelViewer.setContentProvider(new ConnectCmTreeContentProvider(false));
				modelViewer.setLabelProvider(new ConnectCmTreeLabelProvider());			
			}
			loadModelJob.schedule();
		}
	}
	
	@Override
	public String isValid() {
		ConfigurableModel cm = context.get(ConfigurableModel.class);
		if (cm != null) {
			if (cm.getUuid() == null) {
				//data model; does not support alerts
				alertList.getControl().setEnabled(false);
				modelViewer.getControl().setEnabled(false);
			}else {
				
				alertList.getControl().setEnabled(true);
				modelViewer.getControl().setEnabled(true);
				
				//clear alerts
				if (currentModel != null && currentModel.equals(cm)) {
					//do nothing; same model
				}else if (currentModel != null && !currentModel.equals(cm)) {
					//different model; clear alerts and reload
					cmAlerts.put(currentModel, currentAlerts);
					currentAlerts.clear();
					if (cmAlerts.containsKey(cm)) {
						currentAlerts.addAll(cmAlerts.get(cm));
					}
					currentModel = cm;
					
					alertList.refresh();
					loadModelJob.schedule();
				}else if (currentModel == null) {
					//not set yet ignore
				}
			}
		
		}
		
		return null;
	}

	@Override
	public void updatePackage(ICtPackage ctpackage) {
		
	}
	


	private Job loadModelJob = new Job("loading data model") {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			if (!(ctpackage instanceof ICmProvider))  return Status.OK_STATUS;
			ICmProvider cmprovider = (ICmProvider)ctpackage;
			if (currentModel == null) {
				if (cmprovider.isDataModel()) {
					currentModel = new ConfigurableModel();
				}else {
					currentModel = cmprovider.getConfigurableModel();
				}
			}

			if (cmprovider.isDataModel()) {
				DataModel dm = null;
				try(Session s = HibernateManager.openSession()){
					dm = HibernateManager.loadDataModel(SmartDB.getCurrentConservationArea(), s);
					
					ArrayDeque<Category> cats = new ArrayDeque<>();
					cats.addAll(dm.getCategories());
					while(!cats.isEmpty()) {
						Category cc = cats.removeFirst();
						cc.getFullCategoryName();
						cc.getName();
						cc.getAttributes().size();
						if (cc.getActiveChildren() != null) cats.addAll(cc.getActiveChildren());
						
						if (cc.getAttributes() != null) {
							for (CategoryAttribute a : cc.getAttributes()) {
								if (a.getAttribute().getAttributeList() != null) {
									for (AttributeListItem ii : a.getAttribute().getAttributeList()) ii.getName();
								}
								if (a.getAttribute().getTree() != null) {
									ArrayDeque<AttributeTreeNode> tnodes = new ArrayDeque<>();
									tnodes.addAll(a.getAttribute().getActiveTreeNodes());
									while(!tnodes.isEmpty()) {
										AttributeTreeNode n = tnodes.removeFirst();
										n.getName();
										if (n.getActiveChildren() != null)tnodes.addAll(n.getActiveChildren());
									}
								}
							}
						}
					}
				}
				final DataModel fdm = dm;
				Display.getDefault().syncExec(()->modelViewer.setInput(fdm));
				//TODO: dm loading job
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
					modelViewer.setInput(currentModel);
				});

			}
			return Status.OK_STATUS;
		}
		
	};
	
}
