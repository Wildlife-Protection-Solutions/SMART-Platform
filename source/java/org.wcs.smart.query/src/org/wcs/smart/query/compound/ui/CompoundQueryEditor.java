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
package org.wcs.smart.query.compound.ui;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.hibernate.Session;
import org.locationtech.udig.project.internal.Map;
import org.locationtech.udig.project.internal.command.navigation.ZoomCommand;
import org.locationtech.udig.project.ui.internal.MapPart;
import org.locationtech.udig.project.ui.tool.IMapEditorSelectionProvider;
import org.wcs.smart.IProjectionProvider;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.QueryHibernateManager;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.common.model.CompoundMapQuery;
import org.wcs.smart.query.common.model.udig.IQueryService;
import org.wcs.smart.query.common.ui.QueryMapPageEditor;
import org.wcs.smart.query.event.IQueryListener;
import org.wcs.smart.query.event.QueryEventManager;
import org.wcs.smart.query.event.QueryListenerAdapter;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryProxy;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;
import org.wcs.smart.query.ui.QueryEditorUtils;
import org.wcs.smart.query.ui.definition.QueryDefView;
import org.wcs.smart.query.ui.editor.IMapQueryEditor;
import org.wcs.smart.query.ui.editor.QueryEditorInput;

/**
 * Editor for compound queries.
 * 
 * @author Emily
 *
 */
public class CompoundQueryEditor extends MultiPageEditorPart implements MapPart, IMapQueryEditor, IAdaptable, IProjectionProvider{

	public static final String ID = "org.wcs.smart.query.editor.compound"; //$NON-NLS-1$
	
	protected QueryProxy query;
	
	protected CompoundQueryInfoPage page1;
	protected QueryMapPageEditor page2;
	private boolean isDirty = false;
	private Projection currentPrj = null;
	
	private RunCompoundQueryJob runQueryJob;
	
