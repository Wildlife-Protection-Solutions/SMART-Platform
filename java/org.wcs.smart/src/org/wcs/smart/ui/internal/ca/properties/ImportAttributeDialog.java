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
package org.wcs.smart.ui.internal.ca.properties;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.internal.ca.datamodel.xml.generate.NameType;
import org.wcs.smart.internal.ca.datamodel.xml.generate.TreeNodeType;

/**
 * Dialog box for selecting nodes from the xml datamodel 
 * structure.  This dialog uses two tree viewers to allow
 * users to move nodes from one tree to another. 
 * 
 * @author Emily
 *
 */
public class ImportAttributeDialog extends TitleAreaDialog {

	private LabelProvider nodeTypeLabelProvider = new LabelProvider(){
		@Override
		public String getText(Object element) {
			if (element instanceof TreeNodeType){
				return findName((TreeNodeType)element);
			}
			return super.getText(element);
		}
	};
	
	private TreeViewer tv1; //source nodes
	private TreeViewer tv2;	//to import 
	
	private ParentTreeNodeType[] rootNodes;
	private ImportAttributeProcessor processor;

	
	/**
	 * Creates a new attribute dialog
	 * @param parentShell parent shell
	 * @param treeAttribute xml tree attribute
	 */
	public ImportAttributeDialog(Shell parentShell, ImportAttributeProcessor processor) {
		super(parentShell);
		this.processor = processor;
		initAttributeType();
	}
	
	/**
	 * 
	 * @return list of nodes selected by user
	 */
	public List<TreeNodeType> getSelectedNodes(){
		List<TreeNodeType> nodes = new ArrayList<TreeNodeType>();
		for (ParentTreeNodeType p : rootNodes){
			if (p.isInTree()){
				nodes.add(p);
				filterChildren(p);
			}
		}
		return nodes;
	}
	
	/*
	 * removes all children that are not selected
	 */
	private void filterChildren(TreeNodeType t){
		for (Iterator<TreeNodeType> iterator = t.getChildrens().iterator(); iterator.hasNext();) {
			TreeNodeType kid = iterator.next();
			if (!((ParentTreeNodeType)kid).isInTree()){
				iterator.remove();
			}else{
				filterChildren(kid);
			}
		}
	}

