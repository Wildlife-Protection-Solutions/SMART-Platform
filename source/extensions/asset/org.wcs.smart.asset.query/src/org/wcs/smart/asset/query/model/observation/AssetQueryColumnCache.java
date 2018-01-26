package org.wcs.smart.asset.query.model.observation;

import java.text.Collator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.wcs.smart.asset.query.internal.Messages;
import org.wcs.smart.asset.query.model.observation.FixedQueryColumn;
import org.wcs.smart.asset.query.model.observation.AssetAttributeQueryColumn;
import org.wcs.smart.asset.query.model.observation.AssetCategoryQueryColumn;
import org.wcs.smart.asset.query.model.observation.FixedQueryColumn.FixedColumns;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.ca.datamodel.DataModelManager;
import org.wcs.smart.ca.datamodel.IDataModelListener;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.ObservationHibernateManager;
import org.wcs.smart.observation.events.IWaypointEventListener;
import org.wcs.smart.observation.events.WaypointEventManager;
import org.wcs.smart.observation.events.WaypointEventManager.EventType;
import org.wcs.smart.observation.model.ObservationOptions;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.query.QueryDataModelManager;
import org.wcs.smart.query.model.GridQueryColumn;
import org.wcs.smart.query.model.QueryColumn;

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
	private volatile QueryColumn[] AssetQueryColumns = null;
	private volatile QueryColumn[] waypointQueryColumns = null;
	
	private final Object ASSETLOCK = new Object();
	private final Object OBSERVATIONLOCK = new Object();
	private final Object WAYPOINTLOCK = new Object();
	
	private AssetQueryColumnCache(){
		DataModelManager.INSTANCE.addChangeListener(new IDataModelListener() {
			@Override
			public void modified() {
				queryColumns = null;
				AssetQueryColumns = null;
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
	public  QueryColumn[] getObservationQueryColumns() {
		
		if (queryColumns != null){
			return cloneColumns(queryColumns);
		}
		synchronized (OBSERVATIONLOCK) {
			if (queryColumns != null){
				return cloneColumns(queryColumns);
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
								item == FixedQueryColumn.FixedColumns.WAYPOINT_Y ||
								item == FixedQueryColumn.FixedColumns.WAYPOINT_DIRECTION ||
								item == FixedQueryColumn.FixedColumns.WAYPOINT_DISTANCE){
							toAdd.setEdit(true);
						}
					}
				}

				
				
				// add data model category columns
				int numCategory = QueryDataModelManager.getInstance().getActiveDepth();
				for (int i = 0; i < numCategory; i++) {
					QueryColumn toAdd = new AssetCategoryQueryColumn(MessageFormat.format(Messages.QueryColumn_ObservationCategoryTableHeader1, i), i);
					toAdd.setEdit(true);
					cols.add(toAdd);
				}
					
				//sort attributes alphabetically
				List<Attribute> atts = new ArrayList<Attribute>();
				atts.addAll( dataModel.getAttributes() );
				Collections.sort(atts, new Comparator<Attribute>(){
					@Override
					public int compare(Attribute o1, Attribute o2) {
						return Collator.getInstance().compare(o1.getName(),o2.getName());
					}});
					
				for (Attribute att : atts) {
					String name = att.getName();
					QueryColumn toAdd = new AssetAttributeQueryColumn(name, att.getKeyId(), att.getType());
					toAdd.setEdit(true);
					cols.add(toAdd);
				}
				queryColumns = cols.toArray(new QueryColumn[cols.size()]);
				
				
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
		
		return  cloneColumns(queryColumns);
	}

	
	/**
	 * 
	 * @return query columns available to a waypoint query based
	 * on the asset options and the data model of the conservation
	 * area.
	 * This function will access the database the first
	 * time it is called, subsequent calls return cached values. 
	 */
	public  QueryColumn[] getWaypointQueryColumns() {
		
		if (waypointQueryColumns != null){
			return cloneColumns(waypointQueryColumns);
		}
		synchronized (WAYPOINTLOCK) {
			if (waypointQueryColumns != null){
				return cloneColumns(waypointQueryColumns);
			}	
		
		
			Job j = new Job(Messages.QueryColumnCache_LoadingWPQueryColumnJobName){

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				//load from the database 
				ArrayList<QueryColumn> cols = new ArrayList<QueryColumn>();
				
				for (int i = 0; i < FixedQueryColumn.FixedColumns.values().length; i++) {
					FixedQueryColumn.FixedColumns item = FixedQueryColumn.FixedColumns.values()[i];
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
								item == FixedQueryColumn.FixedColumns.WAYPOINT_Y ||
								item == FixedQueryColumn.FixedColumns.WAYPOINT_DIRECTION ||
								item == FixedQueryColumn.FixedColumns.WAYPOINT_DISTANCE){
							toAdd.setEdit(true);
						}
					}
				}
				waypointQueryColumns = cols.toArray(new QueryColumn[cols.size()]);
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
		
		return  cloneColumns(waypointQueryColumns);
	}

	
	/**
	 * 
	 * @return query columns available to a asset query based
	 * on the asset options 
	 */
	
	public  QueryColumn[] getAssetQueryColumns() {
		
		if (AssetQueryColumns != null){
			return cloneColumns(AssetQueryColumns);
		}
		synchronized (ASSETLOCK) {
			if (AssetQueryColumns != null){
				return cloneColumns(AssetQueryColumns);
			}
			
			Job j = new Job(Messages.QueryColumn_LoadingAssetColumnJobName){

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					//load from the database 
					try(Session session = HibernateManager.openSession()){
						session.beginTransaction();
						try {
							ArrayList<QueryColumn> cols = new ArrayList<QueryColumn>();
						
							for (int i = 0; i < FixedQueryColumn.FixedColumns.values().length; i++) {
								FixedQueryColumn.FixedColumns item = FixedQueryColumn.FixedColumns.values()[i];
								boolean add = true;
								if (item == FixedQueryColumn.FixedColumns.WAYPOINT_X||  
											item == FixedQueryColumn.FixedColumns.WAYPOINT_Y||
											item == FixedQueryColumn.FixedColumns.WAYPOINT_COMMENT||
											item == FixedQueryColumn.FixedColumns.WAYPOINT_DATE||
											item == FixedQueryColumn.FixedColumns.WAYPOINT_DIRECTION||
											item == FixedQueryColumn.FixedColumns.WAYPOINT_DISTANCE||
											item == FixedQueryColumn.FixedColumns.WAYPOINT_ID||
											item == FixedQueryColumn.FixedColumns.WAYPOINT_TIME){
									// do nothing, don't want these columns for asset queries
									add = false;
								}else{
									if (item == FixedColumns.CA_ID || item == FixedColumns.CA_NAME){
										add = SmartDB.isMultipleAnalysis();
									}
								}
								if (add){
									FixedQueryColumn c = new FixedQueryColumn(item, Locale.getDefault());

									
									cols.add(c);
								}
									
							}
	
							AssetQueryColumns = cols.toArray(new QueryColumn[cols.size()]);
						
						} finally {
							if (session.getTransaction().isActive()){
								session.getTransaction().rollback();
							}
						}
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
		
		return  cloneColumns(AssetQueryColumns);
	}

	private QueryColumn[] cloneColumns(QueryColumn[] cols){
		QueryColumn[] copies = new QueryColumn[cols.length];
		for (int i = 0; i < copies.length; i ++){
			copies[i] = cols[i].clone();
		}
		return copies;
	}
	
}
