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
package org.wcs.smart.patrol.query.ui.editor;

import java.awt.Point;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.hibernate.Session;
import org.locationtech.udig.project.internal.Map;
import org.locationtech.udig.project.internal.command.navigation.SetViewportBBoxCommand;
import org.locationtech.udig.project.render.IViewportModel;
import org.locationtech.udig.project.ui.internal.MapPart;
import org.locationtech.udig.project.ui.tool.IMapEditorSelectionProvider;
import org.wcs.smart.IProjectionProvider;
import org.wcs.smart.ProjectionUtils;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ConservationAreaManager;
import org.wcs.smart.ca.IAreaModifiedListener;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.map.GeometryFactoryProvider;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.query.internal.Messages;
import org.wcs.smart.patrol.query.map.udig.QueryService;
import org.wcs.smart.patrol.query.model.PatrolQuery;
import org.wcs.smart.patrol.query.model.PatrolQueryFactory;
import org.wcs.smart.patrol.query.model.PatrolQueryResultItem;
import org.wcs.smart.patrol.query.ui.querytable.PatrolTableColumn;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.QueryTypeManager;
import org.wcs.smart.query.common.engine.IQueryResult;
import org.wcs.smart.query.common.engine.MemoryQueryResult;
import org.wcs.smart.query.common.engine.QueryExecutor;
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.query.common.model.udig.IQueryService;
import org.wcs.smart.query.common.ui.QueryMapPageEditor;
import org.wcs.smart.query.common.ui.QueryResultsTable;
import org.wcs.smart.query.event.IQueryListener;
import org.wcs.smart.query.event.QueryAreaModifiedListener;
import org.wcs.smart.query.event.QueryEventManager;
import org.wcs.smart.query.event.QueryListenerAdapter;
import org.wcs.smart.query.model.IQueryEditCommand;
import org.wcs.smart.query.model.IQueryResultInfoProvider;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.query.model.QueryProxy;
import org.wcs.smart.query.ui.QueryEditorUtils;
import org.wcs.smart.query.ui.definition.QueryDefView;
import org.wcs.smart.query.ui.editor.IMapQueryEditor;
import org.wcs.smart.query.ui.editor.QueryEditorInput;
import org.wcs.smart.ui.map.tool.IInfoToolProvider;
import org.wcs.smart.user.UserLevelManager;
import org.wcs.smart.util.ReprojectUtils;
import org.wcs.smart.util.SmartUtils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.operation.distance.DistanceOp;

