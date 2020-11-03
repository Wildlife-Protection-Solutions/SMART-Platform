package org.wcs.smart.query.common.engine.test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.Session;
import org.wcs.smart.query.common.engine.IAttachmentResultItem;
import org.wcs.smart.query.common.engine.IQueryEngine;
import org.wcs.smart.query.common.engine.IResultItem;

public interface IDesktopWOEngine<T extends IResultItem> extends IQueryEngine{

	public String getQueryDataTable();
	
	public String createTempTableName();
	
	public T asQueryResultItem(ResultSet rs, Session session) throws SQLException;
	
	public IAttachmentResultItem asQueryAttachmentResultItem(ResultSet rs, Session session) throws SQLException;
	
	public void dropTables(Connection c) throws SQLException;
	
	public void dropTable(Connection c, String table) throws SQLException;
}
