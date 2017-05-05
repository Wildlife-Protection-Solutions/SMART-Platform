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
package org.wcs.smart.entity.ui;

import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.common.filter.StringFilterComposite;
import org.wcs.smart.common.filter.StringFilterComposite.StringComparison;
import org.wcs.smart.common.filter.StringFilterComposite.TextField;
import org.wcs.smart.entity.internal.Messages;
import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.entity.model.Status;
import org.wcs.smart.hibernate.SmartDB;

/**
 * Filter for filtering entity types.
 * 
 * @author Emily
 *
 */
public class EntityTypeFilter  {

	private static StringFilterComposite.TextField ENTITYTYPE_NAME_FILTER = new StringFilterComposite.TextField(Messages.EntityTypeFilter_NameFieldName, "lbl.value");  //$NON-NLS-1$
	public static StringFilterComposite.TextField[] SEARCH_FIELDS = {ENTITYTYPE_NAME_FILTER};
	
	private EntityType.Type[] types = null;
	private Status[] status = null;
	
	private String strFilter = null;
	private StringComparison stringComparator = null;
	private TextField searchField;
	
	public EntityTypeFilter(){
		setDefaults();
	}
	
	/**
	 * 
	 * @return entity type type filters
	 */
	public EntityType.Type[] getEntityTypeFilters(){
		return this.types;
	}
	
	/**
	 * 
	 * @return entity type status filters
	 */
	public Status[] getEntityTypeStatus(){
		return this.status;
	}
	
	public String getSearchText(){
		return this.strFilter;
	}
	public TextField getSearchField(){
		return this.searchField;
	}
	public StringComparison getStringComparator(){
		return this.stringComparator;
	}
	
	/**
	 * Resets all values to the default
	 */
	public void setDefaults(){
		this.strFilter = null;
		this.stringComparator = null;
		this.searchField = ENTITYTYPE_NAME_FILTER;
		
		this.types = null;
		this.status = new Status[]{Status.ACTIVE};
	}
	
	/**
	 * Sets the entity types type to filter by.  If
	 * null all entity types will be selected.
	 * 
	 * @param types list of patrol types
	 */
	public void setEntityTypes(EntityType.Type[] types){
		this.types = types;
	}
	
	/**
	 * Sets the entity types status to filter by.  If
	 * null all entity types will be selected.
	 * 
	 * @param types list of patrol types
	 */
	public void setEntityStatus(Status[] status){
		this.status = status;
	}
	
	/**
	 * Sets the entity type id filter.  If either are null then
	 * all entity types will be included.
	 * 
	 * @param stringComparator the types of string comparison or null
	 * @param text the text to compare or null
	 */
	public void setEntityTypeStringFilter(StringComparison stringComparator, 
			String text, TextField searchField){
		this.stringComparator = stringComparator;
		this.strFilter = text;
		this.searchField = searchField;
	}
	

	
	/**
	 * Builds a query that returns the following patrol fields:
	 * patrol uuid, patrol id, patrol type, start date, end date
	 * 
	 * @param s
	 * @return
	 */
	public Query buildQuery(Session s){ 
		StringBuilder str = new StringBuilder();
		str.append("SELECT e.uuid, e.keyId, e.name "); //$NON-NLS-1$
		str.append("FROM EntityType e "); //$NON-NLS-1$
		if (searchField != null){
			str.append("left join e.names as lbl with lbl.id.language.uuid = :language "); //$NON-NLS-1$
		}
		str.append("WHERE e.conservationArea = :ca "); //$NON-NLS-1$


		if (types != null && types.length > 0){
			str.append(" AND "); //$NON-NLS-1$
			str.append(" e.type IN (:types) "); //$NON-NLS-1$
		}
		if (status != null && status.length > 0){
			str.append(" AND "); //$NON-NLS-1$
			str.append(" e.status IN (:status) "); //$NON-NLS-1$
		}
		
		if (strFilter != null && stringComparator != null && searchField != null){
			str.append(" AND "); //$NON-NLS-1$
			str.append(" lower(" + searchField.getDbFieldName() + ") like :eid "); //$NON-NLS-1$ //$NON-NLS-2$
		}
		
		str.append(" ORDER BY e.name asc"); //$NON-NLS-1$
		
		Query query = s.createQuery(str.toString());
		query.setParameter("ca", SmartDB.getCurrentConservationArea()); //$NON-NLS-1$
		if (searchField != null){
			query.setParameter("language", SmartDB.getCurrentLanguage().getUuid()); //$NON-NLS-1$
		}
		
		if (types != null && types.length > 0){
			query.setParameterList("types", this.types); //$NON-NLS-1$
		}
		if (status != null && status.length > 0){
			query.setParameterList("status", this.status); //$NON-NLS-1$
		}
		if (strFilter != null && stringComparator != null && searchField != null){
			if (stringComparator == StringComparison.CONTAINS){
				query.setParameter("eid", "%" + this.strFilter.toLowerCase() + "%"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}else{
				query.setParameter("eid", this.strFilter.toLowerCase()); //$NON-NLS-1$
			}
		}
		
		
		return query;
	}

	

}
