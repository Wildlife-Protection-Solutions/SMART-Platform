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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Text;
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
import org.wcs.smart.connect.SmartConnect;
import org.wcs.smart.connect.api.model.AlertType;
import org.wcs.smart.connect.cybertracker.dataentry.ConnectCmTreeContentProvider;
import org.wcs.smart.connect.cybertracker.dataentry.ConnectCmTreeElement;
import org.wcs.smart.connect.cybertracker.dataentry.ConnectCmTreeLabelProvider;
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
public class ConnectDataUiController implements IPackageUiContribution{

	private ICtPackage ctpackage;
	
	private TreeViewer modelViewer;
	private TableViewer alertList;
	
	private Listener onModified;
	
	private Button btnUploadData;
	private Label lblUp1, lblUp2;
	private Text txtDataPeriod;
	
	private Button btnPositionUpdates;
	private Label lblPos1, lblPos2;
	private Text txtPositionPeriod;
	private ComboViewer cmbPositionType;
	
	private List<AlertType> types ;
	
	@Inject
	private IEclipseContext context;
	
	private void validate() {
		if (onModified == null) return;
		onModified.handleEvent(new Event());
	}
	
	@Override
	public Composite createUi(Composite parent, ICtPackage ctpackage, Listener onModified) {
		this.ctpackage = ctpackage;
		this.onModified = onModified;
		
		
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout());
		
		Group g = new Group(main, SWT.FLAT);
		g.setText("Patrol Data Uploads");
		g.setLayout(new GridLayout(4, false));
		g.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		btnUploadData = new Button(g, SWT.CHECK);
		btnUploadData.setSelection(true);
		btnUploadData.addListener(SWT.Selection, e->{
			txtDataPeriod.setEnabled(btnUploadData.getSelection());
			lblUp1.setEnabled(btnUploadData.getSelection());
			lblUp2.setEnabled(btnUploadData.getSelection());
			validate();
		});
		
		lblUp1 = new Label(g, SWT.NONE);
		lblUp1.setText("Upload patrol data every");
		
		txtDataPeriod = new Text(g, SWT.BORDER);
		txtDataPeriod.setText("20");
		txtDataPeriod.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		((GridData)txtDataPeriod.getLayoutData()).widthHint = 30;
		txtDataPeriod.addListener(SWT.Selection, e->validate());
		
		lblUp2 = new Label(g, SWT.NONE);
		lblUp2.setText("minutes");
		
		g = new Group(main, SWT.FLAT);
		g.setText("Position Updates");
		g.setLayout(new GridLayout(5, false));
		g.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		btnPositionUpdates = new Button(g, SWT.CHECK);
		btnPositionUpdates.setSelection(false);
		btnPositionUpdates.addListener(SWT.Selection, e->{
			lblPos1.setEnabled(btnPositionUpdates.getSelection());
			lblPos2.setEnabled(btnPositionUpdates.getSelection());
			txtPositionPeriod.setEnabled(btnPositionUpdates.getSelection());
			cmbPositionType.getControl().setEnabled(btnPositionUpdates.getSelection());
			validate();
		});
		lblPos1 = new Label(g, SWT.NONE);
		lblPos1.setText("Send position updates every ");
		lblPos1.setEnabled(false);
		
		txtPositionPeriod = new Text(g, SWT.BORDER);
		txtPositionPeriod.setText("10");
		txtPositionPeriod.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		((GridData)txtDataPeriod.getLayoutData()).widthHint = 30;
		txtPositionPeriod.addListener(SWT.Modify, e->validate());
		txtPositionPeriod.setEnabled(false);
		
		lblPos2 = new Label(g, SWT.NONE);
		lblPos2.setText("minutes as type ");
		lblPos2.setEnabled(false);
		
