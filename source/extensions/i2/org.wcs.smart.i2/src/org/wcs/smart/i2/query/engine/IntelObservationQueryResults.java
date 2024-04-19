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
package org.wcs.smart.i2.query.engine;

import java.sql.Blob;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.stream.Collectors;

import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.query.NativeQuery;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKBReader;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.model.IntelObservation;
import org.wcs.smart.i2.model.IntelObservationAttribute;
import org.wcs.smart.i2.model.IntelProfile;
import org.wcs.smart.i2.model.IntelRecordSource;
import org.wcs.smart.i2.query.DataModelColumn;
import org.wcs.smart.i2.query.DataModelColumn.GeometryProperty;
import org.wcs.smart.i2.query.FilterQueryColumn;
import org.wcs.smart.i2.query.FixedQueryColumn;
import org.wcs.smart.i2.query.FixedQueryColumn.Column;
import org.wcs.smart.i2.query.IPagedQueryResultSet;
import org.wcs.smart.i2.query.IQueryColumn;
import org.wcs.smart.i2.query.IResultItem;
import org.wcs.smart.i2.query.PagedResultSetIterator;
import org.wcs.smart.i2.query.observation.filter.IColumnIdentifierProvider;
import org.wcs.smart.i2.query.observation.filter.IQueryFilter;
import org.wcs.smart.util.UuidUtils;

import jakarta.persistence.Tuple;

/**
 * Intelligence observation query results
 * 
 * @author Emily
 *
 */
public class IntelObservationQueryResults implements IPagedQueryResultSet {

	//data details
	private String resultsTable = null;
	private int obsCount;
	private int wpCount;
	private int categoryCnt;
	
	//sorting
	private IQueryColumn lastSortColumn = null;
	private IQueryColumn sortColumn = null;
	private IPagedQueryResultSet.SortDirection sortDirection = null;
	
	//filters to column
	private HashMap<IQueryFilter, String> filterToColumn;
	
	//column names to results index
	private HashMap<String, Integer> columnNameToIndex;
	
	private List<IQueryColumn> queryColumns;
	private Envelope bounds;
	
	public IntelObservationQueryResults(){
	}
	
	public void setResultsTable(String resultsTable){
		this.resultsTable = resultsTable;
	}
	
	public void setResultCount(int obsCount, int wpCount){
		this.obsCount = obsCount;
		this.wpCount = wpCount;
	}
	
	public void setCategoryCount(int catCnt){
		this.categoryCnt = catCnt;
	}
	
	public void setBounds(Envelope env){
		this.bounds = env;
	}
	
	public void setColumnNameToIndexMap(HashMap<String, Integer> columnNameToIndex){
		this.columnNameToIndex = columnNameToIndex;
	}
	
	public void setFilterToColumnMap(HashMap<IQueryFilter, String> filterToColumn){
		this.filterToColumn = filterToColumn;
	}
	
	public void setQueryColumns(List<IQueryColumn> columns){
		this.queryColumns = columns;
	}
	
	@Override
	public String getQueryDataTable(){
		return this.resultsTable;
	}
	
	
	public void setSortColumn(IQueryColumn sortColumn, IPagedQueryResultSet.SortDirection sortDirection){
		this.sortColumn = sortColumn;
		this.sortDirection = sortDirection;
	}
	
	@Override
	public List<IQueryColumn> getQueryColumns(){
		return this.queryColumns;
	}
	
	
	private UUID asUuid(Object x){
		if (x == null) return null;
		if (x instanceof UUID) return (UUID) x;
		if (x instanceof byte[]) return UuidUtils.byteToUUID((byte[])x);
		return null;
	}
	
	private Geometry asGeometry(Object x) throws Exception{
		if (x == null) return null;
		if (!(x instanceof Blob)) return null;
		Blob b = (Blob)x;
		WKBReader reader = new WKBReader();
		return reader.read(b.getBytes(1l, (int) b.length()));
	}
	