/**
 * Editor for displaying query results.  The editor includes two pages
 * a tabular results page and a map results page.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class PatrolQueryResultsEditor extends MultiPageEditorPart implements MapPart, IMapQueryEditor, IAdaptable, IProjectionProvider{

	public static final String ID = "org.wcs.smart.query.ui.PatrolQueryResultsEditor";  //$NON-NLS-1$

	private QueryProxy query;
	private PatrolQueryTableResultsPage page1;
	private QueryMapPageEditor page2;
	private boolean isDirty = false;
	
	private IAreaModifiedListener areaListener = null;
	private Projection projection = null;
	
	private boolean editMode = false;
	private List<Listener> editModeModified = new ArrayList<>();
	
	Job runQueryJob = new Job(Messages.PatrolQueryResultsEditor_RunQueryJobName) {
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			setName(Messages.PatrolQueryResultsEditor_RunQueryJobName + getQuery().getName());
			IProgressMonitor mymonitor = page1.createProgressMonitor();
			try {
				MemoryQueryResult<PatrolQueryResultItem> results = (MemoryQueryResult<PatrolQueryResultItem>) QueryExecutor.INSTANCE.executeQuery(getQuery(), null, mymonitor);
				if (monitor.isCanceled() || mymonitor.isCanceled()){
					page1.updateAndShowTable(null);
					return Status.CANCEL_STATUS;
				}
				page1.updateAndShowTable(results.getData());
			} catch (Exception ex) {
				QueryPlugIn.displayLog(Messages.PatrolQueryResultsEditor_ErrorRunningQuery, ex);
				page1.updateAndShowTable(new ArrayList<PatrolQueryResultItem>());
			}
			page2.refresh();
			return Status.OK_STATUS;
		}
	};
	
	private IQueryListener qListener = new QueryListenerAdapter() {
		
		@Override
		public void queryModified(int eventType, Object object) {
			if (object != null && object.equals(PatrolQueryResultsEditor.this.getQuery())){
				if (eventType == IQueryListener.QUERY_DEFINITION_MODIFIED){
					isDirty = true;
					firePropertyChange(PROP_DIRTY);
				}else if (eventType == IQueryListener.QUERY_NAME_MODIFIED){
					boolean lIsDirty = isDirty;
					Query updatedQuery = (Query) object;
					PatrolQueryResultsEditor.this.getQuery().setName(updatedQuery.getName());
					PatrolQueryResultsEditor.this.getQuery().setNames(updatedQuery.getNames());
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
						getSite().getWorkbenchWindow().getActivePage().closeEditor(PatrolQueryResultsEditor.this, false);					
					}});
				
			}

		}
		
		@Override
		public void queryRun(Query query) {
			if (query != null && query.equals(PatrolQueryResultsEditor.this.query.getQuery())){
				refreshQuery();
			}
		}
	};
	
	
	private Job loadQueryLoad = new Job(Messages.PatrolQueryResultsEditor_LoadQueryJobName){
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			QueryEditorInput input = (QueryEditorInput) PatrolQueryResultsEditor.this.getEditorInput();
			
			Session session = HibernateManager.openSession();
			session.beginTransaction();
			try{
				
				Query tquery = (PatrolQuery) session.load(PatrolQuery.class, input.getUuid());
				query = new QueryProxy(tquery);
				query.getQueryType().getDropItemFactory().generateDropItems(query, session);
				
				projection = ProjectionUtils.INSTANCE.createProjectionProvider(session, tquery.getConservationArea()).getProjection();
			}catch (Exception ex){
				QueryPlugIn.displayLog(
						MessageFormat.format(Messages.PatrolQueryResultsEditor_CouldNotParseQueryError, new Object[]{ input.getName() }) + ex.getLocalizedMessage(), ex);
								
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
	public PatrolQueryResultsEditor() {
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
	
	@Override
	public Projection getProjection(){
		return this.projection;
	}
	
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
				this.query = new QueryProxy(PatrolQueryFactory.createPatrolQuery());
				setDirty(false);
			}else{
				loadQueryLoad.schedule();
			}
		}
		QueryEventManager.getInstance().addListener(qListener);
	}

	/**
	 * Called when changes are made to the query properties page.
	 * 
	 */
	public void queryPropertiesChange(){
		setDirty(true);
		page1.setQuery();
		page2.refresh();
	}
	
	/**
	 * @return 
	 * @return the query results display table
	 */
	public QueryResultsTable getQueryResultsTable(){
		return this.page1.getQueryResultsTable();
	}
	
	/**
	 * @return the query
	 */
	public Query getQuery(){
		return getQueryProxy().getQuery();
	}

	/**
	 * @return the query
	 */
	public PatrolQuery getQueryInternal(){
		return (PatrolQuery)getQuery();
	}
	
	/**
	 * 
	 */
	@Override
	public void validate(){
		page1.validate();
	}
	
	/**
	 * Updates the editor name with the query name
	 */
	public void updatePartName(){
		super.setPartName(getEditorInput().getName());
	}
 
 
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
			page1 = new PatrolQueryTableResultsPage(this);
			addPage(0, page1, input);
			setPageText(0, Messages.PatrolQueryResultsEditor_TableTabName);
			setPageImage(0, QueryPlugIn.getDefault().getImageRegistry().get(QueryPlugIn.TABLE_ICON));
			if (this.query != null && this.getQuery().getUuid() == null){
				page1.setQuery();
			}
			
			page2 = new QueryMapPageEditor(this);
			addPage(1, page2, input);
			setPageText(1, Messages.PatrolQueryResultsEditor_MapTabName);
			setPageImage(1, SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.MAP_ICON));
		} catch (final Throwable t) {
			QueryPlugIn.log("Could not open query editor", t); //$NON-NLS-1$
			throw new RuntimeException("Could not open query editor" + t.getMessage(), t); //$NON-NLS-1$
		}finally{
			showBusy(false);
		}
		
