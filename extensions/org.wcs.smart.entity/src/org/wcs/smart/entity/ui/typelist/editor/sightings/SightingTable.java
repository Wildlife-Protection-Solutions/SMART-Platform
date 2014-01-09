package org.wcs.smart.entity.ui.typelist.editor.sightings;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableColumn;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.entity.model.EntityAttribute;
import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.entity.ui.typelist.editor.sightings.SightingTableColumns.FixedColumns;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.common.ui.QueryLazyResultsContentProvider;
import org.wcs.smart.query.model.IPagedQueryResultSet;
import org.wcs.smart.query.model.IResultItem;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.query.model.QueryColumn.ColumnType;
import org.wcs.smart.util.SmartUtils;

public class SightingTable {

	private TableViewer sightingsTable;
	
	public SightingTable(Composite composite){
		sightingsTable = new TableViewer(composite, SWT.BORDER | SWT.VIRTUAL | SWT.FULL_SELECTION | SWT.MULTI);
		
		sightingsTable.getTable().setHeaderVisible(true);
		sightingsTable.getTable().setLinesVisible(true);
		sightingsTable.setContentProvider(new QueryLazyResultsContentProvider(sightingsTable));
		sightingsTable.setItemCount(0);
	}
	
	public void setInput(IPagedQueryResultSet results){
		sightingsTable.setItemCount(0);
		sightingsTable.setInput(null);
		
		sightingsTable.setItemCount(results.getItemCount());
		sightingsTable.setInput(results);
	}
	
	public TableViewer getTable(){
		return this.sightingsTable;
	}
	
	public void setEntityType(EntityType et){
		for (TableColumn tc : sightingsTable.getTable().getColumns()){
			tc.dispose();
		}
		
		for (final QueryColumn col : getQueryColumns(et)){
			TableViewerColumn tv = new TableViewerColumn(sightingsTable, SWT.NONE);
			tv.getColumn().setWidth(100);
			tv.getColumn().setText(col.getName());
			tv.setLabelProvider(new ColumnLabelProvider(){
				public String getText(Object element){
					if (element instanceof IResultItem){
						return asString(col.getValue((IResultItem)element), col.getType());
					}
					return element.toString();
				}
			});
		}
	}
	private List<QueryColumn> getQueryColumns(EntityType type){
		List<QueryColumn> cols = new ArrayList<QueryColumn>();
		
		//fixed columns for waypoint and fixed entity attributes
		for (FixedColumns fixed : SightingTableColumns.FixedColumns.values()){
			QueryColumn column = new SightingQueryColumn(fixed.getGuiName(),fixed.getKey(),fixed.getType());
			
			if (SmartDB.isMultipleAnalysis()){
				cols.add(column);	
			}else{
				if ( fixed != FixedColumns.CA_ID && fixed != FixedColumns.CA_NAME ){
					cols.add(column);	
				}	
			}
		}
		
		//entity attributes
		for (EntityAttribute ea : type.getAttributes()){
			String name = ea.getName();
			String key = "entity:" + SmartUtils.encodeHex(ea.getUuid());
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
			
			QueryColumn column = new SightingQueryColumn(name, key, cType);
			cols.add(column);
		}
	
		//cols.add(new SightingQueryColumn("Observation Summary", "entity:obssum", ColumnType.STRING));
		
		return cols;
	}
	
	private static String asString(Object value, ColumnType type) {
		if (type == ColumnType.BOOLEAN) {
			if ((Boolean) value) {
				return Attribute.BOOLEAN_TRUE_LABEL;
			} else {
				return Attribute.BOOLEAN_FALSE_LABEL;
			}
		} else if (type == ColumnType.DATE) {
			if ((Date)value == null){
				return ""; //$NON-NLS-1$
			}
			return DateFormat.getDateInstance().format((Date) value);
		} else if (type == ColumnType.TIME) {
			if ((Date)value == null){
				return ""; //$NON-NLS-1$
			}
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
