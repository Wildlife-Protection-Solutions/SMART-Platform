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

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.query.NativeQuery;
import org.locationtech.jts.geom.Envelope;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.i2.model.IntelAttributeListItem;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityAttributeValue;
import org.wcs.smart.i2.model.IntelProfile;
import org.wcs.smart.i2.query.FilterQueryColumn;
import org.wcs.smart.i2.query.FixedQueryColumn;
import org.wcs.smart.i2.query.FixedQueryColumn.Column;
import org.wcs.smart.i2.query.IPagedQueryResultSet;
import org.wcs.smart.i2.query.IQueryColumn;
import org.wcs.smart.i2.query.IResultItem;
import org.wcs.smart.i2.query.IntelAttributeQueryColumn;
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
public class IntelEntityRecordQueryResults implements IPagedQueryResultSet {

	//data details
	private String resultsTable = null;
	private int obsCount;
	
	//sorting
	private IQueryColumn lastSortColumn = null;
	private IQueryColumn sortColumn = null;
	private IPagedQueryResultSet.SortDirection sortDirection = null;
	
	//filters to column
	private HashMap<IQueryFilter, String> filterToColumn;
	
	//column names to results index
	private HashMap<String, Integer> columnNameToIndex;
	
	private List<IQueryColumn> queryColumns;
	
	public IntelEntityRecordQueryResults(){
	}
	
	public void setResultsTable(String resultsTable){
		this.resultsTable = resultsTable;
	}
	
	public void setResultCount(int obsCount){
		this.obsCount = obsCount;
	}
		
	public void setColumnNameToIndexMap(HashMap<String, Integer> columnNameToIndex){
		this.columnNameToIndex = columnNameToIndex;
	}
	
