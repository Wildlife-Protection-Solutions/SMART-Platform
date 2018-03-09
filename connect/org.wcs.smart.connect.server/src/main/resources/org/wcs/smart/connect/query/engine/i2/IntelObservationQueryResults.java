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
package org.wcs.smart.connect.query.engine.i2;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.UUID;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.hibernate.Session;
import org.hibernate.jdbc.ReturningWork;
import org.hibernate.query.NativeQuery;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.connect.api.QueryApi;
import org.wcs.smart.connect.query.columns.QueryColumnUtils;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.i2.model.IntelObservationAttribute;
import org.wcs.smart.i2.model.IntelRecordSource;
import org.wcs.smart.i2.query.DataModelColumn;
import org.wcs.smart.i2.query.FilterQueryColumn;
import org.wcs.smart.i2.query.FixedQueryColumn;
import org.wcs.smart.i2.query.FixedQueryColumn.Column;
import org.wcs.smart.i2.query.IPagedQueryResultSet;
import org.wcs.smart.i2.query.IQueryColumn;
import org.wcs.smart.i2.query.IResultItem;
import org.wcs.smart.i2.query.PagedResultSetIterator;
import org.wcs.smart.i2.query.engine.IntelObservationResultItem;
import org.wcs.smart.i2.query.observation.filter.IColumnIdentifierProvider;
import org.wcs.smart.i2.query.observation.filter.IQueryFilter;
import org.wcs.smart.query.common.engine.IQueryResult;
import org.wcs.smart.util.UuidUtils;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKBReader;

/**
 * Intelligence observation query results
 * 
 * @author Emily
 *
 */
public class IntelObservationQueryResults  implements IQueryResult, IPagedQueryResultSet {

	//data details
	private String resultsTable = null;
	private int categoryCnt;
	private int rowCount;
	
	//sorting
	private IQueryColumn lastSortColumn = null;
	private IQueryColumn sortColumn = null;
	private IPagedQueryResultSet.SortDirection sortDirection = null;
	
	//filters to column
	private HashMap<IQueryFilter, String> filterToColumn;
	
	//column names to results index
	private HashMap<String, Integer> columnNameToIndex;
	
	private List<IQueryColumn> queryColumns;
	
	public IntelObservationQueryResults(){
	}
	
	public void setResultsTable(String resultsTable){
		this.resultsTable = resultsTable;
	}
	
	public void setCategoryCount(int catCnt){
		this.categoryCnt = catCnt;
	}
	
	public void setColumnNameToIndexMap(HashMap<String, Integer> columnNameToIndex){
		this.columnNameToIndex = columnNameToIndex;
		for(Entry<String,Integer> e : columnNameToIndex.entrySet()) {
			columnNameToIndex.put(e.getKey(), e.getValue() + 1);
		}
	}
	
	public void setFilterToColumnMap(HashMap<IQueryFilter, String> filterToColumn){
		this.filterToColumn = filterToColumn;
	}
	
	public void setQueryColumns(List<IQueryColumn> columns){
		this.queryColumns = columns;
	}
	
	public String getQueryDataTable(){
		return this.resultsTable;
	}
	
	/**
	 * Sets the total number of records in the results
	 * @param rowCount
	 */
	public void setRowCount(int rowCount) {
		this.rowCount = rowCount;
	}
	
	
	public void setSortColumn(IQueryColumn sortColumn, IPagedQueryResultSet.SortDirection sortDirection){
		this.sortColumn = sortColumn;
		this.sortDirection = sortDirection;
	}
	

	public List<IQueryColumn> getQueryColumns(){
		return this.queryColumns;
	}
	
