package org.wcs.smart.connect.query.engine;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;

import org.wcs.smart.query.common.engine.IQueryResult;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.model.QueryColumn;

public interface IMemoryTableResultSet<T extends IResultItem> extends IQueryResult {
	
	public Iterator<T> getIterator() throws SQLException;
		
	public String getValueAsString(T item, QueryColumn column) throws SQLException;

	public Object getValue(T item, String columnKey) throws SQLException;
}
