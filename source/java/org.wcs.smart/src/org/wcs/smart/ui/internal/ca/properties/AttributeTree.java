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

import java.lang.reflect.InvocationTargetException;
import java.text.Collator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.DataModelManager;
import org.wcs.smart.ca.datamodel.DmObject;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.ui.ca.properties.AttributeInfoPanel;
import org.wcs.smart.ui.ca.properties.AttributeItemDialog;
import org.wcs.smart.ui.properties.AttributeTreeContentProvider;
import org.wcs.smart.ui.properties.AttributeTreeLabelProvider;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Class that creates an attribute tree viewer for
 * a given attribute.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class AttributeTree {

	private static final String LOADING = Messages.AttributeTree_Loading;
	private TreeViewer viewer = null;
	private AttributeTreeChangeListener listener = null;
	private Session currentSession = null;
	private boolean isEditable = false;
	private Attribute attribute;
	private AttributeInfoPanel info;
	
	private List<AttributeTreeNode> deletedNodes = new ArrayList<AttributeTreeNode>();
	
	
	public AttributeTree(AttributeInfoPanel info, boolean isEditable){
		this.isEditable = isEditable;
		this.info = info;
	}
	/**
	 * Sets the listener fired when modifications occur.  
	 * @param listener
	 */
	public void setListener(AttributeTreeChangeListener listener){
		this.listener = listener;
	}
	
	private void fireChangeListener(){
		if (listener != null){
			listener.treeModified();
		}
	}
	
	/**
	 * Update the key associated with the attribute.
	 * <p>
	 * This is done so if the attribute key is updated
	 * then tree nodes are imported the correct
	 * file will be searched for.
	 * </p>
	 * 
	 * @param newKey new attribute key
	 */
	public void updateAttributeKey(String newKey){
		if (this.attribute != null){
			this.attribute.setKeyId(newKey);
		}
	}
	
	public void refresh(Language newLanguage){
		((AttributeTreeLabelProvider)viewer.getLabelProvider()).setLanguage(newLanguage);
		viewer.refresh();
	}
	/**
	 * Sets the attribute input to the attribute tree.  Attribute
	 * tree nodes are cloned for working on; this is done inside a progress
	 * monitor.  When complete the cloned nodes should be merged back
	 * into the original nodes.
	 * 
	 * @param attribute
	 * @param currentSession current hibernate session
	 */
	public void setInput(Attribute attribute, final Session currentSession){
		this.currentSession = currentSession;
		this.attribute = attribute;
		final List<AttributeTreeNode> clonedroots = new ArrayList<AttributeTreeNode>();
		if (attribute.getTree() == null){
			viewer.setInput(clonedroots);
			refreshTree();
		}else{
			viewer.setInput(LOADING);
			refreshTree();
			final Shell currentShell = viewer.getTree().getShell();
			Job j = new Job(Messages.AttributeTree_LoadAttributeTreeMessage){

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					monitor.setTaskName(Messages.AttributeTree_LoadAttributeTreeMessage);
					
					
					if (!isEditable){
						//not editable so we can access the root nodes directly
						List<AttributeTreeNode> roots = AttributeTree.this.attribute.getTree();
						for (AttributeTreeNode n : roots){
							n.getNames().size();
							for (AttributeTreeNode kid : n.getChildren()){
								kid.getNames().size();
							}
						}
						//not editable lets show the loaded roots
						if (currentShell.isDisposed()) return Status.CANCEL_STATUS;
						currentShell.getDisplay().syncExec(new Runnable(){
							@Override
							public void run() {
								if (viewer.getControl().isDisposed()) return;
								
								viewer.setInput(AttributeTree.this.attribute.getTree());
								refreshTree();
								fireChangeListener();
							}});	
						return Status.OK_STATUS;
					}
					
					//editable we need to clone so we can cancel any changes
					for (AttributeTreeNode node : AttributeTree.this.attribute.getTree()) {
						try{
							AttributeTreeNode cloned = node.clone(
								AttributeTree.this.attribute.getConservationArea(),
								AttributeTree.this.attribute.getConservationArea(), 
								null,
								AttributeTree.this.attribute.getConservationArea().getDefaultLanguage().getCode(),
								null, true);
							clonedroots.add(cloned);
						}catch (Exception ex){
							if (!currentSession.isOpen() || monitor.isCanceled()){
								return Status.CANCEL_STATUS;
							}
							throw ex;
						}
					}
					if (currentShell.isDisposed()) return Status.CANCEL_STATUS;
					currentShell.getDisplay().syncExec(new Runnable(){
						@Override
						public void run() {
							if (viewer.getControl().isDisposed()) return;
							
							viewer.setInput(clonedroots);
							refreshTree();
							fireChangeListener();
						}});	
					return Status.OK_STATUS;
				}				
			};
			j.schedule();
		}
	}

	/**
	 * 
	 * @return the list of root nodes
	 */
	@SuppressWarnings("unchecked")
	public List<AttributeTreeNode> getRootNodes(){
		if (viewer.getInput() instanceof List){
			return (List<AttributeTreeNode>)viewer.getInput();
		}else{
			return null;
		}
	}
	

	
	/**
	 * 
	 * @param parent
	 * @param currentLanguage
	 * 
	 * @return composite that describes the attribute tree
	 */
	public Composite createTree(Composite parent){
		
		Composite comp = new Composite(parent, SWT.NONE);
		comp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		comp.setLayout(new GridLayout(isEditable ? 2 : 1, false));
		
		if (isEditable){
			PatternFilter patternFilter = new PatternFilter(){			
				protected boolean isChildMatch(Viewer viewer, Object element) {
					Object parent = ((AttributeTreeContentProvider)((TreeViewer)viewer).getContentProvider()).getParent(element);
					if (parent != null) {
						return (isLeafMatch(viewer, parent) ? true : isChildMatch(viewer, parent));
					}
					return false;
				}
	
				@Override
				protected boolean isLeafMatch(Viewer viewer, Object element) {
					String labelText = ((AttributeTreeLabelProvider) ((TreeViewer) viewer).getLabelProvider()).getText(element);
					if (labelText == null) {
						return false;
					}
					return (wordMatches(labelText) ? true : isChildMatch(viewer,element));
				}
				
			};
			FilteredTree fTree = new FilteredTree(comp, SWT.MULTI | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL, patternFilter, true);
			viewer = fTree.getViewer();
		}else{
			viewer = new TreeViewer(comp, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		}
		
		if (!isEditable){
			viewer.setContentProvider(new AttributeTreeContentProvider(false, false));
		}else{
			viewer.setContentProvider(new AttributeTreeContentProvider(false, true));
		}
		viewer.setLabelProvider(new AttributeTreeLabelProvider());
		viewer.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,true));
		((GridData)viewer.getTree().getLayoutData()).heightHint = 80;
		((GridData)viewer.getTree().getLayoutData()).widthHint = 100;
		viewer.setAutoExpandLevel(2);
		viewer.setInput(LOADING);
		
		if (isEditable){
			int operations = DND.DROP_MOVE;
			Transfer[] transferTypes = new Transfer[]{LocalSelectionTransfer.getTransfer()};
			viewer.addDragSupport(operations, transferTypes , new AttributeTreeDragListener(viewer));
			viewer.addDropSupport(operations, transferTypes, new AttributeTreeDropListener(viewer));
			
			Composite buttonPanel = new Composite(comp, SWT.NONE);
			buttonPanel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
			buttonPanel.setLayout(new GridLayout(1, false));
			
			final Button btnAdd = new Button(buttonPanel, SWT.NONE);
			btnAdd.setText(DialogConstants.ADD_BUTTON_TEXT);
			btnAdd.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
			btnAdd.setEnabled(false);
			btnAdd.setToolTipText(Messages.AttributeTree_AddButton_Tooltip);
			btnAdd.addSelectionListener(new SelectionAdapter(){
				@Override
				public void widgetSelected(SelectionEvent e) {
					addItem(SmartDB.getCurrentConservationArea().getDefaultLanguage());
				}
			});
	
			final Button btnEdit = new Button(buttonPanel, SWT.NONE);
			btnEdit.setEnabled(false);
			btnEdit.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
			btnEdit.setText(DialogConstants.EDIT_BUTTON_TEXT);
			btnEdit.setToolTipText(Messages.AttributeTree_EditButton_Tooltip);
			btnEdit.addSelectionListener(new SelectionAdapter(){
				@Override
				public void widgetSelected(SelectionEvent e) {
					editItem(viewer, ((AttributeTreeLabelProvider)viewer.getLabelProvider()).getLanguage() );
				}
			});
			
			final Button btnImport = new Button(buttonPanel, SWT.NONE);
			btnImport.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
			btnImport.setText(Messages.AttributeTree_ImportButtonText);
			btnImport.setToolTipText(Messages.AttributeTree_ImportButtonTooltip);
			btnImport.addSelectionListener(new SelectionAdapter(){
				@Override
				public void widgetSelected(SelectionEvent e){
					ImportAttributeProcessor processor = new ImportAttributeProcessor(info.getNameKeyComposite().getCurrentKey(),  
							attribute, getRootNodes());
					processor.importAttribute();
					viewer.setInput(getRootNodes());
					fireChangeListener();
					viewer.expandToLevel(2);
				}
			});
			
			final Button btnSort = new Button(buttonPanel, SWT.NONE);
			btnSort.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
			btnSort.setText(Messages.AttributeTree_SortAllButton);
			btnSort.setToolTipText(Messages.AttributeTree_SortAllTooltip);
			btnSort.addSelectionListener(new SelectionAdapter(){
				@Override
				public void widgetSelected(SelectionEvent e){
					List<AttributeTreeNode> nodes = (List<AttributeTreeNode>) viewer.getInput();
					List<AttributeTreeNode> toSort = new ArrayList<AttributeTreeNode>();
					Language lang = ((AttributeTreeLabelProvider)viewer.getLabelProvider()).getLanguage();
					
					sortNodes(nodes, lang);
					toSort.addAll(nodes);
					while(toSort.size() > 0){
						AttributeTreeNode p = toSort.remove(0);
						sortNodes(p.getChildren(), lang);
						toSort.addAll(p.getChildren());
					}
					refreshTree();
				}
			});
			
			Label lbl = new Label(buttonPanel, SWT.SEPARATOR | SWT.HORIZONTAL);
			lbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			
			final Button btnDisable = new Button(buttonPanel, SWT.NONE);
			btnDisable.setText(DialogConstants.DISABLE_BUTTON_TEXT);
			btnDisable.setToolTipText(Messages.AttributeTree_DisableButton_ToolTip);
			btnDisable.setEnabled(false);
			btnDisable.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
			btnDisable.addSelectionListener(new SelectionAdapter(){
				@Override
				public void widgetSelected(SelectionEvent e) {
					disableItem(viewer, !btnDisable.getText().equals(DialogConstants.DISABLE_BUTTON_TEXT));
					
					if (btnDisable.getText().equals(DialogConstants.DISABLE_BUTTON_TEXT)){
						btnDisable.setText(DialogConstants.ENABLE_BUTTON_TEXT);
					}else{
						btnDisable.setText(DialogConstants.DISABLE_BUTTON_TEXT);
					}
				}
			});
		
			final Button btnDisableAll = new Button(buttonPanel, SWT.NONE);
			btnDisableAll.setText(DialogConstants.DISABLEALL_BUTTON_TEXT);
			btnDisableAll.setToolTipText(Messages.AttributeTree_DisableAll_Tooltip);
			btnDisableAll.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
			btnDisableAll.addSelectionListener(new SelectionAdapter(){
				@Override
				public void widgetSelected(SelectionEvent e) {
					for (AttributeTreeNode node : getRootNodes()){
						disableNode(node, false);
					}
					viewer.refresh();
					fireChangeListener();
					viewer.setSelection(viewer.getSelection());
				}
			});
			
			
			final Button btnEnableeAll = new Button(buttonPanel, SWT.NONE);
			btnEnableeAll.setText(DialogConstants.ENABLEALL_BUTTON_TEXT); 
			btnEnableeAll.setToolTipText(Messages.AttributeTree_EnableAll_Tooltip);
			btnEnableeAll.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
			btnEnableeAll.addSelectionListener(new SelectionAdapter(){
				@Override
				public void widgetSelected(SelectionEvent e) {
					for (AttributeTreeNode node : getRootNodes()){
						enableAll(node);
					}
					viewer.refresh();
					fireChangeListener();
					viewer.setSelection(viewer.getSelection());
				}
			});
			
			
			lbl = new Label(buttonPanel, SWT.SEPARATOR | SWT.HORIZONTAL);
			lbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			
			final Button btnDelete = new Button(buttonPanel, SWT.NONE);
			btnDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
			btnDelete.setEnabled(false);
			btnDelete.setToolTipText(Messages.AttributeTree_Delete_Tooltip);
			btnDelete.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
			btnDelete.addSelectionListener(new SelectionAdapter(){
				@Override
				public void widgetSelected(SelectionEvent e) {
					deleteNodes();	
				}
			});
			
			viewer.addSelectionChangedListener(new ISelectionChangedListener() {
				@Override
				public void selectionChanged(SelectionChangedEvent event) {
					if (viewer.getSelection().isEmpty()){
						btnAdd.setEnabled(false);
						btnEdit.setEnabled(false);
						btnDisable.setEnabled(false);
						btnDelete.setEnabled(false);
						return;
					}
					Object x = ((IStructuredSelection)viewer.getSelection()).getFirstElement();
					if (x instanceof AttributeTreeContentProvider.RootNode){
						btnEdit.setEnabled(false);
						btnDisable.setEnabled(false);
						btnDelete.setEnabled(false);
						btnAdd.setEnabled(true);
					}else if (x instanceof AttributeTreeNode){
						btnEdit.setEnabled(true);
						btnDisable.setEnabled(true);
						btnDelete.setEnabled(true);
						btnAdd.setEnabled(true);
						if (((AttributeTreeNode)x).getIsActive()){
							btnDisable.setText(DialogConstants.DISABLE_BUTTON_TEXT);
						}else{
							btnDisable.setText(DialogConstants.ENABLE_BUTTON_TEXT);
						}
					}
					
				}
			});
		}
		return comp;
	}
	
	private void sortNodes(List<AttributeTreeNode> nodes, final Language lang){
		Collections.sort(nodes, new Comparator<AttributeTreeNode>() {
			@Override
			public int compare(AttributeTreeNode o1,
					AttributeTreeNode o2) {
				return Collator.getInstance().compare(o1.findName(lang).toUpperCase(), o2.findName(lang).toUpperCase());
			}
		});
		for (int i = 0; i < nodes.size();i++){
			nodes.get(i).setNodeOrder(i);
		}
	}
	
	private void deleteNodes() {
		
		final ArrayList<AttributeTreeNode> toDelete = new ArrayList<AttributeTreeNode>();
		Language currentLang = ((AttributeTreeLabelProvider)viewer.getLabelProvider()).getLanguage();
		for (Iterator<?> iterator = ((IStructuredSelection)viewer.getSelection()).iterator(); iterator.hasNext();) {
			Object x = (Object) iterator.next();
			if (x instanceof AttributeTreeNode){
				List<AttributeTreeNode> toProcess = new ArrayList<AttributeTreeNode>();
				toProcess.add((AttributeTreeNode)x);
				while(toProcess.size() > 0){
					AttributeTreeNode item = toProcess.remove(0);
					toDelete.remove(item);
					toDelete.add(0, item);
					if (item.getChildren() != null){
						toProcess.addAll(item.getChildren());
					}
				}
			}
		}
		
		if(toDelete.size() == 0){
			return;
		}
		
		String deleteQuestion = null;
		if (toDelete.size()  > 1){
			deleteQuestion = MessageFormat.format(Messages.AttributeTree_ConfirmDelete_DialogMessage1,new Object[]{Integer.valueOf(toDelete.size())});
		}else{
			deleteQuestion= MessageFormat.format(Messages.AttributeTree_ConfirmDelete_DialogMessage2, new Object[]{ toDelete.get(0).findName(currentLang) });
		}
				
				
		boolean ret = MessageDialog.openConfirm(viewer.getTree().getShell(), Messages.AttributeTree_ConfirmDelete_DialogTitle, deleteQuestion);
		if (!ret){
			return;
		}
				
		runInProgressDialog(new IRunnableWithProgress() {
			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException,
				InterruptedException {
				monitor.beginTask(Messages.AttributeTree_DeleteProgress, toDelete.size());
				final Display shell = Display.getDefault();
				for (AttributeTreeNode node : toDelete){
					monitor.subTask(Messages.AttributeTree_DeleteSubProgress + node.getName());
					boolean delete = false;
					try{
						delete = DataModelManager.INSTANCE.validateDelete(node, new NullProgressMonitor(), AttributeTree.this.currentSession);
						if (delete){
							if (node.getParent() != null){
								if (node.getParent().getActiveChildren() != null){
									node.getParent().getActiveChildren().remove(node);
								}
								node.getParent().getChildren().remove(node);
								node.setParent(null);
								node.setAttribute(null);
							}else{
								getRootNodes().remove(node);
								node.setAttribute(null);
							}
							deletedNodes.add(node);
						}
					}catch (final Exception ex){
						shell.syncExec(new Runnable(){
							@Override
							public void run() {
								MessageDialog.openError(Display.getDefault().getActiveShell(), Messages.DeleteManager_DeleteError_Dialog_Title, ex.getLocalizedMessage());
							}
						});	
					}	
					monitor.worked(1);
				}
				shell.syncExec(new Runnable(){
					@Override
					public void run() {
						refreshTree();
						fireChangeListener();
					}});
				
				
			}
		});
	}
	
	public List<AttributeTreeNode> getDeletedNodes(){
		return this.deletedNodes;
	}
	public void clearDeletedNodes(){
		this.deletedNodes.clear();
	}
	/*
	 * Run a taks in a progress monitor
	 * @param runnable
	 */
	private void runInProgressDialog(IRunnableWithProgress runnable){
		ProgressMonitorDialog dialog = new ProgressMonitorDialog(viewer.getTree().getShell());
		try {
			dialog.run(true, true, runnable);		
		} catch (Exception ex) {
			SmartPlugIn.displayLog(Messages.AttributeTree_GenericError, ex);
		}
	}
	
	/*
	 * Enables the attribute tree node and all its children
	 */
	private void enableAll(AttributeTreeNode node){
		node.setIsActive(true);
		if (node.getChildren() == null) return;
		for (AttributeTreeNode child: node.getChildren()){
			enableAll(child);
		}		
	}
	
	/*
	 * disable items in the tree
	 */
	private void disableItem(TreeViewer viewer, boolean enabled){
		for (Iterator<?> iterator = ((IStructuredSelection)viewer.getSelection()).iterator(); iterator.hasNext();) {
			Object x = (Object) iterator.next();
			if (x instanceof AttributeTreeNode){
				disableNode((AttributeTreeNode)x, enabled);	
			}	
		}
		viewer.refresh();
		fireChangeListener();
	}
	
	/*
	 * disables or enables a tree node
	 * 
	 * @param enabled true if the node is to be activated; false to de-activate
	 */
	public void disableNode(AttributeTreeNode node, boolean enabled){
		if (!enabled){
			//disable node and all children
			node.setIsActive(enabled);
			if (node.getChildren() != null){
				for(AttributeTreeNode child: node.getChildren()){
					disableNode(child, enabled);
				}
			}
		}else{
			//enable category and parent
			if (node.getIsActive()){
				return ;
			}
			node.setIsActive(enabled);
			if (node.getParent() != null){
				disableNode(node.getParent(), enabled);
			}
		}
	}
	
	/*
	 * edits an item in the tree
	 */
	private void editItem(TreeViewer viewer, Language currentLanguage){
		Object x = ((IStructuredSelection)viewer.getSelection()).getFirstElement();
		if (x instanceof AttributeTreeNode){
			List<? extends DmObject> siblings = null;
			if ( ((AttributeTreeNode)x).getParent() == null){
				siblings = getRootNodes();
			}else{
				siblings = ((AttributeTreeNode)x).getParent().getChildren();
			}
			AttributeItemDialog dd = new AttributeItemDialog(Display.getCurrent().getActiveShell(), (AttributeTreeNode)x, siblings, currentLanguage);
			int ret = dd.open();
			if (ret == Window.CANCEL){
				return;
			}
		}
		refreshTree();
		fireChangeListener();
	}
	
	/*
	 * adds item to tree
	 */
	private void addItem(Language currentLanguage){
		Object x = ((IStructuredSelection)viewer.getSelection()).getFirstElement();
		if (x instanceof AttributeTreeContentProvider.RootNode || x instanceof AttributeTreeNode){
			List<? extends DmObject> siblings = null;
			AttributeTreeNode parent = null;
			if (x instanceof AttributeTreeContentProvider.RootNode){
				siblings = getRootNodes();
			}else{
				siblings = ((AttributeTreeNode)x).getChildren();
				parent = ((AttributeTreeNode)x);
			}
			
			AttributeTreeNode it = new AttributeTreeNode();
			it.setParent(parent);
			
			AttributeItemDialog dd = new AttributeItemDialog(Display.getCurrent().getActiveShell(), it, siblings, currentLanguage);
			int ret = dd.open();
			if (ret == Window.CANCEL){
				return;
			}
			
			
			if (parent != null){
				if (parent.getChildren() == null){
					parent.setChildren(new ArrayList<AttributeTreeNode>());
				}
				parent.getChildren().add(it);
			}else{
				getRootNodes().add(it);
			}
			viewer.setExpandedState(x, true);
			
			it.setIsActive(true);
			if (siblings == null){
				it.setNodeOrder(0);
			}else{
				it.setNodeOrder(siblings.size());
			}
		
			refreshTree();
			fireChangeListener();
		}
	}
	
	/*
	 * refresh the tree
	 */
	private void refreshTree() {
		viewer.refresh();
	}
	
	public interface AttributeTreeChangeListener{
		public void treeModified();
	}

}


