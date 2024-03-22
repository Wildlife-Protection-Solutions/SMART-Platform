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
package org.wcs.smart.dataentry.dialog;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.dataentry.CmDefaultListsUtil;
import org.wcs.smart.dataentry.CmDefaultTreesUtil;
import org.wcs.smart.dataentry.DataentryHibernateManager;
import org.wcs.smart.dataentry.dialog.ConfigurableModelTreeContentProvider.CmRootNode;
import org.wcs.smart.dataentry.dialog.ConfigurableModelTreeContentProvider.MatrixNode;
import org.wcs.smart.dataentry.dialog.composite.AbstractInfoComposite;
import org.wcs.smart.dataentry.dialog.composite.AbstractInfoComposite.IModelChangedListener;
import org.wcs.smart.dataentry.dialog.composite.BooleanAttributeInfoComposite;
import org.wcs.smart.dataentry.dialog.composite.CmAttributeInfoComposite;
import org.wcs.smart.dataentry.dialog.composite.CmNodeInfoComposite;
import org.wcs.smart.dataentry.dialog.composite.CmRootNodeInfoComposite;
import org.wcs.smart.dataentry.dialog.composite.DateAttributeInfoComposite;
import org.wcs.smart.dataentry.dialog.composite.GeometryAttributeInfoComposite;
import org.wcs.smart.dataentry.dialog.composite.ListAttributeInfoComposite;
import org.wcs.smart.dataentry.dialog.composite.MListAttributeInfoComposite;
import org.wcs.smart.dataentry.dialog.composite.NumericAttributeInfoComposite;
import org.wcs.smart.dataentry.dialog.composite.TextAttributeInfoComposite;
import org.wcs.smart.dataentry.dialog.composite.TreeAttributeInfoComposite;
import org.wcs.smart.dataentry.internal.Messages;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.CmAttributeConfig;
import org.wcs.smart.dataentry.model.CmNode;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.SectionHeader;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.ui.properties.LanguageViewer;

/**
 * Tab with default content for configurable model.
 * If not additional tabs provided this will be the only content displayed (without tab itself).
 * 
 * @author elitvin
 * @since 4.0.0
 */
public class ConfigurableModelEditorDefaultTab implements IConfigurableModelEditorTabContent {

	private static final long MAX_MATRIX_COMBOS = 200;
	
	public static enum ControlButton {
		ADD_GROUP(Messages.AbstractInfoComposite_Button_AddGroup),
		ADD_CATEGORY(Messages.AbstractInfoComposite_Button_AddCategory), 
		DELETE(DialogConstants.DELETE_BUTTON_TEXT);
		
		public String name;
		
		ControlButton(String name){
			this.name = name;
		}
		public Image getImage() {
			if (this == ControlButton.ADD_CATEGORY || this == ControlButton.ADD_GROUP) {
				return SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON);
			}else if (this == ControlButton.DELETE) {
				return SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON);
			}
			return null;
		}
	};
	
	private ConfigurableModelEditDialog dialog;
	
	private LanguageViewer languageViewer;
	private TreeViewer modelTreeViewer;

	private Composite infoInnerPanel;
	private Composite emptyComposite;
	private CmRootNodeInfoComposite rootNodeComposite;
	private CmNodeInfoComposite groupNodeComposite;
	private CmNodeInfoComposite categoryNodeComposite;
	private Map<AttributeType, CmAttributeInfoComposite> attributeComposites;
	
	private HashMap<ControlButton, Button> controlButtons = new HashMap<ControlButton, Button>();
	
