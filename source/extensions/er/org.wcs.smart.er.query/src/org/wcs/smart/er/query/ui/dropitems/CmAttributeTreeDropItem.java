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
package org.wcs.smart.er.query.ui.dropitems;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.dataentry.dialog.CmAttributeTreeContentProvider;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.CmAttributeTreeNode;
import org.wcs.smart.er.query.internal.Messages;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.ui.TreeDropDownViewer;
import org.wcs.smart.query.ui.model.impl.AttributeTreeDropItem;
import org.wcs.smart.util.UuidUtils;

/**
 * Configurable model attribute list drop item.  Only shows
 *  the attribute list items from the configured model.
 *  
 * @author Emily
 *
 */
public class CmAttributeTreeDropItem extends AttributeTreeDropItem {

	private static CmAttributeTreeContentProvider cProvider = 
			new CmAttributeTreeContentProvider(true, false);
	
	private static LabelProvider lProvider = new LabelProvider(){
		@Override
		public String getText(Object element){
			if (element instanceof CmAttributeTreeNode){
				String label = ((CmAttributeTreeNode)element).getName();
				if (label == null || label.isEmpty()){
					label = ((CmAttributeTreeNode)element).getDmTreeNode().getName();
				}
				return label;
			}
			return super.getText(element);
		}
	};
	
	
	private CmAttribute cmAttribute;

	private List<CmAttributeTreeNode> roots = null;
	private TreeDropDownViewer treeviewer;
	private Object input = Collections.singletonList(Messages.CmAttributeTreeDropItem_LoadingString);
	private CmAttributeTreeNode defaultNode = null;
	/*
	 * Job to load the attribute list options
	 */
	private Job loadItemsJobs = new Job(Messages.CmAttributeTreeDropItem_LoadingJobName){

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			
			Session s = HibernateManager.openSession();
			s.beginTransaction();
			try{
				CmAttribute attribute = (CmAttribute) s.load(CmAttribute.class, cmAttribute.getUuid());
				roots = attribute.getCurrentTree();
				List<CmAttributeTreeNode> toLoad = new ArrayList<CmAttributeTreeNode>(roots);
				while(toLoad.size() > 0){
					CmAttributeTreeNode n = toLoad.remove(0);
					n.getName();
					if (n.getDmTreeNode() != null){
						n.getDmTreeNode().getName();
					}
					toLoad.addAll(n.getChildren());
				}
				if (currentSelection != null){
					if (attribute.isUseCustomConfig()){
						defaultNode = (CmAttributeTreeNode) s.createCriteria(CmAttributeTreeNode.class)
							.add(Restrictions.eq("dmTreeNode", currentSelection))  //$NON-NLS-1$
							.add(Restrictions.eq("configurableModel", attribute.getNode().getModel())) //$NON-NLS-1$
							.add(Restrictions.eq("attribute", attribute)) //$NON-NLS-1$
							.uniqueResult();
					}else{
						defaultNode = (CmAttributeTreeNode) s.createCriteria(CmAttributeTreeNode.class)
								.add(Restrictions.eq("dmTreeNode", currentSelection))  //$NON-NLS-1$
								.add(Restrictions.eq("configurableModel", attribute.getNode().getModel())) //$NON-NLS-1$
								.add(Restrictions.eq("dmAttribute", currentSelection.getAttribute())) //$NON-NLS-1$
								.uniqueResult();
					}
				}
				
			}catch(Exception ex){
				QueryPlugIn.log("Could not initialize attribute tree items", ex); //$NON-NLS-1$
			}finally{
				s.getTransaction().rollback();
				s.close();
			}
			input = roots;
			Display.getDefault().asyncExec(new Runnable(){
				@Override
				public void run() {
//					if (defaultNode != null && lblitem != null && !lblitem.isDisposed()){
//						lblitem.setText( formatStringForLabel(defaultNode.getDefaultName()));
//					}
					updateLabel();
					if (treeviewer == null || 
							treeviewer.getTreeViewer().getControl().isDisposed()){
						return;
					}
					treeviewer.getTreeViewer().setInput(roots);
					treeviewer.getTreeViewer().refresh();
				}
					
			});
			
			
			return Status.OK_STATUS;
		}
	};
	
	public CmAttributeTreeDropItem(CmAttribute cmAttribute, CategoryAttribute att) {
		super(att);
		this.cmAttribute = cmAttribute;
		this.key = "category:" + att.getCategory().getHkey() + ":cmattribute:" + att.getAttribute().getType().typeKey + ":" + UuidUtils.uuidToString(cmAttribute.getUuid()) + ":" + att.getAttribute().getKeyId();  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}
	
	@Override
	protected void loadAttributes(){
		loadItemsJobs.schedule();
	}
	
	protected void showTree(){		
		treeviewer = getTreeEditor();
		if (treeviewer == null) {
			return;
		}
		treeviewer.getTreeViewer().setLabelProvider(lProvider);
		treeviewer.getTreeViewer().setContentProvider(cProvider);
		
		treeviewer.getTreeViewer().setInput(input);	
		treeviewer.positionAndShow(CmAttributeTreeDropItem.this.getWidget(), new ISelectionListener(){

			@Override
			public void selectionChanged(IWorkbenchPart part,
					ISelection selection) {
				currentSelection = null;
				CmAttributeTreeNode thisselection = null;
				if (selection != null && !selection.isEmpty()){
					thisselection = (CmAttributeTreeNode) ((IStructuredSelection) selection).getFirstElement();
				}
				if (!lblitem.isDisposed()){
					if (thisselection != null){
						if (thisselection.getDmTreeNode() == null){
							MessageDialog.openError(lblitem.getShell(), Messages.CmAttributeTreeDropItem_ErrorDialog, Messages.CmAttributeTreeDropItem_ErrorMessage);
							lblitem.setText(""); //$NON-NLS-1$
						}else{
							currentSelection = thisselection.getDmTreeNode();
							String txt = thisselection.getName();
							if (txt == null || txt.isEmpty()){
								txt = thisselection.getDmTreeNode().getName();
							}
							lblitem.setText( formatStringForLabel(txt));
						}
					}else{
						lblitem.setText(""); //$NON-NLS-1$
					}
				}
				getTargetPanel().redraw();
				CmAttributeTreeDropItem.this.queryChanged();
			}});

	}

	@Override
	protected void createComposite(Composite parent) {
		super.createComposite(parent);
		updateLabel();
	}
	
	private void updateLabel(){
		if (defaultNode != null){
			if (lblitem.isDisposed()) return;
			lblitem.setText( formatStringForLabel(defaultNode.getName().isEmpty() ? defaultNode.getDmTreeNode().getName() : defaultNode.getName()));
		}
	}
}
