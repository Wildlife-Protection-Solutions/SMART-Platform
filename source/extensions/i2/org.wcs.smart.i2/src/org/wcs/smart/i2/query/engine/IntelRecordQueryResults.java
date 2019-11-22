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

import java.sql.Date;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.locationtech.jts.geom.Envelope;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.i2.model.IntelProfile;
import org.wcs.smart.i2.model.IntelRecordSource;
import org.wcs.smart.i2.query.FixedQueryColumn;
import org.wcs.smart.i2.query.FixedQueryColumn.Column;
import org.wcs.smart.i2.query.IPagedQueryResultSet;
import org.wcs.smart.i2.query.IQueryColumn;
import org.wcs.smart.i2.query.IResultItem;
import org.wcs.smart.i2.query.PagedResultSetIterator;
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
		UUID caUuid = asUuid(rowData[columnNameToIndex.get("ca_uuid")]);
		ConservationArea ca = session.get(ConservationArea.class, caUuid);
		item.setConservationAreaId(ca.getId());
		item.setConservationAreaName(ca.getName());
		
		item.setRecordUuid(asUuid(rowData[columnNameToIndex.get("record_uuid")])); //$NON-NLS-1$
		item.setRecordStatus((String)rowData[columnNameToIndex.get("record_status")]); //$NON-NLS-1$
		item.setRecordTitle((String)rowData[columnNameToIndex.get("record_title")]); //$NON-NLS-1$
		
		UUID sourceUuid = asUuid(rowData[columnNameToIndex.get("source_uuid")]); //$NON-NLS-1$
		if (sourceUuid != null) {
			item.setRecordSource(session.get(IntelRecordSource.class, sourceUuid));
		}
		
		UUID profileUuid = asUuid(rowData[columnNameToIndex.get("profile_uuid")]); //$NON-NLS-1$
		item.setProflie(profileUuid, session.get(IntelProfile.class, profileUuid).getName());
				
		item.setRecordDate((Date)rowData[columnNameToIndex.get("record_date")]);
		
//		//filter columns
//		for (Entry<IQueryFilter, String> filterColumn : filterToColumn.entrySet()){
//			boolean value = false;
//			if (rowData[columnNameToIndex.get(filterColumn.getValue())] != null){//filter columns only contain true and null values
//				value = true;
//			};
//			item.addFilterValue(filterColumn.getKey(), value);
//		}
		
		return item;
	}
	
	
	@Override
	public List<? extends IResultItem> getData(int offset, int pageSize, Session session) {
		final List<IResultItem> items = new ArrayList<>();
		
		String sortSql = configureSort(session);
		String sql = "SELECT * FROM " + resultsTable + sortSql; //$NON-NLS-1$
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
	private String configureSort(Session session){
		if (sortColumn == null || sortDirection == null) return ""; //$NON-NLS-1$
		
		String sql = " order by "; //$NON-NLS-1$
		
		if (sortColumn instanceof FixedQueryColumn){
			if (((FixedQueryColumn) sortColumn).getColumn() == Column.RECORD_STATUS){
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
//		}else if (sortColumn instanceof FilterQueryColumn){
//			String filterKey = ((FilterQueryColumn)sortColumn).getFilterKey();
//			for (Entry<IQueryFilter,String> filter : filterToColumn.entrySet()){
//				if (filter.getKey() instanceof IColumnIdentifierProvider){
//					if (((IColumnIdentifierProvider)filter.getKey()).getUniqueColumnIdentifier().equals(filterKey)){
//						return sql + " " + filter.getValue() + getSortDirectionSql(); //$NON-NLS-1$
//					}
//				}
//			}
		}
		
		return ""; //$NON-NLS-1$
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
