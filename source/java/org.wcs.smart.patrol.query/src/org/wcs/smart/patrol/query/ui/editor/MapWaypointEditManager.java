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
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.eclipse.swt.widgets.Display;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.udig.project.render.IViewportModel;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.query.engine.IWaypointUpdateableResultSet;
import org.wcs.smart.patrol.query.internal.Messages;
import org.wcs.smart.patrol.query.model.IPatrolQueryResultItem;
import org.wcs.smart.query.common.engine.IQueryResult;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.common.engine.ObservationQueryResultItem;
import org.wcs.smart.query.common.engine.WaypointQueryResultItem;
import org.wcs.smart.query.common.model.ISearchabledResultSet;
import org.wcs.smart.query.common.ui.QueryResultsEditor;
import org.wcs.smart.udig.IMapEditManager;
import org.wcs.smart.util.ReprojectUtils;

/**
 * Map edit manager for moving waypoints on a patrol query 
 * results map.
 * 
 * @author Emily
 *
 */
public class MapWaypointEditManager implements IMapEditManager {

	private List<Object> undoCommands = new ArrayList<>();

	private QueryResultsEditor editor;
	
	public MapWaypointEditManager(QueryResultsEditor editor){
		this.editor = editor;
	}
	
	@Override
	public synchronized void moveFeature(Object feature, int x, int y, IViewportModel vm) {
		if (!(feature instanceof IPatrolQueryResultItem))
			return;

		IPatrolQueryResultItem pw = (IPatrolQueryResultItem) feature;
		Coordinate crspx = vm.pixelToWorld(x, y);
		// convert to lat/long
		if (!CRS.equalsIgnoreMetadata(vm.getCRS(), SmartDB.DATABASE_CRS)) {
			try {
				crspx = ReprojectUtils.reproject(crspx.x, crspx.y, vm.getCRS(), SmartDB.DATABASE_CRS);
			} catch (Exception ex) {
				SmartPatrolPlugIn.log(ex.getMessage(), ex);
				return;
			}
		}
		
		doMove(pw, crspx.x, crspx.y);
	}

	private void revert(IPatrolQueryResultItem pw, double x, double y, Float distance, Float direction){
		IQueryResult result = editor.getQuery().getCachedResults(); 
		if (!(result instanceof IWaypointUpdateableResultSet)) return;
		try {
			if (((IWaypointUpdateableResultSet)result).updateWaypointPosition(pw, x, y, distance, direction)){
				
			}
			editor.refreshResults();
		} catch (Exception e) {
			SmartPatrolPlugIn.displayLog(Messages.MapWaypointEditManager_MoveError + e.getMessage(), e);
		}
	
	}
	private void doMove(IPatrolQueryResultItem item, double x, double y){
		IQueryResult result = editor.getQuery().getCachedResults(); 
		if (!(result instanceof IWaypointUpdateableResultSet)) return;
		
		WaypointQueryResultItem pw = (WaypointQueryResultItem)item;
		try {
			double x1 = pw.getWaypointRawX(null);
			double y1 = pw.getWaypointRawY(null);
			Float distance1 = pw.getWaypointDistance();
			Float direction1 = pw.getWaypointDirection();
			
			Float distancen = pw.getWaypointDistance();
			Float directionn = pw.getWaypointDirection();
			double xn = pw.getWaypointRawX(null);
			double yn = pw.getWaypointRawY(null);
			
			if (distancen != null && directionn != null) {
				//update distance and direction
				Float[] data = Waypoint.computeDistanceBearing(new Coordinate(xn, yn), new Coordinate(x,y));
				distancen = data[0];
				directionn = data[1];
			}else {
				xn = x;
				yn = y;
			}
				
			if (((IWaypointUpdateableResultSet)result).updateWaypointPosition(item, xn, yn, distancen, directionn)){
				addUndo(item, x1, y1, distance1, direction1);
				editor.refreshResults();
			}
		} catch (Exception e) {
			SmartPatrolPlugIn.displayLog(Messages.MapWaypointEditManager_MoveError + e.getMessage(), e);
		}
	}
	
