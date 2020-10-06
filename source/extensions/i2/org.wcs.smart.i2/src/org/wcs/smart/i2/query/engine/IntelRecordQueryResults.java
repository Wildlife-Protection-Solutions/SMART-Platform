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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.locationtech.jts.geom.Envelope;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.i2.model.IntelAttribute.AttributeType;
import org.wcs.smart.i2.model.IntelAttributeListItem;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelRecord;
import org.wcs.smart.i2.model.IntelRecordAttributeValue;
import org.wcs.smart.i2.model.IntelRecordAttributeValueList;
import org.wcs.smart.i2.query.FixedQueryColumn;
import org.wcs.smart.i2.query.FixedQueryColumn.Column;
import org.wcs.smart.i2.query.IPagedQueryResultSet;
import org.wcs.smart.i2.query.IQueryColumn;
import org.wcs.smart.i2.query.IResultItem;
import org.wcs.smart.i2.query.IntelRecordAttributeQueryColumn;
import org.wcs.smart.i2.query.PagedResultSetIterator;
import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.util.UuidUtils;

/**
 * Intelligence observation query results
 * 
 * @author Emily
 *
 */
public class IntelRecordQueryResults implements IPagedQueryResultSet {

	//data details
	private String resultsTable = null;
	private int recordCount = 0;
	
	//sorting
	private IQueryColumn lastSortColumn = null;
	private IQueryColumn sortColumn = null;
	private IPagedQueryResultSet.SortDirection sortDirection = null;
	
	//column names to results index
	private HashMap<String, Integer> columnNameToIndex;
	
	private List<IQueryColumn> queryColumns;
	
	public IntelRecordQueryResults(){
	}
	
	public void setResultsTable(String resultsTable){
		this.resultsTable = resultsTable;
	}
	
	public void setResultCount(int recordCount){
		this.recordCount = recordCount;
	}
	
	
	public void setColumnNameToIndexMap(HashMap<String, Integer> columnNameToIndex){
		this.columnNameToIndex = columnNameToIndex;
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
		
		IntelRecordResultItem item = new IntelRecordResultItem();
		UUID caUuid = asUuid(rowData[columnNameToIndex.get("ca_uuid")]); //$NON-NLS-1$
		ConservationArea ca = session.get(ConservationArea.class, caUuid);
		item.setConservationAreaId(ca.getId());
		item.setConservationAreaName(ca.getName());
		item.setConservationAreaUuid(caUuid);
		
		item.setRecordUuid(asUuid(rowData[columnNameToIndex.get("record_uuid")])); //$NON-NLS-1$
		item.setRecordStatus((String)rowData[columnNameToIndex.get("record_status")]); //$NON-NLS-1$
		item.setRecordTitle((String)rowData[columnNameToIndex.get("record_title")]); //$NON-NLS-1$
		
		UUID sourceUuid = asUuid(rowData[columnNameToIndex.get("source_uuid")]); //$NON-NLS-1$
		String sourceName = (String)rowData[columnNameToIndex.get("record_source_name")]; //$NON-NLS-1$
		item.setRecordSource(sourceUuid, sourceName);
		
		UUID profileUuid = asUuid(rowData[columnNameToIndex.get("profile_uuid")]); //$NON-NLS-1$
		String profileName = (String) rowData[columnNameToIndex.get("profile_name")]; //$NON-NLS-1$
		String profileKey = (String)rowData[columnNameToIndex.get("profile_key")]; //$NON-NLS-1$
		item.setProfile(profileKey, profileUuid, profileName);
				
		item.setRecordDate( ((java.sql.Date)rowData[columnNameToIndex.get("record_date")]).toLocalDate() ); //$NON-NLS-1$
		
		IntelRecord r = session.get(IntelRecord.class, item.getRecordUuid());
		for (IntelRecordAttributeValue v : r.getAttributes()) {
			String key = v.getAttribute().getKeyId();
			
			Object value = null;
			
			
			if(v.getAttribute().getAttribute() != null){
				AttributeType type = v.getAttribute().getAttribute().getType();
				switch(type){
				case BOOLEAN:
					value = v.getNumberValue();
					break;
				case DATE:
					value = v.getDateValue();
					break;
					
				case LIST:
					List<IntelAttributeListItem> items2 = new ArrayList<>();
					for (IntelRecordAttributeValueList li : v.getAttributeListItems()) {
						IntelAttributeListItem vli = session.get(IntelAttributeListItem.class, li.getId().getElementUuid());
						items2.add(vli);
					}
					value = items2;
					break;
				case EMPLOYEE:
					List<Employee> items = new ArrayList<>();
					for (IntelRecordAttributeValueList li : v.getAttributeListItems()) {
						items.add(session.get(Employee.class, li.getId().getElementUuid()));
					}
					value = items;
					break;
				case NUMERIC:
					value = v.getNumberValue();
					break;
				case TEXT:
					value = v.getStringValue();
					break;
				case POSITION:
					value = new Double[] {v.getNumberValue(), v.getNumberValue2()};
					break;
				}
			}else if (v.getAttribute().getEntityType() != null){
				List<IntelEntity> items = new ArrayList<>();
				for (IntelRecordAttributeValueList li : v.getAttributeListItems()) {
					IntelEntity ie = (session.get(IntelEntity.class, li.getId().getElementUuid()));
					ie.getIdAttributeAsText();
					items.add(ie);
				}
				value = items;
			}
			
			item.addAttribute(key, value);
		}

		
		return item;
	}
	
	
	@Override
	public List<? extends IResultItem> getData(int offset, int pageSize, Session session) {
		final List<IResultItem> items = new ArrayList<>();
		
		String sql = getSql(session);
		SqlGenerator.logString(sql);
			
		try(ScrollableResults sc = session.createNativeQuery(sql).scroll()){
			if (!sc.setRowNumber(offset)) return items;
			for (int i = 0; i <= pageSize; i ++){
				items.add(asResultItem(sc.get(), session));
				if (!sc.next()) break; //nothing else to get
			}
		}
		return items;
	}

