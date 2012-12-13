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
package org.wcs.smart.query.model.observation;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.PatrolHibernateManager;
import org.wcs.smart.patrol.model.PatrolOptions;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.IResultItem;

/**
 * A column available for output 
 * by a observation query.
 * 
 * @author egouge
 * @since 1.0.0
 */
public abstract class QueryColumn implements Cloneable{


	/**
	 * Possible column types
	 */
	public enum ColumnType{
		INTEGER("Integer", java.sql.Types.INTEGER), //$NON-NLS-1$
		LONG("Integer", java.sql.Types.INTEGER), //$NON-NLS-1$
		NUMBER("Double", java.sql.Types.DOUBLE), //$NON-NLS-1$
		STRING("String", java.sql.Types.VARCHAR), //$NON-NLS-1$
		BOOLEAN("Integer", java.sql.Types.BOOLEAN), //$NON-NLS-1$
		DATE("Date", java.sql.Types.DATE), //$NON-NLS-1$
		TIME("Date", java.sql.Types.TIME); //$NON-NLS-1$
		
		public String geotoolsType;
		public int sqlType;
		ColumnType(String geotoolsType, int sqlType){
			this.geotoolsType = geotoolsType;
			this.sqlType = sqlType;
		}
		
		public int getSqlType(){
			return this.sqlType;
		}
	}
	
	private String key;
	private String name;
	private ColumnType type;
	private boolean isVisible = true;
	
	private static QueryColumn[] queryColumns = null;
	private static QueryColumn[] patrolQueryColumns = null;
	private static QueryColumn[] gridQueryColumns = null;
	
	private static final Object GRIDLOCK = new Object();
	private static final Object PATROLLOCK = new Object();
	private static final Object WAYPOINTLOCK = new Object();
	
	/**
	 * Creates a new query column 
	 * @param name the column name as displayed to the user
	 * @param key the unique identifier for the column
	 * @param type the column data type
	 */
	public QueryColumn(String name, String key, ColumnType type){
		this.name = name;
		this.key = key;
		this.type = type;
	}
	/**
	 * A unique key for the column.
	 * <p>This can be used when persisting 
	 * column information.
	 * </p> 
	 * 
	 * @return unique key
	 */
	public String getKey() {
		return key;
	}

	/**
	 * This is the name of the column.  It is displayed to the user
	 * in the output table.
	 * 
	 * @return the query columns
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the column data type
	 */
	public ColumnType getType(){
		return this.type;
	}
	
	/**
	 * @param type the column type
	 */
	public void setType(ColumnType type){
		this.type = type;
	}
	
	/** 
	 * The hash code is based on the column key
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode(){
		return key.hashCode();
	}
	
	/** Two columns are the same if they have the same kye.
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object o){
		if (o == this){
			return true;
		}
		if (!(o instanceof QueryColumn)){
			return false;
		}
		
		if (key != null){
			return this.key.equals(((QueryColumn)o).key);
		}
		return false;
	}
	
	/**
	 * @return if the column is visible or hidden
	 */
	public boolean isVisible(){
		return this.isVisible;
	}
	/**
	 * @param isVisible if the column is visible or hidden
	 */
	public void setVisible(boolean isVisible){
		this.isVisible = isVisible;
	}
	
	/**
	 * @param item
	 * @return for a given query result item returns
	 * the object associated with this column
	 */
	public abstract Object getValue(IResultItem item) ;
	
	/** Clones the object
	 * @see java.lang.Object#clone()
	 */
	public abstract QueryColumn clone();
	
	
	
