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
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.paws.model.AbstractPawsClass;
import org.wcs.smart.paws.model.PawsConfiguration;
import org.wcs.smart.paws.model.PawsQueryClass;
import org.wcs.smart.paws.model.PawsSimpleClass;
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
									MessageDialog.openError(getShell(), "Error", MessageFormat.format("The query ''{0}'' is not supported.  Only shared queries that return a list of observations are supported.", temp.getName()));
								}
							}	
						}
					}
				}
				if (q != null) classTable.addQueries(q);
			}
		});
		
	}
	
	private void fireModified() {
		//TODO:
	}
	
	public void doSave(PawsConfiguration config, Session session) {
		List<AbstractPawsClass> newItems =  classTable.getClassifications();
		
		for (AbstractPawsClass pc : newItems) {
			pc.setConfiguration(config);
			session.saveOrUpdate(pc);
		}
		
		//need to delete missing items
		List<AbstractPawsClass> currentItems = new ArrayList<>();
		currentItems.addAll(QueryFactory.buildQuery(session, PawsSimpleClass.class, new Object[] {"configuration", config}).list());
		currentItems.addAll(QueryFactory.buildQuery(session, PawsQueryClass.class, new Object[] {"configuration", config}).list());
		
		
		for (AbstractPawsClass pc : currentItems) {
			if (!newItems.contains(pc)) session.delete(pc);
		}
	}
	
	public void initialize(PawsConfiguration config, Session session) {
		List<AbstractPawsClass> currentItems = new ArrayList<>();
		currentItems.addAll(QueryFactory.buildQuery(session, PawsSimpleClass.class, new Object[] {"configuration", config}).list());
		currentItems.addAll(QueryFactory.buildQuery(session, PawsQueryClass.class, new Object[] {"configuration", config}).list());
		
		currentItems.forEach(e->{
			if (e instanceof PawsQueryClass) {
				Query temp = QueryHibernateManager.getInstance().findQuery(session, ((PawsQueryClass) e).getQueryUuid(), QueryTypeManager.INSTANCE.findQueryType( ((PawsQueryClass) e).getQueryType()));
				((PawsQueryClass)e).setCachedQuery(temp);
				temp.getName();
			}else if (e instanceof PawsSimpleClass) {
				PawsSimpleClass sc = (PawsSimpleClass)e;
				sc.getCategory().getFullCategoryName();
				if (sc.getAttribute() != null) sc.getAttribute().getName();
				if (sc.getAttributeListItem() != null) sc.getAttributeListItem().getName();
				if (sc.getAttributeTreeNode() != null) sc.getAttributeTreeNode().getName();
				
			}
			e.getClassification();
		});
		
		Display.getDefault().asyncExec(()->{
			classTable.initItem(currentItems);
		});
	}
}