	@Override
	public IResultItem asResultItem(Object[] rowData, Session session){
		
		IntelObservationResultItem item = new IntelObservationResultItem();
		
		item.setConservationAreaId((String)rowData[columnNameToIndex.get("ca_id")]); //$NON-NLS-1$
		item.setConservationAreaName((String)rowData[columnNameToIndex.get("ca_name")]); //$NON-NLS-1$
		item.setConservationAreaUuid(  UuidUtils.byteToUUID((byte[])rowData[columnNameToIndex.get("ca_uuid")]) ); //$NON-NLS-1$
		
		item.setObservationUuid(asUuid(rowData[columnNameToIndex.get("observation_uuid")])); //$NON-NLS-1$
		item.setLocationUuid(asUuid(rowData[columnNameToIndex.get("location_uuid")])); //$NON-NLS-1$
		item.setRecordUuid(asUuid(rowData[columnNameToIndex.get("record_uuid")])); //$NON-NLS-1$
		item.setRecordStatus((String)rowData[columnNameToIndex.get("record_status")]); //$NON-NLS-1$
		item.setRecordTitle((String)rowData[columnNameToIndex.get("record_title")]); //$NON-NLS-1$
		
		UUID sourceUuid = asUuid(rowData[columnNameToIndex.get("record_source_uuid")]); //$NON-NLS-1$
		if (sourceUuid != null) {
			item.setRecordSource(session.get(IntelRecordSource.class, sourceUuid));
		}
		
		UUID profileUuid = asUuid(rowData[columnNameToIndex.get("profile_uuid")]); //$NON-NLS-1$
		item.setProfile(profileUuid, session.get(IntelProfile.class, profileUuid).getName());
		item.setLocationId((String)rowData[columnNameToIndex.get("loc_id")]); //$NON-NLS-1$
		item.setLocationDate(((Timestamp)rowData[columnNameToIndex.get("loc_datetime")]).toLocalDateTime()); //$NON-NLS-1$
		item.setLocationComment((String)rowData[columnNameToIndex.get("loc_comment")]); //$NON-NLS-1$
		try{
			item.setGeometry(asGeometry(rowData[columnNameToIndex.get("loc_geometry")]), null); //$NON-NLS-1$
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
			IntelObservation obs = session.get(IntelObservation.class, item.getObservationUuid());
			if (obs != null) {
				//if obs == null then record has been deleted
			
				//TODO can't build another query in this session as scrollable results is using it
				
	//			List<IntelObservationAttribute> attributes = 
	//					QueryFactory.buildQuery(session, IntelObservationAttribute.class, "observation.uuid", item.getObservationUuid()).getResultList(); //$NON-NLS-1$
	//			for (IntelObservationAttribute a : attributes){
					for (IntelObservationAttribute a : obs.getObservationAttributes()){
					if (a.getAttribute().getType() == AttributeType.LIST){
						item.addAttribute(a.getAttribute().getKeyId(), a.getAttributeListItem().getName());	
					}else if (a.getAttribute().getType() == AttributeType.MLIST) {
						if (a.getAttributeListItems() != null) {
							item.addAttribute(a.getAttribute().getKeyId(), a.getAttributeListItems().stream().map(e->e.getAttributeListItem().getName()).collect(Collectors.joining(", "))); //$NON-NLS-1$
						}
					}else if (a.getAttribute().getType() == AttributeType.TREE){
						item.addAttribute(a.getAttribute().getKeyId(), a.getAttributeTreeNode().getName());
						
					}else if (a.getAttribute().getType().isGeometry()){
						
						
						item.addAttribute(GeometryProperty.SOURCE.generateKey(a.getAttribute().getKeyId()), a.getGeometry().getSource());
						item.addAttribute(GeometryProperty.PERIMETER.generateKey(a.getAttribute().getKeyId()), a.getGeometry().getPerimeter());
						if (a.getAttribute().getType() == Attribute.AttributeType.POLYGON) {
							item.addAttribute(GeometryProperty.AREA.generateKey(a.getAttribute().getKeyId()), a.getGeometry().getArea());
						}
						item.addAttribute(a.getAttribute().getKeyId(), a.getGeometry().getGeometry());
						
					}else{
						item.addAttribute(a.getAttribute().getKeyId(), a.getAttributeValue());
					}
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
	
	
	@Override
	public List<? extends IResultItem> getData(int offset, int pageSize, Session session) {
		final List<IResultItem> items = new ArrayList<>();
		
		String sortSql = configureSort(session);
		String sql = "SELECT * FROM " + resultsTable + sortSql; //$NON-NLS-1$
		SqlGenerator.logString(sql);
		
		try(ScrollableResults<Tuple> sc = session.createNativeQuery(sql, Tuple.class).scroll()){
			if (!sc.setRowNumber(offset+1)) return items;
			for (int i = 0; i <= pageSize; i ++){
				Tuple t = sc.get();
				Object[] data = new Object[t.getElements().size()];
				for (int j = 0; j < data.length; j ++) {
					data[j] = t.get(j);
				}				
				items.add(asResultItem(data, session));
				if (!sc.next()) break; //nothing else to get
				
			}
		}catch (Exception ex) {
			Intelligence2PlugIn.log(ex.getMessage(), ex);
		}
		return items;
	}

	private String configureSort(Session session){
		if (sortColumn == null || sortDirection == null) {
			return " ORDER BY loc_datetime DESC"; //$NON-NLS-1$
		}
		
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
			}else if (((FixedQueryColumn) sortColumn).getColumn() == Column.RECORD_PROFILE){
				return sql + "profile_uuid" + getSortDirectionSql(); //$NON-NLS-1$
			}else if (((FixedQueryColumn) sortColumn).getColumn() == Column.CA_ID){
				return sql + "lower(ca_id)" + getSortDirectionSql(); //$NON-NLS-1$
			}else if (((FixedQueryColumn) sortColumn).getColumn() == Column.CA_NAME){
				return sql + "lower(ca_name)" + getSortDirectionSql(); //$NON-NLS-1$
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
						session.createNativeQuery(updateQuery, Integer.class).executeUpdate();
						
						updateQuery = "UPDATE " + resultsTable + " SET dbl_sort = (SELECT a.double_value FROM smart.i_observation_attribute a join smart.dm_attribute b on a.attribute_uuid = b.uuid WHERE a.observation_uuid = " + resultsTable + ".observation_uuid and b.keyid ='" + attributeKey + "')"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
						SqlGenerator.logString(updateQuery);
						session.createNativeQuery(updateQuery, Integer.class).executeUpdate();
						break;
					case DATE:
						updateQuery = "UPDATE " + resultsTable + " SET date_sort = null"; //$NON-NLS-1$ //$NON-NLS-2$
						session.createNativeQuery(updateQuery, Integer.class).executeUpdate();
						
						updateQuery = "UPDATE " + resultsTable + " SET date_sort = (SELECT date(a.string_value) FROM smart.i_observation_attribute a join smart.dm_attribute b on a.attribute_uuid = b.uuid WHERE a.observation_uuid = " + resultsTable + ".observation_uuid and b.keyid ='" + attributeKey + "')"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
						SqlGenerator.logString(updateQuery);
						session.createNativeQuery(updateQuery, Integer.class).executeUpdate();
						break;
					case LIST:		
						updateQuery = "UPDATE " + resultsTable + " SET str_sort = null"; //$NON-NLS-1$ //$NON-NLS-2$
						session.createNativeQuery(updateQuery, Integer.class).executeUpdate();
						
						String attribute = "SELECT distinct a.list_element_uuid FROM " + resultsTable + " b join smart.i_observation_attribute a join smart.dm_attribute b on a.attribute_uuid = b.uuid and b.keyid = '" + attributeKey + "' on b.observation_uuid = a.observation_uuid and a.list_element_uuid is not null"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						List<byte[]> listitems = session.createNativeQuery(attribute, byte[].class).list();
						for (byte[] b : listitems){
							UUID uuid = UuidUtils.byteToUUID(b);
							
							AttributeListItem item = (AttributeListItem) session.get(AttributeListItem.class, uuid);
							if (item != null){
								updateQuery = "UPDATE " + resultsTable + " SET str_sort = :value WHERE observation_uuid in (select observation_uuid FROM smart.i_observation_attribute a WHERE " + resultsTable + ".observation_uuid = a.observation_uuid and a.list_element_uuid = :listitem)"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
								
								NativeQuery<?> q = session.createNativeQuery(updateQuery, Integer.class);
								q.setParameter("value", item.getName()); //$NON-NLS-1$
								q.setParameter("listitem", item.getUuid()); //$NON-NLS-1$
								q.executeUpdate();
							}
						}
						break;
					case TEXT:
						updateQuery = "UPDATE " + resultsTable + " SET str_sort = null"; //$NON-NLS-1$ //$NON-NLS-2$
						session.createNativeQuery(updateQuery, Integer.class).executeUpdate();
						
						updateQuery = "UPDATE " + resultsTable + " SET str_sort = (SELECT a.string_value FROM smart.i_observation_attribute a join smart.dm_attribute b on a.attribute_uuid = b.uuid WHERE a.observation_uuid = " + resultsTable + ".observation_uuid and b.keyid ='" + attributeKey + "')"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
						SqlGenerator.logString(updateQuery);
						session.createNativeQuery(updateQuery, Integer.class).executeUpdate();
						break;
					case TREE:
						updateQuery = "UPDATE " + resultsTable + " SET str_sort = null"; //$NON-NLS-1$ //$NON-NLS-2$
						session.createNativeQuery(updateQuery, Integer.class).executeUpdate();
						
						attribute = "SELECT distinct a.tree_node_uuid FROM " + resultsTable + " b join smart.i_observation_attribute a join smart.dm_attribute b on a.attribute_uuid = b.uuid and b.keyid = '" + attributeKey + "' on b.observation_uuid = a.observation_uuid and a.tree_node_uuid is not null"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						List<byte[]> treenodes = session.createNativeQuery(attribute, byte[].class).list();
						for (byte[] b : treenodes){
							AttributeTreeNode item = (AttributeTreeNode) session.get(AttributeTreeNode.class, UuidUtils.byteToUUID(b));
							if (item != null){
								updateQuery = "UPDATE " + resultsTable + " SET str_sort = :value WHERE observation_uuid in (select observation_uuid FROM smart.i_observation_attribute a WHERE " + resultsTable + ".observation_uuid = a.observation_uuid and a.tree_node_uuid = :listitem)"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
								
								NativeQuery<?> q = session.createNativeQuery(updateQuery, Integer.class);
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
	
	@Override
	public Envelope getEnvelope() {
		return this.bounds;
	}

	@Override
	public void setSorting(IQueryColumn sortColumn, SortDirection direction) {
		this.sortColumn = sortColumn;
		this.sortDirection = direction;
	}

	@Override
	public int getItemCount() {
		return obsCount;
	}
	
	public int getWaypointCount(){
		return this.wpCount;
	}

	@Override
	public void dispose(Session session) throws SQLException {
		String sql = "DROP TABLE " + resultsTable; //$NON-NLS-1$
		resultsTable = null;
		SqlGenerator.logString(sql);
		session.createNativeQuery(sql, Integer.class).executeUpdate();
	}

	@Override
	public boolean isDisposed() {
		return resultsTable == null;
	}

	@Override
	public PagedResultSetIterator iterator(Session session) {
		return new PagedResultSetIterator(this, session);
	}



}