		cmbPositionType = new ComboViewer(g, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbPositionType.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		cmbPositionType.getControl().setEnabled(false);
		cmbPositionType.setContentProvider(ArrayContentProvider.getInstance());
		cmbPositionType.setLabelProvider(new LabelProvider() {
			public String getText(Object element) {
				if (element instanceof AlertType) return ((AlertType) element).getLabel();
				return super.getText(element);
			}
		});
		cmbPositionType.setInput("Not Loaded");
		cmbPositionType.addSelectionChangedListener(e->validate());
		
		g = new Group(main, SWT.FLAT);
		g.setText("Alerts");
		g.setLayout(new GridLayout());
		g.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		SashForm sash = new SashForm(g, SWT.HORIZONTAL);
		sash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Composite listComp = new Composite(sash, SWT.NONE);
		listComp.setLayout(new GridLayout(2, false));
		((GridLayout)listComp.getLayout()).marginWidth = 0;
		((GridLayout)listComp.getLayout()).marginHeight = 0;
		
		alertList = new TableViewer(listComp, SWT.BORDER);
		alertList.setContentProvider(ArrayContentProvider.getInstance());
		alertList.getTable().setHeaderVisible(true);
		alertList.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		TableViewerColumn itemCol = new TableViewerColumn(alertList,  SWT.NONE);
		itemCol.getColumn().setText("Alert");
		itemCol.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				return element.toString();
			}
		});
		itemCol.getColumn().pack();
		
		TableViewerColumn alertCol = new TableViewerColumn(alertList,  SWT.NONE);
		alertCol.getColumn().setText("Type");
		alertCol.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				return element.toString();
			}
		});
		alertCol.getColumn().pack();
		
		TableViewerColumn importanceCol = new TableViewerColumn(alertList,  SWT.NONE);
		importanceCol.getColumn().setText("Importance");
		importanceCol.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				return element.toString();
			}
		});
		importanceCol.getColumn().pack();
		
		ToolBar alertToolbar = new ToolBar(listComp, SWT.FLAT | SWT.VERTICAL);
		alertToolbar.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
		
		ToolItem tiAdd= new ToolItem(alertToolbar, SWT.PUSH);
		tiAdd.setEnabled(false);
		tiAdd.setToolTipText("create a new alert");
		tiAdd.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		
		ToolItem tiEdit = new ToolItem(alertToolbar, SWT.PUSH);
		tiEdit.setEnabled(false);
		tiEdit.setToolTipText("edit alert settings");
		tiEdit.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		
		ToolItem tiDelete = new ToolItem(alertToolbar, SWT.PUSH);
		tiDelete.setEnabled(false);
		tiDelete.setToolTipText("delete alert");
		tiDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		
		modelViewer = new TreeViewer(sash, SWT.BORDER | SWT.V_SCROLL);
		modelViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,true));
		modelViewer.setContentProvider(new ConnectCmTreeContentProvider(false));
		modelViewer.setLabelProvider(new ConnectCmTreeLabelProvider());
		
		
		Menu modelMenu = new Menu(modelViewer.getControl());
		
		MenuItem miAdd = new MenuItem(modelMenu, SWT.PUSH);
		miAdd.setText(DialogConstants.ADD_BUTTON_TEXT);
		miAdd.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		miAdd.setEnabled(false);
		miAdd.addListener(SWT.Selection, e->addAlert());
		
		MenuItem miEdit = new MenuItem(modelMenu, SWT.PUSH);
		miEdit.setText(DialogConstants.EDIT_BUTTON_TEXT);
		miEdit.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		miEdit.setEnabled(false);
		miEdit.addListener(SWT.Selection, e->editAlert());
		
		MenuItem miDelete = new MenuItem(modelMenu, SWT.PUSH);
		miDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		miDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		miDelete.setEnabled(false);
		miDelete.addListener(SWT.Selection, e->deleteAlert());
		
		modelViewer.getControl().setMenu(modelMenu);

		modelViewer.addSelectionChangedListener(e->{
			boolean enabled = false;
			Object item = modelViewer.getStructuredSelection().getFirstElement();
			if (item instanceof Category) enabled = true;
			else if (item instanceof AttributeListItem) enabled = true;
			else if (item instanceof AttributeTreeNode) enabled = true;
			else if (item instanceof CmNode) {
				CmNode node = (CmNode)item;
				if (node.getCategory() != null) enabled = true;
			}else if (item instanceof ConnectCmTreeElement) enabled = true;
			
			tiAdd.setEnabled(enabled);
			miAdd.setEnabled(enabled);
		
		});
		
		alertList.addSelectionChangedListener(e->{
			boolean enabled = !alertList.getStructuredSelection().isEmpty();
			tiDelete.setEnabled(enabled);
			tiEdit.setEnabled(enabled);
			miDelete.setEnabled(enabled);
			miEdit.setEnabled(enabled);
		});
		sash.setWeights(new int[] {6,4});
		initModel();
		
		
		return main;
	}

	private void addAlert() {
		
	}
	private void editAlert() {
		
	}
	private void deleteAlert() {
		
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
		return null;
	}

	@Override
	public void updatePackage(ICtPackage ctpackage, Session session) {
		
	}
	
	
	public boolean isTab() { return true; }
	
	public String getTabName() { return "Connect"; }

	private Job loadModelJob = new Job("loading data model") {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			SmartConnect[] connect = new SmartConnect[] {null};
			if (context == null || context.get(SmartConnect.class) == null) {
				Display.getDefault().syncExec(()->{
					ConnectDialog cd = new ConnectDialog(Display.getCurrent().getActiveShell(), true) {
						@Override
						protected Control createDialogArea(Composite parent) {
							setTitle("Connect Alerts");
							getShell().setText("Connect Alerts");
							setMessage("Load alert types from connect");	
							return super.createDialogArea(parent);
						}	
					};
					
					if (cd.open() == Window.OK) {
						connect[0] = cd.getConnection();
						if (context != null) context.set(SmartConnect.class, connect[0]);
					}
				});
			}else {
				connect[0] = context.get(SmartConnect.class);
			}
			
			if (connect[0] != null) {
				try {
					types = connect[0].getAlertTypes();
					Display.getDefault().syncExec(()->{
						cmbPositionType.setInput(types);
					});
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			
			if (!(ctpackage instanceof ICmProvider))  return Status.OK_STATUS;
			ICmProvider cmprovider = (ICmProvider)ctpackage;

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
				ConfigurableModel cm = null;
				try(Session s = HibernateManager.openSession()){
					cm = s.get(ConfigurableModel.class, cmprovider.getConfigurableModel().getUuid());
					ArrayDeque<CmNode> nodes = new ArrayDeque<>();
					nodes.addAll(cm.getNodes());
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
				final ConfigurableModel fcm = cm;
				Display.getDefault().syncExec(()->{
					modelViewer.setInput(fcm);
				});

			}
			return Status.OK_STATUS;
		}
		
	};
}
