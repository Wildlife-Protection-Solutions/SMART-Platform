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

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.hibernate.Session;
import org.locationtech.udig.project.internal.Map;
import org.locationtech.udig.project.ui.internal.MapPart;
import org.locationtech.udig.project.ui.tool.IMapEditorSelectionProvider;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ConservationAreaManager;
import org.wcs.smart.ca.IAreaModifiedListener;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.QueryHibernateManager;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.common.engine.IQueryResult;
import org.wcs.smart.query.common.engine.QueryExecutor;
import org.wcs.smart.query.common.model.Grid;
import org.wcs.smart.query.common.model.GridQueryResult;
import org.wcs.smart.query.common.model.QueryGridResultItem;
import org.wcs.smart.query.common.model.GriddedQuery;
import org.wcs.smart.query.event.IQueryListener;
import org.wcs.smart.query.event.QueryAreaModifiedListener;
import org.wcs.smart.query.event.QueryEventManager;
import org.wcs.smart.query.event.QueryListenerAdapter;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryProxy;
import org.wcs.smart.query.ui.QueryEditorUtils;
import org.wcs.smart.query.ui.definition.QueryDefView;
import org.wcs.smart.query.ui.editor.IQueryEditor;
import org.wcs.smart.query.ui.editor.QueryEditorInput;

/**
 * Editor for displaying query results. The editor includes two pages a tabular
 * results page and a map results page.
 * 
 * @author Jeff
 * @author Emily
 * @since 1.0.0
 */
public abstract class GriddedEditor extends MultiPageEditorPart implements MapPart, IAdaptable, IQueryEditor {

	private QueryProxy query;

	private boolean isDirty = false;
	private GriddedTableResultsPage resultPage;	//table result page
	private GriddedResultsMapEditorPage mapPage;	//map results page

	private boolean firstRun = true;
	
	private IAreaModifiedListener areaListener = null;
	
