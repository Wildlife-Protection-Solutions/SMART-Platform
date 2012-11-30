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
package org.wcs.smart.query.model;

import java.util.Arrays;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

import org.hibernate.Session;
import org.hibernate.annotations.GenericGenerator;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.observation.ObservationQuery;
import org.wcs.smart.query.model.patrol.PatrolQuery;
import org.wcs.smart.query.parser.filter.DateFilter;
import org.wcs.smart.query.ui.gridded.GriddedEditor;
import org.wcs.smart.query.ui.observation.QueryResultsEditor;
import org.wcs.smart.query.ui.patrol.PatrolQueryResultsEditor;
import org.wcs.smart.query.ui.summary.SummaryEditor;

/**
 * Parent query class that contains fields
 * shared across all queries.
 * 
 * @author Emily
 * @since 1.0.0
 */

@Entity
@Inheritance(strategy= InheritanceType.TABLE_PER_CLASS)
public abstract class Query {
	
	//if you add another query type you must update
	//the queryInput constructor
	public enum QueryType{
		OBSERVATION("ObservationQuery", QueryResultsEditor.ID, Messages.Query_ObservationQueryName, ObservationQuery.class), //$NON-NLS-1$
		SUMMARY("SummaryQuery", SummaryEditor.ID, Messages.Query_SummaryQueryName, SummaryQuery.class), //$NON-NLS-1$
		GRIDDED("GriddedQuery", GriddedEditor.ID,  Messages.Query_GriddedQueryName, GriddedQuery.class), //$NON-NLS-1$
		PATROL("PatrolQuery", PatrolQueryResultsEditor.ID, Messages.Query_PatrolQueryName, PatrolQuery.class); //$NON-NLS-1$
		
		private String objectName;
		private String editorId;
		private String uiName;
		private Class<? extends Query> hibrnateClazz;
		
		private QueryType(String objectName, String editorId, String uiName, Class<? extends Query> hibrnateClazz){
			this.objectName = objectName;
			this.editorId = editorId;
			this.uiName = uiName;
			this.hibrnateClazz = hibrnateClazz;
		}
		
		/**
		 * 
		 * @return the hibernate class that represents the 
		 * query object.
		 * 
		 */
		public Class<? extends Query> getHibernateClass(){
			return this.hibrnateClazz;
		}
		
		/**
		 * @return the name displayed on the gui
		 */
		public String getUiName(){
			return this.uiName;
		}
		/**
		 * The name of the java object
		 * that represents this query type.
		 * @return
		 */
		public String getObjectName(){
			return this.objectName;
		}
		
		/**
		 * 
		 * @return the editor associated
		 * with the give query type/
		 */
		public String getEditorId(){
			return this.editorId;
		}
	}
	private byte[] uuid = null;
	private String name = null;
	private Employee owner = null;
	private ConservationArea conservationArea = null;
	private boolean isShared = false;
	private String id;
	
	private QueryFolder ownerFolder = null;
	private boolean isValid = false;
	
	
	public Query(){
		name = Messages.Query_DefaultQueryName;
		conservationArea = SmartDB.getCurrentConservationArea();
		owner =  SmartDB.getCurrentEmployee();
		ownerFolder = null;
		isShared = false;;
	}
	
	/**
	 * @return query uuid
	 */
	@Id
	@GeneratedValue(generator="uuid")
	@GenericGenerator(name= "uuid", strategy="uuid2")
	public byte[] getUuid() {
		return uuid;
	}
	
	/**
	 * @param uuid the query uuid
	 */
	public void setUuid(byte[] uuid) {
		this.uuid = uuid;
	}
	
	/**
	 * @return query name
	 */
	@Column(name = "name")
	public String getName() {
		return name;
	}
	/**
	 * @param name the query name
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * A human-readable conservation area unique identifier for
	 * the query. 
	 * 
	 * @return
	 */
	public String getId(){
		return this.id;
	}
	/**
	 * Sets the query id
	 * @param id
	 */
	public void setId(String id){
		this.id = id;
	}
	
	/**
	 * @return query owner
	 */
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name="creator_uuid", referencedColumnName="uuid")
	public Employee getOwner() {
		return owner;
	}
	/**
	 * @param owner the query owner
	 */
	public void setOwner(Employee owner) {
		this.owner = owner;
	}
	
	/**
	 * @return the conservation area
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="ca_uuid", referencedColumnName="uuid")
	public ConservationArea getConservationArea() {
		return conservationArea;
	}
	/**
	 * @param owner the query owner
	 */
	public void setConservationArea(ConservationArea conservationArea) {
		this.conservationArea = conservationArea;
	}
	
	/**
	 * @return <code>true</code> if conservation area level query; <code>false</code>
	 * if user only query
	 */
	@Column(name="shared")
	public boolean getIsShared(){
		return this.isShared;
	}
	/**
	 * Sets if the query is shared
	 * @param isShared
	 */
	public void setIsShared(boolean isShared){
		this.isShared = isShared;
	}
	
	/**
	 * @return the query folder associated with the query or <code>null</code>
	 * if associated with the root folder
	 */
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name="folder_uuid", referencedColumnName="uuid")
	public QueryFolder getFolder(){
		return this.ownerFolder;
	}
	/**
	 * @param folder the query folder
	 */
	public void setFolder(QueryFolder folder){
		this.ownerFolder = folder;
	}
	
	
//	/**
//	 * @return date created
//	 */
//	public Date getDateCreated() {
//		return dateCreated;
//	}
//	/**
//	 * @param dateCreated the date the query created
//	 */
//	public void setDateCreated(Date dateCreated) {
//		this.dateCreated = dateCreated;
//	}
//	
	
	@Override
	public int hashCode() {
		if (uuid == null){ return super.hashCode(); }
		return Arrays.hashCode(uuid);
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (! (obj instanceof Query)){
			return false;
		}
		Query other = (Query) obj;
		if (getUuid() == null && other.getUuid() == null){
			return super.equals(obj);
		}
		if (!Arrays.equals(getUuid(), other.getUuid()))
			return false;
		return true;
	}
	
	/**
	 * @return the state of the query
	 */
	@Transient
	public boolean isValid(){
		return this.isValid;
	}
	/**
	 * The state of the query.
	 * @param isValid
	 */
	public void setIsValid(boolean isValid){
		this.isValid = isValid;
	}
	
	/**
	 * @return the type of query
	 */
	@Transient
	public abstract QueryType getType();
	
	/**
	 * Generates ui drop items for the given query
	 * @param session
	 * @throws Exception
	 */
	@Transient
	public abstract void generateDropItems(Session session) throws Exception;
	
	/**
	 * 
	 * Compares query definitions.
	 * 
	 * @param other
	 * @return <code>true</code> if the query definitions are the same.
	 */
	@Transient
	public abstract boolean isDefinitionEqual(Query other);
	
	/**
	 * 
	 * Copies the query definition from the
	 * provided query into the current query.
	 * 
	 * @param copy the query to copy from 
	 */
	@Transient
	public abstract void copyFrom(Query copy);

	/**
	 * Creates a copy of the current query.
	 * @return new query
	 */
	public abstract Query clone();
	
	/**
	 * Sets the date filter associated with the current
	 * query.
	 * @param filter date filter
	 */
	public abstract void setDateFilter(DateFilter filter);
}
