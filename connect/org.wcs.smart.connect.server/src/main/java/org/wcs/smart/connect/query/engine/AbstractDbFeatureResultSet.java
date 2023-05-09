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

import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.hibernate.Session;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.IProjectionProvider;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.common.attachment.ISmartAttachment;
import org.wcs.smart.connect.api.QueryApi;
import org.wcs.smart.connect.api.QueryApi.Direction;
import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.connect.query.QueryManager;
import org.wcs.smart.connect.query.columns.QueryColumnUtils;
import org.wcs.smart.observation.model.ObservationAttachment;
import org.wcs.smart.observation.model.WaypointAttachment;
import org.wcs.smart.query.common.engine.IAttachmentResultItem;
import org.wcs.smart.query.common.engine.IQueryResultSetIterator;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.common.engine.ITablePagedQueryResultSet;
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;

/**
 * Converts query results to a feature data source for writing
 * query results to shapefile. 
 * 
 * @author Emily
 *
 */
public abstract class AbstractDbFeatureResultSet<T extends IResultItem> implements ITablePagedQueryResultSet<T> {
	
	public static final int POSTGRESQL_FETCH_SIZE = 1000;
	
	public static final String POINT_GEOM_TYPE = "Point"; //$NON-NLS-1$
	
	public static final String MULTI_POINT_GEOM_TYPE = "MultiPoint"; //$NON-NLS-1$
	
	public static final String LINESTRING_GEOM_TYPE = "LineString"; //$NON-NLS-1$
	
	public static final String MULTI_LINESTRING_GEOM_TYPE = "MultiLineString"; //$NON-NLS-1$
		
	public static final String OBS_UUID_COL_KEY = "obsuuid"; //$NON-NLS-1$
	public static final String WP_UUID_COL_KEY = "wpuuid"; //$NON-NLS-1$
	public static final String CA_UUID_COL_KEY = "cauuid"; //$NON-NLS-1$
	public static final String MISSION_UUID_COL_KEY = "missionuuid"; //$NON-NLS-1$
	public static final String MISSIONTRACK_UUID_COL_KEY = "missiontrackuuid"; //$NON-NLS-1$

	
	protected GeometryFactory gf = new GeometryFactory();
	protected boolean isDisposed = false;
	protected int itemCount;
	public String sortColumn = null;
	public QueryApi.Direction direction = Direction.UP;
	
	public boolean hasSortColumns = false;
	
	protected int lastfrom = 0;
	
	protected int getTo(int from, int pageSize) throws SQLException {
		if (from != lastfrom) throw new SQLException("Do not support going backwards or skipping results");
		int to = from + pageSize;
		if (to >= itemCount) {
			to = itemCount;
		}
		lastfrom = to;
		return to;
	}
	
	/**
	 * Creates the geometry for the given row in the results set.
	 * @param rs
	 * @return
	 * @throws Exception
	 */
	public abstract Geometry createGeometry(T item) throws Exception;
	
	/**
	 * Waypoint UUID  column name
	 * 
	 * @param l
	 * @return
	 */
	public String getWaypointColumnName(Locale l) {
		return  Messages.getString("AbstractDbFeatureResultSet.WpColumnName", l); //$NON-NLS-1$
	}
	
	/**
	 * Conservation Area UUID column name
	 * 
	 * @param l
	 * @return
	 */
	public String getConservationAreaColumnName(Locale l) {
		return  Messages.getString("AbstractDbFeatureResultSet.CAColumnName", l); //$NON-NLS-1$
	}
	
	/**
	 * Observation UUID column name
	 * 
	 * @param l
	 * @return
	 */
	public String getObservationColumnName(Locale l) {
		return  Messages.getString("AbstractDbFeatureResultSet.ObsUuidColumnName", l); //$NON-NLS-1$
	}
	
	/**
	 * Load the query columns for a simple query 
	 * 
	 * @param query
	 * @param l
	 * @param session
	 * @param prj
	 * @return
	 */
	public List<QueryColumn> getQueryColumns(SimpleQuery query, Locale l, Session session, IProjectionProvider prj){
		List<QueryColumn> cols = query.computeQueryColumns(l, session, prj);
		return cols;
	}
	
	/**
	 * Creates a feature id for the given row in the result set
	 * @param rs
	 * @return
	 * @throws Exception
	 */
	public abstract String createId(T item) throws Exception; 
	
	/**
	 * 
	 * @return the geometry type for the feature source
	 */
	public abstract String getGeometryType();

