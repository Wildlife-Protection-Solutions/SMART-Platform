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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.connect.query.QueryManager;
import org.wcs.smart.entity.query.model.EntityQueryResultItem;
import org.wcs.smart.er.query.model.SurveyQueryResultItem;
import org.wcs.smart.observation.model.ObservationOptions;
import org.wcs.smart.observation.query.model.ObservationQueryResultItem;
import org.wcs.smart.patrol.query.model.PatrolQueryResultItem;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.query.model.AttributeQueryColumn;
import org.wcs.smart.query.model.CategoryQueryColumn;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.query.model.QueryColumn.ColumnType;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;

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
	 * and direction.  Will return false if op is null.
	 * @param op
	 * @return
	 */
	public static boolean trackDistanceDirection(ObservationOptions op){
		if (op == null) return false;
		return op.getTrackDistanceDirection();
	}

	/**
	 * Determines if a given observation options tracks the observer.
	 * Will return true if op is null.
	 * @param op
	 * @return
	 */
	public static boolean trackObserver(ObservationOptions op){
		if (op == null) return false;
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
				String[] bits = sq.getVisibleColumns().split(","); //$NON-NLS-1$
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
	@SuppressWarnings("unchecked")
	public static List<QueryColumn> getDataModelColumns(Session session, Locale l, ConservationAreaFilter caFilter) throws SQLException{
		List<QueryColumn> keys = new ArrayList<QueryColumn>();
		
		int ccnt = QueryManager.INSTANCE.getCategoryDepth(session, caFilter);
		for (int i = 0; i < ccnt; i ++){
			keys.add(new CategoryQueryColumn(MessageFormat.format(Messages.getString("QueryColumnUtils.ObservationCategoryColumnName", l), i), i){ //$NON-NLS-1$
				@Override
				public Object getValue(IResultItem item) {
					if (item instanceof ObservationQueryResultItem){
						if (((ObservationQueryResultItem) item).getCategories().length > level){
							return ((ObservationQueryResultItem) item).getCategories()[level];
						}
					}else if (item instanceof EntityQueryResultItem ){
						if (((EntityQueryResultItem) item).getCategories().length > level){
							return ((EntityQueryResultItem) item).getCategories()[level];
						}
					}else if (item instanceof PatrolQueryResultItem ){
						if (((PatrolQueryResultItem) item).getCategories().length > level){
							return ((PatrolQueryResultItem) item).getCategories()[level];
						}
					}else if (item instanceof SurveyQueryResultItem ){
						if (((SurveyQueryResultItem) item).getCategories().length > level){
							return ((SurveyQueryResultItem) item).getCategories()[level];
						}
					}
					return null;
				}

				@Override
				public QueryColumn clone() {
					return null;
				}});
		}
		
		//attributes
		//I want all attributes that are shared across all conservationAreas
		String query = "SELECT keyId, type  FROM Attribute WHERE conservationArea.uuid in (:cauuids) group by keyId, type HAVING count(*) = :cnt order by keyId asc"; //$NON-NLS-1$
		org.hibernate.Query attquery = session.createQuery(query);
		attquery.setParameterList("cauuids", caFilter.getConservationAreaFilterIds()); //$NON-NLS-1$
		attquery.setParameter("cnt", new Long(caFilter.getConservationAreaFilterIds().size())); //$NON-NLS-1$
		
		//this gets the attribute name based on the requested locale name query 
		String nameQueryHql = "SELECT a.value FROM Label a, Attribute c where c.conservationArea.uuid in (:cauuids) AND a.id.element = c.uuid and c.keyId = :attributeKey ORDER By case when upper(a.id.language.code) = :code1 then 1 else case when upper(a.id.language.code) = :code2 then 2 else case when a.id.language.default = true then 3 else 4 end end end "; //$NON-NLS-1$
		String allLocal = l.toString().toUpperCase();
		String local = l.getLanguage().toUpperCase();
		org.hibernate.Query nameQuery = session.createQuery(nameQueryHql);
		
		List<Object[]> attributes = attquery.list();
		List<QueryColumn> attributeColumns = new ArrayList<QueryColumn>();
		for (Object[] attribute : attributes){
			String keyid = (String) attribute[0];
			AttributeType atype = (AttributeType) attribute[1];
			
			nameQuery.setParameter("attributeKey", keyid); //$NON-NLS-1$
			nameQuery.setParameter("code1", allLocal); //$NON-NLS-1$
			nameQuery.setParameter("code2", local); //$NON-NLS-1$
			nameQuery.setParameterList("cauuids", caFilter.getConservationAreaFilterIds()); //$NON-NLS-1$
			nameQuery.setMaxResults(1);
			String name = (String) nameQuery.uniqueResult();
			
			attributeColumns.add(new AttributeQueryColumn(name, keyid, atype) {
				
				@Override
				public Object getValue(IResultItem item) {
					if (item instanceof ObservationQueryResultItem){
						return ((ObservationQueryResultItem) item).getAttributeValue(keyid);
					}else if (item instanceof EntityQueryResultItem ){
						return ((EntityQueryResultItem) item).getAttributeValue(keyid);
					}else if (item instanceof PatrolQueryResultItem ){
						return ((PatrolQueryResultItem) item).getAttributeValue(keyid);
					}else if (item instanceof SurveyQueryResultItem ){
						return ((SurveyQueryResultItem) item).getAttributeValue(keyid);
					}
					return null;
				}
				
				@Override
				public QueryColumn clone() {
					return null;
				}
			});
		}
		sortByName(attributeColumns, l);
		keys.addAll(attributeColumns);
		return keys;
	}
	
	public static void sortByName(List<? extends QueryColumn> columns, Locale l){
		Collections.sort(columns, new Comparator<QueryColumn>() {
			@Override
			public int compare(QueryColumn o1, QueryColumn o2) {
				return Collator.getInstance(l).compare(o1.getName(), o2.getName());
			}
		});
	}
	
	/**
	 * Creates a feature definition string from the list of columns.
	 * 
	 * @param columns the columns to include in the query type
	 * @param supportsTime if Time data type is supported in output type or not.
	 * @Param forShape if the output is going to be shapefile.  In this case the column names are
	 * truncated to 10 characters to ensure shape output is written correctly.
	 * 
	 * @return feature type definition string prefixed with "," 
	 */
	public static String createFeatureDefinitionString(List<QueryColumn> columns, boolean supportsTime, boolean forShape){
		StringBuilder sb = new StringBuilder();
		HashSet<String> names = new HashSet<String>();
		for (int i = 0; i < columns.size(); i++){
			if (!columns.get(i).isVisible()) continue;	//skip non visible columns
			sb.append(","); //$NON-NLS-1$
			String name = columns.get(i).getName();
			name = name.replaceAll(" ", "_"); //$NON-NLS-1$ //$NON-NLS-2$
			name = name.replaceAll("[^\\p{L}\\p{Nd}_]", ""); //$NON-NLS-1$ //$NON-NLS-2$
			
			if (forShape && name.length() > 10){
				name = name.substring(0, 10);
			}
			String tempname = name;
			int cnt = 1;
			while(names.contains(tempname)){
				String postfix = "_" + cnt; //$NON-NLS-1$
				tempname = name + postfix;
				if ( forShape && tempname.length() > 10){
					tempname = name.substring(0, 10-postfix.length()) + postfix;
				}
				cnt++;
			}
			//Name is not a valid attribute name
			if (tempname.equalsIgnoreCase("Name")){ //$NON-NLS-1$
				tempname = tempname +"_"; //$NON-NLS-1$
			}
			names.add(tempname);
			sb.append(tempname);
			sb.append(":"); //$NON-NLS-1$
			if (columns.get(i).getType() == ColumnType.TIME && !supportsTime){
				//time is not supported so convert to string
				sb.append(ColumnType.TIME_STR.geotoolsType);
			}else{
				sb.append(columns.get(i).getType().geotoolsType);
			}
		}
		return sb.toString();
	}
}
