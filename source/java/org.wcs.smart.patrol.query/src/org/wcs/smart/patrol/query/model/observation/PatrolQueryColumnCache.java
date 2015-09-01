package org.wcs.smart.patrol.query.model.observation;

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
import org.wcs.smart.patrol.query.internal.Messages;
import org.wcs.smart.patrol.query.model.observation.FixedQueryColumn.FixedColumns;
import org.wcs.smart.query.QueryDataModelManager;
import org.wcs.smart.query.model.GridQueryColumn;
import org.wcs.smart.query.model.QueryColumn;

/**
 * Query column cache.
 * 
 * @author Emily
 *
 */
public class PatrolQueryColumnCache {

	private static PatrolQueryColumnCache instance = null;
	private static Object INSTANCE_LOCK = new Object();
	
	public static PatrolQueryColumnCache getInstance(){
		if (instance == null){
			synchronized (INSTANCE_LOCK) {
				if (instance == null){
					instance = new PatrolQueryColumnCache();
				}
			}			
		}
		return instance;
	}
	
	private QueryColumn[] queryColumns = null;
	private QueryColumn[] patrolQueryColumns = null;
	private QueryColumn[] waypointQueryColumns = null;
	private QueryColumn[] gridQueryColumns = null;
	
	private final Object GRIDLOCK = new Object();
	private final Object PATROLLOCK = new Object();
	private final Object OBSERVATIONLOCK = new Object();
	private final Object WAYPOINTLOCK = new Object();
	
	private PatrolQueryColumnCache(){
	
		DataModelManager.INSTANCE.addChangeListener(new IDataModelListener() {
			
			@Override
			public void modified() {
				queryColumns = null;
				patrolQueryColumns = null;
				gridQueryColumns = null;
				
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
	 * on the patrol options and the data model of the conservation
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
				ObservationOptions patrolOps = null;
				Session session = HibernateManager.openSession();
				
				try {
					patrolOps = ObservationHibernateManager.getPatrolOptions(SmartDB.getCurrentConservationArea(),session);
				} finally {
					session.close();
				}	
				ArrayList<QueryColumn> cols = new ArrayList<QueryColumn>();
				
				for (int i = 0; i < FixedQueryColumn.FixedColumns.values().length; i++) {
					FixedQueryColumn.FixedColumns item = FixedQueryColumn.FixedColumns.values()[i];
					boolean add = true;
					if (item == FixedQueryColumn.FixedColumns.WAYPOINT_DIRECTION ||  
						item == FixedQueryColumn.FixedColumns.WAYPOINT_DISTANCE){
						add = patrolOps.getTrackDistanceDirection();
						
					}else if(item == FixedQueryColumn.FixedColumns.PATROL_LEG_START_DATE||
								item == FixedQueryColumn.FixedColumns.PATROL_LEG_END_DATE){
							//do nothing, don't want these columns in a waypoint query
						add = false;
					}else if (item == FixedQueryColumn.FixedColumns.CA_ID || item == FixedQueryColumn.FixedColumns.CA_NAME){
						add = SmartDB.isMultipleAnalysis();
					}else if (item == FixedQueryColumn.FixedColumns.WAYPOINT_OBSERVER){
						add = patrolOps.getTrackObserver();
					}
					if (add){
						cols.add(new FixedQueryColumn(item, Locale.getDefault()));
					}
				}

				
				
				// add data model category columns
				int numCategory = QueryDataModelManager.getInstance().getActiveDepth();
				for (int i = 0; i < numCategory; i++) {
					cols.add(new PatrolCategoryQueryColumn(MessageFormat.format(Messages.QueryColumn_ObservationCategoryTableHeader1, i), i));
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
					cols.add(new PatrolAttributeQueryColumn(name, att.getKeyId(), att.getType()));
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
	 * on the patrol options and the data model of the conservation
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
				ObservationOptions patrolOps = null;
				Session session = HibernateManager.openSession();
				
				try {
					patrolOps = ObservationHibernateManager.getPatrolOptions(SmartDB.getCurrentConservationArea(),session);
				} finally {
					session.close();
				}	
				ArrayList<QueryColumn> cols = new ArrayList<QueryColumn>();
				
				for (int i = 0; i < FixedQueryColumn.FixedColumns.values().length; i++) {
					FixedQueryColumn.FixedColumns item = FixedQueryColumn.FixedColumns.values()[i];
					boolean add = true;
					if (item == FixedQueryColumn.FixedColumns.WAYPOINT_DIRECTION ||  
						item == FixedQueryColumn.FixedColumns.WAYPOINT_DISTANCE){
						add = patrolOps.getTrackDistanceDirection();
						
					}else if(item == FixedQueryColumn.FixedColumns.PATROL_LEG_START_DATE||
								item == FixedQueryColumn.FixedColumns.PATROL_LEG_END_DATE){
							//do nothing, don't want these columns in a waypoint query
						add = false;
					}else if (item == FixedQueryColumn.FixedColumns.CA_ID || item == FixedQueryColumn.FixedColumns.CA_NAME){
						add = SmartDB.isMultipleAnalysis();
					}else if (item == FixedQueryColumn.FixedColumns.WAYPOINT_OBSERVER){
						add = false;
					}
					if (add){
						cols.add(new FixedQueryColumn(item, Locale.getDefault()));
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
	 * @return query columns available to a patrol query based
	 * on the patrol options 
	 */
	
	public  QueryColumn[] getPatrolQueryColumns() {
		
		if (patrolQueryColumns != null){
			return cloneColumns(patrolQueryColumns);
		}
		synchronized (PATROLLOCK) {
			if (patrolQueryColumns != null){
				return cloneColumns(patrolQueryColumns);
			}
			
			Job j = new Job(Messages.QueryColumn_LoadingPatrolColumnJobName){

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					//load from the database 
					Session session = HibernateManager.openSession();
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
								// do nothing, don't want these columns for patrol queries
								add = false;
							}else{
								if (item == FixedColumns.CA_ID || item == FixedColumns.CA_NAME){
									add = SmartDB.isMultipleAnalysis();
								}
							}
							if (add){
								cols.add(new FixedQueryColumn(item, Locale.getDefault()));
							}
								
						}

						patrolQueryColumns = cols.toArray(new QueryColumn[cols.size()]);
					
					} finally {
						if (session.getTransaction().isActive()){
							session.getTransaction().rollback();
						}
						session.close();
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
		
		return  cloneColumns(patrolQueryColumns);
	}

	public QueryColumn[] getGridColumns() {
		if (gridQueryColumns != null){
			return cloneColumns(gridQueryColumns);
		}
		synchronized (GRIDLOCK) {
			if (gridQueryColumns != null){
				return cloneColumns(gridQueryColumns);
			}	
			QueryColumn[] tmp = new QueryColumn[GridQueryColumn.GridColumns.values().length];	
			for (int i = 0; i < GridQueryColumn.GridColumns.values().length; i++) {
				GridQueryColumn.GridColumns item = GridQueryColumn.GridColumns.values()[i];
				tmp[i] = new GridQueryColumn(item, Locale.getDefault()); 
			}
			gridQueryColumns  = tmp;
		}
		return cloneColumns(gridQueryColumns);
	}
	
	
	private QueryColumn[] cloneColumns(QueryColumn[] cols){
		QueryColumn[] copies = new QueryColumn[cols.length];
		for (int i = 0; i < copies.length; i ++){
			copies[i] = cols[i].clone();
		}
		return copies;
	}
	
}
