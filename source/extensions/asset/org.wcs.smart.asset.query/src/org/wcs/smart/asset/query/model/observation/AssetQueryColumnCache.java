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
package org.wcs.smart.asset.query.model.observation;

import java.util.ArrayList;
import java.util.Locale;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.wcs.smart.asset.query.internal.Messages;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.ca.datamodel.DataModelManager;
import org.wcs.smart.ca.datamodel.IDataModelListener;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.events.IWaypointEventListener;
import org.wcs.smart.observation.events.WaypointEventManager;
import org.wcs.smart.observation.events.WaypointEventManager.EventType;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.query.DataModelQueryColumns;
import org.wcs.smart.query.QueryDataModelManager;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.query.model.WaypointGeometryQueryColumn;

/**
 * Query column cache.
 * 
 * @author Emily
 *
 */
public class AssetQueryColumnCache {

	private static volatile AssetQueryColumnCache instance = null;
	
	public static AssetQueryColumnCache getInstance(){
		if (instance == null){
			synchronized (AssetQueryColumnCache.class) {
				if (instance == null){
					instance = new AssetQueryColumnCache();
				}
			}			
		}
		return instance;
	}
	
	private volatile QueryColumn[] queryColumns = null;
	private volatile QueryColumn[] queryColumnsId = null;
	private volatile QueryColumn[] waypointQueryColumns = null;
	private volatile QueryColumn[] waypointQueryColumnsId = null;
	
	private final Object OBSERVATIONLOCK = new Object();
	private final Object WAYPOINTLOCK = new Object();
	
