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
import java.util.Map.Entry;
import java.util.UUID;

import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.i2.birt.datasource.ui.IntelEntityAttributeLocaitonWizardPage;
import org.wcs.smart.i2.model.IntelAttributeListItem;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityAttributeValue;
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
		
		String entityType = (String) rowData[columnNameToIndex.get("entity_type")];
		item.setEntityTypeName(entityType);

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

	@Override
	public Envelope getEnvelope() {
		return null;
	}



}
