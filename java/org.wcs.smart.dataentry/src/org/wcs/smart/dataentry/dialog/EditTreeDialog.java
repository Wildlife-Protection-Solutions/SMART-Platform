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

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.ca.Label;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.dataentry.dialog.composite.CmTreeLabelProvider;
import org.wcs.smart.dataentry.dialog.composite.DisplayModeComboViewer;
import org.wcs.smart.dataentry.dialog.composite.ImageSelectionControl;
import org.wcs.smart.dataentry.dialog.composite.ImageSelectionControl.IImageContentProvider;
import org.wcs.smart.dataentry.internal.Messages;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.CmAttributeOption;
import org.wcs.smart.dataentry.model.CmAttributeTreeNode;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.dataentry.model.DisplayMode;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.properties.AttributeTreeContentProvider;
import org.wcs.smart.ui.properties.AttributeTreeLabelProvider;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Rename dialog for providing aliases for configurable model tree attribute nodes 
 * @author elitvin
 * @author Emily
 * @since 3.0.0
 */
public class EditTreeDialog extends TitleAreaDialog {

	protected CmAttribute attribute;
	protected ConfigurableModel editModel;
	
	private Viewer dmTreeViewer;
	private TreeViewer itemViewer;
	private TableViewer nameTable;
	
	private Composite nodeConfigCmp;
	private DisplayModeComboViewer modeViewer;
	private ImageSelectionControl imageControl;
	private org.eclipse.swt.widgets.Label imgControlLabel;

	private NamedItem dmNode;
	private CmAttributeTreeNode cmNode;
	
	private Button btnEnable;
	private Button btnAddSubNodes;
	private Button btnAdd;
	private Button btnRemove;

	private Session session;

	public EditTreeDialog(Shell parentShell, CmAttribute attribute, ConfigurableModel editModel, Session session) {
		super(parentShell);
		this.attribute = attribute;
		this.editModel = editModel;
		this.session = session;
	}