	private Job loadQueryLoad = new Job(Messages.QueryResultsEditor_LoadQueryJobName){
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			QueryEditorInput input = (QueryEditorInput) CompoundQueryEditor.this.getEditorInput();

			Session session = HibernateManager.openSession();
			session.beginTransaction();
			try{
				Query squery = (CompoundMapQuery) QueryHibernateManager.getInstance().findQuery(session, input.getUuid(), input.getType());
				query = new QueryProxy(squery);
				query.getQueryType().getDropItemFactory().generateDropItems(query, session);
			}catch (Exception ex){
				QueryPlugIn.displayLog(MessageFormat.format(
						Messages.QueryResultsEditor_Error_CouldNotParse, new Object[]{ input.getName()})+ ex.getLocalizedMessage(), ex);
			}finally{
				session.getTransaction().rollback();
				session.close();
			}
			return Status.OK_STATUS;
		}};
		
	private IQueryListener qListener = new QueryListenerAdapter() {
		@Override
		public void queryModified(int eventType, Object object) {
			if (object != null && object.equals(CompoundQueryEditor.this.query.getQuery())){
				if (eventType == IQueryListener.QUERY_DEFINITION_MODIFIED){
					isDirty = true;
					firePropertyChange(PROP_DIRTY);
				}else if (eventType == IQueryListener.QUERY_NAME_MODIFIED){
					boolean lIsDirty = isDirty;
					Query updatedQuery = (Query) object;
					CompoundQueryEditor.this.getQueryProxy().getQuery().setName(updatedQuery.getName());
					CompoundQueryEditor.this.getQueryProxy().getQuery().setNames(updatedQuery.getNames());
					((QueryEditorInput)getEditorInput()).setQueryName(updatedQuery.getName());
					
					updatePartName();
					page1.updateQueryName();
					
					isDirty = lIsDirty;
					firePropertyChange(MultiPageEditorPart.PROP_DIRTY);
				}
			}else if (object != null && object instanceof QueryEditorInput 
					&& ((QueryEditorInput)object).getUuid().equals(getQueryProxy().getQuery().getUuid()) 
					&& eventType == IQueryListener.QUERY_DELETED){
				//close part
				getSite().getShell().getDisplay().asyncExec(new Runnable(){
					@Override
					public void run() {
						getSite().getWorkbenchWindow().getActivePage().closeEditor(CompoundQueryEditor.this, false);					
					}});
				
			}
		}
		@Override
		public void queryRun(Query query) {
			if (query != null && query.equals(CompoundQueryEditor.this.query.getQuery())){
				executeQuery();
			}
		}	
	};
	
	
	@Override
	public QueryProxy getQueryProxy() {
		try {
			loadQueryLoad.join();	//wait for the query loading job if applicable
		} catch (InterruptedException e) {
			QueryPlugIn.displayLog(Messages.QueryResultsEditor_Error_CouldNotLoad + e.getLocalizedMessage(), e);
		}
		return this.query;
	}

	/**
	 * Refreshes the summary table
	 */
	public void refreshQueryTable(){
		page1.refreshTable();
	}
	
	/**
	 * Re-run the query and refresh the results.
	 */
	public void executeQuery(){
		runQueryJob.cancel();
		page1.clearTable();
		runQueryJob.schedule();
	}
	
	
	@Override
	public QueryEditorInput getInputInternal() {
		return (QueryEditorInput) getEditorInput();
	}

	@Override
	public void validate() {
	}

	@Override
	public void reparseQuery() {
		Session session = HibernateManager.openSession();
		session.beginTransaction();
		try {
			getQueryProxy().getQueryType().getDropItemFactory().generateDropItems(getQueryProxy(), session);
		} catch (Exception ex) {
			QueryPlugIn.log(ex.getMessage(), ex);
		} finally {
			try{
				session.getTransaction().rollback();
			}catch(Exception ex){
				QueryPlugIn.log(ex.getMessage(), ex);
			}
			session.close();
		}
		QueryEventManager.getInstance().fireRefreshQuery(getQueryProxy().getQuery());
	}

	@Override
	public void setDirty(boolean dirty) {
		this.isDirty = dirty;
		firePropertyChange(MultiPageEditorPart.PROP_DIRTY);
	}

	@Override
	public Projection getProjection() {
		return currentPrj;
	}

	@Override
	public IQueryService createQueryService() {
		return null;
	}

	/**
	 * Updates the editor name with the query name
	 */
	public void updatePartName(){
		super.setPartName(getEditorInput().getName());
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
	
	@Override
	public void showMapPage(ReferencedEnvelope env) {
		if (env != null){
			page2.setInitialZoom(env);
			getMap().sendCommandSync(new ZoomCommand(env));
		}
		for (int i = 0; i < getPageCount(); i ++){
			if (getEditor(i) == page2){
				setActivePage(i);
				return;
			}
		}
	}

	@Override
	public Map getMap() {
		return page2.getMap();
	}

	@Override
	public void openContextMenu() {
		page2.openContextMenu();
		
	}

	@Override
	public void setFont(Control textArea) {
		page2.setFont(textArea);
		
	}

	@Override
	public void setSelectionProvider(
			IMapEditorSelectionProvider selectionProvider) {
		page2.setSelectionProvider(selectionProvider);
		
	}

	@Override
	public IStatusLineManager getStatusLineManager() {
		return page2.getStatusLineManager();
	}

	/**
	 * @see org.eclipse.ui.part.MultiPageEditorPart#dispose()
	 */
	@Override
	public void dispose() {
		super.dispose();
		runQueryJob.dispose();
		query.dispose();
		QueryEventManager.getInstance().removeListener(qListener);
		runQueryJob.cancel();
	}
	
	/**
	 * Saves the current query
	 * @see org.eclipse.ui.part.EditorPart#doSave(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void doSave(IProgressMonitor monitor) {
		Query savedQuery = QueryEditorUtils.doSave(this, monitor);
		if (savedQuery == null){
			//error 
			return;
		}
		if (!savedQuery.equals(query.getQuery())){
			//saved as new query
			this.query = new QueryProxy(savedQuery);
			setInput(new QueryEditorInput(savedQuery));
		}
		updatePartName();
		setDirty(false);
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
		page1.clearTable();
		page2.reset(true);
		
		setDirty(false);
		//this is a bit of a hack to get the querylistview to be updated
		//correctly
		//this cannot be called until setinput has bee called
		getSite().getWorkbenchWindow().getActivePage().activate(getSite().getWorkbenchWindow().getActivePage().findView(QueryDefView.ID));
		getSite().getWorkbenchWindow().getActivePage().activate(getSite().getPart());
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		super.setSite(site);
		super.setInput(input);
		
		if (input instanceof QueryEditorInput){
			QueryEditorInput input2 = ((QueryEditorInput)input);
			if (input2.getUuid() == null){
				//create a new query
				CompoundMapQuery cq = new CompoundMapQuery();
				cq.setConservationArea(SmartDB.getCurrentConservationArea());
				cq.setConservationAreaFilter( (new ConservationAreaFilter(true, SmartDB.getCurrentConservationArea())).asString() );
				cq.setName(Messages.CompoundQueryEditor_NewCompoundQueryName);
				cq.setOwner(SmartDB.getCurrentEmployee());
				
				this.query = new QueryProxy(cq);
				setDirty(false);
			}else{
				loadQueryLoad.schedule();
			}
			super.setTitleImage(input2.getType().getImage());
		}
		
		runQueryJob = new RunCompoundQueryJob(this);
		QueryEventManager.getInstance().addListener(qListener);
	}

	public void setProjection(Projection currentProjection){
		this.currentPrj = currentProjection;
	}
	
	@Override
	public boolean isDirty() {
		return isDirty;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return true;
	}

	@Override
	public void setFocus() {
		page1.setFocus();
	}

	@Override
	public Object getAdapter(Class adapter) {
		if (adapter.isAssignableFrom(Map.class)) {
			return getMap();
		}
		return super.getAdapter(adapter);
	}



	@Override
	protected void createPages() {
		QueryEditorInput input = ((QueryEditorInput) getEditorInput());
		super.setPartName(input.getName());
		showBusy(true);
		try {
			page1 = new CompoundQueryInfoPage(this);
			addPage(0, page1, input);
			setPageText(0, Messages.CompoundQueryEditor_SummariesTabName);
			setPageImage(0, QueryPlugIn.getDefault().getImageRegistry().get(QueryPlugIn.TABLE_ICON));
			page1.updateQueryName();
			
			page2 = new QueryMapPageEditor(this);
			addPage(1, page2, input);
			setPageText(1, Messages.QueryResultsEditor_MappedResultsTabName);
			setPageImage(1, SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.MAP_ICON));
			
			setTitleImage(input.getType().getImage());
			page1.initPage();
			
		} catch (final Throwable t) {
			QueryPlugIn.log("Could not open query editor", t); //$NON-NLS-1$
			throw new RuntimeException("Could not open query editor" + t.getMessage(), t); //$NON-NLS-1$
		}finally{
			showBusy(false);
		}
	}
	
}
