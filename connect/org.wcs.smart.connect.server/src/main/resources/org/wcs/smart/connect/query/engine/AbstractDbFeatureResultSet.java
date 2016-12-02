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
package org.wcs.smart.connect.query.engine;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.hibernate.Session;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.connect.api.QueryApi;
import org.wcs.smart.connect.api.QueryApi.Direction;
import org.wcs.smart.connect.query.QueryManager;
import org.wcs.smart.connect.query.columns.QueryColumnUtils;
import org.wcs.smart.query.common.engine.IQueryResultSetIterator;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.common.engine.ITablePagedQueryResultSet;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

/**
 * Converts query results to a feature data source for writing
 * query results to shapefile. 
 * 
 * @author Emily
 *
 */
public abstract class AbstractDbFeatureResultSet implements ITablePagedQueryResultSet {
	
	public static final String POINT_GEOM_TYPE = "Point"; //$NON-NLS-1$
	
	public static final String MULTI_POINT_GEOM_TYPE = "MultiPoint"; //$NON-NLS-1$
	
	public static final String LINESTRING_GEOM_TYPE = "LineString"; //$NON-NLS-1$
	
	public static final String MULTI_LINESTRING_GEOM_TYPE = "MultiLineString"; //$NON-NLS-1$
		
	protected GeometryFactory gf = new GeometryFactory();
	protected boolean isDisposed = false;
	protected int itemCount;
	public String sortColumn = null;
	public QueryApi.Direction direction = Direction.UP;
	
	public boolean hasSortColumns = false;
	
	/**
	 * Creates the geometry for the given row in the results set.
	 * @param rs
	 * @return
	 * @throws Exception
	 */
	public abstract Geometry createGeometry(IResultItem item) throws Exception;
	
	/**
	 * Creates a feature id for the given row in the result set
	 * @param rs
	 * @return
	 * @throws Exception
	 */
	public abstract String createId(IResultItem item) throws Exception; 
	
	/**
	 * 
	 * @return the geometry type for the feature source
	 */
	public abstract String getGeometryType();

	public String getValueAsString(IResultItem item, QueryColumn qc, Session session){
		return qc.getValueAsString(getValue(item, qc, session));
	}
	
	public Object getValue(IResultItem item, QueryColumn column, Session session){
		return column.getValue(item);
	}
	
	/**
	 * Converts a results set record to a feature
	 * @param rs
	 * @param columns
	 * @param c
	 * @param ftype
	 * @return
	 * @throws Exception
	 */
	public SimpleFeature toFeature(IResultItem item, List<QueryColumn> columns, Session session, SimpleFeatureType ftype)  throws Exception {
		List<Object> data = new ArrayList<Object>();
		data.add(createGeometry(item));
		data.add(createId(item));  
		
		int i = 0;
		for (QueryColumn qc : columns){
			if (qc.isVisible()){
				Object x = getValue(item, qc, session);
				if (x instanceof Boolean){
					if ((Boolean)x){
						x = 0;
					}else{
						x = 1;
					}
				}
				if (qc.getType() == QueryColumn.ColumnType.TIME && 
						ftype.getDescriptor(i++) .getType().getBinding().equals(String.class)
						){
					x = DateFormat.getTimeInstance().format((Date)x);
				}
				data.add(x);
			}
		}
		return SimpleFeatureBuilder.build(ftype, data, (String)data.get(1));
	}
	
	/**
	 * Creates a feature schema from the set of query columns
	 * @param columns
	 * @param supportsTime
	 * @return
	 */
	public String getFeatureSchemaDef(List<QueryColumn> columns, boolean supportsTime){
		StringBuilder sb = new StringBuilder();
		sb.append("the_geom:" + getGeometryType() + ":srid=4326,fid:String"); //$NON-NLS-1$ //$NON-NLS-2$
		String colDef = QueryColumnUtils.createFeatureDefinitionString(columns, supportsTime, true);
		sb.append(colDef);
		return sb.toString();
	}
	

