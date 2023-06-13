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
package org.wcs.smart.paws.ui.config;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.paws.internal.Messages;
import org.wcs.smart.paws.model.AbstractPawsClass;
import org.wcs.smart.paws.model.PawsConfiguration;
import org.wcs.smart.paws.model.PawsQueryClass;
import org.wcs.smart.paws.model.PawsSimpleClass;
import org.wcs.smart.query.QueryDataModelManager;
import org.wcs.smart.query.QueryHibernateManager;
import org.wcs.smart.query.QueryTypeManager;
import org.wcs.smart.query.common.model.ObservationQuery;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.ui.editor.QueryEditorInput;

/**
 * Observation classification composite
 * 
 * @author Emily
 *
 */
public class ClassificationComposite extends Composite{

	ClassificationTableComposite classTable;
	
	public ClassificationComposite(Composite parent, ConfigurationEditor editor) {
		super(parent, SWT.NONE);
		
		setLayout(new GridLayout());
				
		classTable = new ClassificationTableComposite(this);
		classTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		classTable.addListener(SWT.Selection, e->editor.setDirty(true));
		
		DropTarget dtarget = new DropTarget(classTable, DND.DROP_MOVE);
		dtarget.setTransfer(new Transfer[] { LocalSelectionTransfer.getTransfer() });
		dtarget.addDropListener(new DropTargetAdapter() {
			@Override
			public void dragEnter(DropTargetEvent event) {}
			@Override
			public void dragLeave(DropTargetEvent event) {}
			@Override
			public void dragOperationChanged(DropTargetEvent event) {}
			@Override
			public void dragOver(DropTargetEvent event) {}
			@Override
			public void dropAccept(DropTargetEvent event) {}
			@Override
			public void drop(DropTargetEvent event) {
				if (event.detail == DND.DROP_NONE ){
					return;
				}
				StructuredSelection selection = (StructuredSelection) LocalSelectionTransfer.getTransfer().getSelection();
				if (selection == null) return;
				
				List<Query> q = new ArrayList<>();
				try(Session s = HibernateManager.openSession()){
					for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
						Object x = iterator.next();
						if (x instanceof QueryEditorInput) {
							QueryEditorInput dragItem = (QueryEditorInput) x;
							Query temp = QueryHibernateManager.getInstance().findQuery(s, dragItem.getUuid(), dragItem.getType());
							if (temp != null) {
								if (temp instanceof ObservationQuery && temp.getIsShared()){
									q.add(temp);
								}else{
									MessageDialog.openError(getShell(), Messages.ClassificationComposite_ErrorTitle, MessageFormat.format(Messages.ClassificationComposite_QueryNotSupported, temp.getName()));
								}
							}	
						}
					}
				}
				if (q != null) classTable.addQueries(q);
			}
		});
	}
	
	public void doSave(PawsConfiguration config, Session session) {
		List<AbstractPawsClass> allItems =  classTable.getClassifications();
		
		for (AbstractPawsClass pc : allItems) {
			if (pc.getConfiguration() == null){
				pc.setConfiguration(config);
			}else if (!pc.getConfiguration().equals(config)){
				//clear from database
				pc.setUuid(null);
				pc.setConfiguration(config);
			}
			session.merge(pc);
		}
		
		//need to delete missing items
		List<AbstractPawsClass> currentItems = new ArrayList<>();
		currentItems.addAll(QueryFactory.buildQuery(session, PawsSimpleClass.class, new Object[] {"configuration", config}).list()); //$NON-NLS-1$
		currentItems.addAll(QueryFactory.buildQuery(session, PawsQueryClass.class, new Object[] {"configuration", config}).list()); //$NON-NLS-1$
		
		
		for (AbstractPawsClass pc : currentItems) {
			if (!allItems.contains(pc)) session.remove(pc);
		}
	}
	
	public void initialize(PawsConfiguration config, Session session) {
		List<ClassificationData> currentItems = new ArrayList<>();
		
		for (PawsSimpleClass pc : QueryFactory.buildQuery(session, PawsSimpleClass.class, new Object[] {"configuration", config}).list()){ //$NON-NLS-1$
			pc.getClassification();
			
			String lbl = ""; //$NON-NLS-1$
			Category c = QueryDataModelManager.getInstance().getCategory(session, pc.getCategoryHkey());
			if (c == null){
				lbl = MessageFormat.format(Messages.ClassificationComposite_CategoryNotFound, pc.getCategoryHkey());
			}else{
			
				if (pc.getAttributeKey() != null){
					if (pc.getAttributeListItemKey() != null){
						AttributeListItem li = QueryDataModelManager.getInstance().getAttributeListItem(session, pc.getAttributeKey(), pc.getAttributeListItemKey());
						if (li == null){
							lbl = MessageFormat.format(Messages.ClassificationComposite_ListItemNotFound, pc.getAttributeListItemKey());
						}else{
							lbl = ClassificationData.createLabel(c,  li.getAttribute(), li);
						}
					}
					
					if (pc.getAttributeTreeNodeHkey() != null){
						AttributeTreeNode node = QueryDataModelManager.getInstance().getAttributeTreeNode(session, pc.getAttributeKey(), pc.getAttributeTreeNodeHkey());
						if (node == null){
							lbl = MessageFormat.format(Messages.ClassificationComposite_TreeNodeNotFound, pc.getAttributeTreeNodeHkey());
						}else{
							lbl = ClassificationData.createLabel(c,  node.getAttribute(), node);
						}
					}
				}else{
					lbl = ClassificationData.createLabel(c, null, null);
				}
				
			}
			currentItems.add(new ClassificationData(pc, lbl));
		}
		
		for (PawsQueryClass pc : QueryFactory.buildQuery(session, PawsQueryClass.class, new Object[] {"configuration", config}).list()){ //$NON-NLS-1$
			Query temp = QueryHibernateManager.getInstance().findQuery(session, ((PawsQueryClass) pc).getQueryUuid(), QueryTypeManager.INSTANCE.findQueryType( ((PawsQueryClass) pc).getQueryType()));
			String lbl = ""; //$NON-NLS-1$
			if (temp != null){
				((PawsQueryClass)pc).setCachedQuery(temp);
				lbl = temp.getName();
			}else{
				lbl = Messages.ClassificationComposite_QueryNotFound;
			}
			
			pc.getClassification();
			currentItems.add(new ClassificationData(pc, lbl));
		}
		
		
		Display.getDefault().asyncExec(()->{
			classTable.initItem(currentItems);
		});
	}
}
