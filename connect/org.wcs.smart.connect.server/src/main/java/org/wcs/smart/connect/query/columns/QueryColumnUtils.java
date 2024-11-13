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
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.connect.query.QueryManager;
import org.wcs.smart.i2.query.IQueryColumn;
import org.wcs.smart.observation.model.ObservationOptions;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.common.engine.ObservationQueryResultItem;
import org.wcs.smart.query.model.AttributeQueryColumn;
import org.wcs.smart.query.model.AttributeQueryColumn.GeometryProperty;
import org.wcs.smart.query.model.CategoryQueryColumn;
import org.wcs.smart.query.model.GeometryAttributeQueryColumn;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.query.model.QueryColumn.ColumnType;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;

import jakarta.persistence.Tuple;

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
	 * Gets all data model columns for a given locale
	 * @param session
	 * @param l
	 * @param q
	 * @return
	 * @throws SQLException
	 */
	public static List<QueryColumn> getDataModelColumns(Session session, Locale l, ConservationAreaFilter caFilter) throws SQLException{
		List<QueryColumn> keys = new ArrayList<QueryColumn>();
		
		int ccnt = QueryManager.INSTANCE.getActiveCategoryDepth(session, caFilter);
		for (int i = 0; i < ccnt; i ++){
			keys.add(new CategoryQueryColumn(MessageFormat.format(Messages.getString("QueryColumnUtils.ObservationCategoryColumnName", l), i), i){ //$NON-NLS-1$
				@Override
				public Object getValue(IResultItem item) {
					if (item instanceof ObservationQueryResultItem){
						if (((ObservationQueryResultItem) item).getCategories().length > level){
							return ((ObservationQueryResultItem) item).getCategories()[level];
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
		String query = "SELECT keyId, type, max(regex)  FROM Attribute WHERE conservationArea.uuid in (:cauuids) group by keyId, type HAVING count(*) = :cnt order by keyId asc"; //$NON-NLS-1$
		org.hibernate.query.Query<Tuple> attquery = session.createQuery(query, Tuple.class);
		attquery.setParameterList("cauuids", caFilter.getConservationAreaFilterIds()); //$NON-NLS-1$
		attquery.setParameter("cnt", Long.valueOf(caFilter.getConservationAreaFilterIds().size())); //$NON-NLS-1$

		// this gets the attribute name based on the requested locale name query
		String nameQueryHql = "SELECT c.keyId, a.value, case when upper(a.id.language.code) = :code1 then 1 when upper(a.id.language.code) = :code2 then 2 when a.id.language.default = true then 3 else 4 end as lorder " //$NON-NLS-1$
				+ "FROM Label a, Attribute c where c.conservationArea.uuid in (:cauuids) " //$NON-NLS-1$
				+ "AND a.id.element.uuid = c.uuid ORDER BY c.keyId, lorder, c.conservationArea.uuid ";  //$NON-NLS-1$

		String allLocal = l.toString().toUpperCase();
		String local = l.getLanguage().toUpperCase();

		List<Tuple> labels = session.createQuery(nameQueryHql, Tuple.class)
				.setParameter("code1", allLocal) //$NON-NLS-1$
				.setParameter("code2", local) //$NON-NLS-1$
				.setParameterList("cauuids", caFilter.getConservationAreaFilterIds()) //$NON-NLS-1$
				.list();

		HashMap<String, String> attribute2name = new HashMap<>();
		for (Tuple x : labels) {
			String keyid = (String) x.get(0);
			String name = (String) x.get(1);
			if (!attribute2name.containsKey(keyid)) {
				attribute2name.put(keyid, name);
			}
		}

		List<Tuple> attributes = attquery.list();
		List<AttributeInfo> infos = new ArrayList<>();
		
		for (Tuple t : attributes) {
			String keyid = (String) t.get(0);
			Attribute.AttributeType atype = (Attribute.AttributeType) t.get(1);
			String formatstring = (String)t.get(2);
			String name = attribute2name.get(keyid);
			
			AttributeInfo ai = new AttributeInfo(name, keyid, atype, formatstring);
			infos.add(ai);
		}
		
		infos.sort((a,b)->Collator.getInstance(l).compare(a.name, b.name));
		
		List<QueryColumn> attributeColumns = new ArrayList<QueryColumn>();
		for (AttributeInfo attribute : infos) {
			if (attribute.type.isGeometry()) {
				attributeColumns.add(new GeometryAttributeQueryColumn(attribute.name, attribute.keyid, attribute.type, attribute.formatString));
			}else {
				attributeColumns.add(new AttributeQueryColumn(attribute.name, attribute.keyid, attribute.type, attribute.formatString) {
					@Override
					public Object getValue(IResultItem item) {
						if (item instanceof ObservationQueryResultItem){
							Object x = ((ObservationQueryResultItem) item).getAttributeValue(attribute.keyid);
							if (x != null) {
								if(getType() == QueryColumn.ColumnType.DATE) {
									if (x instanceof String) {
										//convert strings to dates
										return LocalDate.parse((String)x, DateTimeFormatter.ISO_LOCAL_DATE);
									}
								}else if(getType() == QueryColumn.ColumnType.TIME) {
									if (x instanceof String) {
										//convert strings to dates
										return LocalTime.parse((String)x, DateTimeFormatter.ISO_LOCAL_TIME);
									}
								}
							}
							
							return x; 
						}
						return null;
					}
					
					@Override
					public QueryColumn clone() {
						return null;
					}
				});
			}
			
			if (attribute.type.isGeometry()) {
				attributeColumns.add(new AttributeQueryColumn(GeometryProperty.SOURCE.getColumnName(attribute.name, l), attribute.keyid, GeometryProperty.SOURCE, Attribute.AttributeType.TEXT));	
				attributeColumns.add(new AttributeQueryColumn(GeometryProperty.PERIMETER.getColumnName(attribute.name, l), attribute.keyid, GeometryProperty.PERIMETER, Attribute.AttributeType.NUMERIC));
				if (attribute.type == Attribute.AttributeType.POLYGON) {
					attributeColumns.add(new AttributeQueryColumn(GeometryProperty.AREA.getColumnName(attribute.name, l), attribute.keyid, GeometryProperty.AREA, Attribute.AttributeType.NUMERIC));
				}
			}
		}
		keys.addAll(attributeColumns);
		return keys;
	}
	
	public static void sortByName(List<? extends QueryColumn> columns, Locale l){
		columns.sort((a,b)->Collator.getInstance(l).compare(a.getName(),  b.getName()));
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
			}else if (columns.get(i).getType() == ColumnType.DATETIME && !supportsTime) {
				//datetime is not supported so convert to string
				sb.append(ColumnType.TIME_STR.geotoolsType);
			}else if (columns.get(i).getType() == ColumnType.GEOMETRY) {
				//convert to string
				sb.append(ColumnType.TIME_STR.geotoolsType);
			}else{
				sb.append(columns.get(i).getType().geotoolsType);
			}
		}
		return sb.toString();
	}
	
	public static String createFeatureDefinitionStringAdvIntel(List<IQueryColumn> columns, boolean supportsTime, boolean forShape){
		StringBuilder sb = new StringBuilder();
		HashSet<String> names = new HashSet<String>();
		for (int i = 0; i < columns.size(); i++){
			if (!columns.get(i).isVisible()) continue;	//skip non visible columns
			sb.append(","); //$NON-NLS-1$
			String name = columns.get(i).getColumnName();
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
			if (columns.get(i).getDataType() == IQueryColumn.Type.TIME && !supportsTime){
				//time is not supported so convert to string
				sb.append(ColumnType.TIME_STR.geotoolsType);
			}else{
				sb.append(columns.get(i).getDataType().getFeatureType());
			}
		}
		return sb.toString();
	}
	
	static class AttributeInfo{
		String name;
		String keyid;
		Attribute.AttributeType type;
		String formatString;
		
		public AttributeInfo(String name, String keyid, Attribute.AttributeType type, String formatString) {
			this.name = name;
			this.keyid = keyid;
			this.type = type;
			this.formatString = formatString;
		}
	}
}