	@SuppressWarnings("unchecked")
	private String getSql(Session session){
		if (sortColumn == null || sortDirection == null) return "SELECT * FROM " + resultsTable; //$NON-NLS-1$
		
		if (sortColumn instanceof FixedQueryColumn){
			String sql = "SELECT * FROM " + resultsTable + " order by "; //$NON-NLS-1$ //$NON-NLS-2$
			
			if (((FixedQueryColumn) sortColumn).getColumn() == Column.RECORD_STATUS){
				return sql + "record_status" + getSortDirectionSql(); //$NON-NLS-1$
			}else if (((FixedQueryColumn) sortColumn).getColumn() == Column.RECORD_TITLE){
				return sql + "lower(record_title)" + getSortDirectionSql(); //$NON-NLS-1$
			}else if (((FixedQueryColumn) sortColumn).getColumn() == Column.RECORD_SOURCE){
				return sql + "lower(record_source_name)" + getSortDirectionSql(); //$NON-NLS-1$
			}else if (((FixedQueryColumn) sortColumn).getColumn() == Column.RECORD_DATE){
				return sql + " record_date " + getSortDirectionSql(); //$NON-NLS-1$
			}else if (((FixedQueryColumn) sortColumn).getColumn() == Column.RECORD_PROFILE){
				return sql + "profile_name" + getSortDirectionSql(); //$NON-NLS-1$
			}else if (((FixedQueryColumn) sortColumn).getColumn() == Column.CA_ID){
				return sql + "lower(ca_id)" + getSortDirectionSql(); //$NON-NLS-1$
			}else if (((FixedQueryColumn) sortColumn).getColumn() == Column.CA_NAME){
				return sql + "lower(ca_name)" + getSortDirectionSql(); //$NON-NLS-1$
			}
		}else if (sortColumn instanceof IntelRecordAttributeQueryColumn) {
			IntelRecordAttributeQueryColumn col = ((IntelRecordAttributeQueryColumn)sortColumn);
			

			
			
			if (col.getAttribute().getAttribute() != null) {
				if (col.getAttribute().getAttribute().getType() == AttributeType.BOOLEAN ||
					col.getAttribute().getAttribute().getType() == AttributeType.DATE ||
					col.getAttribute().getAttribute().getType() == AttributeType.NUMERIC ||
					col.getAttribute().getAttribute().getType() == AttributeType.POSITION||
					col.getAttribute().getAttribute().getType() == AttributeType.TEXT) {
								
					
					StringBuilder sb = new StringBuilder();
					sb.append("SELECT a.* FROM "); //$NON-NLS-1$
					sb.append(resultsTable + " a "); //$NON-NLS-1$
					sb.append(" LEFT JOIN "); //$NON-NLS-1$
					sb.append( " ( smart.i_record_attribute_value v "); //$NON-NLS-1$
					sb.append( " JOIN smart.i_recordsource_attribute b on v.attribute_uuid = b.uuid "); //$NON-NLS-1$
					sb.append(" AND b.keyid = '" + col.getAttribute().getKeyId() + "' "); //$NON-NLS-1$ //$NON-NLS-2$
					sb.append(" JOIN smart.i_recordsource s on s.uuid = b.source_uuid "); //$NON-NLS-1$
					sb.append(" AND s.keyid =  '" + col.getAttribute().getSource().getKeyId() + "' "); //$NON-NLS-1$ //$NON-NLS-2$
					sb.append( ")" ); //$NON-NLS-1$
					sb.append(" ON a.record_uuid = v.record_uuid "); //$NON-NLS-1$
					
					if (col.getAttribute().getAttribute().getType() == AttributeType.BOOLEAN || 
							col.getAttribute().getAttribute().getType() == AttributeType.NUMERIC ) { 
						sb.append(" ORDER BY v.double_value "); //$NON-NLS-1$
						sb.append(getSortDirectionSql());
					}else if (col.getAttribute().getAttribute().getType() == AttributeType.DATE) {
						sb.append(" ORDER BY case when v.string_value is null or v.string_value = '' then null else cast(v.string_value as date) end "); //$NON-NLS-1$
						sb.append(getSortDirectionSql());	
					}else if (col.getAttribute().getAttribute().getType() == AttributeType.POSITION) {
						sb.append(" ORDER BY v.double_value "); //$NON-NLS-1$
						sb.append(getSortDirectionSql());
						sb.append(", v.double_value2 "); //$NON-NLS-1$
						sb.append(getSortDirectionSql());
					}else if (col.getAttribute().getAttribute().getType() == AttributeType.TEXT) {
						sb.append(" ORDER BY v.string_value "); //$NON-NLS-1$
						sb.append(getSortDirectionSql());
					}
					return sb.toString();
				}else if (col.getAttribute().getAttribute().getType() == AttributeType.LIST) {
					//only single attributes are sortable 
					if (lastSortColumn == col) return "SELECT * FROM " + resultsTable + " order by sort_column " + getSortDirectionSql(); //$NON-NLS-1$ //$NON-NLS-2$
					lastSortColumn = col;

					StringBuilder s3 = new StringBuilder();
					s3.append("SELECT distinct a.element_uuid FROM smart.i_record_attribute_value_list a "); //$NON-NLS-1$
					s3.append(" JOIN smart.i_record_attribute_value v on v.uuid = a.value_uuid "); //$NON-NLS-1$
					s3.append(" JOIN smart.i_recordsource_attribute b on b.uuid = v.attribute_uuid and b.keyid = '" + col.getAttribute().getKeyId() + "' "); //$NON-NLS-1$ //$NON-NLS-2$
					s3.append(" JOIN smart.i_recordsource s on s.uuid = b.source_uuid AND s.keyid = '" + col.getAttribute().getSource().getKeyId() + "' "); //$NON-NLS-1$ //$NON-NLS-2$
					s3.append(" JOIN "); //$NON-NLS-1$
					s3.append( resultsTable );
					s3.append(" r on r.record_uuid = v.record_uuid"); //$NON-NLS-1$
					
					session.beginTransaction();
					try {
						session.createNativeQuery("UPDATE " + resultsTable + " SET sort_column = null").executeUpdate(); //$NON-NLS-1$ //$NON-NLS-2$
						
						List<byte[]> items = session.createNativeQuery(s3.toString()).list();
						for (byte[] item : items) {
							IntelAttributeListItem li = session.get(IntelAttributeListItem.class, UuidUtils.byteToUUID(item));
							
							StringBuilder s2 = new StringBuilder();
							s2.append("UPDATE "); //$NON-NLS-1$
							s2.append(resultsTable);
							s2.append(" SET sort_column = :value "); //$NON-NLS-1$
							s2.append(" WHERE record_uuid IN (SELECT a.record_uuid FROM " ); //$NON-NLS-1$
							s2.append(resultsTable + " a"); //$NON-NLS-1$
							s2.append(" JOIN smart.i_record_attribute_value v on v.record_uuid = a.record_uuid "); //$NON-NLS-1$
							s2.append(" JOIN smart.i_record_attribute_value_list li on li.value_uuid = v.uuid "); //$NON-NLS-1$
							s2.append(" AND li.element_uuid = :uuid)"); //$NON-NLS-1$
							
							session.createNativeQuery(s2.toString())
								.setParameter("uuid", li.getUuid()) //$NON-NLS-1$
								.setParameter("value", li.getName()) //$NON-NLS-1$
								.executeUpdate();
							
						}
					}finally {
						session.getTransaction().commit();
					}
					return "SELECT * FROM " + resultsTable + " order by sort_column " + getSortDirectionSql(); //$NON-NLS-1$ //$NON-NLS-2$
				}else if (col.getAttribute().getAttribute().getType() == AttributeType.EMPLOYEE) {
					//only single attributes are sortable 
					if (lastSortColumn == col) return "SELECT * FROM " + resultsTable + " order by sort_column " + getSortDirectionSql(); //$NON-NLS-1$ //$NON-NLS-2$
					lastSortColumn = col;
					
					StringBuilder s3 = new StringBuilder();
					s3.append("SELECT distinct a.element_uuid FROM smart.i_record_attribute_value_list a "); //$NON-NLS-1$
					s3.append(" JOIN smart.i_record_attribute_value v on v.uuid = a.value_uuid "); //$NON-NLS-1$
					s3.append(" JOIN smart.i_recordsource_attribute b on b.uuid = v.attribute_uuid and b.keyid = '" + col.getAttribute().getKeyId() + "' "); //$NON-NLS-1$ //$NON-NLS-2$
					s3.append(" JOIN smart.i_recordsource s on s.uuid = b.source_uuid AND s.keyid = '" + col.getAttribute().getSource().getKeyId() + "' "); //$NON-NLS-1$ //$NON-NLS-2$
					s3.append(" JOIN "); //$NON-NLS-1$
					s3.append( resultsTable );
					s3.append(" r on r.record_uuid = v.record_uuid"); //$NON-NLS-1$
					
					session.beginTransaction();
					try {
						session.createNativeQuery("UPDATE " + resultsTable + " SET sort_column = null").executeUpdate(); //$NON-NLS-1$ //$NON-NLS-2$
						
						List<byte[]> items = session.createNativeQuery(s3.toString()).list();
						for (byte[] item : items) {
							Employee e = session.get(Employee.class, UuidUtils.byteToUUID(item));
							
							StringBuilder s2 = new StringBuilder();
							s2.append("UPDATE "); //$NON-NLS-1$
							s2.append(resultsTable);
							s2.append(" SET sort_column = :value "); //$NON-NLS-1$
							s2.append(" WHERE record_uuid IN (SELECT a.record_uuid FROM " ); //$NON-NLS-1$
							s2.append(resultsTable + " a"); //$NON-NLS-1$
							s2.append(" JOIN smart.i_record_attribute_value v on v.record_uuid = a.record_uuid "); //$NON-NLS-1$
							s2.append(" JOIN smart.i_record_attribute_value_list li on li.value_uuid = v.uuid "); //$NON-NLS-1$
							s2.append(" AND li.element_uuid = :uuid)"); //$NON-NLS-1$
							
							session.createNativeQuery(s2.toString())
								.setParameter("uuid", e.getUuid()) //$NON-NLS-1$
								.setParameter("value", SmartLabelProvider.getShortLabel(e)) //$NON-NLS-1$
								.executeUpdate();
							
						}
					}finally {
						session.getTransaction().commit();
					}
					return "SELECT * FROM " + resultsTable + " order by sort_column " + getSortDirectionSql(); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}else if (col.getAttribute().getEntityType() != null) {
				//only single attributes are sortable 
				if (lastSortColumn == col) return "SELECT * FROM " + resultsTable + " order by sort_column " + getSortDirectionSql(); //$NON-NLS-1$ //$NON-NLS-2$
				lastSortColumn = col;

				StringBuilder s3 = new StringBuilder();
				s3.append("SELECT distinct a.element_uuid FROM smart.i_record_attribute_value_list a "); //$NON-NLS-1$
				s3.append(" JOIN smart.i_record_attribute_value v on v.uuid = a.value_uuid "); //$NON-NLS-1$
				s3.append(" JOIN smart.i_recordsource_attribute b on b.uuid = v.attribute_uuid and b.keyid = '" + col.getAttribute().getKeyId() + "' "); //$NON-NLS-1$ //$NON-NLS-2$
				s3.append(" JOIN smart.i_recordsource s on s.uuid = b.source_uuid AND s.keyid = '" + col.getAttribute().getSource().getKeyId() + "' "); //$NON-NLS-1$ //$NON-NLS-2$
				s3.append(" JOIN "); //$NON-NLS-1$
				s3.append( resultsTable );
				s3.append(" r on r.record_uuid = v.record_uuid"); //$NON-NLS-1$
				
				session.beginTransaction();
				try {
					session.createNativeQuery("UPDATE " + resultsTable + " SET sort_column = null").executeUpdate(); //$NON-NLS-1$ //$NON-NLS-2$
					
					List<byte[]> items = session.createNativeQuery(s3.toString()).list();
					for (byte[] item : items) {
						IntelEntity li = session.get(IntelEntity.class, UuidUtils.byteToUUID(item));
						
						StringBuilder s2 = new StringBuilder();
						s2.append("UPDATE "); //$NON-NLS-1$
						s2.append(resultsTable);
						s2.append(" SET sort_column = :value "); //$NON-NLS-1$
						s2.append(" WHERE record_uuid IN (SELECT a.record_uuid FROM " ); //$NON-NLS-1$
						s2.append(resultsTable + " a"); //$NON-NLS-1$
						s2.append(" JOIN smart.i_record_attribute_value v on v.record_uuid = a.record_uuid "); //$NON-NLS-1$
						s2.append(" JOIN smart.i_recordsource_attribute b on b.uuid = v.attribute_uuid and b.keyid = '" + col.getAttribute().getKeyId() + "' "); //$NON-NLS-1$ //$NON-NLS-2$
						s2.append(" JOIN smart.i_recordsource s on s.uuid = b.source_uuid AND s.keyid = '" + col.getAttribute().getSource().getKeyId() + "' "); //$NON-NLS-1$ //$NON-NLS-2$
						s2.append(" JOIN smart.i_record_attribute_value_list li on li.value_uuid = v.uuid "); //$NON-NLS-1$
						s2.append(" AND li.element_uuid = :uuid)"); //$NON-NLS-1$
						
						session.createNativeQuery(s2.toString())
							.setParameter("uuid", li.getUuid()) //$NON-NLS-1$
							.setParameter("value", li.getIdAttributeAsText()) //$NON-NLS-1$
							.executeUpdate();
						
					}
				}finally {
					session.getTransaction().commit();
				}
				return "SELECT * FROM " + resultsTable + " order by sort_column " + getSortDirectionSql(); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		//no sorting
		return "SELECT * FROM " + resultsTable; //$NON-NLS-1$
	}
	
	private String getSortDirectionSql(){
		if (sortDirection == SortDirection.UP) return " asc"; //$NON-NLS-1$
		return " desc"; //$NON-NLS-1$
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
		return recordCount;
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

	@Override
	public PagedResultSetIterator iterator(Session session) {
		return new PagedResultSetIterator(this, session);
	}



}
