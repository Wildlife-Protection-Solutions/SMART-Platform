package org.wcs.smart.patrol.ui;

import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;
import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.PatrolEventManager;
import org.wcs.smart.patrol.PatrolEventManager.EventType;
import org.wcs.smart.patrol.PatrolEventManager.IPatrolEventListener;
import org.wcs.smart.patrol.PatrolHibernateManager;
import org.wcs.smart.patrol.PatrolUtils;
import org.wcs.smart.patrol.model.PatrolType;

public class PatrolListView extends ViewPart {

	public static final String ID = "org.wcs.smart.patrol.ui.PatrolListView";
	private TableViewer patrolListViewer;
	
	private Job updateJob = new Job("Update Patrol List") {
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			monitor.beginTask("Loading Patrols", 1);
			Session s = PatrolHibernateManager.openSession();
			s.beginTransaction();
			try{
				//String query = "p.uuid, p.id, p.patrolType from Patrol p";
				String strquery = "select p.uuid, p.id, p.patrolType from Patrol p WHERE p.conservationArea = :ca ORDER BY p.startDate desc";
				Query query = s.createQuery(strquery).setParameter("ca", SmartDB.getCurrentConservationArea());

				List results = query.list();
				final PatrolEditorInput[] input = new PatrolEditorInput[results.size()];
				int i = 0;
				for (Iterator iterator = results.iterator(); iterator.hasNext();) {
					Object[] data = (Object[]) iterator.next();					
					input[i++] = new PatrolEditorInput((byte[])data[0], (String)data[1], (PatrolType.Type)data[2]);
				}
				
				monitor.internalWorked(0.5);
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						patrolListViewer.setInput(input);
						patrolListViewer.refresh();
					}
				});
			}finally{
				s.getTransaction().rollback();
				s.close();
			}
			return Status.OK_STATUS;
		}
	};
	
	private PatrolEventManager.IPatrolEventListener patrolListener = new IPatrolEventListener(){
		@Override
		public void eventFired() {
			updateContent();
			
		}};
	
	public PatrolListView() {

	}

	@Override
	public void createPartControl(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		main.setLayout(new GridLayout(1, false));
		
		patrolListViewer = new TableViewer(main, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION);
		Table list = patrolListViewer.getTable();
		list.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		list.setBounds(0, 0, 88, 68);
		
		patrolListViewer.setLabelProvider(new LabelProvider(){
			
			@Override
			public Image getImage(Object element){
				if (element instanceof PatrolEditorInput){
					PatrolEditorInput p = (PatrolEditorInput)element;
					return PatrolUtils.getImage(p.getType());			
				}
				return null;
			}
			@Override
			public String getText(Object element) {
				if (element instanceof PatrolEditorInput){
					return ((PatrolEditorInput)element).getPatrolId();
				}
				return super.getText(element);
			}
		});
		patrolListViewer.setContentProvider(ArrayContentProvider.getInstance());
		patrolListViewer.setInput(new Object[]{"Loading..."});
		patrolListViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		updateContent();
		
		PatrolEventManager.getInstance().addListener(EventType.PATROL_ADDED, patrolListener);
		PatrolEventManager.getInstance().addListener(EventType.PATROL_DELETED, patrolListener);
		
		
		patrolListViewer.addDoubleClickListener(new IDoubleClickListener() {
			
			@Override
			public void doubleClick(DoubleClickEvent event) {
				PatrolEditorInput p = (PatrolEditorInput)((IStructuredSelection)patrolListViewer.getSelection()).getFirstElement();
				if (p != null){
					try {
						IWorkbenchPage page = getSite().getPage();
						page.openEditor(p, PatrolEditor.ID);
					} catch (PartInitException e) {
						//TODO:
						throw new RuntimeException(e);
					}
				}
				
			}
		});
	}

	public void updateContent(){
		updateJob.cancel();
		updateJob.schedule();		
	}
	
	
	@Override
	public void setFocus() {
	}

	
}
