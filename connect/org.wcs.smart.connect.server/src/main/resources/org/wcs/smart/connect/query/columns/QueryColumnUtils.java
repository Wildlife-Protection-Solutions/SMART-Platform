/*
 * Copyright (C) 2015 Wildlife Conservation Society
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
package org.wcs.smart.connect.query.columns;

import java.sql.SQLException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.connect.query.QueryManager;
import org.wcs.smart.observation.model.ObservationOptions;
import org.wcs.smart.patrol.query.model.observation.PatrolCategoryQueryColumn;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.query.model.AttributeQueryColumn;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryColumn;

/**
 * Provides data model columns for a given conservation 
 * area. 
 * @author Emily
 *
 */
public class QueryColumnUtils {

	/**
	 * Loads the observation options from the database for a given 
	 * conservation area.
	 * 
	 * @param ca
	 * @param s
	 * @return
	 */
	public static ObservationOptions getOptions(ConservationArea ca, Session s){
		return (ObservationOptions) s.get(ObservationOptions.class, ca.getUuid());
	}

	/**
	 * Determines if a given observation options tracks the distance
	 * and direction.  Will return true if op is null.
	 * @param op
	 * @return
	 */
	public static boolean trackDistanceDirection(ObservationOptions op){
		if (op == null) return true;
		return op.getTrackDistanceDirection();
	}

	/**
	 * Determines if a given observation options tracks the observer.
	 * Will return true if op is null.
	 * @param op
	 * @return
	 */
	public static boolean trackObserver(ObservationOptions op){
		if (op == null) return true;
		return op.getTrackObserver();
	}
	
	/**
	 * Removes columns from the list that are not visible in the query.
	 * This function updates the columns list.
	 * 
	 * @param columns
	 * @param query
	 */
	public static void filterQueryColumns(List<QueryColumn> columns, Query query){
		if (query instanceof SimpleQuery){
			SimpleQuery sq = (SimpleQuery)query;
			
			if (sq.getVisibleColumns() != null){
				HashSet<String> keys = new HashSet<String>();
				String[] bits = sq.getVisibleColumns().split(",");
				for (String key : bits){
					keys.add(key);
				}
				for (Iterator<QueryColumn> iterator = columns.iterator(); iterator.hasNext();) {
					QueryColumn column = (QueryColumn) iterator.next();
					if (!keys.contains(column.getKey())){
						iterator.remove();
					}	
				}
			}
		}
	}
	
	/**
	 * Gets all data model columns for a given locale
	 * @param session
	 * @param l
	 * @param q
	 * @return
	 * @throws SQLException
	 */
	//TODO: CCAA queries
	@SuppressWarnings("unchecked")
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
