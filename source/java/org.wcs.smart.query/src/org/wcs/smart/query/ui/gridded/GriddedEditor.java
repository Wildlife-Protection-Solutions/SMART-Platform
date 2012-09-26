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
package org.wcs.smart.query.ui.gridded;

import java.util.ArrayList;
import java.util.List;

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
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.IQueryListener;
import org.wcs.smart.query.QueryEventManager;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.model.GridResultItem;
import org.wcs.smart.query.model.GriddedQuery;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryInput;
import org.wcs.smart.query.ui.IQueryEditor;
import org.wcs.smart.query.ui.QueryEditorUtils;
import org.wcs.smart.query.ui.definition.QueryDefView;
import org.wcs.smart.query.ui.querytable.QueryResultsTable;

/**
 * Editor for displaying query results. The editor includes two pages a tabular
 * results page and a map results page.
 * 
 * @author Jeff
 * @author Emily
 * @since 1.0.0
 */
public class GriddedEditor extends MultiPageEditorPart implements MapPart, IAdaptable, IQueryEditor {

	public static final String ID = "org.wcs.smart.query.ui.GriddedEditor";

	private GriddedQuery query;

	private boolean isDirty = false;
	private GriddedTableResultsPage resultPage;	//table result page
	private GriddedResultsMapEditorPage mapPage;	//map results page

	
	private IQueryListener qListener = new IQueryListener() {
		@Override
		public void queryChanged(Query query) {
			if (query != null && query.equals(GriddedEditor.this.query)){
				isDirty = true;
				firePropertyChange(MultiPageEditorPart.PROP_DIRTY);
			}
		}

		@Override
		public void queryRun(Query query) {
			if (query != null && query.equals(GriddedEditor.this.query)){
				refreshQuery();
			}
		}
		
		@Override
		public void queryNameUpdated(Query query) {
			if (query != null && query.equals(GriddedEditor.this.query)){
				boolean lIsDirty = isDirty;
				GriddedEditor.this.query.setName(query.getName());
				((QueryInput)getEditorInput()).setQueryName(query.getName());
				updatePartName();
				resultPage.updateQueryName();
				
				isDirty = lIsDirty;
				firePropertyChange(MultiPageEditorPart.PROP_DIRTY);
			}
		}
	};
	
	
	private Job loadQueryLoad = new Job("Load Query Job"){
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			QueryInput input = (QueryInput) GriddedEditor.this.getEditorInput();
			
			Session session = HibernateManager.openSession();
			session.beginTransaction();
			try{
				query = (GriddedQuery) session.load(GriddedQuery.class, input.getUuid());
				query.getFilterDropItems();
				query.getValueDropItems();
				query.generateDropItems(session);

			}catch (Exception ex){
				QueryPlugIn.displayLog("Could not parse query: " + input.getName()+ ".\n\n" + ex.getMessage(), ex);
			}finally{
				session.getTransaction().commit(); 
				session.close();
			}
			
			
			if (resultPage != null){
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						resultPage.setQuery();
						setDirty(false);
					}
				});
			}
			
			return Status.OK_STATUS;
		}};
		
	/**
	 * Creates a new editor
	 */
	public GriddedEditor() {
		super();		
	}

	
	/**
	 * @see org.eclipse.ui.part.MultiPageEditorPart#dispose()
	 */
	@Override
	public void dispose() {
		super.dispose();
		QueryEventManager.getInstance().removeQueryChangedEvent(qListener);
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
				this.query = new GriddedQuery();
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
	public QueryResultsTable getQueryResultsTable(){
		return this.resultPage.getQueryResultsTable();
	}
	
	/**
	 * @return the query
	 */
	public GriddedQuery getQueryInternal(){
		return  (GriddedQuery) getQuery();
	}
	
	/**
	 * @return the query
	 */
	public Query getQuery(){
		try {
			loadQueryLoad.join();	//wait for the query loading job if applicable
		} catch (InterruptedException e) {
			QueryPlugIn.displayLog("Could not load query." + e.getMessage(), e);
		}
		
		return this.query;
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
		QueryInput input = ((QueryInput) getEditorInput());
		super.setPartName(input.getName());
		showBusy(true);
		try {
			resultPage = new GriddedTableResultsPage(this);
			addPage(0, resultPage, input);
			setPageText(0, "Tabular Results");
			if (this.query != null && this.query.getUuid() == null){
				resultPage.setQuery();
			}
			
			mapPage = new GriddedResultsMapEditorPage(this);
			addPage(1, mapPage, input);
			setPageText(1, "Mapped Results");
			
		} catch (final Throwable t) {
			QueryPlugIn.log("Could not open query editor", t);
		}finally{
			showBusy(false);
		}
	}
	
	/**
	 * Re-run the query and refresh the results.
	 */
	public void refreshQuery(){
		//update date filter
		getQueryInternal().setDateFilter(resultPage.getDateFilter());
		
		if (!getQuery().isValid()){
			MessageDialog.openError(getSite().getShell(), "Error", "Query invalid.  Please fix query definition and try again.");
			return;
		}
		
		//show progress area
		resultPage.showProgressArea();
		
		//run query
		final IProgressMonitor mymonitor = resultPage.createProgressMonitor();
		Job runQueryJob = new Job("Running query: " + this.query.getName()) {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					List<GridResultItem> results = query.getQueryResults(mymonitor);
					
					resultPage.updateAndShowTable(results, mymonitor);
				} catch (Exception ex) {
					QueryPlugIn.displayLog("Could not execute query.", ex);
					resultPage.updateAndShowTable(new ArrayList<GridResultItem>(), mymonitor);
				}
				mapPage.refresh();
				return Status.OK_STATUS;
			}
		};
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
			this.query = (GriddedQuery) savedQuery;
			setInput(new QueryInput(savedQuery));
			newQuery = true;
		}
		
		if (newQuery){
			resultPage.setQuery();			
		}
		updatePartName();
		setDirty(false);
		
		//TODO: this is a bit of a hack to get the querylistview to be updated
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
		Query savedQuery = QueryEditorUtils.doSaveAs(this, true);
		if (savedQuery == null){
			return;
		}
		
		this.query = (GriddedQuery) savedQuery;
		setInput(new QueryInput(savedQuery));
		updatePartName();
		resultPage.setQuery();
		
		setDirty(false);
	}

	/**
	 * @see net.refractions.udig.project.ui.internal.MapPart#getMap()
	 */
	@Override
	public Map getMap() {
		if (mapPage == null){
			return null;
		}
		return 	mapPage.getMap();
	}

	/**
	 * @see net.refractions.udig.project.ui.internal.MapPart#openContextMenu()
	 */
	@Override
	public void openContextMenu() {
		mapPage.openContextMenu();
		
	}

	/**
	 * @see net.refractions.udig.project.ui.internal.MapPart#setFont(org.eclipse.swt.widgets.Control)
	 */
	@Override
	public void setFont(Control textArea) {
		mapPage.setFont(textArea);
		
	}

	/**
	 * @see net.refractions.udig.project.ui.internal.MapPart#setSelectionProvider(net.refractions.udig.project.ui.tool.IMapEditorSelectionProvider)
	 */
	@Override
	public void setSelectionProvider(
			IMapEditorSelectionProvider selectionProvider) {
		mapPage.setSelectionProvider(selectionProvider);
		
	}

	/**
	 * @see net.refractions.udig.project.ui.internal.MapPart#getStatusLineManager()
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
	public QueryInput getInputInternal(){
		return (QueryInput) getEditorInput();
	}
}

	
