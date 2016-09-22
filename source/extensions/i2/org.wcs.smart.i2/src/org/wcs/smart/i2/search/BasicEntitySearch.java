/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2.search;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityType;

public class BasicEntitySearch implements IIntelEntitySearch{

	
	private int maxResultCnt = MAX_RESULT_CNT;
	private String searchString = null;
	private List<IntelEntityType> entityTypeFilter = null;
	
	public BasicEntitySearch(String searchString){
		this.searchString = searchString;
	}
	
	public BasicEntitySearch(String searchString, int maxResults){
		this.searchString = searchString;
		this.maxResultCnt = maxResults;
	}
	
	public BasicEntitySearch(String searchString, List<IntelEntityType> entityTypeFilter){
		this.searchString = searchString;
		this.entityTypeFilter = entityTypeFilter;
	}
	
	public List<IntelEntity> doSearch(Session session){

		if (searchString == null || searchString.isEmpty()){
			
			Criteria c = session.createCriteria(IntelEntity.class);
			if (entityTypeFilter != null && !entityTypeFilter.isEmpty()){
				c.add(Restrictions.in("entityType", entityTypeFilter));
			}
			c.setMaxResults(maxResultCnt);
			
			List<IntelEntity> items = c.list();
			for (IntelEntity it : items){
				lazyLoadEntity(it, session);
			}
			return items;
		}
		//exact match id attribute		
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT e");
		sb.append(" FROM IntelEntity e, ");
		sb.append(" IntelEntityType t, ");
		sb.append(" IntelEntityAttributeValue v left join v.attributeListItem i left Join i.names a ");
		sb.append(" WHERE ");
		sb.append(" e.entityType.uuid = t.uuid AND ");
		sb.append(" v.id.attribute = t.idAttribute AND ");
		sb.append(" v.id.entity = e AND ");
		if (entityTypeFilter != null && !entityTypeFilter.isEmpty()){
			sb.append("t IN (:types) AND ");
		}
		sb.append(" ( ( LOWER(v.stringValue) = :textSearch)  OR ");
		sb.append(" ( LOWER(a.value) = :textSearch1 ) ) ");
		
		Query query = session.createQuery(sb.toString());
		if (entityTypeFilter != null && !entityTypeFilter.isEmpty()){
			query.setParameterList("types", entityTypeFilter);
		}
		query.setString("textSearch", searchString.toLowerCase());
		query.setString("textSearch1", searchString.toLowerCase());
		
		List<IntelEntity> queryResults = query.list();
		
		List<IntelEntity> results = new ArrayList<IntelEntity>();
		for (IntelEntity ie : queryResults){
			if (!results.contains(ie)){
				lazyLoadEntity(ie, session);
				results.add(ie);
				if(results.size() == maxResultCnt) break;
			}
		}
		
		//fuzzy match id attribute	
		sb = new StringBuilder();
		sb.append("SELECT e");
		sb.append(" FROM IntelEntity e, ");
		sb.append(" IntelEntityType t, ");
		sb.append(" IntelEntityAttributeValue v left join v.attributeListItem i left Join i.names a ");
		sb.append(" WHERE ");
		sb.append(" e.entityType.uuid = t.uuid AND ");
		sb.append(" v.id.attribute = t.idAttribute AND ");
		sb.append(" v.id.entity = e AND ");
		if (entityTypeFilter != null && !entityTypeFilter.isEmpty()){
			sb.append("t IN (:types) AND ");
		}
		sb.append(" ( ( LOWER(v.stringValue) like :textSearch)  OR ");
		sb.append(" ( LOWER(a.value) like :textSearch1 ) ) ");
		
		query = session.createQuery(sb.toString());
		if (entityTypeFilter != null && !entityTypeFilter.isEmpty()){
			query.setParameterList("types", entityTypeFilter);
		}
		query.setString("textSearch", "%" + searchString.toLowerCase() + "%");
		query.setString("textSearch1","%" +  searchString.toLowerCase() + "%");
		queryResults = query.list();
				
		for (IntelEntity ie : queryResults){
			if (!results.contains(ie)){
				lazyLoadEntity(ie, session);
				results.add(ie);
				if(results.size() == maxResultCnt) break;
			}
		}
		return queryResults;
	}
	
	private void lazyLoadEntity(IntelEntity it, Session session){
		it.getIdAttributeAsText();
		it.getEntityType();
		if (it.getPrimaryAttachment() != null){
			try {
				it.getPrimaryAttachment().getCopyFromLocation();
				it.getPrimaryAttachment().computeFileLocation(session);
			} catch (Exception e) {
				Intelligence2PlugIn.log("Unable to compute attachment location", e);
			}
		}
	}
}
