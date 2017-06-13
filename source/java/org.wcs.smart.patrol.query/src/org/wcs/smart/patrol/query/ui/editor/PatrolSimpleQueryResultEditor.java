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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.EditingSupport;
import org.locationtech.udig.project.render.IViewportModel;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.query.model.PatrolObservationQuery;
import org.wcs.smart.patrol.query.model.PatrolQueryFactory;
import org.wcs.smart.patrol.query.model.PatrolQueryResultItem;
import org.wcs.smart.patrol.query.model.PatrolWaypointQuery;
import org.wcs.smart.patrol.query.ui.querytable.PatrolTableColumn;
import org.wcs.smart.query.common.engine.IPagedQueryResultSet;
import org.wcs.smart.query.common.engine.IQueryResult;
import org.wcs.smart.query.common.engine.IQueryResultSetIterator;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.common.ui.QueryResultsEditor;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.query.ui.editor.QueryEditorInput;
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
		
		if (((QueryEditorInput)getEditorInput()).getType().getKey().equals(PatrolObservationQuery.KEY)){
			page2.getMap().getBlackboard().put(IInfoToolProvider.class.getCanonicalName(), getObservationQueryInfoProvider());
		}
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
				try{
					IQueryResult r = getQueryInternal().getCachedResults();
					if (r == null) return null;
					
					Coordinate world = vm.pixelToWorld(x, y);
					Coordinate db = ReprojectUtils.reproject(world.x, world.y, vm.getCRS(), SmartDB.DATABASE_CRS);
					
					if (r instanceof IPagedQueryResultSet){
						HashMap<UUID, Set<PatrolQueryResultItem>> items = new HashMap<>();
						
						try(IQueryResultSetIterator<? extends IResultItem> fIterator = ((IPagedQueryResultSet)r).iterator(IPagedQueryResultSet.MAP_PAGE_SIZE)){
							double distance = Double.POSITIVE_INFINITY;
							
							while(fIterator.hasNext()){
								PatrolQueryResultItem i = (PatrolQueryResultItem) fIterator.next();
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
						
						return new InfoPoint(vm.worldToPixel(px), null, sb.toString());	
					}
				}catch (Exception ex){
					SmartPatrolPlugIn.log(ex.getMessage(), ex);
					ex.printStackTrace();
				}
				return null;
			}
			
		};
	}


}