//		if (((QueryEditorInput)getEditorInput()).getType().getKey().equals(PatrolQuery.KEY)){
			page2.getMap().getBlackboard().put(IInfoToolProvider.BLACKBOARD_KEY, getPatrolInfoProvider());
//		}
//		
//		if (canEditResults()){
//			page2.getMap().getBlackboard().put(IMapEditManager.BLACKBOARD_KEY, new MapWaypointEditManager(this));
//		}
	}

	
	protected CellLabelProvider getColumnLabelProvider(QueryColumn column, List<QueryColumn> allColumns){
		return PatrolTableColumn.getLabelProvider(column, allColumns);
	}
	
	
	protected EditingSupport getEditingSupport(ColumnViewer viewer, QueryColumn column) {
		EditingSupport s = QueryColumnEditingSupport.getCellEditor(viewer, column, this);
		if (s != null) return s;
		return null;
	}
	
	@Override
	public boolean canEditResults(){
		if (!UserLevelManager.INSTANCE.supportsUser(SmartDB.getCurrentEmployee(), UserLevelManager.ADMIN, UserLevelManager.MANAGER)) return false;
		String queryType = ((QueryEditorInput)getEditorInput()).getType().getKey();
		if (queryType.equals(PatrolQuery.KEY)){
			return true;
		}
		return false;
	}
	
	

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
	 * Re-run the query and refresh the results.
	 */
	public void refreshQuery(){
		runQueryJob.cancel();
		
		//update date filter
		getQueryInternal().setDateFilter(page1.getDateFilter());
		if (!getQueryProxy().isValid()){
			MessageDialog.openError(getSite().getShell(), Messages.PatrolQueryResultsEditor_Error_DialogTitle, Messages.PatrolQueryResultsEditor_InvalidQueryError);
			return;
		}
		page1.getQueryResultsTable().setInput(null);
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
			this.query = new QueryProxy((PatrolQuery) savedQuery);
			setInput(new QueryEditorInput(savedQuery));
			newQuery = true;
		}
		
		if (newQuery){
			page1.setQuery();			
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
		page1.getQueryResultsTable().clearColumns();
		page1.setQuery();
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
	
	@Override
	public void setFocus() {
		super.setFocus();
		
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
	 * @return the editor input as query input
	 */
	public QueryEditorInput getInputInternal(){
		return (QueryEditorInput) getEditorInput();
	}
	
	public QueryProxy getQueryProxy(){
		try {
			loadQueryLoad.join();	//wait for the query loading job if applicable
		} catch (InterruptedException e) {
			QueryPlugIn.displayLog(Messages.PatrolQueryResultsEditor_CouldNotLoadQueryError + e.getLocalizedMessage(), e);
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
				try{
					Display.getDefault().syncExec(new Runnable(){
						@Override
						public void run() {
							try{
								query.getQueryType().getDropItemFactory().generateDropItems(getQueryProxy(), session);
							}catch (Exception ex){
								QueryPlugIn.log(ex.getMessage(), ex);
							}
						}});
				}finally{
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


	@Override
	public IQueryService createQueryService() {
		return new QueryService((SimpleQuery)getQuery(), this);
	}
	
	
	/**
	 * Refreshes the map and viewer after edits completed
	 */
	@Override
	public void refreshResults(){
		TableViewer viewer = getQueryResultsTable().getTable();
		page1.refreshCount();
		page2.refresh();
		viewer.refresh(true);
		getMap().getRenderManager().refresh(null);
	}
	
	
	
	private IInfoToolProvider getPatrolInfoProvider(){
		return new IInfoToolProvider(){
			@Override
			public InfoPoint findFeature(int x, int y, IViewportModel vm) {
				//clear menu
				Menu m = page2.getMapViewer().getMenu();
				if (m != null) m.dispose();
				page2.getMapViewer().getControl().setMenu(null);
				try{
					IQueryResult r = getQueryInternal().getCachedResults();
					if (r == null) return null;
										
					Coordinate world = vm.pixelToWorld(x, y);
					
					Coordinate db = ReprojectUtils.reproject(world.x, world.y, vm.getCRS(), SmartDB.DATABASE_CRS);
					
					if (r instanceof MemoryQueryResult){
						MemoryQueryResult<PatrolQueryResultItem> results = (MemoryQueryResult<PatrolQueryResultItem>)r;
						double distance = Double.POSITIVE_INFINITY;
						PatrolQueryResultItem nearest = null;
						com.vividsolutions.jts.geom.Point toTest = GeometryFactoryProvider.getFactory().createPoint(db);
						for (PatrolQueryResultItem ri : results.getData()){
							Geometry g = ri.asGeometry(PatrolQueryResultItem.TRACK_GEOMCOLUMN_KEY);
							if (g.getEnvelopeInternal().contains(db)){
								double d = g.distance(toTest);
								if (d < distance){
									distance = d;
									nearest = ri;
								}
							}
						}

						if (nearest == null) return null;
						Geometry g = nearest.asGeometry(PatrolQueryResultItem.TRACK_GEOMCOLUMN_KEY);
						Coordinate[] c = DistanceOp.nearestPoints(g, toTest);
						if (c.length == 0) return null;
		
						Coordinate px = ReprojectUtils.reproject(c[0].x, c[0].y, SmartDB.DATABASE_CRS, vm.getCRS());
						Point pnt = vm.worldToPixel(px);
						if (pnt.distance(x, y) > 5) return null;
						StringBuilder sb = new StringBuilder();
						
						sb.append(nearest.getPatrolId());
						sb.append(" ("); //$NON-NLS-1$
						sb.append(nearest.getPatrolLegId());
						sb.append(")\n"); //$NON-NLS-1$
						sb.append(DateFormat.getDateInstance().format(nearest.getPatrolLegStartDate()) + " - " + DateFormat.getDateInstance().format(nearest.getPatrolLegEndDate()) ); //$NON-NLS-1$
							
						createMenu(page2.getMapViewer().getControl(), nearest);
						return new InfoPoint(pnt, null, sb.toString());	
					}
				}catch (Exception ex){
					SmartPatrolPlugIn.log(ex.getMessage(), ex);
					ex.printStackTrace();
				}
				return null;
			}
			
			private void createMenu(Control control, PatrolQueryResultItem toUpdate){
				
				Menu existingMenu = control.getMenu();
				if(existingMenu != null && !existingMenu.isDisposed()){
					existingMenu.dispose();
				}
				if (query == null) return;
				IQueryType queryType = QueryTypeManager.INSTANCE.findQueryType(getQuery().getTypeKey());
				if (queryType.getResultProviders().length > 0) {
					Menu menuTable = new Menu(control);
					control.setMenu(menuTable);
					control.addListener(SWT.MouseMove, new Listener(){

						@Override
						public void handleEvent(Event event) {
							control.removeListener(SWT.MouseMove, this);
							menuTable.dispose();
							control.setMenu(null);
						}
					
					});

					List<IQueryEditCommand> editItems = new ArrayList<>();
					for (final IQueryResultInfoProvider item : queryType.getResultProviders()) {
						// Create menu item
						if (!SmartDB.isMultipleAnalysis() || item.supportsCcaa()){
							if (!item.supportsMap()) continue;
							if (!(item instanceof IQueryEditCommand) || (item instanceof IQueryEditCommand && getEditMode())){
								if (item instanceof IQueryEditCommand){
									editItems.add((IQueryEditCommand) item);
									continue;
								}
								MenuItem miTest = new MenuItem(menuTable, SWT.NONE);
								if (item.getImage() != null){
									miTest.setImage(item.getImage());
								}
								miTest.setText(item.getName());
								miTest.addListener(SWT.Selection, e->{
									
									item.doWork(toUpdate);
									
								});
							}
						}
					}
					if (!editItems.isEmpty()) new MenuItem(menuTable, SWT.SEPARATOR);
					
					for (final IQueryEditCommand item : editItems) {
						MenuItem miTest = new MenuItem(menuTable, SWT.NONE);
						if (item.getImage() != null){
							miTest.setImage(item.getImage());
						}
						miTest.setText(item.getName());
						miTest.addListener(SWT.Selection, e->{
							if (item.doWork(toUpdate, getQuery().getCachedResults())){
								refreshResults();
							}
							
						});
					}
				}
			}
		};	
	}
}
