package org.wcs.smart.query.ui.editor;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.hibernate.Session;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.catalog.IService;
import org.locationtech.udig.project.internal.Map;
import org.locationtech.udig.project.internal.commands.AddLayersCommand;
import org.locationtech.udig.project.ui.internal.MapPart;
import org.locationtech.udig.project.ui.tool.IMapEditorSelectionProvider;
import org.wcs.smart.IProjectionProvider;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.QueryHibernateManager;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.QueryTypeManager;
import org.wcs.smart.query.common.engine.IPagedQueryResultSet;
import org.wcs.smart.query.common.engine.IQueryResult;
import org.wcs.smart.query.common.engine.MemoryQueryResult;
import org.wcs.smart.query.common.engine.QueryExecutor;
import org.wcs.smart.query.common.model.CompoundMapQuery;
import org.wcs.smart.query.common.model.CompoundMapQueryLayer;
import org.wcs.smart.query.common.model.udig.IQueryService;
import org.wcs.smart.query.common.ui.QueryMapPageEditor;
import org.wcs.smart.query.compound.ui.CompoundDefinitionPanel;
import org.wcs.smart.query.compound.ui.QueryDropItem;
import org.wcs.smart.query.event.IQueryListener;
import org.wcs.smart.query.event.QueryEventManager;
import org.wcs.smart.query.event.QueryListenerAdapter;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.IMappableQueryType;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryProxy;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;
import org.wcs.smart.query.ui.QueryEditorUtils;
import org.wcs.smart.query.ui.definition.QueryDefView;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.util.ReprojectUtils;

public class CompoundQueryEditor extends MultiPageEditorPart implements MapPart, IMapQueryEditor, IAdaptable, IProjectionProvider{

	public static final String ID = "org.wcs.smart.query.editor.compound"; //$NON-NLS-1$
	
	protected QueryProxy query;
	
	protected CompoundQueryInfoPage page1;
	protected QueryMapPageEditor page2;
	private boolean isDirty = false;
	private Projection currentPrj = null;
	