	/**
	 * 
	 * @return query columns available to a waypoint query based
	 * on the patrol options and the data model of the conservation
	 * area.
	 * This function will access the database the first
	 * time it is called, subsequent calls return cached values. 
	 */
	public static  QueryColumn[] getWaypointQueryColumns() {
		
		if (queryColumns != null){
			return cloneColumns(queryColumns);
		}
		synchronized (WAYPOINTLOCK) {
			if (queryColumns != null){
				return cloneColumns(queryColumns);
			}	
		
		
			Job j = new Job(Messages.QueryColumn_LoadingObservationColumnJobName){

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				//load from the database 
				DataModel dataModel = null;
				PatrolOptions patrolOps = null;
				Session session = HibernateManager.openSession();
				
				try {
					patrolOps = PatrolHibernateManager.getPatrolOptions(SmartDB.getCurrentConservationArea(),session);
					session.beginTransaction();
					dataModel = HibernateManager.loadDataModel(SmartDB.getCurrentConservationArea(), session);
					
					ArrayList<QueryColumn> cols = new ArrayList<QueryColumn>();
				
					for (int i = 0; i < FixedQueryColumn.FixedColumns.values().length; i++) {
						FixedQueryColumn.FixedColumns item = FixedQueryColumn.FixedColumns.values()[i];
						if (item == FixedQueryColumn.FixedColumns.WAYPOINT_DIRECTION ||  
							item == FixedQueryColumn.FixedColumns.WAYPOINT_DISTANCE){
						
							if (patrolOps.getTrackDistanceDirection()){
								cols.add(new FixedQueryColumn(item));
							}
						}else if(item == FixedQueryColumn.FixedColumns.PATROL_LEG_START_DATE||
									item == FixedQueryColumn.FixedColumns.PATROL_LEG_END_DATE){
							//do nothing, don't want these columns in a waypoint query
						}else{
							cols.add(new FixedQueryColumn(item));
						}
					}

					// add data model category columns
					int numCategory = 0;
					for (Category cat : dataModel.getActiveCategories()) {
						numCategory = Math.max(numCategory, getDepth(cat));
					}

					for (int i = 0; i < numCategory; i++) {
						cols.add(new CategoryQueryColumn(Messages.QueryColumn_ObservationCategoryTableHeader + i, i));
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
						cols.add(new AttributeQueryColumn(name, att.getKeyId(), att.getType()));
					}

					queryColumns = cols.toArray(new QueryColumn[cols.size()]);
				
				} finally {
					session.getTransaction().commit();
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
		
		return  cloneColumns(queryColumns);
	}

	
	/**
	 * 
	 * @return query columns available to a patrol query based
	 * on the patrol options 
	 */
	
	public static  QueryColumn[] getPatrolQueryColumns() {
		
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
					
					try {
						ArrayList<QueryColumn> cols = new ArrayList<QueryColumn>();
					
						for (int i = 0; i < FixedQueryColumn.FixedColumns.values().length; i++) {
							FixedQueryColumn.FixedColumns item = FixedQueryColumn.FixedColumns.values()[i];
							if (item == FixedQueryColumn.FixedColumns.WAYPOINT_X||  
										item == FixedQueryColumn.FixedColumns.WAYPOINT_Y||
										item == FixedQueryColumn.FixedColumns.WAYPOINT_COMMENT||
										item == FixedQueryColumn.FixedColumns.WAYPOINT_DATE||
										item == FixedQueryColumn.FixedColumns.WAYPOINT_DIRECTION||
										item == FixedQueryColumn.FixedColumns.WAYPOINT_DISTANCE||
										item == FixedQueryColumn.FixedColumns.WAYPOINT_ID||
										item == FixedQueryColumn.FixedColumns.WAYPOINT_TIME){
								// do nothing, don't want these columns for patrol queries
							}else{
								cols.add(new FixedQueryColumn(item));
							}
						}

						patrolQueryColumns = cols.toArray(new QueryColumn[cols.size()]);
					
					} finally {
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

	
	private static QueryColumn[] cloneColumns(QueryColumn[] cols){
		QueryColumn[] copies = new QueryColumn[cols.length];
		for (int i = 0; i < copies.length; i ++){
			copies[i] = cols[i].clone();
		}
		return copies;
	}
	
	/**
	 * Compute the maximum category depth.
	 * 
	 * @param cat category
	 * @return maximum depth
	 */
	private static int getDepth(Category cat) {
		int maxDepth = 0;
		for (Category child : cat.getChildren()) {
			if (child.getIsActive()) {
				maxDepth = Math.max(maxDepth, getDepth(child));
			}
		}
		return maxDepth + 1;
	}
	
	
	public static QueryColumn[] getGridColumns() {
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
				tmp[i] = new GridQueryColumn(item); 
			}
			gridQueryColumns  = tmp;
		}
		return cloneColumns(gridQueryColumns);
	}
}
