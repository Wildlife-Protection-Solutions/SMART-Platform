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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.locationtech.udig.project.render.IViewportModel;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.query.model.PatrolObservationQuery;
import org.wcs.smart.patrol.query.model.PatrolQueryFactory;
import org.wcs.smart.patrol.query.model.PatrolQueryResultItem;
import org.wcs.smart.patrol.query.model.PatrolWaypointQuery;
import org.wcs.smart.patrol.query.ui.querytable.PatrolTableColumn;
import org.wcs.smart.query.QueryTypeManager;
import org.wcs.smart.query.common.engine.IQueryResult;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.common.model.ISearchabledResultSet;
import org.wcs.smart.query.common.ui.QueryResultsEditor;
import org.wcs.smart.query.model.IQueryEditCommand;
import org.wcs.smart.query.model.IQueryResultInfoProvider;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.query.ui.editor.QueryEditorInput;
import org.wcs.smart.udig.EditPointTool;
import org.wcs.smart.udig.IMapEditManager;
import org.wcs.smart.udig.UndoTool;
import org.wcs.smart.ui.map.MapToolComposite;
import org.wcs.smart.ui.map.tool.IInfoToolProvider;
import org.wcs.smart.user.UserLevelManager;
import org.wcs.smart.util.ReprojectUtils;
import org.wcs.smart.util.SmartUtils;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Editor for displaying query results.  The editor includes two pages
 * a tabular results page and a map results page.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class PatrolSimpleQueryResultEditor extends QueryResultsEditor{

	public static final String ID = "org.wcs.smart.query.ui.QueryResultsEditor";  //$NON-NLS-1$

	
	/**
	 * Creates a new query of the given type
	 * @param type
	 * @return
	 */
	public Query createNewQuery(IQueryType type){
		return PatrolQueryFactory.createQuery(type);
	}
	
	@Override
	public String[] getEditTools(){
		return new String[]{
				MapToolComposite.SEPERATOR_TOOL_ID,
				EditPointTool.ID,
				UndoTool.ID,
				MapToolComposite.SEPERATOR_TOOL_ID
		};
	}
	
	private void updateEditTools(){		
		if (canEditResults()){
			page2.enableTool(EditPointTool.ID, getEditMode());
			IMapEditManager mgr = (IMapEditManager) getMap().getBlackboard().get(IMapEditManager.BLACKBOARD_KEY);
			if (mgr != null && mgr.canUndo()){
				page2.enableTool(UndoTool.ID, getEditMode());	
			}else{
				page2.enableTool(UndoTool.ID, false);
			}
		}else{
			page2.enableTool(UndoTool.ID, false);
			page2.enableTool(EditPointTool.ID, false);
		}
	}
	
	@Override
	protected CellLabelProvider getColumnLabelProvider(QueryColumn column, List<QueryColumn> allColumns){
		return PatrolTableColumn.getLabelProvider(column, allColumns);
	}
	
	@Override
	protected EditingSupport getEditingSupport(ColumnViewer viewer, QueryColumn column) {
		EditingSupport s = QueryColumnEditingSupport.getCellEditor(viewer, column, this);
		if (s != null) return s;
		return super.getEditingSupport(viewer, column);
	}
	
	@Override	
	protected void createPages() {
		super.createPages();
		
		if (((QueryEditorInput)getEditorInput()).getType().getKey().equals(PatrolObservationQuery.KEY) || 
				((QueryEditorInput)getEditorInput()).getType().getKey().equals(PatrolWaypointQuery.KEY)){
			page2.getMap().getBlackboard().put(IInfoToolProvider.BLACKBOARD_KEY, getObservationQueryInfoProvider());
		}
		
		if (canEditResults()){
			page2.getMap().getBlackboard().put(IMapEditManager.BLACKBOARD_KEY, new MapWaypointEditManager(this));
		}
		addEditModeModifiedListener(e->{
			updateEditTools();
		});	
		updateEditTools();
	}
	
	@Override
	public boolean canEditResults(){
		if (!UserLevelManager.INSTANCE.supportsUser(SmartDB.getCurrentEmployee(), UserLevelManager.ADMIN, UserLevelManager.MANAGER)) return false;
		String queryType = ((QueryEditorInput)getEditorInput()).getType().getKey();
		if (queryType.equals(PatrolObservationQuery.KEY) || queryType.equalsIgnoreCase(PatrolWaypointQuery.KEY)){
			return true;
		}
		return false;
	}
			
	private IInfoToolProvider getObservationQueryInfoProvider(){
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
					
					int xll = x - 5;
					int yll = y - 5;
					int xur = x + 5;
					int yur = y + 5;
					
					Coordinate world = vm.pixelToWorld(x, y);
					Coordinate worldll = vm.pixelToWorld(xll, yll);
					Coordinate worldur = vm.pixelToWorld(xur, yur);
					
					Coordinate db = ReprojectUtils.reproject(world.x, world.y, vm.getCRS(), SmartDB.DATABASE_CRS);
					Coordinate dbll = ReprojectUtils.reproject(worldll.x, worldll.y, vm.getCRS(), SmartDB.DATABASE_CRS);
					Coordinate dbur = ReprojectUtils.reproject(worldur.x, worldur.y, vm.getCRS(), SmartDB.DATABASE_CRS);
					
					if (r instanceof ISearchabledResultSet){
						
						List<IResultItem> searchResults = ((ISearchabledResultSet)r).search(dbll.x, dbll.y, dbur.x,  dbur.y);
						
						HashMap<UUID, Set<PatrolQueryResultItem>> items = new HashMap<>();
						double distance = Double.POSITIVE_INFINITY;
						for (IResultItem ri : searchResults){
							PatrolQueryResultItem i = (PatrolQueryResultItem)ri;
							Coordinate c = new Coordinate(i.getWaypointX(null), i.getWaypointY(null));
							double d = c.distance(db);
							
							if (d < distance){
								items.clear();
								HashSet<PatrolQueryResultItem> set = (HashSet<PatrolQueryResultItem>) items.get(i.getWaypointUuid());
								if (set == null){
									set = new HashSet<>();
									items.put(i.getWaypointUuid(), set);
								}
								set.add(i);
								
								distance = d;
							}else if (d == distance){
								HashSet<PatrolQueryResultItem> set = (HashSet<PatrolQueryResultItem>) items.get(i.getWaypointUuid());
								if (set == null){
									set = new HashSet<>();
									items.put(i.getWaypointUuid(), set);
								}
								set.add(i);
							}
						}

						if (items.isEmpty()) return null;
					
						PatrolQueryResultItem first = items.values().iterator().next().iterator().next();
						
						Coordinate px = ReprojectUtils.reproject(first.getWaypointX(null), first.getWaypointY(null), SmartDB.DATABASE_CRS, vm.getCRS());
						Point pnt = vm.worldToPixel(px);
						if (pnt.distance(x, y) > 5) return null;
						StringBuilder sb = new StringBuilder();
						
						for (Set<PatrolQueryResultItem> i : items.values()){
							if (sb.length() != 0) sb.append("\n"); //$NON-NLS-1$
							first = i.iterator().next();
							sb.append(first.getPatrolId() + " (" + first.getWaypointId() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
							sb.append("\n"); //$NON-NLS-1$
							sb.append(DateFormat.getDateTimeInstance().format(SmartUtils.combineDateTime(first.getWpDateTime(), first.getWaypointTime())));
							sb.append("\n"); //$NON-NLS-1$
							for (PatrolQueryResultItem result : i){
								if (result.getCategories() != null && result.getCategories().length > 0){
									sb.append(result.getCategories()[result.getCategories().length-1]);
									sb.append("\n"); //$NON-NLS-1$
								}
							}
						}
						
						createMenu(page2.getMapViewer().getControl(), first);
						return new InfoPoint(vm.worldToPixel(px), null, sb.toString());	
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