	//TODO: run query job must load projection
	private Job runQueryJob = new Job("Executing compound query"){

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			runQueryJob.setName(Messages.QueryResultsEditor_RunQueryJobName + getQueryProxy().getQuery().getName());
			
			//load the current view projection
			currentPrj = HibernateManager.getCurrentViewProjection();
			if (currentPrj != null && currentPrj.getParsedCoordinateReferenceSystem() == null){
				try{
					currentPrj.setParsedCoordinateReferenceSystem(ReprojectUtils.stringToCrs(currentPrj.getDefinition()));
				}catch (Exception ex){
					//eat me
				}
			}
			
			CompoundMapQuery query = (CompoundMapQuery) getQueryProxy().getQuery();
			List<QueryItem> items = new ArrayList<QueryItem>();
					
			Session s = HibernateManager.openSession();
			try{
				for (CompoundMapQueryLayer layer : query.getLayers()){
					IQueryType type =  QueryTypeManager.INSTANCE.findQueryType(layer.getQueryType());
					Query q = QueryHibernateManager.getInstance().findQuery(s, layer.getQueryUuid(),type);
					if (q != null){
						q.getName();
						q.getId();
						QueryItem qi = new QueryItem(q, type);
					
						for (DropItem it : getQueryProxy().getDropItems(CompoundDefinitionPanel.ID)){
							if (((QueryDropItem)it).getQueryUuid().equals(q.getUuid())){
								Display.getDefault().syncExec(new Runnable(){

									@Override
									public void run() {
										qi.setDateFilter(((QueryDropItem)it).getDateField());
										q.setDateFilter(((QueryDropItem)it).getDateField());		
									}
									
								});
								
								break;
							}
						}
						items.add(qi);
					}
				}
			}finally{
				s.close();
			}
			
			page1.setupTable(items);
			
			for (final QueryItem i : items){
				
				Job runQueryJob = new Job("run sub query job"){
					@Override
					protected IStatus run(IProgressMonitor monitor) {
						Session s = HibernateManager.openSession();
						try{
							ProgressMonitorWrapper wrapper = new ProgressMonitorWrapper(monitor, i.getProgressBar());
							IQueryResult results = QueryExecutor.INSTANCE.executeQuery(i.getQuery(), s, wrapper);
							i.getQuery().setCachedResults(results);
							
							if (i.getQueryType() instanceof IMappableQueryType){
								IService qService = (IService)((IMappableQueryType)i.getQueryType()).createQueryService(i.getQuery(), CompoundQueryEditor.this);
								try{
									addLayers(qService, monitor);
								}catch (Exception ex){
									ex.printStackTrace();
								}
							}
							if (results instanceof IPagedQueryResultSet){
								i.setTotalCnt(((IPagedQueryResultSet) results).getItemCount());
							}else if (results instanceof MemoryQueryResult<?>){
								i.setTotalCnt(((MemoryQueryResult)results).getData().size());
							}
						}catch(Exception ex){
							ex.printStackTrace();
							i.setTotalCnt(-1);
						}finally{
							s.close();
						}
						Display.getDefault().asyncExec(new Runnable(){

							@Override
							public void run() {
								page1.refreshTable();
								
							}
							
						});
						return Status.OK_STATUS;
					}
				};
				runQueryJob.schedule();
				
			}
			
			
			return Status.OK_STATUS;
			
		}
		
	};
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
					refreshQuery();
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
	 * Re-run the query and refresh the results.
	 */
	public void refreshQuery(){
		//TODO:
//		//cancel existing run query job.
		runQueryJob.cancel();
		getQueryProxy().getQueryDefinitionPanel().saveItems(getQueryProxy());

//		if (!getQueryProxy().isValid()){
//			MessageDialog.openError(getSite().getShell(), Messages.QueryResultsEditor_Error_DialogTitle, Messages.QueryResultsEditor_InvalidQueryError);
//			return;
//		}
//		
//		//clear existing results
//		page1.getQueryResultsTable().setInput((IPagedQueryResultSet)null);	
//		//show progress area
//		page1.showProgressArea();
//
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
		// TODO Auto-generated method stub
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
	public void showMapPage() {
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
	 * Saves the current query
	 * @see org.eclipse.ui.part.EditorPart#doSave(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void doSave(IProgressMonitor monitor) {
		boolean newQuery = getQueryProxy().getQuery().getUuid() == null;
		
		Query savedQuery = QueryEditorUtils.doSave(this, monitor);
		if (savedQuery == null){
			//error 
			return;
		}
		if (!savedQuery.equals(query.getQuery())){
			//saved as new query
			this.query = new QueryProxy(savedQuery);
			setInput(new QueryEditorInput(savedQuery));
			newQuery = true;
		}
		
		if (newQuery){
			//TODO: update page1 summary?
//			page1.setQuery()
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
//		
//		page1.getQueryResultsTable().clearColumns();
//		page1.setQuery();
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
				cq.setName("New Compound Query");
				cq.setOwner(SmartDB.getCurrentEmployee());
				
				this.query = new QueryProxy(cq);
				setDirty(false);
			}else{
				loadQueryLoad.schedule();
			}
			super.setTitleImage(input2.getType().getImage());
		}
		
		QueryEventManager.getInstance().addListener(qListener);
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
			setPageText(0, "Queries Summary");
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
					//TODO:
//					SimpleQuery q = getQueryInternal();
//					q.getQueryColumns(Locale.getDefault(), null, QueryResultsEditor.this);
//					
					getSite().getShell().getDisplay().syncExec(new Runnable(){

						@Override
						public void run() {
							if (query != null && getQueryProxy().getQuery().getUuid() == null){
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
	
	
	private void addLayers(IService service, IProgressMonitor monitor) throws IOException{
		List<IGeoResource> layers = (List<IGeoResource>) service.resources(monitor);
		
		AddLayersCommand command = new AddLayersCommand(layers) {
			@Override
			public void run(IProgressMonitor monitor) throws Exception {
				super.run(monitor);
				//TODO: - styles and style listeners
//				if (parentEditor.getQueryProxy().getQuery() instanceof StyledQuery){
//					//update layer style
//					final StyledQuery sq = ((StyledQuery)parentEditor.getQueryProxy().getQuery());
//					if (sq.getStyle() != null){
//						for (ILayer layer : getLayers()){
//							try{
//								String dataType = layer.getGeoResource().getIdentifier().getRef();
//								if (dataType == null){
//									dataType = layer.getGeoResource().getID().toString();
//								}
//								QueryStyleParser.INSTANCE.applyStyle(sq, dataType, (StyleBlackboard) layer.getStyleBlackboard());
//								//do this to ensure the correct events are fired
//								((Layer)layer).setStyleBlackboard((StyleBlackboard)layer.getStyleBlackboard());
//								
//							}catch (Exception ex){
//								QueryPlugIn.log(ex.getMessage(), ex);
//							}
//						}
//					}
//					
//					//add style listeners
//					for (final ILayer layer : getLayers()){
//						layer.addListener(styleListener);
//					}
//				}
			}
		};
		if (getMap() == null || getMap().getRenderManager() == null || getMap().getRenderManagerInternal().isDisposed()) return;
		getMap().sendCommandASync(command);
	}
}
