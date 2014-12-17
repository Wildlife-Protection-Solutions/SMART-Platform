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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.EditingSupport;
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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.ca.Label;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.dataentry.dialog.composite.CmTreeLabelProvider;
import org.wcs.smart.dataentry.internal.Messages;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.CmAttributeTreeNode;
import org.wcs.smart.dataentry.model.ConfigurableModel;
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
	protected Session currentSession;
	
	private Viewer dmTreeViewer;
	private Viewer itemViewer;
	private TableViewer nameTable;

	private NamedItem dmNode;
	private CmAttributeTreeNode cmNode;
	
	private Button btnEnable;
	private Button btnAddSubNodes;
	
	public EditTreeDialog(Shell parentShell, CmAttribute attribute, ConfigurableModel editModel, Session currentSession) {
		super(parentShell);
		this.attribute = attribute;
		this.currentSession = currentSession;
		this.editModel = editModel;
	}

	protected Control createDialogArea(Composite parent) {
		parent = (Composite) super.createDialogArea(parent);
		
		setTitle(attribute.getName());
		setMessage(getDialogMessage());
		getShell().setText(Messages.ConfigurableModelEditDialog_Title);
		
		SashForm comp = new SashForm(parent, SWT.HORIZONTAL);
		comp.setLayoutData(new GridData(SWT.FILL,SWT.FILL, true, true));

		Composite left = new Composite(comp, SWT.NONE);
		GridLayout lgl = new GridLayout();
		lgl.marginHeight = lgl.marginWidth = 0;
		left.setLayout(lgl);
		left.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		org.eclipse.swt.widgets.Label dmLabel = new org.eclipse.swt.widgets.Label(left, SWT.NONE);
		dmLabel.setText("Data Model Tree Values:");
		
		dmTreeViewer = createDmTreeViewer(left);

		btnAddSubNodes = new Button(left, SWT.CHECK);
		btnAddSubNodes.setText("Automatically add subnodes");
		btnAddSubNodes.setSelection(true);

		Button btnAddConf = new Button(left, SWT.PUSH);
		btnAddConf.setText("Add to Configurable Tree");
		btnAddConf.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				addToConfigurableTree();
			}
		});
		btnAddConf.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER,false, false));
		
		Composite middle = new Composite(comp, SWT.NONE);
		GridLayout gl = new GridLayout();
		gl.marginHeight = gl.marginWidth = 0;
		middle.setLayout(gl);
		middle.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		org.eclipse.swt.widgets.Label cmLabel = new org.eclipse.swt.widgets.Label(middle, SWT.NONE);
		cmLabel.setText("Configurable Model Tree Values:");

		itemViewer = createItemViewer(middle);
		
		Composite btnPanel = new Composite(middle, SWT.NONE);
		GridLayout gla = new GridLayout(3, false);
		gla.marginHeight = gla.marginWidth = 0;
		btnPanel.setLayout(gla);
		btnPanel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

		Button btnAddGrp = new Button(btnPanel, SWT.PUSH);
		btnAddGrp.setText("Add Group");
		btnAddGrp.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				addGroupItem();
			}
		});
		
		btnEnable = new Button(btnPanel, SWT.PUSH);
		GridData gd = new GridData(SWT.RIGHT, SWT.CENTER, false, false);
		gd.verticalIndent = 2;
		gd.horizontalIndent = 2;
		btnEnable.setLayoutData(gd);
		btnEnable.setText(DialogConstants.ENABLE_BUTTON_TEXT);
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
				currentSession.flush();
				itemViewer.refresh();
				updateEnableButtonText();
			}
		});
		super.setButtonLayoutData(btnEnable);

		Button btnDelete = new Button(btnPanel, SWT.PUSH);
		btnDelete.setText("Delete");
		btnDelete.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				deleteItems();
			}
		});
		
		createNameTable(comp);
		
		comp.setWeights(new int[]{30,30,40});
		itemViewer.refresh();
		
		return parent;
	}

	protected void deleteItems() {
		for (Iterator<?> i = ((StructuredSelection)itemViewer.getSelection()).iterator(); i.hasNext();) {
			CmAttributeTreeNode cmTreeNode = (CmAttributeTreeNode) i.next();
			CmAttributeTreeNode parent = cmTreeNode.getParent();
			List<CmAttributeTreeNode> children = getTargetList(parent);
			children.remove(cmTreeNode);
			updateNodeOrder(children);
			cmTreeNode.setParent(null);
			currentSession.delete(cmTreeNode);
		}
		currentSession.flush();
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
		cmTreeNode.updateName(SmartDB.getCurrentLanguage(), "New Group");
		cmTreeNode.setNodeOrder(children.size());
		children.add(cmTreeNode);
		currentSession.saveOrUpdate(cmTreeNode);
		currentSession.flush();
		itemViewer.refresh();
	}

	protected void addToConfigurableTree() {
		CmAttributeTreeNode parent = getTargetCmNode();
		for (Iterator<?> i = ((StructuredSelection)dmTreeViewer.getSelection()).iterator(); i.hasNext();) {
			AttributeTreeNode dmTreeNode = (AttributeTreeNode) i.next();
			addCmTreeNode(dmTreeNode, parent, btnAddSubNodes.getSelection());
		}
		currentSession.flush();
		itemViewer.refresh();
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
		currentSession.saveOrUpdate(cmTreeNode);
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
//		tree.addSelectionChangedListener(new ISelectionChangedListener() {
//			
//			@Override
//			public void selectionChanged(SelectionChangedEvent event) {
//				Object x = ((StructuredSelection)tree.getSelection()).getFirstElement();
//				AttributeTreeNode currentNode = null;
//				CmAttributeTreeNode currentCmNode = null;
//				if (x instanceof AttributeTreeNode){
//					currentNode = (AttributeTreeNode) x;
//					currentCmNode = getConfiguredNode(x);
//				}
//				EditTreeDialog.this.setCurrentSelection(currentNode, currentCmNode);
//			}
//		});
		tree.expandToLevel(2);
		
		return tree;
	}
	
	protected Viewer createItemViewer(Composite parent) {
		final TreeViewer tree = new TreeViewer(parent, SWT.MULTI | SWT.BORDER);
		tree.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)tree.getTree().getLayoutData()).heightHint = 300;
		
		tree.setContentProvider(new CmAttributeTreeContentProvider(false, true));
		tree.setLabelProvider(new CmTreeLabelProvider(currentSession, editModel));
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
//					if (cmNode == null){
//						cmNode = createNewAlaisItem(dmNode);
//					}
					cmNode.updateName(((Language)element), (String)value);
				}
				if (cmNode != null){
					currentSession.saveOrUpdate(cmNode);
					currentSession.flush();
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
		updateEnableButtonText();
	}

	private void updateEnableButtonText(){

		if (this.cmNode == null || this.cmNode.getIsActive()){
			btnEnable.setText(DialogConstants.DISABLE_BUTTON_TEXT);
		}else{
			btnEnable.setText(DialogConstants.ENABLE_BUTTON_TEXT);
		}
	}
	
	protected String getDialogMessage() {
		return Messages.RenameTreeDialog_DialogMessage;
	}
	
	private void processItem(CmAttributeTreeNode cmTreeNode, boolean enable, boolean updateSelection){
		if (updateSelection) {
			setCurrentSelection(cmTreeNode.getDmTreeNode(), cmTreeNode);
		}
		cmTreeNode.setIsActive(enable);
		currentSession.saveOrUpdate(cmTreeNode);
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