	private AssetQueryColumnCache(){
		DataModelManager.INSTANCE.addChangeListener(new IDataModelListener() {
			@Override
			public void modified() {
				queryColumns = null;
			}
		});
		
		WaypointEventManager.getInstance().addListener(EventType.WAYPOINT_OPTIONS_MODIFIED, new IWaypointEventListener() {
			@Override
			public void handleEvent(Waypoint wp) {
				queryColumns = null;
			}
		});
	}
	
	
	/**
	 * 
	 * @return query columns available to a waypoint query based
	 * on the asset options and the data model of the conservation
	 * area.
	 * This function will access the database the first
	 * time it is called, subsequent calls return cached values. 
	 */
	public  QueryColumn[] getObservationQueryColumns(boolean includeIds) {
		
		if (!includeIds && queryColumns != null){
			return cloneColumns(queryColumns);
		}
		if (includeIds && queryColumnsId != null){
			return cloneColumns(queryColumnsId);
		}
		synchronized (OBSERVATIONLOCK) {
			if (!includeIds && queryColumns != null){
				return cloneColumns(queryColumns);
			}
			if (includeIds && queryColumnsId != null){
				return cloneColumns(queryColumnsId);
			}
			//outside job to prevent deadlocking
			final DataModel dataModel = QueryDataModelManager.getInstance().getDataModel();
			
			Job j = new Job(Messages.QueryColumn_LoadingObservationColumnJobName){

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				//load from the database 
				ArrayList<QueryColumn> cols = new ArrayList<QueryColumn>();
				
				for (int i = 0; i < FixedQueryColumn.FixedColumns.values().length; i++) {
					FixedQueryColumn.FixedColumns item = FixedQueryColumn.FixedColumns.values()[i];
					if (item == FixedQueryColumn.FixedColumns.OBS_GROUP_ID) continue;
					if (item == FixedQueryColumn.FixedColumns.WAYPOINT_UUID) continue;
					if (item == FixedQueryColumn.FixedColumns.OBSERVATION_UUID) continue;
					
					boolean add = true;
					if (item == FixedQueryColumn.FixedColumns.CA_ID || item == FixedQueryColumn.FixedColumns.CA_NAME){
						add = SmartDB.isMultipleAnalysis();
					}
					if (add){
						QueryColumn toAdd = new FixedQueryColumn(item, Locale.getDefault());
						cols.add(toAdd);
						
						if (item == FixedQueryColumn.FixedColumns.WAYPOINT_ID ||
								item == FixedQueryColumn.FixedColumns.WAYPOINT_TIME ||
								item == FixedQueryColumn.FixedColumns.WAYPOINT_COMMENT ||
								item == FixedQueryColumn.FixedColumns.WAYPOINT_X ||
								item == FixedQueryColumn.FixedColumns.WAYPOINT_Y 
//								item == FixedQueryColumn.FixedColumns.WAYPOINT_DIRECTION ||
//								item == FixedQueryColumn.FixedColumns.WAYPOINT_DISTANCE
								){
							toAdd.setEdit(true);
						}
					}
				}

				// add data model category columns
				cols.addAll(DataModelQueryColumns.generateDataModelQueryColumns(dataModel, false));
				
				
				QueryColumn qc = new FixedQueryColumn(FixedQueryColumn.FixedColumns.OBS_GROUP_ID, Locale.getDefault());
				qc.setEdit(false);
				cols.add(qc);

				cols.add(new WaypointGeometryQueryColumn(Locale.getDefault()));
				
				if (!includeIds) {
					queryColumns = cols.toArray(new QueryColumn[cols.size()]);
				}else {
					cols.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.WAYPOINT_UUID, Locale.getDefault()));
					cols.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.OBSERVATION_UUID, Locale.getDefault()));					
					queryColumnsId = cols.toArray(new QueryColumn[cols.size()]);
				}
				
				
				return Status.OK_STATUS;
			}
			};
			j.schedule();
			try{
				j.join();
			}catch (Exception ex){
				throw new IllegalStateException(ex);
			}
		}
		
		if (!includeIds){
			return cloneColumns(queryColumns);
		}
		return cloneColumns(queryColumnsId);		
	}

	
	/**
	 * 
	 * @return query columns available to a waypoint query based
	 * on the asset options and the data model of the conservation
	 * area.
	 * This function will access the database the first
	 * time it is called, subsequent calls return cached values. 
	 */
	public  QueryColumn[] getWaypointQueryColumns(boolean includeIds) {
		
		if (!includeIds && waypointQueryColumns != null){
			return cloneColumns(waypointQueryColumns);
		}
		if (includeIds && waypointQueryColumnsId != null){
			return cloneColumns(waypointQueryColumnsId);
		}
		synchronized (WAYPOINTLOCK) {
			if (!includeIds && waypointQueryColumns != null){
				return cloneColumns(waypointQueryColumns);
			}
			if (includeIds && waypointQueryColumnsId != null){
				return cloneColumns(waypointQueryColumnsId);
			}	
		
		
			Job j = new Job(Messages.QueryColumnCache_LoadingWPQueryColumnJobName){

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				//load from the database 
				ArrayList<QueryColumn> cols = new ArrayList<QueryColumn>();
				
				for (int i = 0; i < FixedQueryColumn.FixedColumns.values().length; i++) {
					FixedQueryColumn.FixedColumns item = FixedQueryColumn.FixedColumns.values()[i];
					
					if (item == FixedQueryColumn.FixedColumns.OBS_GROUP_ID) continue;
					if (item == FixedQueryColumn.FixedColumns.WAYPOINT_UUID) continue;
					if (item == FixedQueryColumn.FixedColumns.OBSERVATION_UUID) continue;

					boolean add = true;
					if (item == FixedQueryColumn.FixedColumns.CA_ID || item == FixedQueryColumn.FixedColumns.CA_NAME){
						add = SmartDB.isMultipleAnalysis();
					}
					if (add){
						FixedQueryColumn toAdd = new FixedQueryColumn(item, Locale.getDefault());
						cols.add(toAdd);
						
						if (item == FixedQueryColumn.FixedColumns.WAYPOINT_ID ||
								item == FixedQueryColumn.FixedColumns.WAYPOINT_TIME ||
								item == FixedQueryColumn.FixedColumns.WAYPOINT_COMMENT ||
								item == FixedQueryColumn.FixedColumns.WAYPOINT_X ||
								item == FixedQueryColumn.FixedColumns.WAYPOINT_Y
//								item == FixedQueryColumn.FixedColumns.WAYPOINT_DIRECTION ||
//								item == FixedQueryColumn.FixedColumns.WAYPOINT_DISTANCE 
								){
							toAdd.setEdit(true);
						}
					}
				}
				cols.add(new WaypointGeometryQueryColumn(Locale.getDefault()));
				if (includeIds ) {
					cols.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.WAYPOINT_UUID, Locale.getDefault()));
					waypointQueryColumnsId = cols.toArray(new QueryColumn[cols.size()]);
				}else {

					waypointQueryColumns = cols.toArray(new QueryColumn[cols.size()]);
				}
				return Status.OK_STATUS;
			}
			};
			j.schedule();
			try{
				j.join();
			}catch (Exception ex){
				throw new IllegalStateException(ex);
			}
		}
		
		if (!includeIds){
			return cloneColumns(waypointQueryColumns);
		}else {
			return cloneColumns(waypointQueryColumnsId);
		}
		
	}


	private QueryColumn[] cloneColumns(QueryColumn[] cols){
		QueryColumn[] copies = new QueryColumn[cols.length];
		for (int i = 0; i < copies.length; i ++){
			copies[i] = cols[i].clone();
		}
		return copies;
	}
	
}
