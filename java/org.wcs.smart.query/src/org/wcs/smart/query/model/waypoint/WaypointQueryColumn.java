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
package org.wcs.smart.query.model.waypoint;

import java.util.ArrayList;

import org.hibernate.Session;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.PatrolHibernateManager;
import org.wcs.smart.patrol.model.PatrolOptions;
import org.wcs.smart.query.model.QueryResultItem;

/**
 * A column available for output 
 * by a waypoint query.
 * 
 * @author egouge
 * @since 1.0.0
 */
public abstract class WaypointQueryColumn implements Cloneable{


	/**
	 * Possible column types
	 */
	public enum ColumnType{
		INTEGER("Integer"),
		NUMBER("Double"),
		STRING("String"),
		BOOLEAN("Integer"),
		DATE("Date"),
		TIME("Date");
		
		public String geotoolsType;
		ColumnType(String geotoolsType){
			this.geotoolsType = geotoolsType;
		}
	}
	
	private String key;
	private String name;
	private ColumnType type;
	private boolean isVisible = true;
	
	/**
	 * Creates a new query column 
	 * @param name the column name as displayed to the user
	 * @param key the unique identifier for the column
	 * @param type the column data type
	 */
	public WaypointQueryColumn(String name, String key, ColumnType type){
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
		if (!(o instanceof WaypointQueryColumn)){
			return false;
		}
		
		if (key != null){
			return this.key.equals(((WaypointQueryColumn)o).key);
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
	public abstract Object getValue(QueryResultItem item) ;
	
	/** Clones the object
	 * @see java.lang.Object#clone()
	 */
	public abstract WaypointQueryColumn clone();
	
	
	private static WaypointQueryColumn[] queryColumns = null;
	
	/**
	 * 
	 * @return query columns available to a waypoint query based
	 * on the patrol options and the data model of the conservation
	 * area.
	 * This function will access the database the first
	 * time it is called, subsequent calls return cached values. 
	 */
	public static  WaypointQueryColumn[] getWaypointQueryColumns() {
		
		if (queryColumns != null){
			return cloneColumns(queryColumns);
		}
		
		//load from the database 
		DataModel dataModel = null;
		PatrolOptions patrolOps = null;
		Session session = HibernateManager.openSession();
		
		try {
			patrolOps = PatrolHibernateManager.getPatrolOptions(SmartDB.getCurrentConservationArea(),session);
			session.beginTransaction();
			dataModel = HibernateManager.loadDataModel(SmartDB.getCurrentConservationArea(), session);
		
		
		ArrayList<WaypointQueryColumn> cols = new ArrayList<WaypointQueryColumn>();
		
		for (int i = 0; i < FixedQueryColumn.FixedColumns.values().length; i++) {
			
			FixedQueryColumn.FixedColumns item = FixedQueryColumn.FixedColumns.values()[i];
			if (item == FixedQueryColumn.FixedColumns.WAYPOINT_DIRECTION ||  
					item == FixedQueryColumn.FixedColumns.WAYPOINT_DISTANCE){
				
				if (patrolOps.getTrackDistanceDirection()){
					cols.add(new FixedQueryColumn(item));
				}
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
			cols.add(new CategoryQueryColumn("Observation Category  " + i, i));
		}

		for (Attribute att : dataModel.getAttributes()) {
			String name = att.getName();
			cols.add(new AttributeQueryColumn(name, att.getKeyId(), att.getType()));
		}

		queryColumns = cols.toArray(new WaypointQueryColumn[cols.size()]);
		
		} finally {
			session.getTransaction().rollback();
			session.close();
		}
		return  cloneColumns(queryColumns);
	}
	
	private static WaypointQueryColumn[] cloneColumns(WaypointQueryColumn[] cols){
		WaypointQueryColumn[] copies = new WaypointQueryColumn[cols.length];
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
}
