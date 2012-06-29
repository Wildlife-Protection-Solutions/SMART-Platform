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

import java.lang.reflect.InvocationTargetException;
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
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.hibernate.Session;
import org.wcs.smart.ca.Employee.SmartUserLevel;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.IQueryFolderListener;
import org.wcs.smart.query.IQueryListener;
import org.wcs.smart.query.QueryEventManager;
import org.wcs.smart.query.QueryHibernateManager;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryFolder;
import org.wcs.smart.query.model.QueryInput;
import org.wcs.smart.query.model.QueryResultItem;
import org.wcs.smart.query.model.observation.ObservationQuery;
import org.wcs.smart.query.ui.definition.QueryDefView;
import org.wcs.smart.query.ui.querylist.SaveQueryDialog;
import org.wcs.smart.query.ui.querytable.QueryResultsTable;

/**
 * Editor for displaying query results.  The editor includes two pages
 * a tabular results page and a map results page.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class QueryResultsEditor extends MultiPageEditorPart implements MapPart, IAdaptable{

	public static final String ID = "org.wcs.smart.query.ui.QueryResultsEditor"; 

	private ObservationQuery query;
	private QueryResultsTablePage page1;
	private QueryMapPageEditor page2;
	private boolean isDirty = false;
	
	private IQueryListener qListener = new IQueryListener() {
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
	};
	
	
	private Job loadQueryLoad = new Job("Load Query Job"){
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
				QueryPlugIn.displayLog("Could not parse query: " + input.getName()+ ".\n\n" + ex.getMessage(), ex);
			}finally{
				session.getTransaction().rollback();
				session.close();
			}
			
			
			if (page1 != null){
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						page1.setQuery();
						setDirty(false);
					}
				});
			}
			
			return Status.OK_STATUS;
		}};
	/**
	 * Creates a new editor
	 */
	public QueryResultsEditor() {
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
				this.query = new ObservationQuery();
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
		return this.page1.getQueryResultsTable();
	}
	
	/**
	 * @return the query
	 */
	public ObservationQuery getQuery(){
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
		super.setPartName(query.getName());
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
			setPageText(0, "Tabular Results");
			if (this.query != null && this.query.getUuid() == null){
				page1.setQuery();
			}
			
			page2 = new QueryMapPageEditor(this);
			addPage(1, page2, input);
			setPageText(1, "Mapped Results");
			
		} catch (final Throwable t) {
			QueryPlugIn.log("Could not open query editor", t);
		}finally{
			showBusy(false);
		}
	}
		
	private void updateQuery(){
		//update date filter
		getQuery().setDateFilter(page1.getDateFilter());
	}

	/**
	 * Re-run the query and refresh the results.
	 */
	public void refreshQuery(){
		//update date filter
		updateQuery();
		
		if (!getQuery().isValid()){
			MessageDialog.openError(getSite().getShell(), "Error", "Query invalid.  Please fix query definition and try again.");
			return;
		}
		
		//show progress area
		page1.showProgressArea();
		
		//run query
		final IProgressMonitor mymonitor = page1.createProgressMonitor();
		Job runQueryJob = new Job("Running query: " + this.query.getName()) {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					List<QueryResultItem> results = query.getQueryResults(mymonitor);
					page1.updateAndShowTable(results, mymonitor);
				} catch (Exception ex) {
					QueryPlugIn.displayLog("Could not execute query.", ex);
					page1.updateAndShowTable(new ArrayList<QueryResultItem>(), mymonitor);
				}
				page2.refresh();
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
		//validate if user can save the current query
		if (query.getIsShared() && 
				SmartDB.getCurrentEmployee().getSmartUserLevel() != SmartUserLevel.ADMIN && 
				SmartDB.getCurrentEmployee().getSmartUserLevel() != SmartUserLevel.MANAGER ){			
			boolean ret = MessageDialog.openQuestion(getContainer().getShell(), "Save", "You do not have permission to overwrite this query.  Would you like to save it as a new query?");
			if (ret){
				doSaveAs();
			}
			return;
		}
		
		//ensure query is valid
		if (!query.isValid()){
			MessageDialog.openError(getSite().getShell(), "Save", "You cannot save an invalid query.  Please ensure fix the errors in the query and try saving again.");
			monitor.setCanceled(true);
			return;
		}else if (query.getName().trim().length() == 0){
			MessageDialog.openError(getSite().getShell(), "Save", "Query name must not be blank.");
			monitor.setCanceled(true);
			return;
		}
				
		//update the query definition 
		updateQuery();
		
		boolean newQuery = false;
		if (query.getUuid() == null){
			newQuery = true;
			//new query; we need to get folder location
			SaveQueryDialog dialog = new SaveQueryDialog(getContainer().getShell(), query, false);
			if (dialog.open() != IDialogConstants.OK_ID){
				monitor.setCanceled(true);
				return;
			}
			
			QueryFolder qf = dialog.getQueryFolder() ; 
			if (qf == null){
				QueryPlugIn.displayLog("Query not saved.  Could not determine folder.", null);
				monitor.setCanceled(true);
				return;
			}
			
			if (!qf.isRootFolder()){
				query.setFolder(qf);
				query.setIsShared(qf.getEmployee() == null);
			
			}else if (qf.getUuid().equals(QueryHibernateManager.CA_QUERY_KEY)){
				query.setIsShared(true);
			}
			query.setOwner(SmartDB.getCurrentEmployee());
			query.setConservationArea(SmartDB.getCurrentConservationArea());
			
		}
		
		if (!saveQuery(false)){
			monitor.setCanceled(true);
			return;
		}
		
		if (newQuery){
			QueryEventManager.getInstance().fireFolderChangedListeners(IQueryFolderListener.QUERY_ADDED, query);
			((QueryInput)super.getEditorInput()).setUuid(query.getUuid());
			((QueryInput)super.getEditorInput()).setId(query.getId()); 
		}else{
			QueryEventManager.getInstance().fireFolderChangedListeners(IQueryFolderListener.QUERY_SAVED, query);
		}
	
		setDirty(false);
	}

	private boolean saveQuery(boolean generateDropItems){
		boolean newQuery = query.getId() == null;
		
		Session s = HibernateManager.openSession();
		s.beginTransaction();
		try{
			if (newQuery){
				query.setId(QueryHibernateManager.generateQueryId(s));
				page1.setQuery();
			}
			if (generateDropItems){
				query.generateDropItems(s);
			}
			s.saveOrUpdate(query);
			s.getTransaction().commit();
			updatePartName();
			return true;
		}catch (Exception ex){
			QueryPlugIn.displayLog("Could not save query: " + ex.getMessage(), ex);
			s.getTransaction().rollback();
			if (newQuery){
				query.setUuid(null);
				query.setId(null);
			}
			return false;
		}finally{
			s.close();
		}

	}
	
	@Override
	public void doSaveAs() {
		
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(getContainer().getShell());
		try {
			pmd.run(false, false, new IRunnableWithProgress() {
				
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {
					
					//ensure query is valid
					if (!getQuery().isValid()){
						MessageDialog.openError(getSite().getShell(), "Save", "You cannot save an invalid query.  Please ensure fix the errors in the query and try saving again.");
						return;
					}
					
					monitor.beginTask("Save As...", 3);
					monitor.subTask("Cloning query...");
					updateQuery();
					ObservationQuery newQuery = getQuery().clone();
					
					monitor.worked(1);
					
					monitor.subTask("Getting save location...");
					SaveQueryDialog dialog = new SaveQueryDialog(getContainer().getShell(), query, true);
					if (dialog.open() != IDialogConstants.OK_ID){
						return;
					}
					
					newQuery.setName(dialog.getQueryName());
					if (newQuery.getName().trim().length() == 0){
						MessageDialog.openError(getSite().getShell(), "Save", "Query name must not be blank.");
						monitor.setCanceled(true);
						return;
					}
					
					QueryFolder qf = dialog.getQueryFolder();
					if (!qf.isRootFolder()){
						newQuery.setFolder(qf);
						newQuery.setIsShared(qf.getEmployee() == null);
					
					}else if (qf.getUuid().equals(QueryHibernateManager.CA_QUERY_KEY)){
						newQuery.setIsShared(true);
					}
					newQuery.setOwner(SmartDB.getCurrentEmployee());
					newQuery.setConservationArea(SmartDB.getCurrentConservationArea());
					
					
					ObservationQuery oldQuery = QueryResultsEditor.this.query;
					
					QueryResultsEditor.this.query = newQuery;
					monitor.subTask("Saving query...");
					if (!saveQuery(true)){
						QueryResultsEditor.this.query = oldQuery;
						return ;
					}
					monitor.worked(1);
					
					page1.setQuery();
					monitor.worked(1);
					
					QueryEventManager.getInstance().fireFolderChangedListeners(IQueryFolderListener.QUERY_ADDED, query);
					QueryResultsEditor.this.setInput(new QueryInput(newQuery));
					
					setDirty(false);
					monitor.worked(1);
					
					//TODO: update the Query Def View; see if there is a better way to do this
					QueryDefView view = (QueryDefView)getSite().getWorkbenchWindow().getActivePage().findView(QueryDefView.ID);
					if(view != null){
						if (view.getQuery().equals(oldQuery)){
							view.setQuery(newQuery);
						}
					}
					
					//TODO: this is a bit of a hack to get the querylistview to be updated
					//correctly
					getSite().getWorkbenchWindow().getActivePage().activate(view);
					getSite().getWorkbenchWindow().getActivePage().activate(getSite().getPart());
										
				}
			});
		} catch (Exception ex) {
			QueryPlugIn.displayLog("Error saving query: " + ex.getMessage(), ex);
		}
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
	
	@Override
	public void setFocus() {
		
	}

}