	public String getValueAsString(IntelObservationResultItem item, IQueryColumn qc, Locale l, Session session){
		return qc.getValue(item, l);
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
	public SimpleFeature toFeature(IntelObservationResultItem item, List<IQueryColumn> columns, Session session, SimpleFeatureType ftype)  throws Exception {
		List<Object> data = new ArrayList<Object>();
		data.add(item.getGeometry());
		data.add(item.getLocationId() + "." + UuidUtils.uuidToString(item.getLocationUuid()));   //$NON-NLS-1$
		
		int i = 0;
		for (IQueryColumn qc : columns){
			if (qc.isVisible()){
				Object x = qc.getValue(item);
				if (x instanceof Boolean){
					if ((Boolean)x){
						x = 0;
					}else{
						x = 1;
					}
				}
				if (qc.getDataType() == IQueryColumn.Type.TIME && 
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
	public String getFeatureSchemaDef(List<IQueryColumn> columns, boolean supportsTime){
		StringBuilder sb = new StringBuilder();
		sb.append("the_geom:Geometry:srid=4326,fid:String"); //$NON-NLS-1$
		String colDef = QueryColumnUtils.createFeatureDefinitionStringAdvIntel(columns, supportsTime, true);
		sb.append(colDef);
		return sb.toString();
	}
	
	private UUID asUuid(Object x) {
		if (x == null) return null;
		if (x instanceof UUID) return (UUID) x;
		if (x instanceof byte[]) return UuidUtils.byteToUUID((byte[])x);
		return null;
	}
	
	private Geometry asGeometry(Object x) throws Exception{
		if (x == null) return null;
		if (!(x instanceof byte[])) return null;
		WKBReader reader = new WKBReader();
		return reader.read((byte[])x);
	}
	
	@Override
	public ConnectIntelObservationResultItem asResultItem(Object[] rowData, Session session) {
		
		ConnectIntelObservationResultItem item = new ConnectIntelObservationResultItem();
		
		item.setObservationUuid(asUuid(rowData[columnNameToIndex.get("observation_uuid")])); //$NON-NLS-1$
		item.setLocationUuid(asUuid(rowData[columnNameToIndex.get("location_uuid")])); //$NON-NLS-1$
		item.setRecordUuid(asUuid(rowData[columnNameToIndex.get("record_uuid")])); //$NON-NLS-1$
		item.setRecordStatus((String)rowData[columnNameToIndex.get("record_status")]); //$NON-NLS-1$
		item.setRecordTitle((String)rowData[columnNameToIndex.get("record_title")]); //$NON-NLS-1$
		
		UUID recordSourceUuid = asUuid(rowData[columnNameToIndex.get("source_uuid")]); //$NON-NLS-1$
		if (recordSourceUuid != null) {
			item.setRecordSource(session.get(IntelRecordSource.class, recordSourceUuid));
		}
		
		item.setLocationId((String)rowData[columnNameToIndex.get("loc_id")]); //$NON-NLS-1$
		item.setLocationDate((Timestamp)rowData[columnNameToIndex.get("loc_datetime")]); //$NON-NLS-1$
		item.setLocationComment((String)rowData[columnNameToIndex.get("loc_comment")]); //$NON-NLS-1$
		try{
			item.setGeometry(asGeometry(rowData[ columnNameToIndex.get("loc_geometry")]), null); //$NON-NLS-1$
		}catch (Exception ex){
			ex.printStackTrace();
			item.setGeometry(null, ex);
		}
		item.setCategoryUuid(asUuid(rowData[columnNameToIndex.get("category_uuid")])); //$NON-NLS-1$
		
		List<String> categories = new ArrayList<String>();
		for (int i = 0; i < categoryCnt; i ++){
			Object x = rowData[columnNameToIndex.get("category_" + i)]; //$NON-NLS-1$
			if (x != null) categories.add((String)x);
		}
		item.setCategoryLabels(categories.toArray(new String[categories.size()]));
		
		//add attachments
		if (item.getObservationUuid() != null){
			List<IntelObservationAttribute> attributes = 
					QueryFactory.buildQuery(session, IntelObservationAttribute.class, "id.observation.uuid", item.getObservationUuid()).getResultList(); //$NON-NLS-1$
			for (IntelObservationAttribute a : attributes){
				if (a.getAttribute().getType() == AttributeType.LIST){
					item.addAttribute(a.getAttribute().getKeyId(), a.getAttributeListItem().getName());	
				}else if (a.getAttribute().getType() == AttributeType.TREE){
					item.addAttribute(a.getAttribute().getKeyId(), a.getAttributeTreeNode().getName());
				}else{
					item.addAttribute(a.getAttribute().getKeyId(), a.getAttributeValue());
				}
			}
		}
		
		
		//filter columns
		for (Entry<IQueryFilter, String> filterColumn : filterToColumn.entrySet()){
			boolean value = false;
			if (rowData[columnNameToIndex.get(filterColumn.getValue())] != null){//filter columns only contain true and null values
				value = true;
			};
			item.addFilterValue(filterColumn.getKey(), value);
		}
		
		return item;
	}
	
	@SuppressWarnings("unchecked")
	public String configureSort(Session session){
		if (sortColumn == null || sortDirection == null) return ""; //$NON-NLS-1$
		
		String sql = " order by "; //$NON-NLS-1$
		
		if (sortColumn instanceof FixedQueryColumn){
			if (((FixedQueryColumn) sortColumn).getColumn() == Column.LOC_COMMENT){
				return sql + "lower(loc_comment)" + getSortDirectionSql(); //$NON-NLS-1$
			}else if (((FixedQueryColumn) sortColumn).getColumn() == Column.LOC_ID){
				return sql + "lower(loc_id)" + getSortDirectionSql(); //$NON-NLS-1$
			}else if (((FixedQueryColumn) sortColumn).getColumn() == Column.LOC_DATE){
				return sql + "loc_datetime" + getSortDirectionSql(); //$NON-NLS-1$
			}else if (((FixedQueryColumn) sortColumn).getColumn() == Column.LOC_TIME){
				return sql + "time(loc_datetime)" + getSortDirectionSql(); //$NON-NLS-1$
			}else if (((FixedQueryColumn) sortColumn).getColumn() == Column.RECORD_STATUS){
				return sql + "record_status" + getSortDirectionSql(); //$NON-NLS-1$
			}else if (((FixedQueryColumn) sortColumn).getColumn() == Column.RECORD_TITLE){
				return sql + "lower(record_title)" + getSortDirectionSql(); //$NON-NLS-1$
			}
		}else if (sortColumn instanceof FilterQueryColumn){
			String filterKey = ((FilterQueryColumn)sortColumn).getFilterKey();
			for (Entry<IQueryFilter,String> filter : filterToColumn.entrySet()){
				if (filter.getKey() instanceof IColumnIdentifierProvider){
					if (((IColumnIdentifierProvider)filter.getKey()).getUniqueColumnIdentifier().equals(filterKey)){
						return sql + " " + filter.getValue() + getSortDirectionSql(); //$NON-NLS-1$
					}
				}
			}
		}else if (sortColumn instanceof DataModelColumn){
			DataModelColumn dm =(DataModelColumn)sortColumn;
			if (dm.getLevel() >= 0 && dm.getLevel() < categoryCnt){
				return sql + " category_" + dm.getLevel() + getSortDirectionSql(); //$NON-NLS-1$
			}else{
				if (lastSortColumn != sortColumn){
					session.getTransaction().begin();
					
					String attributeKey = ((DataModelColumn) sortColumn).getAttributeKey();
					switch(((DataModelColumn) sortColumn).getAttributeType()){
					case BOOLEAN:
					case NUMERIC:
						String updateQuery = "UPDATE " + resultsTable + " SET dbl_sort = null"; //$NON-NLS-1$ //$NON-NLS-2$
						session.createNativeQuery(updateQuery).executeUpdate();
						
						updateQuery = "UPDATE " + resultsTable + " SET dbl_sort = (SELECT a.double_value FROM smart.i_observation_attribute a join smart.dm_attribute b on a.attribute_uuid = b.uuid WHERE a.observation_uuid = " + resultsTable + ".observation_uuid and b.keyid ='" + attributeKey + "')"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
						SqlGenerator.logString(updateQuery);
						session.createNativeQuery(updateQuery).executeUpdate();
						break;
					case DATE:
						updateQuery = "UPDATE " + resultsTable + " SET date_sort = null"; //$NON-NLS-1$ //$NON-NLS-2$
						session.createNativeQuery(updateQuery).executeUpdate();
						
						updateQuery = "UPDATE " + resultsTable + " SET date_sort = (SELECT date(a.string_value) FROM smart.i_observation_attribute a join smart.dm_attribute b on a.attribute_uuid = b.uuid WHERE a.observation_uuid = " + resultsTable + ".observation_uuid and b.keyid ='" + attributeKey + "')"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
						SqlGenerator.logString(updateQuery);
						session.createNativeQuery(updateQuery).executeUpdate();
						break;
					case LIST:		
						updateQuery = "UPDATE " + resultsTable + " SET str_sort = null"; //$NON-NLS-1$ //$NON-NLS-2$
						session.createNativeQuery(updateQuery).executeUpdate();
						
						String attribute = "SELECT distinct a.list_element_uuid FROM " + resultsTable + " b join smart.i_observation_attribute a join smart.dm_attribute b on a.attribute_uuid = b.uuid and b.keyid = '" + attributeKey + "' on b.observation_uuid = a.observation_uuid and a.list_element_uuid is not null"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						List<?> listitems = session.createNativeQuery(attribute).list();
						for (Object b : listitems){
							UUID uuid = null;
							if (b instanceof UUID){ 
								uuid = (UUID) b;
							}else if (b instanceof byte[]){
								uuid = UuidUtils.byteToUUID((byte[]) b);
							}
							AttributeListItem item = (AttributeListItem) session.get(AttributeListItem.class, uuid);
							if (item != null){
								updateQuery = "UPDATE " + resultsTable + " SET str_sort = :value WHERE observation_uuid in (select observation_uuid FROM smart.i_observation_attribute a WHERE " + resultsTable + ".observation_uuid = a.observation_uuid and a.list_element_uuid = :listitem)"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
								
								NativeQuery<?> q = session.createNativeQuery(updateQuery);
								q.setParameter("value", item.getName()); //$NON-NLS-1$
								q.setParameter("listitem", item.getUuid()); //$NON-NLS-1$
								q.executeUpdate();
							}
						}
						break;
					case TEXT:
						updateQuery = "UPDATE " + resultsTable + " SET str_sort = null"; //$NON-NLS-1$ //$NON-NLS-2$
						session.createNativeQuery(updateQuery).executeUpdate();
						
						updateQuery = "UPDATE " + resultsTable + " SET str_sort = (SELECT a.string_value FROM smart.i_observation_attribute a join smart.dm_attribute b on a.attribute_uuid = b.uuid WHERE a.observation_uuid = " + resultsTable + ".observation_uuid and b.keyid ='" + attributeKey + "')"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
						SqlGenerator.logString(updateQuery);
						session.createNativeQuery(updateQuery).executeUpdate();
						break;
					case TREE:
						updateQuery = "UPDATE " + resultsTable + " SET str_sort = null"; //$NON-NLS-1$ //$NON-NLS-2$
						session.createNativeQuery(updateQuery).executeUpdate();
						
						attribute = "SELECT distinct a.tree_node_uuid FROM " + resultsTable + " b join smart.i_observation_attribute a join smart.dm_attribute b on a.attribute_uuid = b.uuid and b.keyid = '" + attributeKey + "' on b.observation_uuid = a.observation_uuid and a.tree_node_uuid is not null"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						List<byte[]> treenodes = session.createNativeQuery(attribute).list();
						for (byte[] b : treenodes){
							AttributeTreeNode item = (AttributeTreeNode) session.get(AttributeTreeNode.class, UuidUtils.byteToUUID(b));
							if (item != null){
								updateQuery = "UPDATE " + resultsTable + " SET str_sort = :value WHERE observation_uuid in (select observation_uuid FROM smart.i_observation_attribute a WHERE " + resultsTable + ".observation_uuid = a.observation_uuid and a.tree_node_uuid = :listitem)"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
								
								NativeQuery<?> q = session.createNativeQuery(updateQuery);
								q.setParameter("value", item.getName()); //$NON-NLS-1$
								q.setParameter("listitem", item.getUuid()); //$NON-NLS-1$
								q.executeUpdate();
							}
						}
						break;
					default:
						break;
					}
					session.getTransaction().commit();
					
					lastSortColumn = sortColumn;
				}
				
				switch(((DataModelColumn) sortColumn).getAttributeType()){
				case BOOLEAN:
				case NUMERIC:
					return sql + "dbl_sort" + getSortDirectionSql(); //$NON-NLS-1$
				case DATE:
					return sql + "date_sort" + getSortDirectionSql(); //$NON-NLS-1$
				case LIST:
				case TEXT:
				case TREE:
					return sql + "str_sort" + getSortDirectionSql(); //$NON-NLS-1$
				default:
					break;
				}
			}
		}
		
		return ""; //$NON-NLS-1$
	}
	
	private String getSortDirectionSql(){
		if (sortDirection == SortDirection.UP) return " asc"; //$NON-NLS-1$
		return " desc"; //$NON-NLS-1$
	}

	public void setSorting(String name, QueryApi.Direction dir) {
		this.sortColumn = null;
		this.sortDirection = SortDirection.UP;
		for (IQueryColumn c : getQueryColumns()) {
			if (c.getColumnName().equalsIgnoreCase(name)) {
				this.sortColumn = c;
				break;
			}
		}

		if (dir == QueryApi.Direction.UP) {
			sortDirection = SortDirection.UP;
		}else {
			sortDirection = SortDirection.DOWN;
		}
	}

	@Override
	public void dispose(Session session) throws SQLException {
		String sql = "DROP TABLE " + resultsTable; //$NON-NLS-1$
		resultsTable = null;
		SqlGenerator.logString(sql);
		session.createNativeQuery(sql).executeUpdate();
	}

	@Override
	public boolean isDisposed() {
		return resultsTable == null;
	}
	
	private String getSqlSort() {
		if (sortDirection == SortDirection.DOWN) {
			return "DESC"; //$NON-NLS-1$
		}else {
			return "ASC"; //$NON-NLS-1$
		}
	}
	

	@Override
	public void setSorting(IQueryColumn sortColumn, SortDirection direction) {
		this.sortDirection = direction;
		this.sortColumn = sortColumn;
	}

	@Override
	public List<? extends IResultItem> getData(int offset, int pageSize, Session session) {
		throw new UnsupportedOperationException("use iterator instead"); //$NON-NLS-1$
	}

	@Override
	public Envelope getEnvelope() {
		throw new UnsupportedOperationException("results envelope not supported on connect"); //$NON-NLS-1$;
	}

	@Override
	public PagedResultSetIterator iterator(Session session) {
		IPagedQueryResultSet results = this;
		return new PagedResultSetIterator( results,session ) {
			
			private ResultSet rs = null;
			
			protected void createResultSet() {
				rs = session.doReturningWork(new ReturningWork<ResultSet>() {

					@Override
					public ResultSet execute(Connection c) throws SQLException {
						if (sortColumn != null) {
							return c.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
									ResultSet.CONCUR_READ_ONLY).executeQuery("SELECT * FROM " + getQueryDataTable() + " ORDER BY sortkeydbl " + getSqlSort() + ", sortkeytxt " + getSqlSort()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						}else {
							return c.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
									ResultSet.CONCUR_READ_ONLY).executeQuery("SELECT * FROM " + getQueryDataTable()); //$NON-NLS-1$
						}
						
					}});					
			}
			
			public boolean hasNext(){
				if (results == null) return false;
				if (rs == null) createResultSet();
				try {
					return rs.next();
				} catch (SQLException e) {
					throw new IllegalStateException(e);
				}
			}
			
			public IResultItem next(){
				try {
					Object[] data = new Object[rs.getMetaData().getColumnCount()+1];
					for (int i = 1; i < data.length; i ++) {
						data[i] = rs.getObject(i);
					}
					return results.asResultItem(data, session);
				}catch (SQLException e) {
					throw new IllegalStateException(e);
				}
			}
			
			public void close() {
				try {
					rs.close();	
				}catch(SQLException e) {
					throw new IllegalStateException(e);
				}
				
			}
		};
	}

	@Override
	public int getItemCount() {
		return this.rowCount;
	}

}
