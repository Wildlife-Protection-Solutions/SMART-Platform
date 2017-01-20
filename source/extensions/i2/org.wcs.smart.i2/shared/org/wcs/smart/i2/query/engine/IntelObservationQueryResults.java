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

import org.hibernate.SQLQuery;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.i2.model.IntelObservationAttribute;
import org.wcs.smart.i2.query.DataModelColumn;
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

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKBReader;

/**
 * Intelligence observation query results
 * 
 * @author Emily
 *
 */
public class IntelObservationQueryResults implements IPagedQueryResultSet {

	//data details
	private String resultsTable = null;
	private int totalItems;
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
	
	public IntelObservationQueryResults(){
	}
	
	public void setResultsTable(String resultsTable){
		this.resultsTable = resultsTable;
	}
	
	public void setResultCount(int count){
		this.totalItems = count;
	}
	
	public void setCategoryCount(int catCnt){
		this.categoryCnt = catCnt;
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
	
	
	private UUID asUuid(ScrollableResults sc, int index){
		Object x = sc.get(index);
		if (x == null) return null;
		if (x instanceof UUID) return (UUID) x;
		if (x instanceof byte[]) return UuidUtils.byteToUUID((byte[])x);
		return null;
	}
	
	private Geometry asGeometry(ScrollableResults sc, int index) throws Exception{
		Object x = sc.get(index);
		if (x == null) return null;
		if (!(x instanceof Blob)) return null;
		
		Blob b = (Blob)x;
		WKBReader reader = new WKBReader();
		return reader.read(b.getBytes(1l, (int) b.length()));
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public IResultItem asResultItem(ScrollableResults sc, Session session){
		
		IntelObservationResultItem item = new IntelObservationResultItem();
		
		item.setObservationUuid(asUuid(sc,columnNameToIndex.get("observation_uuid")));
		item.setLocationUuid(asUuid(sc,columnNameToIndex.get("location_uuid")));
		item.setRecordUuid(asUuid(sc,columnNameToIndex.get("record_uuid")));
		item.setRecordStatus((String)sc.get(columnNameToIndex.get("record_status")));
		item.setRecordTitle((String)sc.get(columnNameToIndex.get("record_title")));
		
		item.setLocationId((String)sc.get(columnNameToIndex.get("loc_id")));
		item.setLocationDate((Timestamp)sc.get(columnNameToIndex.get("loc_datetime")));
		item.setLocationComment((String)sc.get(columnNameToIndex.get("loc_comment")));
		try{
			item.setGeometry(asGeometry(sc, columnNameToIndex.get("loc_geometry")), null);
		}catch (Exception ex){
			ex.printStackTrace();
			item.setGeometry(null, ex);
		}
		item.setCategoryUuid(asUuid(sc,columnNameToIndex.get("category_uuid")));
		
		List<String> categories = new ArrayList<String>();
		for (int i = 0; i < categoryCnt; i ++){
			Object x = sc.get(columnNameToIndex.get("category_" + i));
			if (x != null) categories.add((String)x);
		}
		item.setCategoryLabels(categories.toArray(new String[categories.size()]));
		
		//add attachments
		if (item.getObservationUuid() != null){
			List<IntelObservationAttribute> attributes = session.createCriteria(IntelObservationAttribute.class)
					.add(Restrictions.eq("id.observation.uuid", item.getObservationUuid()))
					.list();
			
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
			if (sc.get(columnNameToIndex.get(filterColumn.getValue())) != null){//filter columns only contain true and null values
				value = true;
			};
			item.addFilterValue(filterColumn.getKey(), value);
		}
		
		return item;
	}
	
	
	@Override
	public List<? extends IResultItem> getData(int offset, int pageSize) {
		final List<IResultItem> items = new ArrayList<>();

		Session session = HibernateManager.openSession();
		try{
			String sortSql = configureSort(session);
			String sql = "SELECT * FROM " + resultsTable + sortSql;
			SqlGenerator.logString(sql);
			ScrollableResults sc = session.createSQLQuery(sql).scroll();
			try{
				if (!sc.setRowNumber(offset)) return items;
				for (int i = 0; i <= pageSize; i ++){
					items.add(asResultItem(sc, session));
					if (!sc.next()) break; //nothing else to get
				}
			}finally{
				sc.close();
			}	
		}finally{
			session.close();
		}
		return items;
	}

	
	@SuppressWarnings("unchecked")
	private String configureSort(Session session){
		if (sortColumn == null || sortDirection == null) return "";
		
		String sql = " order by ";
		
		if (sortColumn instanceof FixedQueryColumn){
			if (((FixedQueryColumn) sortColumn).getColumn() == Column.LOC_COMMENT){
				return sql + "lower(loc_comment)" + getSortDirectionSql();
			}else if (((FixedQueryColumn) sortColumn).getColumn() == Column.LOC_ID){
				return sql + "lower(loc_id)" + getSortDirectionSql();
			}else if (((FixedQueryColumn) sortColumn).getColumn() == Column.LOC_DATE){
				return sql + "loc_datetime" + getSortDirectionSql();
			}else if (((FixedQueryColumn) sortColumn).getColumn() == Column.LOC_TIME){
				return sql + "time(loc_datetime)" + getSortDirectionSql();
			}else if (((FixedQueryColumn) sortColumn).getColumn() == Column.RECORD_STATUS){
				return sql + "record_status" + getSortDirectionSql();
			}else if (((FixedQueryColumn) sortColumn).getColumn() == Column.RECORD_TITLE){
				return sql + "lower(record_title)" + getSortDirectionSql();
			}
		}else if (sortColumn instanceof FilterQueryColumn){
			String filterKey = ((FilterQueryColumn)sortColumn).getFilterKey();
			for (Entry<IQueryFilter,String> filter : filterToColumn.entrySet()){
				if (filter.getKey() instanceof IColumnIdentifierProvider){
					if (((IColumnIdentifierProvider)filter.getKey()).getUniqueColumnIdentifier().equals(filterKey)){
						return sql + " " + filter.getValue() + getSortDirectionSql();
					}
				}
			}
		}else if (sortColumn instanceof DataModelColumn){
			DataModelColumn dm =(DataModelColumn)sortColumn;
			if (dm.getLevel() >= 0 && dm.getLevel() < categoryCnt){
				return sql + " category_" + dm.getLevel() + getSortDirectionSql();
			}else{
				if (lastSortColumn != sortColumn){
					session.getTransaction().begin();
					
					String attributeKey = ((DataModelColumn) sortColumn).getAttributeKey();
					switch(((DataModelColumn) sortColumn).getAttributeType()){
					case BOOLEAN:
					case NUMERIC:
						String updateQuery = "UPDATE " + resultsTable + " SET dbl_sort = null";
						session.createSQLQuery(updateQuery).executeUpdate();
						
						updateQuery = "UPDATE " + resultsTable + " SET dbl_sort = (SELECT a.double_value FROM smart.i_observation_attribute a join smart.dm_attribute b on a.attribute_uuid = b.uuid WHERE a.observation_uuid = " + resultsTable + ".observation_uuid and b.keyid ='" + attributeKey + "')";
						SqlGenerator.logString(updateQuery);
						session.createSQLQuery(updateQuery).executeUpdate();
						break;
					case DATE:
						updateQuery = "UPDATE " + resultsTable + " SET date_sort = null";
						session.createSQLQuery(updateQuery).executeUpdate();
						
						updateQuery = "UPDATE " + resultsTable + " SET date_sort = (SELECT date(a.string_value) FROM smart.i_observation_attribute a join smart.dm_attribute b on a.attribute_uuid = b.uuid WHERE a.observation_uuid = " + resultsTable + ".observation_uuid and b.keyid ='" + attributeKey + "')";
						SqlGenerator.logString(updateQuery);
						session.createSQLQuery(updateQuery).executeUpdate();
						break;
					case LIST:		
						updateQuery = "UPDATE " + resultsTable + " SET str_sort = null";
						session.createSQLQuery(updateQuery).executeUpdate();
						
						String attribute = "SELECT distinct a.list_element_uuid FROM " + resultsTable + " b join smart.i_observation_attribute a join smart.dm_attribute b on a.attribute_uuid = b.uuid and b.keyid = '" + attributeKey + "' on b.observation_uuid = a.observation_uuid and a.list_element_uuid is not null";
						List<Object> listitems = session.createSQLQuery(attribute).list();
						for (Object b : listitems){
							UUID uuid = null;
							if (b instanceof UUID){ 
								uuid = (UUID) b;
							}else if (b instanceof byte[]){
								uuid = UuidUtils.byteToUUID((byte[]) b);
							}
							AttributeListItem item = (AttributeListItem) session.get(AttributeListItem.class, uuid);
							if (item != null){
								updateQuery = "UPDATE " + resultsTable + " SET str_sort = :value WHERE observation_uuid in (select observation_uuid FROM smart.i_observation_attribute a WHERE " + resultsTable + ".observation_uuid = a.observation_uuid and a.list_element_uuid = :listitem)";
								
								SQLQuery q = session.createSQLQuery(updateQuery);
								q.setParameter("value", item.getName());
								q.setParameter("listitem", item.getUuid());
								q.executeUpdate();
							}
						}
						break;
					case TEXT:
						updateQuery = "UPDATE " + resultsTable + " SET str_sort = null";
						session.createSQLQuery(updateQuery).executeUpdate();
						
						updateQuery = "UPDATE " + resultsTable + " SET str_sort = (SELECT a.string_value FROM smart.i_observation_attribute a join smart.dm_attribute b on a.attribute_uuid = b.uuid WHERE a.observation_uuid = " + resultsTable + ".observation_uuid and b.keyid ='" + attributeKey + "')";
						SqlGenerator.logString(updateQuery);
						session.createSQLQuery(updateQuery).executeUpdate();
						break;
					case TREE:
						updateQuery = "UPDATE " + resultsTable + " SET str_sort = null";
						session.createSQLQuery(updateQuery).executeUpdate();
						
						attribute = "SELECT distinct a.tree_node_uuid FROM " + resultsTable + " b join smart.i_observation_attribute a join smart.dm_attribute b on a.attribute_uuid = b.uuid and b.keyid = '" + attributeKey + "' on b.observation_uuid = a.observation_uuid and a.tree_node_uuid is not null";
						List<byte[]> treenodes = session.createSQLQuery(attribute).list();
						for (byte[] b : treenodes){
							AttributeTreeNode item = (AttributeTreeNode) session.get(AttributeTreeNode.class, UuidUtils.byteToUUID(b));
							if (item != null){
								updateQuery = "UPDATE " + resultsTable + " SET str_sort = :value WHERE observation_uuid in (select observation_uuid FROM smart.i_observation_attribute a WHERE " + resultsTable + ".observation_uuid = a.observation_uuid and a.tree_node_uuid = :listitem)";
								
								SQLQuery q = session.createSQLQuery(updateQuery);
								q.setParameter("value", item.getName());
								q.setParameter("listitem", item.getUuid());
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
					return sql + "dbl_sort" + getSortDirectionSql();
				case DATE:
					return sql + "date_sort" + getSortDirectionSql();
				case LIST:
				case TEXT:
				case TREE:
					return sql + "str_sort" + getSortDirectionSql();
				default:
					break;
				}
			}
		}
		
		return "";
	}
	
	private String getSortDirectionSql(){
		if (sortDirection == SortDirection.UP) return " asc";
		return " desc";
	}
	
	@Override
	public Envelope getEnvelope() {
		return null;
	}

	@Override
	public void setSorting(IQueryColumn sortColumn, SortDirection direction) {
		this.sortColumn = sortColumn;
		this.sortDirection = direction;
	}

	@Override
	public int getItemCount() {
		return totalItems;
	}

	@Override
	public void dispose(Session session) throws SQLException {
		String sql = "DROP TABLE " + resultsTable;
		resultsTable = null;
		SqlGenerator.logString(sql);
		session.createSQLQuery(sql).executeUpdate();
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
