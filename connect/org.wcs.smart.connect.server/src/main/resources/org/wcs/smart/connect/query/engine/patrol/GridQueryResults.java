package org.wcs.smart.connect.query.engine.patrol;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;

import org.wcs.smart.connect.query.engine.IMemoryTableResultSet;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.common.model.GridResultItem;
import org.wcs.smart.query.model.GridQueryColumn;
import org.wcs.smart.query.model.QueryColumn;

public class GridQueryResults implements IMemoryTableResultSet<GridResultItem> {

	private PsqlGridEngine engine;
	private Collection<GridResultItem> items;
	
	public GridQueryResults(PsqlGridEngine engine, Collection<GridResultItem> items){
		this.engine = engine;
		this.items = items;
	}

	@Override
	public Iterator<GridResultItem> getIterator() throws SQLException {
		return items.iterator();
	}


	@Override
	public String getValueAsString(GridResultItem item, QueryColumn column)
			throws SQLException {
		Object v = getValue(item, column.getKey());
		if (v == null) return "";
		if (v instanceof Number){
			return ((Number)v).toString();
		}
		return "";
	}

	@Override
	public Object getValue(GridResultItem item, String columnKey)
			throws SQLException {
		if (columnKey.equals(GridQueryColumn.GridColumns.TILE_X)){
			return item.getTileX();
		}else if (columnKey.equals(GridQueryColumn.GridColumns.TILE_Y)){
			return item.getTileY();
		}else if (columnKey.equals(GridQueryColumn.GridColumns.VALUE)){
			return item.getValue();
		}
		return null;
	}
	

}
