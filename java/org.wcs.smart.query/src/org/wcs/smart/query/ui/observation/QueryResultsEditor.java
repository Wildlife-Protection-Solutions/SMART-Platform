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
package org.wcs.smart.query.ui.observation;

import java.text.MessageFormat;

import net.refractions.udig.project.internal.Map;
import net.refractions.udig.project.ui.internal.MapPart;
import net.refractions.udig.project.ui.tool.IMapEditorSelectionProvider;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationAreaManager;
import org.wcs.smart.ca.IAreaModifiedListener;
import org.wcs.smart.ca.datamodel.DataModelManager;
import org.wcs.smart.ca.datamodel.IDataModelListener;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.IQueryListener;
import org.wcs.smart.query.QueryEventManager;
import org.wcs.smart.query.QueryListenerAdapter;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.IObservationPagedQueryResultSet;
import org.wcs.smart.query.model.IPagedQueryResultSet;
import org.wcs.smart.query.model.ObservationQuery;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryFactory;
import org.wcs.smart.query.model.QueryInput;
import org.wcs.smart.query.ui.IQueryEditor;
import org.wcs.smart.query.ui.QueryAreaModifiedListener;
import org.wcs.smart.query.ui.QueryDataModelModifiedListener;
import org.wcs.smart.query.ui.QueryEditorUtils;
import org.wcs.smart.query.ui.definition.QueryDefView;
import org.wcs.smart.query.ui.querytable.QueryLazyResultsTable;