	@Override
	public EditPoint findFeature(int x, int y, IViewportModel vm) {
		try{
			if (editor.getQuery().getCachedResults() instanceof ISearchabledResultSet){
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
				
				HashMap<UUID, Set<WaypointQueryResultItem>> items = new HashMap<>();
				List<IResultItem> searchResults = ((ISearchabledResultSet)editor.getQuery().getCachedResults()).search(dbll.x, dbll.y, dbur.x, dbur.y);
				
				double distance = Double.POSITIVE_INFINITY;
				
				for (IResultItem pi : searchResults){
//					IPatrolQueryResultItem i = (IPatrolQueryResultItem)pi;
					WaypointQueryResultItem i = (WaypointQueryResultItem)pi;
					Coordinate c = new Coordinate(i.getWaypointX(null), i.getWaypointY(null));
					double d = c.distance(db);
					
					if (d < distance){
						items.clear();
						HashSet<WaypointQueryResultItem> set = (HashSet<WaypointQueryResultItem>) items.get(i.getWaypointUuid());
						if (set == null){
							set = new HashSet<>();
							items.put(i.getWaypointUuid(), set);
						}
						set.add(i);
						
						distance = d;
					}else if (d == distance){
						HashSet<WaypointQueryResultItem> set = (HashSet<WaypointQueryResultItem>) items.get(i.getWaypointUuid());
						if (set == null){
							set = new HashSet<>();
							items.put(i.getWaypointUuid(), set);
						}
						set.add(i);
					}
				}
				
				if (items.isEmpty()) return null;
				
				Entry<UUID, Set<WaypointQueryResultItem>> item = items.entrySet().iterator().next();
				
				WaypointQueryResultItem first = item.getValue().iterator().next();
				
				Coordinate px = ReprojectUtils.reproject(first.getWaypointX(null), first.getWaypointY(null), SmartDB.DATABASE_CRS, vm.getCRS());
				Point pnt = vm.worldToPixel(px);
				if (pnt.distance(x, y) > 5) return null;
				StringBuilder sb = new StringBuilder();
				
			
				sb.append(((IPatrolQueryResultItem)first).getPatrolId() + " (" + first.getWaypointId() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
				sb.append("\n"); //$NON-NLS-1$
				sb.append(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).format(first.getWaypointDateTime()));
				sb.append("\n"); //$NON-NLS-1$
				
				for (WaypointQueryResultItem result : item.getValue()){
					if (result instanceof ObservationQueryResultItem) {
						ObservationQueryResultItem oi = (ObservationQueryResultItem)result;
						if (oi.getCategories() != null && oi.getCategories().length > 0){
							sb.append(oi.getCategories()[oi.getCategories().length-1]);
							sb.append("\n"); //$NON-NLS-1$
						}
					}
				}
				
				return new EditPoint(vm.worldToPixel(px), first, sb.toString());	
			}
		}catch (Exception ex){
			SmartPatrolPlugIn.log(ex.getMessage(), ex);
		}
		return null;
		
	}

	private void addUndo(IPatrolQueryResultItem wp, double x, double y, Float distance, Float direction) {
		undoCommands.add(0, new Object[] { wp, x, y, distance, direction });
		if (undoCommands.size() > 100) {
			undoCommands.remove(undoCommands.size() - 1);
		}
	}

	@Override
	public synchronized void undo() {
		if (undoCommands.isEmpty())
			return;

		Object c = undoCommands.remove(0);
		Object[] data = (Object[]) c;

		IPatrolQueryResultItem pw = (IPatrolQueryResultItem) data[0];
		double x = (double) data[1];
		double y = (double) data[2];
		Float distance = (Float) data[3];
		Float direction = (Float) data[4];
		Display.getDefault().syncExec(()->revert(pw, x, y, distance, direction));
	}


	@Override
	public boolean canUndo() {
		return !undoCommands.isEmpty();
	}
}
