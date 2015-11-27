package org.wcs.smart.connect.query.columns;

import java.sql.SQLException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.connect.query.QueryManager;
import org.wcs.smart.patrol.query.model.observation.PatrolCategoryQueryColumn;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.model.AttributeQueryColumn;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryColumn;

public class DataModelColumnProvider {

	//TODO: CCAA queries
	public static List<QueryColumn> getDataModelColumns(Session session, Locale l, Query q) throws SQLException{
		List<QueryColumn> keys = new ArrayList<QueryColumn>();
		
		int ccnt = QueryManager.INSTANCE.getCategoryDepth(session, q.getConservationArea().getUuid());
		for (int i = 0; i < ccnt; i ++){
			keys.add(new PatrolCategoryQueryColumn("Category " + i, i));
		}
		
		//attributes
		List<Attribute> atts = session.createCriteria(Attribute.class)
				.add(Restrictions.eq("conservationArea", q.getConservationArea()))
				.list();
		List<QueryColumn> attributes = new ArrayList<QueryColumn>();
		for (Attribute a : atts){
			attributes.add(new AttributeQueryColumn(a.getName(), a.getKeyId(), a.getType()) {
				
				@Override
				public Object getValue(IResultItem arg0) {
					return null;
				}
				
				@Override
				public QueryColumn clone() {
					return null;
				}
			});
		}
		
		Collections.sort(attributes, new Comparator<QueryColumn>() {
			@Override
			public int compare(QueryColumn o1, QueryColumn o2) {
				return Collator.getInstance(l).compare(o1.getName(), o2.getName());
			}
		});
		keys.addAll(attributes);
		return keys;
	}
}
