package org.wcs.smart.i2.search;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityType;

public class BasicSearch implements IIntelSearch{

	public static final int MAX_RESULT_CNT = 500;
	
	private int maxResultCnt = MAX_RESULT_CNT;
	private String searchString = null;
	private List<IntelEntityType> entityTypeFilter = null;
	
	public BasicSearch(String searchString){
		this.searchString = searchString;
	}
	
	public BasicSearch(String searchString, List<IntelEntityType> entityTypeFilter){
		this.searchString = searchString;
		this.entityTypeFilter = entityTypeFilter;
	}
	
	public List<IntelEntity> doSearch(Session session){
		//exact match id attribute
		if (searchString == null || searchString.isEmpty()){
			
			Criteria c = session.createCriteria(IntelEntity.class);
			if (entityTypeFilter != null && !entityTypeFilter.isEmpty()){
				c.add(Restrictions.in("entityType", entityTypeFilter));
			}
			c.setMaxResults(maxResultCnt);
			
			List<IntelEntity> items = c.list();
			for (IntelEntity it : items){
				it.getIdAttributeAsText();
			}
			return items;
		}
		
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
		sb.append(" ( a.value = :textSearch1 ) ) ");
		
		Query query = session.createQuery(sb.toString());
		if (entityTypeFilter != null && !entityTypeFilter.isEmpty()){
			query.setParameterList("types", entityTypeFilter);
		}
		query.setString("textSearch", searchString.toLowerCase());
		query.setString("textSearch1", searchString);
		
		List<IntelEntity> queryResults = query.list();
		
		List<IntelEntity> results = new ArrayList<IntelEntity>();
		for (IntelEntity ie : queryResults){
			if (!results.contains(ie)){
				ie.getIdAttributeAsText();
				results.add(ie);
				if(results.size() == maxResultCnt) break;
			}
		}
		return queryResults;
	}
}