	@Override
	public Envelope getEnvelope() {
		throw new UnsupportedOperationException("Get envelope not supported for result set."); //$NON-NLS-1$
	}

	@Override
	public List<? extends IResultItem> getData(int offset, int pagesize) {
		throw new UnsupportedOperationException("Can not load all results into memory in this environment"); //$NON-NLS-1$
	}
	

	public abstract void updateSortColumn(Session session) throws SQLException;
	
	public abstract List<IResultItem> getResults(Session session, ResultSet rs, int from, int pageSize) throws SQLException;
	

	public int getItemCount() {
		return itemCount;
	}

	public void setItemCount(int itemCount) {
		this.itemCount = itemCount;
	}

	@Override
	public IQueryResultSetIterator<? extends IResultItem> iterator(int pagesize) {
		throw new UnsupportedOperationException("Can not create a result set without a session in this environment"); //$NON-NLS-1$
	}

	@Override
	public IQueryResultSetIterator<? extends IResultItem> iterator(int pagesize,
			Session session) {
		return new QueryResultSetIterator<IResultItem>(this, pagesize, session);
	}

	public void setSorting(final String sortColumn, QueryApi.Direction direction) {
		this.sortColumn = sortColumn;
		this.direction = direction;
	}

	@Override
	public void setSorting(QueryColumn arg0, int arg1) {
		//NOT SUPPORTED
		//Sorting only supported with String Argument, see above function.
	}
	
	@Override
	public void dispose(Session session) throws SQLException{
		this.isDisposed = true;
	}
	
	@Override
	public boolean isDisposed(){
		return this.isDisposed;
	}
		
	public void updateSortColumnGeneral(Session session, String queryDataTable, ConservationAreaFilter caFilter, String value, String typePrefix, String tableListSuffix, String tableTreeSuffix, String uuidColumn) throws SQLException {
		updateSortColumnGeneral(session, queryDataTable, caFilter, value, typePrefix, tableListSuffix, tableTreeSuffix, uuidColumn, true);
	}
	
