package org.wcs.smart.entity.ui.typelist.editor;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.EditorPart;
import org.hibernate.Session;
import org.wcs.smart.entity.query.DerbyEntitySightingEngine;
import org.wcs.smart.entity.query.EntityQuery;
import org.wcs.smart.entity.query.SightingPagedResults;
import org.wcs.smart.entity.ui.typelist.editor.sightings.EntityFilterComposite;
import org.wcs.smart.entity.ui.typelist.editor.sightings.SightingTable;
import org.wcs.smart.entity.ui.typelist.editor.sightings.SightingTableColumns;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.ui.QueryDateFilterComposite;

public class SightingPage extends EditorPart implements IEntityTypeEditorPage {


	private EntityTypeEditor parentEditor;
	
	private EntityFilterComposite entityFilter;
	private SightingTable sightingTable;
	private QueryDateFilterComposite dateComp ;
	
	public SightingPage(EntityTypeEditor editor){
		this.parentEditor = editor;
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
	
	}

	@Override
	public void doSaveAs() {

	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		super.setSite(site);
		super.setInput(input);
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void createPartControl(Composite parent) {
		FormToolkit toolkit = new FormToolkit(Display.getCurrent());
		toolkit.setBorderStyle(SWT.BORDER);
		
		Form form = toolkit.createForm(parent);
		form.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		

		GridLayout glayout = new GridLayout();
		glayout.verticalSpacing = 0;
		glayout.marginHeight = 0;
		form.getBody().setLayout(glayout);
		form.setText("Sightings");
		
		
		Group g = new Group(form.getBody(), SWT.NONE);
		toolkit.adapt(g);
		g.setText("Filters");
		g.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		g.setLayout(new GridLayout(2, false));
		
		Label l1 = toolkit.createLabel(g, "Date Filter:");
		
		dateComp = new QueryDateFilterComposite(g, null, SightingTableColumns.SIGHTING_DATE_FILTERS);
		dateComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		dateComp.adapt(toolkit);
		
		Label l2 = toolkit.createLabel(g, "Entity Filter:");
		
		entityFilter = new EntityFilterComposite(g);
		entityFilter.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		entityFilter.adapt(toolkit);
		
		
		Button btnRefresh = toolkit.createButton(g, "Update Table", SWT.PUSH);
		btnRefresh.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateResultsTable();
			}

		});
		sightingTable = new SightingTable(form.getBody());
		sightingTable.getTable().getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		toolkit.adapt(sightingTable.getTable().getTable());
	}
	

	private EntityQuery currentQuery;
	
	Job runJob = new Job("Execute Sightings Page"){

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			EntityQuery query = currentQuery;
			Session session = HibernateManager.openSession();
			try{
				DerbyEntitySightingEngine queryEngine = new DerbyEntitySightingEngine();
				final SightingPagedResults results = queryEngine.executeDerbyQuery(query, session, monitor);
				
				Display.getDefault().syncExec(new Runnable(){
					@Override
					public void run() {
						sightingTable.setInput(results);
					}});
				
			}catch (Exception ex){
				//TODO: do something here
				ex.printStackTrace();
			}finally{
				session.close();
			}
			return Status.OK_STATUS;
		}
		
	};
	
	private void updateResultsTable(){
		currentQuery = new EntityQuery(parentEditor.getEntityType(),
			dateComp.getDateFilter(), entityFilter.getFilter());
		
		runJob.cancel();
		runJob.schedule();
	}
	
	
	
	@Override
	public void setFocus() {
		
	}
	

	@Override
	public void updatePage(Session currentSession, boolean typeModified) {
		entityFilter.setEntities(parentEditor.getEntityType().getEntities());
		sightingTable.setEntityType(parentEditor.getEntityType());
	}

}