/**
 * Drag listener for attribute tree
 * 
 * @author Emily
 * 
 */
class AttributeTreeDragListener implements DragSourceListener {

	private TreeViewer viewer;

	public AttributeTreeDragListener(TreeViewer viewer) {
		this.viewer = viewer;
	}

	/**
	 * @see org.eclipse.swt.dnd.DragSourceListener#dragStart(org.eclipse.swt.dnd.DragSourceEvent)
	 */
	@Override
	public void dragStart(DragSourceEvent event) {
		LocalSelectionTransfer.getTransfer()
				.setSelection(viewer.getSelection());
		event.doit = true;

	}

	/**
	 * @see org.eclipse.swt.dnd.DragSourceListener#dragSetData(org.eclipse.swt.dnd.DragSourceEvent)
	 */
	@Override
	public void dragSetData(DragSourceEvent event) {
		if (LocalSelectionTransfer.getTransfer()
				.isSupportedType(event.dataType)) {
			event.data = viewer.getSelection();
		}

	}

	/**
	 * @see org.eclipse.swt.dnd.DragSourceListener#dragFinished(org.eclipse.swt.dnd.DragSourceEvent)
	 */
	@Override
	public void dragFinished(DragSourceEvent event) {
		LocalSelectionTransfer.getTransfer().setSelection(null);
		viewer.refresh();
	}

}