	public void updateSortColumnGeneral(Session session, String queryDataTable, ConservationAreaFilter caFilter, String value, String typePrefix, 
			String tableListSuffix, String tableTreeSuffix, String uuidColumn, boolean hasExtraTables) throws SQLException {
		if(!hasExtraTables){
			//I don't know how to sort these query types, they don't use temp tables so we can't use the same method as the rest.
			throw new UnsupportedOperationException("Sorting not suppported for this Query Type"); //$NON-NLS-1$
		}
		if (!hasSortColumns) {
			// add the sort columns
			session.createSQLQuery("ALTER TABLE " + queryDataTable + " add column sortKeyDbl float").executeUpdate(); //$NON-NLS-1$ //$NON-NLS-2$
			session.createSQLQuery("ALTER TABLE " + queryDataTable + " add column sortKeyTxt varchar(1024)").executeUpdate(); //$NON-NLS-1$ //$NON-NLS-2$
			hasSortColumns = true;
		}

		String key = sortColumn; //This is a bit horrible since users have to send in the attribute key of the column they want to sort. But I don't have a better solution at this point.

		//TODO: this will not work for CCAA
		Attribute.AttributeType type = QueryManager.INSTANCE.getAttributeType(session, key, caFilter); // session will not be closed on purpose

		
		if(type == null){
			//update to the sort column
			StringBuilder sql = new StringBuilder();
			sql.append("UPDATE "); //$NON-NLS-1$
			sql.append(queryDataTable);
			sql.append(" SET sortKeyTxt = " + sortColumn); //$NON-NLS-1$
			session.createSQLQuery(sql.toString()).executeUpdate();
			
		}else{
			
			switch (type) {
			case BOOLEAN:
			case NUMERIC:
				StringBuilder sql = new StringBuilder();
				sql.append("UPDATE "); //$NON-NLS-1$
				sql.append(queryDataTable);
				sql.append(" SET sortKeyDbl = "); //$NON-NLS-1$
				sql.append("(SELECT wpoa.NUMBER_VALUE FROM "); //$NON-NLS-1$
				sql.append("smart.WP_OBSERVATION_ATTRIBUTES wpoa join smart.DM_ATTRIBUTE a on a.uuid = wpoa.attribute_uuid "); //$NON-NLS-1$
				sql.append("and a.keyid = '"); //$NON-NLS-1$
				sql.append(key);
				sql.append("'"); //$NON-NLS-1$
				sql.append(" WHERE wpoa.observation_uuid = "); //$NON-NLS-1$
				sql.append(queryDataTable);
				sql.append(typePrefix + "uuid)"); //$NON-NLS-1$
				session.createSQLQuery(sql.toString()).executeUpdate();
				break;
			case TEXT:
			case DATE:
				sql = new StringBuilder();
				sql.append("UPDATE "); //$NON-NLS-1$
				sql.append(queryDataTable);
				sql.append(" SET sortKeyTxt = "); //$NON-NLS-1$
				sql.append("(SELECT wpoa.STRING_VALUE FROM "); //$NON-NLS-1$
				sql.append("smart.WP_OBSERVATION_ATTRIBUTES wpoa join smart.DM_ATTRIBUTE a on a.uuid = wpoa.attribute_uuid "); //$NON-NLS-1$
				sql.append("and a.keyid = '"); //$NON-NLS-1$
				sql.append(key);
				sql.append("'"); //$NON-NLS-1$
				sql.append(" WHERE wpoa.observation_uuid = "); //$NON-NLS-1$
				sql.append(queryDataTable);
				sql.append(typePrefix + "uuid)"); //$NON-NLS-1$
				session.createSQLQuery(sql.toString()).executeUpdate();
				break;
			case LIST:
				sql = new StringBuilder();
				sql.append("UPDATE "); //$NON-NLS-1$
				sql.append(queryDataTable);
				sql.append(" SET sortKeyTxt = "); //$NON-NLS-1$
				sql.append("(SELECT rl." + value + " FROM "); //$NON-NLS-1$
				sql.append("smart.WP_OBSERVATION_ATTRIBUTES wpoa join "); //$NON-NLS-1$
				sql.append(queryDataTable);
				sql.append(tableListSuffix + " rl on rl." + uuidColumn + " = wpoa.list_element_uuid "); //$NON-NLS-1$
				sql.append("join smart.DM_ATTRIBUTE a on a.uuid = wpoa.attribute_uuid and a.keyid = '"); //$NON-NLS-1$
				sql.append(key);
				sql.append("'"); //$NON-NLS-1$
				sql.append(" WHERE wpoa.observation_uuid = "); //$NON-NLS-1$
				sql.append(queryDataTable);
				sql.append(typePrefix + "uuid)"); //$NON-NLS-1$
				session.createSQLQuery(sql.toString()).executeUpdate();
				break;
			case TREE:
				sql = new StringBuilder();
				sql.append("UPDATE ");//$NON-NLS-1$
				sql.append(queryDataTable);
				sql.append(" SET sortKeyTxt = ");//$NON-NLS-1$
				sql.append("(SELECT rl." + value + " FROM smart.WP_OBSERVATION_ATTRIBUTES wpoa join "); //$NON-NLS-1$
				sql.append(queryDataTable);
				sql.append(tableTreeSuffix + " rl on rl." + uuidColumn + " = wpoa.tree_node_uuid "); //$NON-NLS-1$
				sql.append("join smart.DM_ATTRIBUTE a on a.uuid = wpoa.attribute_uuid and a.keyid = '"); //$NON-NLS-1$
				sql.append(key);
				sql.append("'"); //$NON-NLS-1$
				sql.append(" WHERE wpoa.observation_uuid = "); //$NON-NLS-1$
				sql.append(queryDataTable);
				sql.append(typePrefix + "uuid)"); //$NON-NLS-1$
				session.createSQLQuery(sql.toString()).executeUpdate();
				break;
			}
		}
		
	}
	
	
}