	/*
	 * Determines the name to display.
	 * First this function tries to find the label associated with the current
	 * language.  If not found, it uses the default code selected by the user.  If
	 * it can't find that then it returns the key.
	 */
	private String findName(TreeNodeType node){
		String defaultLabel = null;
		for (NameType type : node.getNames()){
			if (type.getLanguageCode().equals(SmartDB.getCurrentLanguage().getCode())){
				return type.getValue();
			}else if (type.getLanguageCode().equals(this.processor.getDefaultLangCode())){
				defaultLabel = type.getValue();
			}
		}
		if (defaultLabel != null){
			return defaultLabel;
		}
		return node.getKey();
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Control main = super.createDialogArea(parent);
		
		Composite comp = new Composite((Composite)main, SWT.NONE);
		comp.setLayout(new GridLayout(3, false));
		comp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		PatternFilter patternFilter = new PatternFilter(){			
			protected boolean isChildMatch(Viewer viewer, Object element) {
				Object parent = ((ITreeContentProvider)tv1.getContentProvider()).getParent(element);
				if (parent != null) {
					return (isLeafMatch(viewer, parent) ? true : isChildMatch(viewer, parent));
				}
				return false;
			}

			@Override
			protected boolean isLeafMatch(Viewer viewer, Object element) {
				String labelText = nodeTypeLabelProvider.getText(element);
				if (labelText == null) {
					return false;
				}
				return (wordMatches(labelText) ? true : isChildMatch(viewer,element));
			}
			
		};
		FilteredTree fTree = new FilteredTree(comp, SWT.MULTI | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL, patternFilter, true);
		
		tv1 = fTree.getViewer();
		tv1.setLabelProvider(nodeTypeLabelProvider);
		tv1.setContentProvider(new TreeContentProvider(false));
		tv1.setInput(rootNodes);
		tv1.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)tv1.getTree().getLayoutData()).widthHint = 100;
		tv1.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				addSelected();	
			}
		});
		
		Composite buttonPanel = new Composite(comp, SWT.NONE);
		buttonPanel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		buttonPanel.setLayout(new GridLayout(1, false));
		Button btnAdd = new Button(buttonPanel,SWT.PUSH);
		btnAdd.setText("->"); //$NON-NLS-1$
		btnAdd.setToolTipText(Messages.ImportAttributeDialog_AddButtonTooltip);
		btnAdd.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		btnAdd.addSelectionListener(new SelectionAdapter() {			
			@Override
			public void widgetSelected(SelectionEvent e) {
				addSelected();
			}
			
		});
		
		Button btnRemove = new Button(buttonPanel,SWT.PUSH);
		btnRemove.setText("<-"); //$NON-NLS-1$
		btnRemove.setToolTipText(Messages.ImportAttributeDialog_RemoveButtonTooltip);
		btnRemove.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		btnRemove.addSelectionListener(new SelectionAdapter() {			
			@Override
			public void widgetSelected(SelectionEvent e) {
				removeSelected();
			}
			
		});
		
		tv2 = new TreeViewer(comp, SWT.BORDER | SWT.MULTI);
		tv2.setLabelProvider(nodeTypeLabelProvider);
		tv2.setContentProvider(new TreeContentProvider(true));
		tv2.setInput(rootNodes);
		tv2.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)tv2.getTree().getLayoutData()).widthHint = 100;
		((GridData)tv2.getTree().getLayoutData()).heightHint = 350;
		
		Link changeLink = new Link(comp, SWT.NONE);
		changeLink.setText("<a>" + Messages.ImportAttributeDialog_ChangeSourceLabel + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
		changeLink.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				changeSelected();
			}
		});
		
		getShell().setText(MessageFormat.format(Messages.ImportAttributeDialog_DialogText, new Object[]{processor.getAttribute().getName()}));
		setTitle(processor.getAttribute().getName());
		setMessage(Messages.ImportAttributeDialog_DialogMessage);
		return main;
	}
	
	/*
	 * inits the root nodes from the
	 * current matched attribute in the processor
	 */
	private void initAttributeType(){
		//create root nodes
		List<ParentTreeNodeType> roots = new ArrayList<ParentTreeNodeType>();
		for (TreeNodeType tn : processor.getMatchedAttribute().getTrees()){
			roots.add(new ParentTreeNodeType(tn));
		}
		rootNodes = roots.toArray(new ParentTreeNodeType[roots.size()]);
	}
	
	/*
	 * change the input file to select nodes
	 * from
	 */
	private void changeSelected(){
		//validate
		for (ParentTreeNodeType n : rootNodes){
			if (n.isInTree()){
				boolean cont = MessageDialog.openQuestion(getShell(), Messages.ImportAttributeDialog_ConfirmDialogText, Messages.ImportAttributeDialog_ClearWarning);
				if (!cont){
					return;
				}
			}
		}
		//get file
		File file = processor.promptForFile();
		if (file == null){
			return;
		}
		//read file
		try {
			processor.readDataModel(file);
		} catch (Exception e) {
			SmartPlugIn.displayLog(Messages.ImportAttributeDialog_ErrorReadingFile, e);
			return;
		}
		//validate file
		if (!processor.validateInputFile()){
			return;
		}
		
		//update ui
		initAttributeType();
		tv1.setInput(rootNodes);
		tv2.setInput(rootNodes);
		tv1.refresh();
		tv2.refresh();
	}
	
	/*
	 * adds selected nodes from tree 1 to tree 2
	 */
	private void addSelected(){
		IStructuredSelection selection = (IStructuredSelection)tv1.getSelection();
		for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
			Object item = (Object) iterator.next();
			if (item instanceof ParentTreeNodeType ){
				setInTreeCascadeToKids( (ParentTreeNodeType)item, true);
				setInTreeCascadeUp( (ParentTreeNodeType)item);
				tv2.refresh();
				tv2.expandToLevel(item, TreeViewer.ALL_LEVELS);
			}
		}
	}
	
	/*
	 * removes selected nodes from tree 2
	 */
	private void removeSelected(){
		IStructuredSelection selection = (IStructuredSelection)tv2.getSelection();
		for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
			Object item = (Object) iterator.next();
			if (item instanceof ParentTreeNodeType ){
				setInTreeCascadeToKids( (ParentTreeNodeType)item, false);
			}			
			
		}
		tv2.refresh();
		
	}
	
	private void setInTreeCascadeUp(ParentTreeNodeType node){
		if (node == null) return;
		node.setIsInTree(true);
		setInTreeCascadeUp(node.getParent());
	}
	
	private void setInTreeCascadeToKids(ParentTreeNodeType node, boolean isInTree){
		node.setIsInTree(isInTree);
		for (TreeNodeType kid : node.getChildrens()){
			setInTreeCascadeToKids((ParentTreeNodeType)kid, isInTree);
		}
	}
	
	@Override
	public boolean isResizable(){
		return true;
	}
	
	/**
	 * Content provider for trees
	 * @author Emily
	 *
	 */
	private class TreeContentProvider implements ITreeContentProvider{

		private ParentTreeNodeType[] rootNodes;
		private boolean inTreeFlag = false;
		
		public TreeContentProvider(boolean inTreeFlag){
			this.inTreeFlag = inTreeFlag;
		}
		
		@Override
		public void dispose() {

		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			this.rootNodes = (ParentTreeNodeType[])newInput;
		}

		@Override
		public Object[] getElements(Object inputElement) {
			if (!inTreeFlag){
				return rootNodes;
			}
			ArrayList<ParentTreeNodeType> roots = new ArrayList<ParentTreeNodeType>();
			for (ParentTreeNodeType tn : rootNodes){
				if (tn.isInTree()){
					roots.add(tn);
				}
			}
			return roots.toArray(new ParentTreeNodeType[roots.size()]);
			
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof ParentTreeNodeType){
				if (!inTreeFlag){
					return ((ParentTreeNodeType) parentElement).getChildrens().toArray();
				}else{
					ArrayList<ParentTreeNodeType> roots = new ArrayList<ParentTreeNodeType>();
					for (TreeNodeType tn : ((ParentTreeNodeType)parentElement).getChildrens()){
						if (((ParentTreeNodeType)tn).isInTree()){
							roots.add((ParentTreeNodeType)tn);
						}
					}
					return roots.toArray(new ParentTreeNodeType[roots.size()]);		
				}
			}
			return null;
		}

		@Override
		public Object getParent(Object element) {
			if (element instanceof ParentTreeNodeType){
				return ((ParentTreeNodeType) element).getParent();
			}
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			if (element instanceof ParentTreeNodeType){
				if (!inTreeFlag){
					return ((ParentTreeNodeType) element).getChildrens().size() > 0;
				}else{
					for (TreeNodeType tn : ((ParentTreeNodeType)element).getChildrens()){
						if (((ParentTreeNodeType)tn).isInTree()){
							return true;
						}
					}
					return false;
				}
			}
			return false;
		}
		
	}
	
	/**
	 * wrapper around a tree node that keeps a reference
	 * to the parent object.
	 * @author Emily
	 *
	 */
	class ParentTreeNodeType extends TreeNodeType{
		
		private ParentTreeNodeType parent = null;
		private boolean isInTree = false;
		
		public ParentTreeNodeType(){
		}
		
		public ParentTreeNodeType(TreeNodeType type){
			this.isactive = type.isIsactive();
			this.key = type.getKey();
			this.names = type.getNames();
			for (TreeNodeType child : type.getChildrens()){
				ParentTreeNodeType t = new ParentTreeNodeType(child);
				if (this.childrens == null){
					this.childrens = new ArrayList<TreeNodeType>();
				}
				this.childrens.add(t);
				t.parent = this;
			}
		
		}
		public ParentTreeNodeType getParent(){
			return this.parent;
		}
		public void setParent(ParentTreeNodeType parent){
			this.parent = parent;
		}
		
		public boolean isInTree(){
			return this.isInTree;
		}
		public void setIsInTree(boolean inTree){
			this.isInTree = inTree;
		}
	}
}