	private IQueryListener qListener = new QueryListenerAdapter() {
		@Override
		public void queryModified(int eventType, Object object) {
			if (object != null && object instanceof Query &&
					object.equals(GriddedEditor.this.getQuery())){
				
				if (eventType == IQueryListener.QUERY_DEFINITION_MODIFIED){
					isDirty = true;
					firePropertyChange(MultiPageEditorPart.PROP_DIRTY);
				}else if (eventType == IQueryListener.QUERY_NAME_MODIFIED){
					boolean lIsDirty = isDirty;
					Query updatedQuery = (Query) object;
					GriddedEditor.this.getQuery().setName(updatedQuery.getName());
					GriddedEditor.this.getQuery().setNames(updatedQuery.getNames());
					((QueryEditorInput)getEditorInput()).setQueryName(updatedQuery.getName());
					updatePartName();
					resultPage.updateQueryName();
					
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
						getSite().getWorkbenchWindow().getActivePage().closeEditor(GriddedEditor.this, false);					
					}});
				
			}
			
		}

		@Override
		public void queryRun(Query query) {
			if (query != null && query.equals(GriddedEditor.this.query.getQuery())){
				refreshQuery();
			}
		}
		
	};
	
	
	private Job loadQueryLoad = new Job(Messages.GriddedEditor_LoadQueryJobName){
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			QueryEditorInput input = (QueryEditorInput) GriddedEditor.this.getEditorInput();
			
			Session session = HibernateManager.openSession();
			session.beginTransaction();
			try{
				GriddedQuery gquery = (GriddedQuery) QueryHibernateManager.getInstance().findQuery(session, input.getUuid(), input.getType());
				query = new QueryProxy(gquery);
				query.getQueryType().getDropItemFactory().generateDropItems(query, session);
				
				
			}catch (Exception ex){
				QueryPlugIn.displayLog(
						MessageFormat.format(Messages.GriddedEditor_ErrorParsingQuery, new Object[]{input.getName()}) + ex.getLocalizedMessage(), ex);
			}finally{
				session.getTransaction().commit(); 
				session.close();
			}
			
			
			if (resultPage != null){
				getSite().getShell().getDisplay().asyncExec(new Runnable() {
					@Override
					public void run() {
						resultPage.setQuery();
						setDirty(false);
					}
				});
			}
			
			return Status.OK_STATUS;
		}};
		
		
		/* run query job */
		private Job runQueryJob = new Job(Messages.GriddedEditor_RunQueryJob) {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				setName(Messages.GriddedEditor_RunQueryJob + getQuery().getName());
				IProgressMonitor mymonitor = resultPage.createProgressMonitor();
				boolean isFirstRun = firstRun;
				GriddedEditor.this.firstRun = false;
				
				try {
					IQueryResult results = QueryExecutor.INSTANCE.executeQuery(getQuery(), null, mymonitor);
					if (monitor.isCanceled() || mymonitor.isCanceled()){
						return Status.CANCEL_STATUS;
					}
					resultPage.updateAndShowTable(((GridQueryResult)results).getData(), mymonitor);
				} catch (Exception ex) {
					String message = "Could not execute query." + "\n\n"; //$NON-NLS-1$ //$NON-NLS-2$
					if (ex.getCause() != null && ex.getCause().getCause()== Grid.GRID_TO_BIG_EXCEPTION){
						message += ex.getCause().getCause().getLocalizedMessage();
					}else if (ex.getCause() != null){
						message += ex.getCause().getLocalizedMessage();
					}else{
						message += ex.getLocalizedMessage();
					}
					QueryPlugIn.displayLog(message, ex);
					resultPage.updateAndShowTable(new ArrayList<QueryGridResultItem>(), mymonitor);
				}
				mapPage.refresh(isFirstRun);
				return Status.OK_STATUS;
			}
		};
		
	/**
	 * Creates a new editor
	 */
	public GriddedEditor() {
		super();	
		
		areaListener = new QueryAreaModifiedListener(this);
		ConservationAreaManager.getInstance().addAreaChangeListener(areaListener);
	}
	
	/**
	 * Loads the query for the editor
	 * @param session
	 * @param uuid
	 * @return
	 */
	public abstract GriddedQuery createQuery();
	
	/**
	 * @see org.eclipse.ui.part.MultiPageEditorPart#dispose()
	 */
	@Override
	public void dispose() {
		super.dispose();
		QueryEventManager.getInstance().removeListener(qListener);
		ConservationAreaManager.getInstance().removeAreaChangeListener(areaListener);
		query.dispose();
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
				this.query = new QueryProxy(createQuery());
				setDirty(false);
			}else{
				loadQueryLoad.schedule();
			}
			QueryEventManager.getInstance().addListener(qListener);
		}
		
		
	}

	/**
	 * @return the query results display table
	 */
	public QueryResultsTable getQueryResultsTable(){
		return this.resultPage.getQueryResultsTable();
	}
	
	/**
	 * @return the query
	 */
	public GriddedQuery getQueryInternal(){
		return  (GriddedQuery) getQuery();
	}
	
	public QueryProxy getQueryProxy(){
		try {
			loadQueryLoad.join();	//wait for the query loading job if applicable
		} catch (InterruptedException e) {
			QueryPlugIn.displayLog(Messages.GriddedEditor_ErrorLoadingQuery + e.getLocalizedMessage(), e);
		}
		
		return this.query;
	}
	
	/**
	 * @return the query
	 */
	public Query getQuery(){
		return getQueryProxy().getQuery();
	}

	/**
	 * Updates the editor name with the query name
	 */
	public void updatePartName(){
		super.setPartName(getEditorInput().getName());
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
			resultPage = new GriddedTableResultsPage(this);
			addPage(0, resultPage, input);
			setPageText(0, Messages.GriddedEditor_TableResultsTabName);
			setPageImage(0, QueryPlugIn.getDefault().getImageRegistry().get(QueryPlugIn.TABLE_ICON));
			if (this.query != null && this.getQuery().getUuid() == null){
				resultPage.setQuery();
			}
			
			mapPage = new GriddedResultsMapEditorPage(this);
			addPage(1, mapPage, input);
			setPageText(1, Messages.GriddedEditor_MappedResultsTabName);
			setPageImage(1, SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.MAP_ICON));
			
			setTitleImage(input.getType().getImage());
		} catch (final Throwable t) {
			QueryPlugIn.log(Messages.GriddedEditor_Error_CouldNotOpen, t);
			throw new RuntimeException(Messages.GriddedEditor_Error_CouldNotOpen + t.getMessage(), t);
		}finally{
			showBusy(false);
		}
	}
	
	/**
	 * Re-run the query and refresh the results.
	 */
	public void refreshQuery(){
		runQueryJob.cancel();
		//update date filter
		getQueryInternal().setDateFilter(resultPage.getDateFilter());
		
		if (!getQueryProxy().isValid()){
			MessageDialog.openError(getSite().getShell(), Messages.GriddedEditor_Error_DialogTitle, Messages.GriddedEditor_InvalidQuery_DialogMessage);
			return;
		}
		
		//show progress area
		resultPage.showProgressArea();
		
		//run query
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
	
	@Override
	public void validate(){
		resultPage.validate();
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
			this.query = new QueryProxy( (GriddedQuery) savedQuery);
			setInput(new QueryEditorInput(savedQuery));
			newQuery = true;
		}
		
		if (newQuery){
			resultPage.setQuery();			
		}
		updatePartName();
		setDirty(false);
		
		//this is a bit of a hack to get the querylistview to be updated
		//correctly
		//this cannot be called until setinput has bee called
		getSite().getWorkbenchWindow().getActivePage().activate(getSite().getWorkbenchWindow().getActivePage().findView(QueryDefView.ID));
		getSite().getWorkbenchWindow().getActivePage().activate(getSite().getPart());
	}
	
	/** 
	 * Saves a copy of the query as a new query
	 * @see org.eclipse.ui.part.EditorPart#doSaveAs()
	 */
	@Override
	public void doSaveAs() {
		QueryProxy savedQuery = QueryEditorUtils.doSaveAs(this, true);
		if (savedQuery == null){
			return;
		}
		this.query = savedQuery;
		setInput(new QueryEditorInput(savedQuery.getQuery()));
		updatePartName();
		resultPage.getQueryResultsTable().clearColumns();
		
		resultPage.setQuery();
		mapPage.reset();
		
		setDirty(false);
		
		//this is a bit of a hack to get the querylistview selection to be updated
		//to the new saved-as query
		//this cannot be called until setinput has been called
		getSite().getWorkbenchWindow().getActivePage().activate(getSite().getWorkbenchWindow().getActivePage().findView(QueryDefView.ID));
		getSite().getWorkbenchWindow().getActivePage().activate(getSite().getPart());
	}

	/**
	 * @see org.locationtech.udig.project.ui.internal.MapPart#getMap()
	 */
	@Override
	public Map getMap() {
		if (mapPage == null){
			return null;
		}
		return 	mapPage.getMap();
	}

	/**
	 * @see org.locationtech.udig.project.ui.internal.MapPart#openContextMenu()
	 */
	@Override
	public void openContextMenu() {
		mapPage.openContextMenu();
		
	}

	/**
	 * @see org.locationtech.udig.project.ui.internal.MapPart#setFont(org.eclipse.swt.widgets.Control)
	 */
	@Override
	public void setFont(Control textArea) {
		mapPage.setFont(textArea);
		
	}

	/**
	 * @see org.locationtech.udig.project.ui.internal.MapPart#setSelectionProvider(org.locationtech.udig.project.ui.tool.IMapEditorSelectionProvider)
	 */
	@Override
	public void setSelectionProvider(
			IMapEditorSelectionProvider selectionProvider) {
		mapPage.setSelectionProvider(selectionProvider);
		
	}

	/**
	 * @see org.locationtech.udig.project.ui.internal.MapPart#getStatusLineManager()
	 */
	@Override
	public IStatusLineManager getStatusLineManager() {
		return mapPage.getStatusLineManager();
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


	@Override
	public void reparseQuery() {
		//running it its own job so it has its own hibernate session
		//and does not interfere with other sessions.
		Job j = new Job("update drop items") { //$NON-NLS-1$
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				final Session session = HibernateManager.openSession();
				session.beginTransaction();
				try{
					getSite().getShell().getDisplay().syncExec(new Runnable(){
						@Override
						public void run() {
							try{
								query.getQueryType().getDropItemFactory().generateDropItems(query, session);
							}catch (Exception ex){
								QueryPlugIn.log(ex.getMessage(), ex);
							}
						}});
				}finally{
					try{
						session.getTransaction().rollback();
					}catch (Exception ex){
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
}

	