	public void setFilterToColumnMap(List<Object[]> filterToColumn){
		this.filterToColumn = new HashMap<>();
		for (Object[] o : filterToColumn) {
			this.filterToColumn.put((IQueryFilter)o[0], (String)o[1]);
		}
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
		
	@Override
	public IResultItem asResultItem(Object[] rowData, Session session){
		
		EntityRecordQueryResultItem item = new EntityRecordQueryResultItem();
		
		item.setEntityUuid(asUuid(rowData[columnNameToIndex.get("entity_uuid")])); //$NON-NLS-1$
		item.setEntityLastModified( ((Timestamp)rowData[columnNameToIndex.get("date_modified")]).toLocalDateTime());  //$NON-NLS-1$
		String entityType = (String) rowData[columnNameToIndex.get("entity_type")]; //$NON-NLS-1$
		item.setEntityTypeName(entityType);

		item.setConservationAreaId((String)rowData[columnNameToIndex.get("ca_id")]); //$NON-NLS-1$
		item.setConservationAreaName((String)rowData[columnNameToIndex.get("ca_name")]); //$NON-NLS-1$
		item.setConservationAreaUuid(  UuidUtils.byteToUUID((byte[])rowData[columnNameToIndex.get("ca_uuid")]) ); //$NON-NLS-1$
		UUID puuid = asUuid(rowData[columnNameToIndex.get("profile_uuid")]); //$NON-NLS-1$
		String name = session.get(IntelProfile.class, puuid).getName();
		item.setProflie(puuid, name);
		
		IntelEntity e = session.get(IntelEntity.class, item.getEntityUuid());
		item.setEntityId(e.getIdAttributeAsText());
		
		for (IntelEntityAttributeValue v : e.getAttributes()) {
			if (v.getAttributeValue() instanceof Employee) ((Employee)v.getAttributeValue()).getFamilyName();
			if (v.getAttributeValue() instanceof IntelAttributeListItem) ((IntelAttributeListItem)v.getAttributeValue()).getName();
			item.addAttribute(v.getAttribute().getKeyId(), v.getAttributeValue());
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
		}
		return items;
	}

	
	private String configureSort(Session session){
		if (sortColumn == null || sortDirection == null) {
			return " "; //$NON-NLS-1$
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
			}else if (((FixedQueryColumn) sortColumn).getColumn() == Column.ENTITY_TYPE){
				return sql + "lower(entity_type)" + getSortDirectionSql(); //$NON-NLS-1$
			}else if (((FixedQueryColumn) sortColumn).getColumn() == Column.CA_ID){
				return sql + "lower(ca_id)" + getSortDirectionSql(); //$NON-NLS-1$
			}else if (((FixedQueryColumn) sortColumn).getColumn() == Column.CA_NAME){
				return sql + "lower(ca_name)" + getSortDirectionSql(); //$NON-NLS-1$
			}else if (((FixedQueryColumn) sortColumn).getColumn() == Column.ENTITY_PROFILE){
				return sql += " profile_uuid " + getSortDirectionSql(); //$NON-NLS-1$
			}else if (((FixedQueryColumn) sortColumn).getColumn() == Column.ENTITY_ID){
				session.beginTransaction();
				try {
					List<byte[]> entities = session.createNativeQuery("SELECT distinct entity_uuid FROM " + resultsTable + " WHERE entity_id is null", byte[].class).list(); //$NON-NLS-1$ //$NON-NLS-2$
					String updateQuerystr = "UPDATE " + resultsTable + " SET entity_id = :name WHERE entity_uuid = :uuid"; //$NON-NLS-1$ //$NON-NLS-2$
					SqlGenerator.logString(updateQuerystr);
					NativeQuery<?> updateQuery = session.createNativeQuery(updateQuerystr, Integer.class);
					for (byte[] e : entities) {
						UUID entityUuid = UuidUtils.byteToUUID(e);
						
						IntelEntity ie = session.get(IntelEntity.class, entityUuid);
						String name = ""; //$NON-NLS-1$
						if (ie == null) {
							name = UuidUtils.uuidToString(entityUuid);
						}else {
							name = ie.getIdAttributeAsText();
						}
						updateQuery
							.setParameter("name", name) //$NON-NLS-1$
							.setParameter("uuid", entityUuid) //$NON-NLS-1$
							.executeUpdate();
					}
				}finally {
					session.getTransaction().commit();
				}
				return sql + "lower(entity_id)" + getSortDirectionSql(); //$NON-NLS-1$
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
		
		}else if (sortColumn instanceof IntelAttributeQueryColumn) {
			if (lastSortColumn != sortColumn){
				session.getTransaction().begin();
				
				String attributeKey = ((IntelAttributeQueryColumn) sortColumn).getAttribute().getKeyId();
				switch(((IntelAttributeQueryColumn) sortColumn).getAttribute().getType()){
				case BOOLEAN:
				case NUMERIC:
					String updateQuery = "UPDATE " + resultsTable + " SET dbl_sort = null"; //$NON-NLS-1$ //$NON-NLS-2$
					session.createNativeQuery(updateQuery, Integer.class).executeUpdate();
					updateQuery = "UPDATE " + resultsTable + " SET dbl_sort = (SELECT a.double_value FROM smart.i_entity_attribute_value a join smart.i_attribute b on a.attribute_uuid = b.uuid WHERE a.entity_uuid = " + resultsTable + ".entity_uuid and b.keyid ='" + attributeKey + "')"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
					SqlGenerator.logString(updateQuery);
					session.createNativeQuery(updateQuery, Integer.class).executeUpdate();
					break;
				case DATE:
					updateQuery = "UPDATE " + resultsTable + " SET date_sort = null"; //$NON-NLS-1$ //$NON-NLS-2$
					session.createNativeQuery(updateQuery, Integer.class).executeUpdate();
					updateQuery = "UPDATE " + resultsTable + " SET date_sort = (SELECT date(a.string_value) FROM smart.i_entity_attribute_value a join smart.i_attribute b on a.attribute_uuid = b.uuid WHERE a.entity_uuid = " + resultsTable + ".entity_uuid and b.keyid ='" + attributeKey + "')"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
					SqlGenerator.logString(updateQuery);
					session.createNativeQuery(updateQuery, Integer.class).executeUpdate();
					break;
				case LIST:		
					updateQuery = "UPDATE " + resultsTable + " SET str_sort = null"; //$NON-NLS-1$ //$NON-NLS-2$
					session.createNativeQuery(updateQuery, Integer.class).executeUpdate();
					SqlGenerator.logString(updateQuery);
					String attribute = "SELECT distinct a.list_item_uuid FROM " + resultsTable + " b join smart.i_entity_attribute_value a join smart.i_attribute b on a.attribute_uuid = b.uuid and b.keyid = '" + attributeKey + "' on b.entity_uuid = a.entity_uuid and a.list_item_uuid is not null"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					List<byte[]> listitems = session.createNativeQuery(attribute, byte[].class).list();
					for (byte[] b : listitems){
						UUID uuid = UuidUtils.byteToUUID(b);
						
						IntelAttributeListItem item = (IntelAttributeListItem) session.get(IntelAttributeListItem.class, uuid);
						if (item != null){
							updateQuery = "UPDATE " + resultsTable + " SET str_sort = :value WHERE entity_uuid in (select entity_uuid FROM smart.i_entity_attribute_value a WHERE " + resultsTable + ".entity_uuid = a.entity_uuid and a.list_item_uuid = :listitem)"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							SqlGenerator.logString(updateQuery);
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
					
					updateQuery = "UPDATE " + resultsTable + " SET str_sort = (SELECT a.string_value FROM smart.i_entity_attribute_value a join smart.i_attribute b on a.attribute_uuid = b.uuid WHERE a.entity_uuid = " + resultsTable + ".entity_uuid and b.keyid ='" + attributeKey + "')"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
					SqlGenerator.logString(updateQuery);
					session.createNativeQuery(updateQuery, Integer.class).executeUpdate();
					break;
				default:
					break;
				}
				session.getTransaction().commit();
				
				lastSortColumn = sortColumn;
			}
			
			switch(((IntelAttributeQueryColumn) sortColumn).getAttribute().getType()){
			case BOOLEAN:
			case NUMERIC:
				return sql + "dbl_sort" + getSortDirectionSql(); //$NON-NLS-1$
			case DATE:
				return sql + "date_sort" + getSortDirectionSql(); //$NON-NLS-1$
			case LIST:
			case TEXT:
				return sql + "lower(str_sort)" + getSortDirectionSql(); //$NON-NLS-1$
			default:
				break;
			}
		}
		
		return ""; //$NON-NLS-1$
	}
	
	private String getSortDirectionSql(){
		if (sortDirection == SortDirection.UP) return " asc"; //$NON-NLS-1$
		return " desc"; //$NON-NLS-1$
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

	@Override
	public Envelope getEnvelope() {
		return null;
	}



}
