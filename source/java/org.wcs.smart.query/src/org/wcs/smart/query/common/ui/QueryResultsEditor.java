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
package org.wcs.smart.query.common.ui;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.hibernate.Session;
import org.locationtech.udig.project.internal.Map;
import org.locationtech.udig.project.internal.command.navigation.SetViewportBBoxCommand;
import org.locationtech.udig.project.ui.internal.MapPart;
import org.locationtech.udig.project.ui.tool.IMapEditorSelectionProvider;
import org.wcs.smart.IProjectionProvider;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ConservationAreaManager;
import org.wcs.smart.ca.IAreaModifiedListener;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.QueryHibernateManager;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.common.engine.IPagedQueryResultSet;
import org.wcs.smart.query.common.engine.IQueryResult;
import org.wcs.smart.query.common.engine.QueryExecutor;
import org.wcs.smart.query.common.model.ObservationQuery;
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.query.common.model.WaypointQuery;
import org.wcs.smart.query.common.model.udig.IQueryService;
import org.wcs.smart.query.event.IQueryListener;
import org.wcs.smart.query.event.QueryAreaModifiedListener;
import org.wcs.smart.query.event.QueryEventManager;
import org.wcs.smart.query.event.QueryListenerAdapter;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.IMappableQueryType;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.query.model.QueryProxy;
import org.wcs.smart.query.ui.QueryEditorUtils;
import org.wcs.smart.query.ui.definition.QueryDefView;
import org.wcs.smart.query.ui.editor.IMapQueryEditor;
import org.wcs.smart.query.ui.editor.QueryEditorInput;
import org.wcs.smart.udig.IMapEditManager;
import org.wcs.smart.udig.UndoTool;
import org.wcs.smart.util.ReprojectUtils;

/**
 * Editor for displaying query results.  The editor includes two pages
 * a tabular results page and a map results page.
 * 
 * @author Emily
 * @since 1.0.0
 */
public abstract class QueryResultsEditor extends MultiPageEditorPart implements MapPart, IMapQueryEditor, IAdaptable, IProjectionProvider{

	protected QueryProxy query;
	protected QueryResultsTablePage page1;
	protected QueryMapPageEditor page2;
	private boolean isDirty = false;
	private Projection currentPrj = null;
	private boolean editMode = false;
	/*
	 * Listener for changes to area names/ids
	 */
	private IAreaModifiedListener areaListener = null;
	
	private List<Listener> editModeModified = new ArrayList<Listener>();
	