/**
 * Editor for displaying query results.  The editor includes two pages
 * a tabular results page and a map results page.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class QueryResultsEditor extends MultiPageEditorPart implements MapPart, IQueryEditor, IAdaptable{

	public static final String ID = "org.wcs.smart.query.ui.QueryResultsEditor";  //$NON-NLS-1$

	private ObservationQuery query;
	private QueryResultsTablePage page1;
	private QueryMapPageEditor page2;
	private boolean isDirty = false;
	
	/*
	 * Listener for changes to area names/ids
	 */
	private IAreaModifiedListener areaListener = null;
	private IDataModelListener dmListener = null;
	
	/**
	 * Job to run the query and refresh the results
	 */
	private Job runQueryJob = new Job(Messages.QueryResultsEditor_RunQueryJobName) {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			runQueryJob.setName(Messages.QueryResultsEditor_RunQueryJobName + query.getName());
			final IProgressMonitor mymonitor = page1.createProgressMonitor();
			try {
				IObservationPagedQueryResultSet results = (IObservationPagedQueryResultSet) query.getPagedQueryResults(mymonitor);
				if (monitor.isCanceled() || mymonitor.isCanceled()){
					return Status.CANCEL_STATUS;
				}
				page1.updateAndShowTable(results, mymonitor);
			} catch (Exception ex) {
				QueryPlugIn.displayLog(Messages.QueryResultsEditor_ErrorRunningQuery, ex);
				page1.updateAndShowTable(null, mymonitor);
			}
			page2.refresh();
			return Status.OK_STATUS;
		}
	};
	
	private IQueryListener qListener = new QueryListenerAdapter() {
		@Override
		public void queryChanged(Query query) {
			if (query != null && query.equals(QueryResultsEditor.this.query)){
				isDirty = true;
				firePropertyChange(MultiPageEditorPart.PROP_DIRTY);
			}
		}

		@Override
		public void queryRun(Query query) {
			if (query != null && query.equals(QueryResultsEditor.this.query)){
				refreshQuery();
			}
		}
		
		@Override
		public void queryNameUpdated(Query query) {
			if (query != null && query.equals(QueryResultsEditor.this.query)){
				boolean lIsDirty = isDirty;
				QueryResultsEditor.this.query.setName(query.getName());
				QueryResultsEditor.this.query.setNames(query.getNames());
				
				((QueryInput)getEditorInput()).setQueryName(query.getName());
				updatePartName();
				page1.updateQueryName();
				
				isDirty = lIsDirty;
				firePropertyChange(MultiPageEditorPart.PROP_DIRTY);
			}
		}
	};
	
	
	private Job loadQueryLoad = new Job(Messages.QueryResultsEditor_LoadQueryJobName){
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			QueryInput input = (QueryInput) QueryResultsEditor.this.getEditorInput();

			Session session = HibernateManager.openSession();
			session.beginTransaction();
			try{
				query = (ObservationQuery) session.load(ObservationQuery.class, input.getUuid());
				query.getDropItems();
				query.generateDropItems(session);
			}catch (Exception ex){
				QueryPlugIn.displayLog(MessageFormat.format(
						Messages.QueryResultsEditor_Error_CouldNotParse, new Object[]{ input.getName()})+ ex.getLocalizedMessage(), ex);
			}finally{
				session.getTransaction().rollback();
				session.close();
			}
			
			
			if (page1 != null){
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						page1.initPage();
						setDirty(false);
					}
				});
			}
			
			return Status.OK_STATUS;
		}};
		
	/**
	 * 
	 * Creates a new editor
	 */
	public QueryResultsEditor() {
		super();		
	
		areaListener = new QueryAreaModifiedListener(this);
		ConservationAreaManager.getInstance().addAreaChangeListener(areaListener);
		
		dmListener = new QueryDataModelModifiedListener(this);
		DataModelManager.getInstance().addChangeListener(dmListener);
	}

	
	/**
	 * @see org.eclipse.ui.part.MultiPageEditorPart#dispose()
	 */
	@Override
	public void dispose() {
		super.dispose();
		QueryEventManager.getInstance().removeQueryChangedEvent(qListener);
		if (areaListener != null){
			ConservationAreaManager.getInstance().removeAreaChangeListener(areaListener);
		}
		if (dmListener != null){
			DataModelManager.getInstance().removeChangeListener(dmListener);
		}
		runQueryJob.cancel();
	}
	
	/**
	 * @see org.eclipse.ui.part.MultiPageEditorPart#init(org.eclipse.ui.IEditorSite, org.eclipse.ui.IEditorInput)
	 */
	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		super.setSite(site);
		super.setInput(input);
		
		if (input instanceof QueryInput){
			QueryInput input2 = ((QueryInput)input);
			if (input2.getUuid() == null){
				//create a new query
				this.query = QueryFactory.createObservationQuery();
				setDirty(false);
			}else{
				loadQueryLoad.schedule();
			}
		}
		QueryEventManager.getInstance().addQueryChangedEvent(qListener);
	}

	/**
	 * @return the query results display table
	 */
	public QueryLazyResultsTable getQueryResultsTable() {
		return this.page1.getQueryResultsTable();
	}
	
	/**
	 * @return the query
	 */
	public Query getQuery(){
		try {
			loadQueryLoad.join();	//wait for the query loading job if applicable
		} catch (InterruptedException e) {
			QueryPlugIn.displayLog(Messages.QueryResultsEditor_Error_CouldNotLoad + e.getLocalizedMessage(), e);
		}
		
		return this.query;
	}

	/**
	 * @return the query
	 */
	public ObservationQuery getQueryInternal(){
		return (ObservationQuery) getQuery();
	}
	
	/**
	 * Updates the editor name with the query name
	 */
	public void updatePartName(){
		super.setPartName(getEditorInput().getName());
	}
 
	@Override
	public void validate(){
		page1.validate();
	}
	
	/**
	 * Sets the dirty state of the editor
	 * @param isDirty
	 */
	public void setDirty(boolean isDirty){
		this.isDirty = isDirty;
		firePropertyChange(MultiPageEditorPart.PROP_DIRTY);
	}
	/**
	 * This editor has two pages:
	 * <ol><li>Tabular Results - the query results shown in a tabular form</li>
	 * <li>Map Results - the query results displayed in a map</li>
	 * </ol>
	 * 
	 * @see org.eclipse.ui.part.MultiPageEditorPart#createPages()
	 */
	@Override
	protected void createPages() {
		QueryInput input = ((QueryInput) getEditorInput());
		super.setPartName(input.getName());
		showBusy(true);
		try {
			page1 = new QueryResultsTablePage(this);
			addPage(0, page1, input);
			setPageText(0, Messages.QueryResultsEditor_TableResultsTabName);
			page1.updateQueryName();
			
			page2 = new QueryMapPageEditor(this);
			addPage(1, page2, input);
			setPageText(1, Messages.QueryResultsEditor_MappedResultsTabName);
			
			//run this in a job as it needs
			//to load the data model to get the query
			//columns and this may take a while
			Job j = new Job(Messages.QueryResultsEditor_initquerylobname){
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					ObservationQuery q = getQueryInternal();
					q.getQueryColumns();
					
					Display.getDefault().syncExec(new Runnable(){

						@Override
						public void run() {
							if (query != null && query.getUuid() == null){
								page1.initPage();
							}
							
						}});
					return Status.OK_STATUS;
				}
			};
			j.schedule();
		} catch (final Throwable t) {
			QueryPlugIn.log("Could not open query editor", t); //$NON-NLS-1$
		}finally{
			showBusy(false);
		}
	}

	/**
	 * Re-run the query and refresh the results.
	 */
	public void refreshQuery(){
		//cancel existing run query job.
		runQueryJob.cancel();
		
		//update date filter
		((ObservationQuery)getQuery()).setDateFilter(page1.getDateFilter());
		
		if (!getQuery().isValid()){
			MessageDialog.openError(getSite().getShell(), Messages.QueryResultsEditor_Error_DialogTitle, Messages.QueryResultsEditor_InvalidQueryError);
			return;
		}
		
		//clear existing results
		page1.getQueryResultsTable().setInput((IPagedQueryResultSet)null);	
		//show progress area
		page1.showProgressArea();
	
		runQueryJob.schedule();
	}
	


	@Override
	public boolean isSaveAsAllowed() {
		return true;
	}

	
	@Override
	public boolean isDirty(){
		return this.isDirty;
	}
	
	
	/**
	 * Saves the current query
	 * @see org.eclipse.ui.part.EditorPart#doSave(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void doSave(IProgressMonitor monitor) {
		boolean newQuery = query.getUuid() == null;
		
		Query savedQuery = QueryEditorUtils.doSave(this, monitor);
		if (savedQuery == null){
			//error 
			return;
		}
		if (savedQuery != query){
			//saved as new query
			this.query = (ObservationQuery) savedQuery;
			setInput(new QueryInput(savedQuery));
			newQuery = true;
		}
		
		if (newQuery){
			page1.initPage();			
		}
		updatePartName();
		setDirty(false);
		
	}

	
	@Override
	public void doSaveAs() {
		Query savedQuery = QueryEditorUtils.doSaveAs(this, true);
		if (savedQuery == null){
			return;
		}
		this.query = (ObservationQuery) savedQuery;
		setInput(new QueryInput(savedQuery));
		updatePartName();
		page1.getQueryResultsTable().clearColumns();
		page1.initPage();
		
		setDirty(false);
		
		//this is a bit of a hack to get the querylistview to be updated
		//correctly
		//this cannot be called until setinput has bee called
		getSite().getWorkbenchWindow().getActivePage().activate(getSite().getWorkbenchWindow().getActivePage().findView(QueryDefView.ID));
		getSite().getWorkbenchWindow().getActivePage().activate(getSite().getPart());
	}

	/**
	 * @see net.refractions.udig.project.ui.internal.MapPart#getMap()
	 */
	@Override
	public Map getMap() {
		if (page2 == null){
			return null;
		}
		return 	page2.getMap();
	}

	/**
	 * @see net.refractions.udig.project.ui.internal.MapPart#openContextMenu()
	 */
	@Override
	public void openContextMenu() {
		page2.openContextMenu();
		
	}

	/**
	 * @see net.refractions.udig.project.ui.internal.MapPart#setFont(org.eclipse.swt.widgets.Control)
	 */
	@Override
	public void setFont(Control textArea) {
		page2.setFont(textArea);
		
	}

	/**
	 * @see net.refractions.udig.project.ui.internal.MapPart#setSelectionProvider(net.refractions.udig.project.ui.tool.IMapEditorSelectionProvider)
	 */
	@Override
	public void setSelectionProvider(
			IMapEditorSelectionProvider selectionProvider) {
		page2.setSelectionProvider(selectionProvider);
		
	}

	/**
	 * @see net.refractions.udig.project.ui.internal.MapPart#getStatusLineManager()
	 */
	@Override
	public IStatusLineManager getStatusLineManager() {
		return page2.getStatusLineManager();
	}

	/**
	 * @see org.eclipse.ui.part.MultiPageEditorPart#getAdapter(java.lang.Class)
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Object getAdapter(Class adaptee) {
		if (adaptee.isAssignableFrom(Map.class)) {
			return getMap();
		}
		return super.getAdapter(adaptee);
	}
	
	/**
	 * @return the editor input as query input
	 */
	public QueryInput getInputInternal(){
		return (QueryInput) getEditorInput();
	}
	
	@Override
	public void reparseQuery() {
		//running it its own job so it has its own hibernate session
		//and does not interfere with other sessions.
		Job j = new Job("update drop items") { //$NON-NLS-1$
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				final Session session = HibernateManager.openSession();
				try{
				Display.getDefault().syncExec(new Runnable(){
						@Override
						public void run() {
							try{
								getQuery().generateDropItems(session);
							}catch (Exception ex){
								QueryPlugIn.log(ex.getMessage(), ex);
							}
						}});
				}finally{
					session.close();
				}
						return Status.OK_STATUS;
			}
		};
		j.setSystem(true);
		j.schedule();
		try {
			j.join();
		} catch (InterruptedException e) {
			QueryPlugIn.log(e.getMessage(), e);
		}
				
		QueryEventManager.getInstance().fireQueryRefreshListeners(getQuery());
	}
}