	protected Control createDialogArea(Composite parent) {
		parent = (Composite) super.createDialogArea(parent);
		
		setTitle(attribute.getName());
		setMessage(getDialogMessage());
		getShell().setText(Messages.ConfigurableModelEditDialog_Title);
		
		SashForm comp = new SashForm(parent, SWT.HORIZONTAL);
		comp.setLayoutData(new GridData(SWT.FILL,SWT.FILL, true, true));

		Group left = new Group(comp, SWT.NONE);
		left.setLayout( new GridLayout());
		left.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		left.setText(Messages.EditTreeDialog_DataModelValues);
		
		btnAddSubNodes = new Button(left, SWT.CHECK);
		btnAddSubNodes.setText(Messages.EditTreeDialog_AutoAddSubnodes);
		btnAddSubNodes.setSelection(true);
		
		dmTreeViewer = createDmTreeViewer(left);
		((TreeViewer)dmTreeViewer).addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				addToConfigurableTree();
			}
		});
		
		Group middle = new Group(comp, SWT.NONE);
		middle.setLayout(new GridLayout());
		middle.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		middle.setText(Messages.EditTreeDialog_ConfigurableModelValues);
		
		Composite btnComp = new Composite(middle, SWT.NONE);
		btnComp.setLayout(new GridLayout(4, true));
		btnComp.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, false));
		
		btnAdd = new Button(btnComp, SWT.PUSH);
		btnAdd.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		btnAdd.setEnabled(false);
		btnAdd.setText(Messages.EditTreeDialog_AddNodeButton);
		btnAdd.setToolTipText(Messages.EditTreeDialog_AddNodeTooltip);
		btnAdd.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				addToConfigurableTree();
			}
		});
				
		Button btnAddGrp = new Button(btnComp, SWT.PUSH);
		btnAddGrp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		btnAddGrp.setText(Messages.EditTreeDialog_AddGroup);
		btnAddGrp.setToolTipText(Messages.EditTreeDialog_NewGroupTooltip);
		btnAddGrp.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				addGroupItem();
			}
		});
		
		btnRemove = new Button(btnComp, SWT.PUSH);
		btnRemove.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		btnRemove.setText(DialogConstants.DELETE_BUTTON_TEXT);
		btnRemove.setToolTipText(Messages.EditTreeDialog_DeleteTooltip);
		btnRemove.setEnabled(false);
		btnRemove.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				deleteItems();
			}
		});
		
		btnEnable = new Button(btnComp, SWT.PUSH);
		btnEnable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		btnEnable.setText(DialogConstants.ENABLE_BUTTON_TEXT);
		btnEnable.setToolTipText(Messages.EditTreeDialog_EnableTooltip);
		btnEnable.setEnabled(false);
		btnEnable.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean enable = true;
				if (btnEnable.getText().equals(DialogConstants.DISABLE_BUTTON_TEXT)){
					enable = false;
				}
				IStructuredSelection selection = (IStructuredSelection) itemViewer.getSelection();
				for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
					Object type = iterator.next();
					if (type instanceof CmAttributeTreeNode){
						enableItem((CmAttributeTreeNode)type, enable);
					}
					
				}
				itemViewer.refresh();
				updateEnableButtonText();
			}
		});
		//super.setButtonLayoutData(btnEnable);
		
		itemViewer = createItemViewer(middle);
		
		Composite btnPanel = new Composite(middle, SWT.NONE);
		GridLayout gla = new GridLayout(3, false);
		gla.marginHeight = gla.marginWidth = 0;
		btnPanel.setLayout(gla);
		btnPanel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

		Group right = new Group(comp, SWT.NONE);
		right.setLayout( new GridLayout());
		right.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		right.setText(Messages.EditTreeDialog_TreeNodeProperties);

		createNameTable(right);
		nodeConfigCmp = createNodeConfigControls(right);
		nodeConfigCmp.setVisible(false);
		
		comp.setWeights(new int[]{28,37,35});
		itemViewer.refresh();		
		return parent;
	}
	
	@Override
	public Point getInitialSize(){
		Point size = super.getInitialSize();
		return new Point(Math.min(size.x, 900), size.y);
	}

	protected void deleteItems() {
		for (Iterator<?> i = ((StructuredSelection)itemViewer.getSelection()).iterator(); i.hasNext();) {
			CmAttributeTreeNode cmTreeNode = (CmAttributeTreeNode) i.next();
			
			List<CmAttributeTreeNode> kids = new ArrayList<CmAttributeTreeNode>();
			kids.addAll(cmTreeNode.getChildren());
			while(kids.size() > 0){
				CmAttributeTreeNode kid = kids.remove(0);
				if (kid.getChildren().size() ==0){
					if (kid.getParent() != null) kid.getParent().getChildren().remove(kid);
					kid.setParent(null);
				}else{
					kids.addAll(kid.getChildren());
					kids.add(kid);
				}
			}
			
			CmAttributeTreeNode parent = cmTreeNode.getParent();
			List<CmAttributeTreeNode> children = getTargetList(parent);
			children.remove(cmTreeNode);
			updateNodeOrder(children);
			cmTreeNode.setParent(null);
		}
		itemViewer.refresh();
	}

	protected void addGroupItem() {
		CmAttributeTreeNode parent = getTargetCmNode();
		List<CmAttributeTreeNode> children = getTargetList(parent);
		CmAttributeTreeNode cmTreeNode = new CmAttributeTreeNode();
		cmTreeNode.setConfigurableModel(editModel);
		cmTreeNode.setParent(parent);
		if (attribute.isUseCustomConfig()) {
			cmTreeNode.setAttribute(attribute);
		} else {
			cmTreeNode.setDmAttribute(attribute.getAttribute());
		}
		cmTreeNode.setIsActive(true);
		cmTreeNode.updateName(SmartDB.getCurrentLanguage(), Messages.EditTreeDialog_NewGroup);
		cmTreeNode.setNodeOrder(children.size());
		children.add(cmTreeNode);
		itemViewer.refresh();
		itemViewer.expandToLevel(cmTreeNode, 1);
	}

	protected void addToConfigurableTree() {
		CmAttributeTreeNode parent = getTargetCmNode();
		for (Iterator<?> i = ((StructuredSelection)dmTreeViewer.getSelection()).iterator(); i.hasNext();) {
			AttributeTreeNode dmTreeNode = (AttributeTreeNode) i.next();
			addCmTreeNode(dmTreeNode, parent, btnAddSubNodes.getSelection());
		}
		itemViewer.refresh();
		itemViewer.expandToLevel(parent, 1);
	}

	private void addCmTreeNode(AttributeTreeNode dmTreeNode, CmAttributeTreeNode parent, boolean addSubNodes) {
		CmAttributeTreeNode cmTreeNode = new CmAttributeTreeNode();
		cmTreeNode.setConfigurableModel(editModel);
		cmTreeNode.setParent(parent);
		if (attribute.isUseCustomConfig()) {
			cmTreeNode.setAttribute(attribute);
		} else {
			cmTreeNode.setDmAttribute(attribute.getAttribute());
		}
		cmTreeNode.setDmTreeNode(dmTreeNode);
		cmTreeNode.setIsActive(dmTreeNode.getIsActive());
		List<CmAttributeTreeNode> target = getTargetList(parent);
		cmTreeNode.setNodeOrder(target.size());
		target.add(cmTreeNode);
		if (addSubNodes && dmTreeNode.getActiveChildren() != null && !dmTreeNode.getActiveChildren().isEmpty()) {
			for (AttributeTreeNode child : dmTreeNode.getActiveChildren()) {
				addCmTreeNode(child, cmTreeNode, addSubNodes);
			}
		}
	}

	private void updateNodeOrder(List<CmAttributeTreeNode> list) {
		for (int i = 0; i < list.size(); i++) {
			list.get(i).setNodeOrder(i);
		}
	}
	
	private CmAttributeTreeNode getTargetCmNode() {
		Object x = ((StructuredSelection)itemViewer.getSelection()).getFirstElement();
		if (x instanceof CmAttributeTreeNode) {
			return (CmAttributeTreeNode) x;
		}
		return null;
	}

	private List<CmAttributeTreeNode> getTargetList(CmAttributeTreeNode node) {
		return node != null ? node.getChildren() : attribute.getCurrentTree();
	}	
	
	protected Viewer createDmTreeViewer(Composite parent) {
		final TreeViewer tree = new TreeViewer(parent, SWT.MULTI | SWT.BORDER);
		tree.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)tree.getTree().getLayoutData()).heightHint = 300;
		
		tree.setContentProvider(new AttributeTreeContentProvider(true, false));
		tree.setLabelProvider(new AttributeTreeLabelProvider());
		tree.setInput(attribute.getAttribute());
		tree.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				boolean isEmpty = tree.getSelection().isEmpty();
				if (isEmpty){
					btnAdd.setEnabled(false);
					return;
				}
				btnAdd.setEnabled(false);
				for (Iterator<?> it = ((IStructuredSelection)tree.getSelection()).iterator(); it.hasNext();){
					if (it.next() instanceof AttributeTreeNode){
						btnAdd.setEnabled(true);
						return;
					}
				}
			}
		});
		tree.expandToLevel(2);
		
		return tree;
	}
	
	protected TreeViewer createItemViewer(Composite parent) {
		final TreeViewer tree = new TreeViewer(parent, SWT.MULTI | SWT.BORDER);
		tree.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)tree.getTree().getLayoutData()).heightHint = 300;
		
		tree.setContentProvider(new CmAttributeTreeContentProvider(false, true));
		tree.setLabelProvider(new CmTreeLabelProvider(editModel));
		tree.setInput(attribute);
		tree.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				Object x = ((StructuredSelection)tree.getSelection()).getFirstElement();
				AttributeTreeNode currentNode = null;
				CmAttributeTreeNode currentCmNode = null;
				if (x instanceof CmAttributeTreeNode) {
					currentCmNode = (CmAttributeTreeNode) x;
					currentNode = currentCmNode.getDmTreeNode();
				}
				EditTreeDialog.this.setCurrentSelection(currentNode, currentCmNode);
			}
		});
		Transfer[] transferTypes = new Transfer[]{LocalSelectionTransfer.getTransfer()};
		tree.addDragSupport(DND.DROP_MOVE, transferTypes , new CmAttributeTreeDragListener(tree));
		tree.addDropSupport(DND.DROP_MOVE, transferTypes, new CmAttributeTreeDropListener(tree));
		
		tree.expandToLevel(2);
		return tree;
	}
	
	/**
	 * Creates a table with one row for each language.
	 * 
	 * @param parent
	 */
	private void createNameTable(Composite parent) {
		nameTable = new TableViewer(parent, SWT.FULL_SELECTION | SWT.BORDER);
		nameTable.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		nameTable.setContentProvider(ArrayContentProvider.getInstance());
		nameTable.getTable().setHeaderVisible(true);
		nameTable.getTable().setLinesVisible(true);
		
		TableViewerColumn colLang = new TableViewerColumn(nameTable, SWT.NONE);
		colLang.getColumn().setWidth(100);
		colLang.getColumn().setText(Messages.AbstractRenameDialog_LanguageColumnName);
		colLang.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof Language){
					return ((Language) element).getDisplayName();
				}
			  	return super.getText(element);
			}

		});
		
		TableViewerColumn colName = new TableViewerColumn(nameTable, SWT.NONE);
		colName.getColumn().setWidth(150);
		colName.getColumn().setText(Messages.AbstractRenameDialog_ConfiguredName);
		colName.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof Language){
					if (cmNode != null){
						String label = cmNode.findNameNull((Language) element);
						if (label != null){
							return label;
						}
					}
					if (dmNode != null){
						return dmNode.findName((Language) element);
					}
					return ""; //$NON-NLS-1$
				}
			  	return super.getText(element);
			}
			
			@Override
			public Color getForeground(Object element) {
				if (cmNode == null || cmNode.findNameNull((Language)element) == null){
					return Display.getDefault().getSystemColor(SWT.COLOR_GRAY);
				}
				return null;
			}
		});
		
		colName.setEditingSupport(new EditingSupport(nameTable) {
			private TextCellEditor editor =  new TextCellEditor(nameTable.getTable());
		
			@Override
			protected void setValue(Object element, Object value) {
				Language lang = (Language)element;
				String newValue = (String)value;
				
				if (newValue.trim().length() == 0){
					if (cmNode != null){
						for (Iterator<Label> iterator = cmNode.getNames().iterator(); iterator.hasNext();) {
							Label l = iterator.next();
							if (l.getLanguage().equals(lang)){
								iterator.remove();
							}
						}
					}
				}else if(dmNode == null || !dmNode.findName(lang).equals(newValue)){
					cmNode.updateName(((Language)element), (String)value);
				}

				nameTable.refresh();
				itemViewer.refresh();
			}
			
			@Override
			protected Object getValue(Object element) {
				if (cmNode != null){
					String label = cmNode.findNameNull(((Language)element));
					if (label != null){
						return label;
					}
				}
				if (dmNode != null){
					return dmNode.findName(((Language)element));
				}
				return ""; //$NON-NLS-1$
			}
			
			@Override
			protected CellEditor getCellEditor(Object element) {
				return editor;
			}
			
			@Override
			protected boolean canEdit(Object element) {
				return true;
			}
		});
		
		TableViewerColumn dmName = new TableViewerColumn(nameTable, SWT.NONE);
		dmName.getColumn().setWidth(150);
		dmName.getColumn().setText(Messages.AbstractRenameDialog_DataModelColumnName);
		dmName.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof Language){
					if (dmNode != null){
						return dmNode.findName((Language) element);
					}else{
						return ""; //$NON-NLS-1$
					}
				}
			  	return super.getText(element);
			}
		});
		
		nameTable.setInput(SmartDB.getCurrentConservationArea().getLanguages());
		nameTable.getTable().setEnabled(false);
	}

	private Composite createNodeConfigControls(Composite parent) {
		Composite containerCmp = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		layout.marginHeight = layout.marginWidth = 0;
		containerCmp.setLayout(layout);
		containerCmp.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, false, false));
		
		org.eclipse.swt.widgets.Label label = new org.eclipse.swt.widgets.Label(containerCmp, SWT.NONE);
		label.setText(Messages.EditTreeDialog_DisplayMode);
		modeViewer = new DisplayModeComboViewer(containerCmp);
		modeViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		modeViewer.setSelection(new StructuredSelection(attribute.getCurrentDisplayMode() != null ? attribute.getCurrentDisplayMode() : DisplayMode.DEFAULT_DISPLAY_MODE));
		modeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				if (cmNode == null) {
					//we are configuring a root of the tree; its configuration is stored as a part of attribute
					attribute.setCurrentDisplayMode(modeViewer.getSelectedDisplayMode());
					//we need to save either configurable model global setting (for default configuration)
					//or attribute option (for custom configuration), this is way we try to save both below
					attribute.getCmAttributeOptions().get(CmAttributeOption.ID_DISPLAY_MODE);
					editModel.getAttributeSettings().get(attribute.getAttribute());
				} else {
					cmNode.setDisplayMode(modeViewer.getSelectedDisplayMode());
				}
			}
		});
		
		imgControlLabel = new org.eclipse.swt.widgets.Label(containerCmp, SWT.NONE);
		imgControlLabel.setText(Messages.EditTreeDialog_Image);
		imageControl = new ImageSelectionControl(containerCmp, new IImageContentProvider() {
			@Override
			public File getImageFile() {
				return cmNode != null ? cmNode.getImageFile() : null;
			}

			@Override
			public void setImageFile(File file) {
				if (cmNode != null) {
					cmNode.setImageFile(file);
					if (cmNode.getUuid() != null) {
						//need this logic to correctly trigger intercepter
						session.evict(cmNode);
						session.saveOrUpdate(cmNode);
					}
					imageControl.redrawCanvas();
				}
			}
		});
		return containerCmp;
	}
	
	/**
	 * Sets the current selection from the item viewer.  If null
	 * the name table will be disabled.
	 * 
	 * @param dmNode
	 * @param cmNode
	 */
	public void setCurrentSelection(NamedItem dmNode, CmAttributeTreeNode cmNode){
		this.dmNode = dmNode;
		this.cmNode = cmNode;
		nameTable.refresh();
		
		nameTable.getTable().setEnabled(cmNode != null);
		btnEnable.setEnabled(cmNode != null);
		btnRemove.setEnabled(cmNode != null);
		updateEnableButtonText();
		nodeConfigCmp.setVisible(itemViewer.getSelection() != null && !itemViewer.getSelection().isEmpty());
		updateDisplayModeControl();
		imageControl.setVisible(cmNode != null);
		imgControlLabel.setVisible(cmNode != null);
		imageControl.redrawCanvas();
	}

	private void updateDisplayModeControl() {
		if (cmNode == null) {
			//value for root
			modeViewer.setSelection(new StructuredSelection(attribute.getCurrentDisplayMode() != null ? attribute.getCurrentDisplayMode() : DisplayMode.DEFAULT_DISPLAY_MODE));
		} else {
			modeViewer.setSelection(new StructuredSelection(cmNode.getDisplayMode() != null ? cmNode.getDisplayMode() : DisplayMode.DEFAULT_DISPLAY_MODE));
		}
		
	}
	
	private void updateEnableButtonText(){

		if (this.cmNode == null || this.cmNode.getIsActive()){
			btnEnable.setText(DialogConstants.DISABLE_BUTTON_TEXT);
		}else{
			btnEnable.setText(DialogConstants.ENABLE_BUTTON_TEXT);
		}
	}
	
	protected String getDialogMessage() {
		return attribute.isUseCustomConfig() ? Messages.EditTreeDialog_MessageCustom : Messages.EditTreeDialog_MessageDefault;
	}
	
	private void processItem(CmAttributeTreeNode cmTreeNode, boolean enable, boolean updateSelection){
		if (updateSelection) {
			setCurrentSelection(cmTreeNode.getDmTreeNode(), cmTreeNode);
		}
		cmTreeNode.setIsActive(enable);
	}
	
	protected void enableItem(CmAttributeTreeNode cmTreeNode, boolean enable){
		processItem(cmTreeNode, enable, true);
		
		if (!enable){
			//process all children
			List<CmAttributeTreeNode> itemsToProcess = new ArrayList<CmAttributeTreeNode>();
			itemsToProcess.addAll(cmTreeNode.getChildren());
			while(itemsToProcess.size() > 0){
				CmAttributeTreeNode kid = itemsToProcess.remove(0);
				processItem(kid, enable, false);
				itemsToProcess.addAll(kid.getChildren());
			}
		
		}else{
			//need to enable all parents
			CmAttributeTreeNode parent = ((CmAttributeTreeNode)cmTreeNode).getParent();
			while(parent != null){
				processItem(parent, enable, false);
				parent = parent.getParent();
			}
		}
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
	}
	
	@Override
	public boolean isResizable(){
		return true;
	}
	
}