	/**
	 * Job to run the query and refresh the results
	 */
	private Job runQueryJob = new Job(Messages.QueryResultsEditor_RunQueryJobName) {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			runQueryJob.setName(Messages.QueryResultsEditor_RunQueryJobName + getQuery().getName());
			
			//load the current view projection
			currentPrj = HibernateManager.getCurrentViewProjection();
			if (currentPrj != null && currentPrj.getParsedCoordinateReferenceSystem() == null){
				try{
					currentPrj.setParsedCoordinateReferenceSystem(ReprojectUtils.stringToCrs(currentPrj.getDefinition()));
				}catch (Exception ex){
					//eat me
				}
			}
			final IProgressMonitor mymonitor = page1.createProgressMonitor();
			try {
				IQueryResult results = QueryExecutor.INSTANCE.executeQuery(getQuery(), null, mymonitor); 
				if (monitor.isCanceled() || mymonitor.isCanceled()){
					page1.updateAndShowTable(null);
					return Status.CANCEL_STATUS;
				}
				page1.updateAndShowTable((IPagedQueryResultSet)results);
			} catch (Exception ex) {
				String message = Messages.QueryResultsEditor_ErrorRunningQuery;
				if (ex.getCause() != null){
					message += " " + ex.getCause().getMessage(); //$NON-NLS-1$
				}
				QueryPlugIn.displayLog(message, ex);
				page1.updateAndShowTable(null);
			}
			page2.refresh();
			return Status.OK_STATUS;
		}
	};
	
	private IQueryListener qListener = new QueryListenerAdapter() {
		@Override
		public void queryModified(int eventType, Object object) {
			if (object != null && object.equals(QueryResultsEditor.this.query.getQuery())){
				if (eventType == IQueryListener.QUERY_DEFINITION_MODIFIED){
					isDirty = true;
					firePropertyChange(PROP_DIRTY);
				}else if (eventType == IQueryListener.QUERY_NAME_MODIFIED){
					boolean lIsDirty = isDirty;
					Query updatedQuery = (Query) object;
					QueryResultsEditor.this.getQuery().setName(updatedQuery.getName());
					QueryResultsEditor.this.getQuery().setNames(updatedQuery.getNames());
					((QueryEditorInput)getEditorInput()).setQueryName(updatedQuery.getName());
					
					updatePartName();
					page1.updateQueryName();
					
					isDirty = lIsDirty;
					firePropertyChange(MultiPageEditorPart.PROP_DIRTY);
				}
			}else if (object != null && object instanceof QueryEditorInput 
					&& ((QueryEditorInput)object).getUuid().equals(getQuery().getUuid()) 
					&& eventType == IQueryListener.QUERY_DELETED){
				//close part
				getSite().getShell().getDisplay().asyncExec(new Runnable(){
					@Override
					public void run() {
						getSite().getWorkbenchWindow().getActivePage().closeEditor(QueryResultsEditor.this, false);					
					}});
				
			}

		}

		@Override
		public void queryRun(Query query) {
			if (query != null && query.equals(QueryResultsEditor.this.query.getQuery())){
				refreshQuery();
			}
		}
	
	};
	
	
	private Job loadQueryLoad = new Job(Messages.QueryResultsEditor_LoadQueryJobName){
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			QueryEditorInput input = (QueryEditorInput) QueryResultsEditor.this.getEditorInput();

			Session session = HibernateManager.openSession();
			session.beginTransaction();
			try{
				Query squery = (SimpleQuery) QueryHibernateManager.getInstance().findQuery(session, input.getUuid(), input.getType());
				query = new QueryProxy(squery);
				query.getQueryType().getDropItemFactory().generateDropItems(query, session);
			}catch (Exception ex){
				QueryPlugIn.displayLog(MessageFormat.format(
						Messages.QueryResultsEditor_Error_CouldNotParse, new Object[]{ input.getName()})+ ex.getLocalizedMessage(), ex);
			}finally{
				session.getTransaction().rollback();
				session.close();
			}
			
			
			if (page1 != null){
				getSite().getShell().getDisplay().asyncExec(new Runnable() {
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
	}
	
	@Override
	public void showMapPage(ReferencedEnvelope env) {
		if (env != null){
			page2.setInitialZoom(env);
			getMap().sendCommandSync(new SetViewportBBoxCommand(env));
		}
		for (int i = 0; i < getPageCount(); i ++){
			if (getEditor(i) == page2){
				setActivePage(i);
				return;
			}
		}
	}
	
	public void showTablePage() {
		for (int i = 0; i < getPageCount(); i ++){
			if (getEditor(i) == page1){
				setActivePage(i);
				return;
			}
		}
	}
	
	/**
	 * @see org.eclipse.ui.part.MultiPageEditorPart#dispose()
	 */
	@Override
	public void dispose() {
		super.dispose();
		
		query.dispose();
		QueryEventManager.getInstance().removeListener(qListener);
		if (areaListener != null){
			ConservationAreaManager.getInstance().removeAreaChangeListener(areaListener);
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
		
		if (input instanceof QueryEditorInput){
			QueryEditorInput input2 = ((QueryEditorInput)input);
			if (input2.getUuid() == null){
				//create a new query
				this.query = new QueryProxy(createNewQuery(input2.getType()));
				setDirty(false);
			}else{
				loadQueryLoad.schedule();
			}
			super.setTitleImage(input2.getType().getImage());
		}
		
		QueryEventManager.getInstance().addListener(qListener);
	}
	
	/**
	 * Called when changes are made to the query properties page.
	 * 
	 */
	public void queryPropertiesChange(){
		setDirty(true);
		page1.initPage();
		page2.refresh();
	}
	
	public void refreshQueryProperties(){
		page1.initPage();
		page2.refresh();
	}
	
	/**
	 * @return the query results display table
	 */
	public QueryLazyResultsTable getQueryResultsTable() {
		return this.page1.getQueryResultsTable();
	}
	
	/**
	 * Loads the query from the database; if you want the cached query use getQueryProxy()
	 * @return the query
	 */
	public Query getQuery(){
		return getQueryProxy().getQuery();
	}

	/**
	 * @return the query
	 */
	public SimpleQuery getQueryInternal(){
		return (SimpleQuery) getQuery();
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
		QueryEditorInput input = ((QueryEditorInput) getEditorInput());
		super.setPartName(input.getName());
		showBusy(true);
		try {
			page1 = new QueryResultsTablePage(this);
			addPage(0, page1, input);
			setPageText(0, Messages.QueryResultsEditor_TableResultsTabName);
			setPageImage(0, QueryPlugIn.getDefault().getImageRegistry().get(QueryPlugIn.TABLE_ICON));
			page1.updateQueryName();
			
			
			page2 = new QueryMapPageEditor(this);
			addPage(1, page2, input);
			setPageText(1, Messages.QueryResultsEditor_MappedResultsTabName);
			setPageImage(1, SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.MAP_ICON));
			
			setTitleImage(input.getType().getImage());
			
			//run this in a job as it needs
			//to load the data model to get the query
			//columns and this may take a while
			Job j = new Job(Messages.QueryResultsEditor_initquerylobname){
				@Override
				protected IStatus run(IProgressMonitor monitor) {
//					SimpleQuery q = getQueryInternal();
//					q.getQueryColumns(Locale.getDefault(), null, QueryResultsEditor.this);
//					
					getSite().getShell().getDisplay().syncExec(new Runnable(){

						@Override
						public void run() {
							if (query != null && getQuery().getUuid() == null){
								page1.initPage();
							}
							
						}});
					return Status.OK_STATUS;
				}
			};
			j.schedule();
		} catch (final Throwable t) {
			QueryPlugIn.log("Could not open query editor", t); //$NON-NLS-1$
			throw new RuntimeException("Could not open query editor" + t.getMessage(), t); //$NON-NLS-1$
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
		((SimpleQuery)getQuery()).setDateFilter(page1.getDateFilter());
		
		if (!getQueryProxy().isValid()){
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
		boolean newQuery = getQuery().getUuid() == null;
		
		Query savedQuery = QueryEditorUtils.doSave(this, monitor);
		if (savedQuery == null){
			//error 
			return;
		}
		if (savedQuery != query.getQuery()){
			//saved as new query
			this.query = new QueryProxy((SimpleQuery) savedQuery);
			setInput(new QueryEditorInput(savedQuery));
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
		QueryProxy savedQuery = QueryEditorUtils.doSaveAs(this, true);
		if (savedQuery == null){
			return;
		}
		this.query = savedQuery;
		setInput(new QueryEditorInput(savedQuery.getQuery()));
		updatePartName();
		page1.getQueryResultsTable().clearColumns();
		page1.initPage();
		page2.reset(true);
		
		setDirty(false);
		
		//this is a bit of a hack to get the querylistview to be updated
		//correctly
		//this cannot be called until setinput has bee called
		getSite().getWorkbenchWindow().getActivePage().activate(getSite().getWorkbenchWindow().getActivePage().findView(QueryDefView.ID));
		getSite().getWorkbenchWindow().getActivePage().activate(getSite().getPart());
	}

	/**
	 * @see org.locationtech.udig.project.ui.internal.MapPart#getMap()
	 */
	@Override
	public Map getMap() {
		if (page2 == null){
			return null;
		}
		return 	page2.getMap();
	}

	/**
	 * @see org.locationtech.udig.project.ui.internal.MapPart#openContextMenu()
	 */
	@Override
	public void openContextMenu() {
		page2.openContextMenu();
		
	}

	/**
	 * @see org.locationtech.udig.project.ui.internal.MapPart#setFont(org.eclipse.swt.widgets.Control)
	 */
	@Override
	public void setFont(Control textArea) {
		page2.setFont(textArea);
		
	}

	/**
	 * @see org.locationtech.udig.project.ui.internal.MapPart#setSelectionProvider(org.locationtech.udig.project.ui.tool.IMapEditorSelectionProvider)
	 */
	@Override
	public void setSelectionProvider(
			IMapEditorSelectionProvider selectionProvider) {
		page2.setSelectionProvider(selectionProvider);
		
	}

	/**
	 * @see org.locationtech.udig.project.ui.internal.MapPart#getStatusLineManager()
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
	public QueryEditorInput getInputInternal(){
		return (QueryEditorInput) getEditorInput();
	}
	
	public QueryProxy getQueryProxy(){
		try {
			loadQueryLoad.join();	//wait for the query loading job if applicable
		} catch (InterruptedException e) {
			QueryPlugIn.displayLog(Messages.QueryResultsEditor_Error_CouldNotLoad + e.getLocalizedMessage(), e);
		}
		return this.query;
	}
	
	@Override
	public void reparseQuery() {
		//running it its own job so it has its own hibernate session
		//and does not interfere with other sessions.
		Job j = new Job("update drop items") { //$NON-NLS-1$
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				final Session session = HibernateManager.openSession();
				session.beginTransaction();
				try {
					getSite().getShell().getDisplay().syncExec(new Runnable() {
						@Override
						public void run() {
							try {
								getQueryProxy().getQueryType().getDropItemFactory().generateDropItems(getQueryProxy(), session);
							} catch (Exception ex) {
								QueryPlugIn.log(ex.getMessage(), ex);
							}
						}
					});
				} finally {
					try{
						session.getTransaction().rollback();
					}catch(Exception ex){
						QueryPlugIn.log(ex.getMessage(), ex);
					}
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
				
		QueryEventManager.getInstance().fireRefreshQuery(getQuery());
	}
	
	/**
	 * Creates info section which appears above the table; can be null 
	 * or overwritten by subclasses.
	 * 
	 * @return
	 */
	protected ISummaryInfo createInfoSection(){
		if (getQuery() instanceof ObservationQuery){
			return new ObservationQuerySummaryInfo();
		}else if (getQuery() instanceof WaypointQuery){
			return new WaypointQuerySummaryInfo();
		}
		return null;
	}

	/**
	 * The projection to use for displaying results
	 */
	@Override
	public Projection getProjection(){
		return this.currentPrj;
	}
	
	
	/**
	 * Creates a new query of the given type
	 * @param type
	 * @return
	 */
	public abstract Query createNewQuery(IQueryType type);

	
	/**
	 * Gets the edit mode state
	 * @return
	 */
	@Override
	public boolean getEditMode(){
		return this.editMode;
	}
	/**
	 * Sets if edit mode is enabled or disabled
	 * @param enabled
	 */
	public void setEditMode(boolean enabled){
		if (canEditResults()){
			this.editMode = enabled;
		}else{
			this.editMode = false;
		}
		for (Listener l : editModeModified){
			l.handleEvent(null);
		}
	}
	
	@Override
	public void addEditModeModifiedListener(Listener l){
		editModeModified.add(l);
	}
	
	
	/**
	 * Refreshes the map and viewer after edits completed
	 */
	public void refreshResults(){
		TableViewer viewer = getQueryResultsTable().getTable();
		if (viewer.getContentProvider() instanceof QueryLazyResultsContentProvider) {
			((QueryLazyResultsContentProvider) viewer.getContentProvider()).clear();
		}
		refreshQueryProperties();
		viewer.refresh(true);
		
		IMapEditManager mgr = (IMapEditManager) getMap().getBlackboard().get(IMapEditManager.BLACKBOARD_KEY);
		if (mgr != null){
			page2.enableTool(UndoTool.ID, canEditResults() && getEditMode() && mgr.canUndo());
		}
		
		getMap().getRenderManager().refresh(null);
	}
	
	
	/**
	 * Creates a query service for the map 
	 * @return
	 */
	public IQueryService createQueryService(){
		IQueryType type = ((QueryEditorInput)getEditorInput()).getType();
		if (type instanceof IMappableQueryType){
			return ((IMappableQueryType)type).createQueryService(getQuery(), this);
		}
		return null;
	}
	
	
	/**
	 * Column label provider
	 *  
	 * @param column The column to get the label provider for
	 * @param allColumns all columns in the table, incase the label provider requires data from
	 * another column
	 * @return
	 */
	protected abstract CellLabelProvider getColumnLabelProvider(QueryColumn column, List<QueryColumn> allColumns);
	
	/**
	 * Editing options for the column
	 *  
	 * @param column 
	 * @return editing options for the column, or null if not supported
	 */
	protected EditingSupport getEditingSupport(ColumnViewer viewer, QueryColumn column){ 
		return null;
	}
}