/**
 * Drop listener for attribute tree
 * 
 * @author Emily
 * 
 */
class AttributeTreeDropListener extends ViewerDropAdapter {

	private TreeViewer viewer;

	/**
	 * @param viewer
	 */
	protected AttributeTreeDropListener(TreeViewer viewer) {
		super(viewer);
		this.viewer = viewer;

	}

	/**
	 * @see org.eclipse.jface.viewers.ViewerDropAdapter#performDrop(java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public boolean performDrop(Object data) {

		StructuredSelection selection = (StructuredSelection) LocalSelectionTransfer
				.getTransfer().getSelection();
		if (selection == null) {
			return false;
		}
		Object obj = selection.getFirstElement();
		int loc = getCurrentLocation();
		if (obj instanceof AttributeTreeNode
				&& getCurrentTarget() instanceof AttributeTreeNode) {
			Attribute.moveAttributeTreeNode((List<AttributeTreeNode>)viewer.getInput(),
					(AttributeTreeNode) obj,
					(AttributeTreeNode) getCurrentTarget(),
					loc == LOCATION_BEFORE);
			return true;
		}
		return false;
	}

	/**
	 * @see org.eclipse.jface.viewers.ViewerDropAdapter#validateDrop(java.lang.Object,
	 *      int, org.eclipse.swt.dnd.TransferData)
	 */
	@Override
	public boolean validateDrop(Object target, int operation,
			TransferData transferType) {

		StructuredSelection selection = (StructuredSelection) LocalSelectionTransfer
				.getTransfer().getSelection();
		if (selection == null) {
			return false;
		}
		Object obj = selection.getFirstElement();

		if (obj instanceof AttributeTreeNode) {
			AttributeTreeNode toMove = (AttributeTreeNode) obj;

			if (target instanceof AttributeTreeNode) {
				AttributeTreeNode toMoveTo = (AttributeTreeNode) target;
				if ((toMoveTo.getParent() == null && toMove.getParent() == null)
						|| (toMoveTo.getParent() != null
								&& toMove.getParent() != null && toMoveTo
								.getParent().equals(toMove.getParent()))) {
					return true;
				}
			}
		}
		return false;
	}

}