	public String getValueAsString(T item, QueryColumn qc, Session session){
		return getValueAsString(item, qc, session, true);
	}
	
	/**
	 * Gets the value for the query column from the item.
	 * 
	 * @param item
	 * @param qc
	 * @param session
	 * @param formatted if true the formatted value is returned, if false the raw value is returned
	 * @return
	 */
	public String getValueAsString(T item, QueryColumn qc, Session session, boolean formatted){
		return qc.getValueAsString(getValue(item, qc, session), formatted);
	}
	
	public Object getValue(T item, QueryColumn column, Session session){
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
	public SimpleFeature toFeature(T item, List<QueryColumn> columns, Session session, SimpleFeatureType ftype)  throws Exception {
		List<Object> data = new ArrayList<Object>();
		data.add(createGeometry(item));
		data.add(createId(item));  
		
		int i = 2;
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
				Class<?> bindingType = ftype.getDescriptor(i).getType().getBinding();
				i++;
				if (qc.getType() == QueryColumn.ColumnType.TIME &&  bindingType.equals(String.class)){
					x = DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM).format((Temporal)x);
				}else if (qc.getType() == QueryColumn.ColumnType.DATETIME && bindingType.equals(String.class)){
					//this is a datetime object which needs to be converted to a string
					x = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).format((Temporal)x);
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
	public String getFeatureSchemaDef(List<QueryColumn> columns, boolean supportsTime, boolean forShape){
		StringBuilder sb = new StringBuilder();
		sb.append("the_geom:" + getGeometryType() + ":srid=4326,fid:String"); //$NON-NLS-1$ //$NON-NLS-2$
		String colDef = QueryColumnUtils.createFeatureDefinitionString(columns, supportsTime, forShape);
		sb.append(colDef);
		return sb.toString();
	}
	

	@Override
	public Envelope getEnvelope() {
		throw new UnsupportedOperationException("Get envelope not supported for result set."); //$NON-NLS-1$
	}

	@Override
	public List<T> getData(int offset, int pagesize) {
		throw new UnsupportedOperationException("Can not load all results into memory in this environment"); //$NON-NLS-1$
	}
	

	public abstract void updateSortColumn(Session session) throws SQLException;
	
	public abstract List<T> getResults(Session session, ResultSet rs, int from, int pageSize) throws SQLException;
	

	public int getItemCount() {
		return itemCount;
	}

	public void setItemCount(int itemCount) {
		this.itemCount = itemCount;
	}

	@Override
	public IQueryResultSetIterator<T> iterator(int pagesize) {
		throw new UnsupportedOperationException("Can not create a result set without a session in this environment"); //$NON-NLS-1$
	}

	@Override
	public IQueryResultSetIterator<T> iterator(int pagesize,
			Session session) {
		return new QueryResultSetIterator<T>(this, pagesize, session);
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
	
	public void updateSortColumnGeneral(Session session, String queryDataTable, String labelTable, 
			ConservationAreaFilter caFilter, String typePrefix) throws SQLException {
		
//		if(!hasExtraTables){
//			//I don't know how to sort these query types, they don't use temp tables so we can't use the same method as the rest.
//			throw new UnsupportedOperationException("Sorting not suppported for this Query Type"); //$NON-NLS-1$
//		}
		if (!hasSortColumns) {
			// add the sort columns
			session.createNativeQuery("ALTER TABLE " + queryDataTable + " add column sortKeyDbl float").executeUpdate(); //$NON-NLS-1$ //$NON-NLS-2$
			session.createNativeQuery("ALTER TABLE " + queryDataTable + " add column sortKeyTxt varchar(" + Attribute.STRING_ATTRIBUTE_MAX_LENGTH + ")").executeUpdate(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
			session.createNativeQuery(sql.toString()).executeUpdate();
			
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
				session.createNativeQuery(sql.toString()).executeUpdate();
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
				session.createNativeQuery(sql.toString()).executeUpdate();
				break;
			case LIST:
				if (labelTable == null) throw new UnsupportedOperationException(MessageFormat.format("Cannot sort by column with key {0} for this query type.", key)); //$NON-NLS-1$
				sql = new StringBuilder();
				sql.append("UPDATE "); //$NON-NLS-1$
				sql.append(queryDataTable);
				sql.append(" SET sortKeyTxt = "); //$NON-NLS-1$
				sql.append("(SELECT rl.value FROM "); //$NON-NLS-1$ 
				sql.append("smart.WP_OBSERVATION_ATTRIBUTES wpoa join "); //$NON-NLS-1$
				sql.append(labelTable);
				sql.append(" rl on rl.uuid = wpoa.list_element_uuid "); //$NON-NLS-1$ 
				sql.append("join smart.DM_ATTRIBUTE a on a.uuid = wpoa.attribute_uuid and a.keyid = '"); //$NON-NLS-1$
				sql.append(key);
				sql.append("'"); //$NON-NLS-1$
				sql.append(" WHERE wpoa.observation_uuid = "); //$NON-NLS-1$
				sql.append(queryDataTable);
				sql.append(typePrefix + "uuid)"); //$NON-NLS-1$
				session.createNativeQuery(sql.toString()).executeUpdate();
				break;
			case TREE:
				if (labelTable == null) throw new UnsupportedOperationException(MessageFormat.format("Cannot sort by column with key {0} for this query type.", key)); //$NON-NLS-1$
				sql = new StringBuilder();
				sql.append("UPDATE ");//$NON-NLS-1$
				sql.append(queryDataTable);
				sql.append(" SET sortKeyTxt = ");//$NON-NLS-1$
				sql.append("(SELECT rl.value FROM smart.WP_OBSERVATION_ATTRIBUTES wpoa join "); //$NON-NLS-1$ 
				sql.append(labelTable + " rl on rl.uuid = wpoa.tree_node_uuid "); //$NON-NLS-1$ 
				sql.append("join smart.DM_ATTRIBUTE a on a.uuid = wpoa.attribute_uuid and a.keyid = '"); //$NON-NLS-1$
				sql.append(key);
				sql.append("'"); //$NON-NLS-1$
				sql.append(" WHERE wpoa.observation_uuid = "); //$NON-NLS-1$
				sql.append(queryDataTable);
				sql.append(typePrefix + "uuid)"); //$NON-NLS-1$
				session.createNativeQuery(sql.toString()).executeUpdate();
				break;
			case MLIST:
				throw new UnsupportedOperationException("Sorting by MULIT LIST attributes is not supported."); //$NON-NLS-1$
			}
		}
		
	}
	
	protected void setAttachmentField(Session session, ResultSet rs, IAttachmentResultItem item) throws SQLException {
		UUID auuid = (UUID) rs.getObject("attach_uuid"); //$NON-NLS-1$
		ISmartAttachment a = session.get(ObservationAttachment.class, auuid);
		if (a == null) {
			a = session.get(WaypointAttachment.class, auuid);
		}
		try {
			a.computeFileLocation(session);
		} catch (Exception e) {
			//TODO: log this
		}
		item.setAttachment(a);
	}
	
	protected String getImageQueryWaypoint(String datatable, String imagetable) {
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT r.*, b.attach_uuid as attach_uuid FROM " ); //$NON-NLS-1$
		sb.append(datatable + " r "); //$NON-NLS-1$
		sb.append(" join " + imagetable + " b "); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append("on r.wp_uuid = b.wp_uuid "); //$NON-NLS-1$
		
		return sb.toString();
	}
	
	protected String getImageQueryObservation(String datatable, String imagetable, String selectpartwp, String selectpartobs) {
		StringBuilder sb = new StringBuilder();
		
		sb.append("SELECT " + selectpartwp + ", b.attach_uuid as attach_uuid FROM " ); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append(datatable + " r "); //$NON-NLS-1$
		sb.append(" join " + imagetable + " b "); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append("on ( b.ob_uuid is not null and r.ob_uuid = b.ob_uuid ) "); //$NON-NLS-1$
		
		sb.append(" UNION "); //$NON-NLS-1$

		sb.append("SELECT foo.*, b.attach_uuid as attach_uuid FROM "); //$NON-NLS-1$
		sb.append("(SELECT DISTINCT " + selectpartobs + " FROM " + datatable + " r ) foo"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		sb.append(" join " + imagetable + " b "); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append(" ON b.wp_uuid = foo.wp_uuid and b.ob_uuid is null"); //$NON-NLS-1$
		
		return sb.toString();
	}
	
	protected int createImageDataObservation(Session session, String dataTable, String imageTempTable) {
		
		StringBuilder sb = new StringBuilder();
		sb.append("CREATE TABLE "); //$NON-NLS-1$
		sb.append(imageTempTable);
		sb.append("(attach_uuid uuid, "); //$NON-NLS-1$
		sb.append("wp_uuid uuid, "); //$NON-NLS-1$
		sb.append("ob_uuid uuid)"); //$NON-NLS-1$
		session.createNativeQuery(sb.toString()).executeUpdate();
		
		sb = new StringBuilder();
		sb.append(" INSERT INTO "); //$NON-NLS-1$
		sb.append(imageTempTable + "(attach_uuid, wp_uuid, ob_uuid) "); //$NON-NLS-1$
		sb.append(" SELECT z.attach_uuid, z.wp_uuid, z.ob_uuid FROM ( "); //$NON-NLS-1$
		sb.append("SELECT c.uuid as attach_uuid, a.wp_time, a.wp_id, a.wp_uuid as wp_uuid, a.ob_uuid as ob_uuid" ); //$NON-NLS-1$
		sb.append(" FROM "); //$NON-NLS-1$
		sb.append( dataTable + " a "); //$NON-NLS-1$
		sb.append(" JOIN "); //$NON-NLS-1$
		sb.append(" smart.observation_attachment c on a.ob_uuid = c.obs_uuid "); //$NON-NLS-1$
		sb.append( " UNION "); //$NON-NLS-1$
		sb.append("SELECT c.uuid as attach_uuid, a.wp_time, a.wp_id, a.wp_uuid as wp_uuid, cast(null as uuid) as ob_uuid" ); //$NON-NLS-1$
		sb.append(" FROM "); //$NON-NLS-1$
		sb.append( dataTable + " a "); //$NON-NLS-1$
		sb.append(" JOIN "); //$NON-NLS-1$
		sb.append(" smart.wp_attachments c on c.wp_uuid = a.wp_uuid "); //$NON-NLS-1$
		sb.append(" ) z ORDER BY z.wp_time desc, z.wp_id "); //$NON-NLS-1$

		session.createNativeQuery(sb.toString()).executeUpdate();
		
		sb = new StringBuilder();
		sb.append("SELECT count(*) FROM "); //$NON-NLS-1$
		sb.append(imageTempTable);
		
		int imageDataCnt = ((BigInteger) session.createNativeQuery(sb.toString()).uniqueResult()).intValue();
		return imageDataCnt;
		
	}
	
	protected int createImageDataWaypoint(Session session, String dataTable, String imageTempTable) {
		
		StringBuilder sb = new StringBuilder();
		sb.append("CREATE TABLE "); //$NON-NLS-1$
		sb.append(imageTempTable);
		sb.append("(attach_uuid uuid, wp_uuid uuid)"); //$NON-NLS-1$
		session.createNativeQuery(sb.toString()).executeUpdate();
		
		sb = new StringBuilder();
		sb.append(" INSERT INTO "); //$NON-NLS-1$
		sb.append(imageTempTable + " (attach_uuid, wp_uuid) "); //$NON-NLS-1$
		sb.append(" SELECT z.uuid, z.wp_uuid "); //$NON-NLS-1$
		sb.append("FROM "); //$NON-NLS-1$
		sb.append(" (SELECT distinct e.uuid, a.wp_time, a.wp_id, a.wp_uuid FROM "); //$NON-NLS-1$
		sb.append(dataTable);
		sb.append(" a join "); //$NON-NLS-1$
		sb.append("(SELECT uuid, wp_uuid as wp_uuid FROM smart.wp_attachments "); //$NON-NLS-1$
		sb.append(" UNION "); //$NON-NLS-1$
		sb.append("SELECT b.uuid, g.wp_uuid as wp_uuid FROM "); //$NON-NLS-1$
		sb.append(" smart.wp_observation_group g join smart.wp_observation c on c.wp_group_uuid = g.uuid "); //$NON-NLS-1$
		sb.append(" join smart.observation_attachment b on c.uuid = b.obs_uuid) e "); //$NON-NLS-1$
		sb.append("on a.wp_uuid = e.wp_uuid"); //$NON-NLS-1$
		sb.append(" ORDER BY a.wp_time desc, a.wp_id ) z "); //$NON-NLS-1$

		session.createNativeQuery(sb.toString()).executeUpdate();
		
		sb = new StringBuilder();
		sb.append("SELECT count(*) FROM "); //$NON-NLS-1$
		sb.append(imageTempTable);
		
		int imageDataCnt = ((BigInteger) session.createNativeQuery(sb.toString()).uniqueResult()).intValue();		
		return imageDataCnt;
		
	}

}