//	private List<CmAttributeConfig> deletedConfigs = new ArrayList<>();
//	private List<CmAttributeConfig> addedConfigs = new ArrayList<>();
	
	
	private ScrolledComposite propertiesComp, helpComp;
	private Composite  stackPanel;
	private SectionHeader propertySectionHeader; 
	private HelpContentComposite helpPanel;
	
	public ConfigurableModelEditorDefaultTab(ConfigurableModelEditDialog dialog) {
		this.dialog = dialog;
	}

	@Override
	public void setDialog(ConfigurableModelEditDialog dialog) {
		this.dialog = dialog;
	}
	
	@Override
	public String getTabName() {
		return Messages.ConfigurableModelEditorDefaultTab_TabName;
	}

	private void selectPropertiesPage() {
		((StackLayout)stackPanel.getLayout()).topControl = propertiesComp;
		stackPanel.layout(true);
	}
	private void selectHelpPage() {
		((StackLayout)stackPanel.getLayout()).topControl = helpComp;
		stackPanel.layout(true);
	}
	
	@Override
	public Composite createTabContent(Composite parent) {
		ConfigurableModel model = dialog.getModel();
		SashForm container = new SashForm(parent, SWT.HORIZONTAL);
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite innerLeft = new Composite(container, SWT.NONE);
		innerLeft.setLayout(new GridLayout());
		
		languageViewer = new LanguageViewer(innerLeft, SWT.DROP_DOWN | SWT.READ_ONLY, SmartDB.getCurrentConservationArea());
		languageViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		modelTreeViewer = new TreeViewer(innerLeft, SWT.V_SCROLL | SWT.H_SCROLL| SWT.BORDER | SWT.MULTI);
		modelTreeViewer.setLabelProvider(new ConfigurableModelLabelProvider());
		modelTreeViewer.setContentProvider(new ConfigurableModelTreeContentProvider(true));
		modelTreeViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)modelTreeViewer.getControl().getLayoutData()).widthHint = 100;
		((GridData)modelTreeViewer.getControl().getLayoutData()).heightHint = 100;
		modelTreeViewer.setInput(model);
		modelTreeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				updateRightPanelState();
			}
		});
		Transfer[] transferTypes = new Transfer[]{LocalSelectionTransfer.getTransfer()};
		modelTreeViewer.addDragSupport(DND.DROP_MOVE, transferTypes , new ConfigurableModelTreeDragListener(modelTreeViewer));
		modelTreeViewer.addDropSupport(DND.DROP_MOVE, transferTypes, new ConfigurableModelTreeDropListener(modelTreeViewer) {
			@Override
			public boolean performDrop(Object data) {
				boolean ok = super.performDrop(data);
				if (ok) {
					dialog.notifyChangesMade();
					updateRightPanelState();
				}
				return ok;
			}
		});
		modelTreeViewer.expandToLevel(2);
		
		
		
		
		languageViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				((ConfigurableModelLabelProvider)modelTreeViewer.getLabelProvider()).setLanguage(languageViewer.getCurrentSelection());
				modelTreeViewer.refresh(true);
				updateRightPanelState();
			}
		});
		
		Composite rightPanel = new Composite(container, SWT.NONE);
		rightPanel.setLayout(new GridLayout(1, false));
		
		
		Composite buttonPanel = new Composite(rightPanel, SWT.NONE);
		buttonPanel.setLayout(new GridLayout(3, false));
		((GridLayout)buttonPanel.getLayout()).marginHeight = 0;
		((GridLayout)buttonPanel.getLayout()).marginWidth = 0;
		buttonPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		for (final ControlButton cbtn : ControlButton.values()){
			Button btn = new Button(buttonPanel, SWT.PUSH);
			btn.setText(cbtn.name);
			btn.setBackground(buttonPanel.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
			
			if (cbtn == ControlButton.ADD_CATEGORY || cbtn == ControlButton.ADD_GROUP) {
				btn.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
			}else if (cbtn == ControlButton.DELETE) {
				btn.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
			}
			
			btn.addListener(SWT.Selection, e->doControlButtonPress(cbtn));				
			btn.setEnabled(false);
			dialog.setButtonLayoutData(btn);
			controlButtons.put(cbtn,btn);
		}
		
		Menu treeMenu = new Menu(modelTreeViewer.getControl());
		
		
		treeMenu.addMenuListener(new MenuListener() {
			
			@Override
			public void menuShown(MenuEvent e) {
				boolean canAttributeGroup = !modelTreeViewer.getStructuredSelection().isEmpty();
				boolean canAttributeUngroup = false;
				
				boolean isVisible = true;
				
				CmNode parent = null;
				
				boolean canDelete = false;
				boolean canAddCategory = false;
				boolean canAddGroup = false;
				
				if (modelTreeViewer.getStructuredSelection().size() == 1) {
					Object item = modelTreeViewer.getStructuredSelection().getFirstElement();
					
					if (item instanceof CmNode && ((CmNode)item).getCategory() == null) {
						canAddCategory = true;
						canAddGroup = true;
					}
					if (item instanceof ConfigurableModelTreeContentProvider.CmRootNode) {
						canAddCategory = true;
						canAddGroup = true;
					}
				}
				
				List<CmAttribute> selectedItems = new ArrayList<>();
				for (Iterator<?> iterator2 = modelTreeViewer.getStructuredSelection().iterator(); iterator2.hasNext();) {
					
					Object item = iterator2.next();
					
					if (item instanceof CmNode) canDelete = true;
					
					if (!(item instanceof CmAttribute)) {
						isVisible = false;
						break;
					}
					
					CmAttribute attribute = (CmAttribute)item;
					
					if (attribute.isGrouped()) {
						canAttributeUngroup = true;
						canAttributeGroup = false;
					}
					
					if (parent == null) {
						parent = attribute.getNode();
					}else {
						if (parent != attribute.getNode()) {
							canAttributeGroup = false;
						}
					}
					if (attribute.getAttribute().getType() == Attribute.AttributeType.DATE ||
							attribute.getAttribute().getType() == Attribute.AttributeType.TREE ||
							attribute.getAttribute().getType() == Attribute.AttributeType.MLIST) {
						canAttributeGroup = false;
					}else {
						selectedItems.add(attribute);
					}
				}

				//can only group if the group has at least one list or we are adding a lit
				if (isVisible && canAttributeGroup) {
					canAttributeGroup = ((ConfigurableModelTreeContentProvider)modelTreeViewer.getContentProvider()).isGroupValid(parent, selectedItems, null);
				}
				if (isVisible && canAttributeUngroup) {
					canAttributeUngroup = ((ConfigurableModelTreeContentProvider)modelTreeViewer.getContentProvider()).isGroupValid(parent, null, selectedItems);
				}
				
				for (MenuItem mi : treeMenu.getItems()) mi.dispose();
				
				if (canAddCategory) {
					MenuItem mi = new MenuItem(treeMenu, SWT.PUSH);
					mi.setText(ControlButton.ADD_CATEGORY.name);
					mi.addListener(SWT.Selection, evt->doControlButtonPress(ControlButton.ADD_CATEGORY));
					mi.setImage(ControlButton.ADD_CATEGORY.getImage());
				}
				if (canAddGroup) {
					MenuItem mi = new MenuItem(treeMenu, SWT.PUSH);
					mi.setText(ControlButton.ADD_GROUP.name);
					mi.addListener(SWT.Selection, evt->doControlButtonPress(ControlButton.ADD_GROUP));
					mi.setImage(ControlButton.ADD_GROUP.getImage());
				}
				
				if ( isVisible ) {
					MenuItem miAddGroup = new MenuItem(treeMenu, SWT.PUSH);
					miAddGroup.setText(Messages.ConfigurableModelEditorDefaultTab_AddToAttributeGroup);
					miAddGroup.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
					miAddGroup.addListener(SWT.Selection, lt->groupSelectedAttributes());
					miAddGroup.setEnabled(canAttributeGroup);
				}
				if ( isVisible ) {
					MenuItem miRemoveGroup = new MenuItem(treeMenu, SWT.PUSH);
					miRemoveGroup.setText(Messages.ConfigurableModelEditorDefaultTab_RemoveFromAttributeEGroup);
					miRemoveGroup.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
					miRemoveGroup.addListener(SWT.Selection, lt->unGroupSelectedAttributes());
					miRemoveGroup.setEnabled(canAttributeUngroup);
				}
				
				if (canDelete) {
					if (treeMenu.getItemCount() > 1) new MenuItem(treeMenu, SWT.SEPARATOR);
					
					MenuItem mi = new MenuItem(treeMenu, SWT.PUSH);
					mi.setText(ControlButton.DELETE.name);
					mi.addListener(SWT.Selection, evt->doDeleteNodes());
					mi.setImage(ControlButton.DELETE.getImage());
				}
				
			}
			
			@Override
			public void menuHidden(MenuEvent e) {
			}
		});
			
		
		modelTreeViewer.getControl().setMenu(treeMenu);
		
		
		propertySectionHeader = new SectionHeader(rightPanel, SWT.None, 
				new String[] {Messages.ConfigurableModelEditorDefaultTab_PropertiesTabLabel, Messages.ConfigurableModelEditorDefaultTab_HelpContentTabLabel},
				new Listener[] {
						e->selectPropertiesPage(),
						e->selectHelpPage()
				});
		propertySectionHeader.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		stackPanel = new Composite(rightPanel, SWT.NONE);
		stackPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		stackPanel.setLayout(new StackLayout());
		
		propertiesComp = new ScrolledComposite(stackPanel, SWT.V_SCROLL | SWT.H_SCROLL );
		propertiesComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		// always show the focus control
		propertiesComp.setShowFocusedControl(true);
		propertiesComp.setExpandHorizontal(true);
		propertiesComp.setExpandVertical(true);
		
		infoInnerPanel = new Composite(propertiesComp, SWT.NONE);

		StackLayout layout = new StackLayout();
		layout.marginHeight = 2;
		infoInnerPanel.setLayout(layout);
		infoInnerPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		emptyComposite = new Composite(infoInnerPanel, SWT.NONE);
		emptyComposite.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
		
		IModelChangedListener modelChangeListener = new IModelChangedListener() {
			@Override
			public void modelChanged() {
				modified();
			}
		};

		//NOTE: session is already opened and should not be closed until dialog is closed
		
		rootNodeComposite = new CmRootNodeInfoComposite(infoInnerPanel, model, dialog.getSession());
		rootNodeComposite.addModelChangedListener(modelChangeListener);

		groupNodeComposite = new CmNodeInfoComposite(infoInnerPanel, model, dialog.getSession(), true);
		groupNodeComposite.addModelChangedListener(modelChangeListener);
		
		categoryNodeComposite = new CmNodeInfoComposite(infoInnerPanel, model, dialog.getSession(), false);
		categoryNodeComposite.addModelChangedListener(modelChangeListener);

		attributeComposites = new HashMap<AttributeType, CmAttributeInfoComposite>();
		CmAttributeInfoComposite attrComposite;

		attrComposite = new NumericAttributeInfoComposite(infoInnerPanel, model, dialog.getSession());
		attrComposite.addModelChangedListener(modelChangeListener);
		attributeComposites.put(AttributeType.NUMERIC, attrComposite);
		
		attrComposite = new TextAttributeInfoComposite(infoInnerPanel, model, dialog.getSession());
		attrComposite.addModelChangedListener(modelChangeListener);
		attributeComposites.put(AttributeType.TEXT, attrComposite);

		attrComposite = new ListAttributeInfoComposite(infoInnerPanel, dialog);
		attrComposite.addModelChangedListener(modelChangeListener);
		attributeComposites.put(AttributeType.LIST, attrComposite);

		attrComposite = new MListAttributeInfoComposite(infoInnerPanel, dialog);
		attrComposite.addModelChangedListener(modelChangeListener);
		attributeComposites.put(AttributeType.MLIST, attrComposite);
		
		attrComposite = new TreeAttributeInfoComposite(infoInnerPanel, dialog);
		attrComposite.addModelChangedListener(modelChangeListener);
		attributeComposites.put(AttributeType.TREE, attrComposite);

		attrComposite = new GeometryAttributeInfoComposite(infoInnerPanel, model, dialog.getSession());
		attrComposite.addModelChangedListener(modelChangeListener);
		attributeComposites.put(AttributeType.LINE, attrComposite);
		
		attrComposite = new GeometryAttributeInfoComposite(infoInnerPanel, model, dialog.getSession());
		attrComposite.addModelChangedListener(modelChangeListener);
		attributeComposites.put(AttributeType.POLYGON, attrComposite);
		
		attrComposite = new BooleanAttributeInfoComposite(infoInnerPanel, model, dialog.getSession());
		attrComposite.addModelChangedListener(modelChangeListener);
		attributeComposites.put(AttributeType.BOOLEAN, attrComposite);

		attrComposite = new DateAttributeInfoComposite(infoInnerPanel, model, dialog.getSession());
		attrComposite.addModelChangedListener(modelChangeListener);
		attributeComposites.put(AttributeType.DATE, attrComposite);
		
		container.setWeights(new int[]{40,60});
		

		propertiesComp.setContent(infoInnerPanel);
		propertiesComp.setMinSize(infoInnerPanel.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		
		
		
		helpComp = new ScrolledComposite(stackPanel, SWT.V_SCROLL | SWT.H_SCROLL );
		helpComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		// always show the focus control
		helpComp.setShowFocusedControl(true);
		helpComp.setExpandHorizontal(true);
		helpComp.setExpandVertical(true);
		
		helpPanel = new HelpContentComposite(helpComp, ()->dialog.notifyChangesMade());
		
		helpComp.setContent(helpPanel);
		helpComp.setMinSize(helpPanel.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		
		propertySectionHeader.selectPanel(0);
		
		//set  language
		((ConfigurableModelLabelProvider)modelTreeViewer.getLabelProvider()).setLanguage(languageViewer.getCurrentSelection());
		modelTreeViewer.refresh();
		updateRightPanelState();
		
		return container;
	}

	private void modified() {
		dialog.notifyChangesMade();
		modelTreeViewer.refresh();
		dialog.getSession().flush();
	}
	
	private void doDeleteNodes() {
		IStructuredSelection selection = modelTreeViewer.getStructuredSelection();
		List<CmNode> toDelete = new ArrayList<>();
		for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
			Object item = iterator.next();
			if (item instanceof CmNode) {
				toDelete.add((CmNode)item);
			}
		}
		if (toDelete.isEmpty()) return;
		
		if (!MessageDialog.openQuestion(dialog.getShell(), "Delete", "Are you sure you want to removed the selected nodes. This action cannot be undone.")) {
			return;
		}
		
		ProgressMonitorDialog ddialog = new ProgressMonitorDialog(dialog.getShell());
		try {
			ddialog.run(true, false, monitor->{
				monitor.beginTask("Deleting selected nodes", toDelete.size() + 2);
				for (CmNode node : toDelete) {
					monitor.subTask(node.getName());
					deleteNode(node, dialog.getSession());
					monitor.worked(1);
				}
				monitor.subTask("saving...");
				dialog.getSession().flush();
				monitor.done();
			});
		}catch (Exception ex) {
			SmartPlugIn.log(ex.getMessage(), ex);
		}

		
		modified();
	}
	
	
	private void groupSelectedAttributes() {

		List<CmAttribute> items = new ArrayList<>();
		for (Iterator<?> iterator = modelTreeViewer.getStructuredSelection().iterator(); iterator.hasNext();) {
			Object item = iterator.next();
			if (!(item instanceof CmAttribute)) continue;
			items.add((CmAttribute) item);
		}
		if (items.isEmpty()) return;
		
		((ConfigurableModelTreeContentProvider)modelTreeViewer.getContentProvider()).addToGroup(items, true);
		modelTreeViewer.refresh();
		
		dialog.notifyChangesMade();
	}
	
	private void unGroupSelectedAttributes() {
		List<CmAttribute> items = new ArrayList<>();
		for (Iterator<?> iterator = modelTreeViewer.getStructuredSelection().iterator(); iterator.hasNext();) {
			Object item = iterator.next();
			if (!(item instanceof CmAttribute)) continue;
			items.add((CmAttribute) item);			
		}
		if (items.isEmpty()) return;
		
		((ConfigurableModelTreeContentProvider)modelTreeViewer.getContentProvider()).removeFromGroup(items, true);
		modelTreeViewer.refresh();
		
		dialog.notifyChangesMade();
	}
	
	private void doControlButtonPress(ControlButton cbtn) {
		Control currentPanel = ((StackLayout)infoInnerPanel.getLayout()).topControl;
		if (currentPanel instanceof AbstractInfoComposite){
			((AbstractInfoComposite)currentPanel).processButton(cbtn);
			
			//we we add or remove something make sure the current selection is expanded
			Object x = ((StructuredSelection)modelTreeViewer.getSelection()).getFirstElement();
			if (x != null) modelTreeViewer.setExpandedState(x, true);
		}
	}
	
	
	private void updateRightPanelState() {
		IStructuredSelection selection = (IStructuredSelection) modelTreeViewer.getSelection();
		Object obj = selection.getFirstElement();

		if (obj instanceof CmNode) {
			CmNode node = (CmNode) obj;
			CmNodeInfoComposite cmp = node.isGroup() ? groupNodeComposite : categoryNodeComposite;
			cmp.setSourceObject(node, languageViewer.getCurrentSelection());
			((StackLayout)infoInnerPanel.getLayout()).topControl = cmp;
			propertySectionHeader.selectPanel(0);
			propertySectionHeader.enableTab(1, false);
			helpPanel.setAttribute(null);
		} else if (obj instanceof CmAttribute) {
			CmAttribute attr = (CmAttribute)obj;
			CmAttributeInfoComposite attrComposite = attributeComposites.get(attr.getAttribute().getType());
			if (attrComposite != null) {
				attrComposite.setSourceObject((CmAttribute)obj, languageViewer.getCurrentSelection());
				((StackLayout)infoInnerPanel.getLayout()).topControl = attrComposite;
			} else {
				((StackLayout)infoInnerPanel.getLayout()).topControl = emptyComposite;
			}
			propertySectionHeader.enableTab(1, true);
			helpPanel.setAttribute((CmAttribute)obj);
		} else if (obj instanceof CmRootNode) {
			rootNodeComposite.setSourceObject((CmRootNode)obj, languageViewer.getCurrentSelection());
			((StackLayout)infoInnerPanel.getLayout()).topControl = rootNodeComposite;
			propertySectionHeader.selectPanel(0);
			propertySectionHeader.enableTab(1, false);
			helpPanel.setAttribute(null);
		} else {
			((StackLayout)infoInnerPanel.getLayout()).topControl = emptyComposite;
			propertySectionHeader.selectPanel(0);
			propertySectionHeader.enableTab(1, false);
			helpPanel.setAttribute(null);
		}
		infoInnerPanel.layout();
		
		//update buttons
		Control control = ((StackLayout)infoInnerPanel.getLayout()).topControl;
		if (control instanceof AbstractInfoComposite){
			for (Iterator<Entry<ControlButton, Button>> iterator = controlButtons.entrySet().iterator(); iterator.hasNext();) {
				Entry<ControlButton, Button> type = (Entry<ControlButton, Button>) iterator.next();
				type.getValue().setEnabled( ((AbstractInfoComposite)control).isButtonValid(type.getKey()));
			}
		}else{
			for (Button btn : controlButtons.values()){
				btn.setEnabled(false);				
			}
		}
//		
//		for (Button btn : controlButtons.values()){
//			((MenuItem)btn.getData(MENU_ITEM_KEY)).setEnabled(btn.isEnabled());
//		}
	}
	
	@Override
	public void performSave(Session s) {
		s.flush();
		if (dialog.getModel().getUuid() == null) s.persist(dialog.getModel());
		dialog.getModel().getDefaultConfigs().values().forEach(e->{if (e.getUuid() == null) s.persist(e);});
	}
	
	/**
	 * Does validation if it is required for the tab.
	 * @return error message or null
	 */
	@Override
	public String validate() {
		//ensure all matrixnodes have at least one list attribute and one non-list attribute
		//and that all list attributes appear first
		ConfigurableModelTreeContentProvider provider = ((ConfigurableModelTreeContentProvider)modelTreeViewer.getContentProvider());
		
		List<Object> nodestovalidate = new ArrayList<>();
		for (Object start : provider.getElements(null)) {
			nodestovalidate.add(start);
		}
		while(!nodestovalidate.isEmpty()) {
			Object x = nodestovalidate.remove(0);
			
			if (x instanceof MatrixNode) {
				//valid node
				MatrixNode mn = (MatrixNode)x;
				int listcnt = 0;
				int othercnt = 0;
				
				long totalListItemCnt = 1;
				
				String cname = mn.getParent().getCategory().getName();
				for (int i = 0; i < mn.getKids().size(); i ++) {
					CmAttribute a = mn.getKids().get(i);
					if (a.getAttribute().getType() == Attribute.AttributeType.LIST) {
						if (othercnt > 0) {
							return MessageFormat.format(Messages.ConfigurableModelEditorDefaultTab_InvalidOrder, cname);
						}
						
						long cnt = a.getConfig().getList().stream().filter(e->e.getIsActive()).count();
						totalListItemCnt = totalListItemCnt * cnt;
						listcnt++;
					}else {
						othercnt ++;
					}
				}
				
				if (listcnt == 0) return MessageFormat.format(Messages.ConfigurableModelEditorDefaultTab_MissingList, cname);
				if (othercnt == 0) return MessageFormat.format(Messages.ConfigurableModelEditorDefaultTab_MissingNonList, cname);
				if (totalListItemCnt > MAX_MATRIX_COMBOS) 
					return MessageFormat.format(Messages.ConfigurableModelEditorDefaultTab_TooManyCombos, cname, totalListItemCnt, MAX_MATRIX_COMBOS);
				
			}else {
				//add children
				for (Object kid : provider.getChildren(x)) nodestovalidate.add(kid);
			}
		}
		return null;
	}

	@Override
	public int getTabIndex() {
		return 0;
	}
	
	public static List<CmAttributeConfig> deleteNode(CmNode node, Session session) {
		ConfigurableModel model = node.getModel();
		CmNode parentNode = node.getParent();
		List<CmAttributeConfig> deleted = new ArrayList<>();
		node.setParent(null);
		if (parentNode == null) {
			//this is the root node
			model.getNodes().remove(node);
			//re-order nodes
			int i = 0;
			for (CmNode n : model.getNodes()){
				n.setNodeOrder(i++);
			}
		} else {
			//not a root node
			parentNode.getChildren().remove(node);
				
			//re-order nodes
			int i = 0;
			for (CmNode n : parentNode.getChildren()){
				n.setNodeOrder(i++);
			}
		}
			
		//remove default tree mapping if present
		Set<Attribute> existingTrees = CmDefaultTreesUtil.getPresentedTreeAttributes(model);
		for (Attribute a : CmDefaultTreesUtil.getPresentedTreeAttributes(node)) {
			if (!existingTrees.contains(a)) {
				//attribute is not present in CM anymore -> remove all related configurations
				model.getDefaultConfigs().remove(a);
				deleted.addAll(removeRelatedConfigs(a, node, session));		
			}
		}
		//remove default list mapping if present
		Set<Attribute> existingLists = CmDefaultListsUtil.getPresentedListAttributes(model);
		for (Attribute a : CmDefaultListsUtil.getPresentedListAttributes(node)) {
			if (!existingLists.contains(a)) {
				//attribute is not present in CM anymore -> remove all related configurations
				model.getDefaultConfigs().remove(a);
				deleted.addAll(removeRelatedConfigs(a, node, session));
			}
		}
		return deleted;		
	}
	
	private static List<CmAttributeConfig> removeRelatedConfigs(Attribute a, CmNode node, Session session) {
		List<CmAttributeConfig> deleted = new ArrayList<>(); 
		List<CmAttributeConfig> configs = DataentryHibernateManager.getCmAttributeConfigs(session, 
				node.getModel(), a);
		for (CmAttributeConfig cfg : configs) {
			if (node.getCmAttributes().isEmpty()) continue;
			//for hibernate
			node.getCmAttributes().forEach(cma->{
				if (cfg.equals(cma.getConfig())) {
					cma.setConfig(null);
				}
			});
			
			session.remove(cfg);
			deleted.add(cfg);

			//for hibernate
			if (cfg.getList() != null)cfg.getList().forEach(f->f.setConfig(null));
			if (cfg.getTree() != null)cfg.getTree().forEach(f->f.setConfig(null));
		}
		return deleted;
		
	}
}
