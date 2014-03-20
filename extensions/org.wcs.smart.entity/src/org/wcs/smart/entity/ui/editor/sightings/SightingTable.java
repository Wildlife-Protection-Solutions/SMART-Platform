/*
 * Copyright (C) 2012 Wildlife Conservation Society
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
package org.wcs.smart.entity.ui.editor.sightings;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.entity.internal.Messages;
import org.wcs.smart.entity.model.EntityAttribute;
import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.entity.query.SightingQueryColumn;
import org.wcs.smart.entity.query.SightingQueryColumn.FixedColumns;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.QueryDataModelManager;
import org.wcs.smart.query.common.ui.QueryLazyResultsContentProvider;
import org.wcs.smart.query.common.ui.QueryTableViewerColumn;
import org.wcs.smart.query.model.IPagedQueryResultSet;
import org.wcs.smart.query.model.IResultItem;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.query.model.QueryColumn.ColumnType;

/**
 * Sightings table for displaying and managing sighting
 * query results.
 * 
 * @author Emily
 *
 */
public class SightingTable {

	private TableViewer sightingsTable;
	private List<QueryColumn>  currentCols;
	
	private QueryLazyResultsContentProvider sorter;
	private List<QueryTableViewerColumn> tableColumns = null;
	
	public SightingTable(Composite composite){
		sightingsTable = new TableViewer(composite, SWT.BORDER | SWT.VIRTUAL | SWT.FULL_SELECTION | SWT.MULTI);
		
		sightingsTable.getTable().setHeaderVisible(true);
		sightingsTable.getTable().setLinesVisible(true);
		sorter = new QueryLazyResultsContentProvider(sightingsTable);
		sightingsTable.setContentProvider(sorter);
		sightingsTable.setItemCount(0);
		
	}
	
	/**
	 * Update the table contents 
	 * @param results
	 */
	public void setInput(IPagedQueryResultSet results){
		if (sightingsTable.getTable().isDisposed()){
			return;
		}
		if (results == null){
			sightingsTable.setItemCount(0);
			sightingsTable.setInput(null);
		}else{
			sightingsTable.setItemCount(results.getItemCount());
			sightingsTable.setInput(results);
		}
	}
	
	/**
	 * 
	 * @return the table viewer component
	 */
	public TableViewer getTable(){
		return this.sightingsTable;
	}
	
	/**
	 * Updates/sets the entity type.  This causes all columns in the table
	 * to be disposed and recreated.
	 * @param et
	 */
	public void setEntityType(EntityType et){
		sightingsTable.getTable().setRedraw(false);
		if (tableColumns != null){
			for (QueryTableViewerColumn column : tableColumns){
				column.getTableColumn().getColumn().dispose();
			}
			tableColumns = null;
		}
		if (sorter != null){
			sorter.setSortColumn(null);
		}
		
		tableColumns = new ArrayList<QueryTableViewerColumn>();
		currentCols = getQueryColumns(et);
		for (final QueryColumn col : currentCols){
			tableColumns.add(new QueryTableViewerColumn(sightingsTable,col, sorter, new ColumnLabelProvider(){
				public String getText(Object element){
					if (element instanceof IResultItem){
						return asString(col.getValue((IResultItem)element), col.getType());
					}
					return element.toString();
				}
			}));
			
		}
		
		sightingsTable.getTable().setRedraw(true);
	}
	
	/**
	 * 
	 * @return the current columns in the table
	 */
	public List<QueryColumn> getCurrentColumns(){
		return this.currentCols;
		
	}
	
	private List<QueryColumn> getQueryColumns(EntityType type){
		List<QueryColumn> cols = new ArrayList<QueryColumn>();
		
		//fixed columns for waypoint and fixed entity attributes
		for (FixedColumns fixed : SightingQueryColumn.FixedColumns.values()){
			QueryColumn column = new SightingQueryColumn(fixed.getGuiName(),fixed.getKey(),fixed.getType(), fixed.dbColName);
			if (SmartDB.isMultipleAnalysis()){
				cols.add(column);	
			}else{
				if ( fixed != FixedColumns.CA_ID && fixed != FixedColumns.CA_NAME ){
					cols.add(column);	
				}	
			}
		}
		
		//entity attributes
		if (type.getAttributes() != null){
			for (EntityAttribute ea : type.getAttributes()){
				String name = ea.getName();
				String key = "entity:" + ea.getKeyId(); //$NON-NLS-1$
				ColumnType cType = ColumnType.STRING;
				if (ea.getDmAttribute().getType() == AttributeType.LIST ||
						ea.getDmAttribute().getType() == AttributeType.TREE ||
						ea.getDmAttribute().getType() == AttributeType.TEXT ){
					cType = ColumnType.STRING;
				}else if (ea.getDmAttribute().getType() == AttributeType.DATE){
					cType = ColumnType.DATE;
				}else if (ea.getDmAttribute().getType() == AttributeType.BOOLEAN){
					cType = ColumnType.BOOLEAN;
				}else if (ea.getDmAttribute().getType() == AttributeType.NUMERIC){
					cType = ColumnType.NUMBER;
				}
				
				QueryColumn column = new SightingQueryColumn(
						ea.getEntityType().getName() + "|" + name, //$NON-NLS-1$
						key, cType, "ea_" + ea.getKeyId()); //$NON-NLS-1$ 
				cols.add(column);
			}
		}
		//data model category
		int catCount = QueryDataModelManager.getInstance().getActiveDepth();
		for(int i = 0; i < catCount; i++){
			cols.add(new SightingQueryColumn(
					MessageFormat.format(Messages.SightingTable_ObservationCategoryLabel, new Object[]{i}), "cat:" + i,  //$NON-NLS-1$
					ColumnType.STRING, "category_" + i));  //$NON-NLS-1$
		}
		
		return cols;
	}
	
	private static String asString(Object value, ColumnType type) {
		if (value == null){
			return ""; //$NON-NLS-1$
		}
		if (type == ColumnType.BOOLEAN) {
			if ((Double) value >= 0.5) {
				return Attribute.BOOLEAN_TRUE_LABEL;
			} else {
				return Attribute.BOOLEAN_FALSE_LABEL;
			}
		} else if (type == ColumnType.DATE) {
			return DateFormat.getDateInstance().format((Date) value);
		} else if (type == ColumnType.TIME) {
			return DateFormat.getTimeInstance().format((Date) value);
		} else if (type == ColumnType.STRING) {
			return (String) value;
		} else if (type == ColumnType.INTEGER) {
			return String.valueOf((Integer) value);
		} else if (type == ColumnType.LONG) {
			return String.valueOf((Long) value);
		} else if (type == ColumnType.NUMBER) {
			if (value instanceof Double) {
				return String.valueOf((Double) value);
			} else if (value instanceof Float) {
				return String.valueOf((Float) value);
			} else if (value instanceof Integer) {
				return String.valueOf((Integer) value);
			}
		}
		return ""; //$NON-NLS-1$

	}
}
