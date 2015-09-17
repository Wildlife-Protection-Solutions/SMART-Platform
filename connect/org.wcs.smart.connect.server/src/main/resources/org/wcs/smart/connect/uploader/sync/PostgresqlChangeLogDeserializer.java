package org.wcs.smart.connect.uploader.sync;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.hibernate.Session;
import org.wcs.smart.connect.replication.changelog.ChangeLogDeserializer;
import org.wcs.smart.connect.replication.changelog.ChangeLogItem;

public class PostgresqlChangeLogDeserializer extends ChangeLogDeserializer {

	public PostgresqlChangeLogDeserializer(Path changeLogFile) {
		super(changeLogFile);
	}

	@Override
	public void processFile(final Session session) throws Exception{
		session.createSQLQuery("SET CONSTRAINTS ALL DEFERRED").executeUpdate();
		super.processFile(session);
	}
	
	@Override
	protected boolean shouldProcess(ChangeLogItem it) {
		//TODO: review this
		if (ChangeLogManager.INSTANCE.constains(session, it)){
			//we already have this item
			return false;
		}
		//TODO: look for conflict; throw exception if conflict
		
		return true;
	}
	
	@Override
	protected void processFileDelete(ChangeLogItem arg0, Connection arg1)
			throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void processFileInsert(ChangeLogItem arg0, Connection arg1)
			throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void processFileUpdate(ChangeLogItem arg0, Connection arg1)
			throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void saveItem(ChangeLogItem item, Session s) throws Exception {
		ChangeLogManager.INSTANCE.insertItem(s, item);
	}

	@Override
	protected void processDataDelete(ChangeLogItem item, Connection c) throws Exception{
		StringBuilder sb = new StringBuilder();
		sb.append("DELETE FROM " + item.getTableName());
		sb.append(" WHERE " + item.getFieldName1() + " = ?");
		if (item.getFieldName2() != null){
			sb.append(" AND " + item.getFieldName2()  + " = ?");
		}
		PreparedStatement ps = c.prepareStatement(sb.toString());
		ps.setObject(1, item.getKey1());
		if (item.getKey2() != null){
			ps.setObject(2, item.getKey2());	
		}else if (item.getKey2String() != null){
			ps.setString(2, item.getKey2String());
		}
		int up = ps.executeUpdate();
		//this check is not valid as we only provide the last change in the change log.  if and
		//item is created then deleted we will only provide the delete event
		//in the change log which will have nothing to delete here.
//		if (up != 1){
//			throw new SQLException("Invalid number of row deleted.");
//		}
	}
	@Override
	protected void processDataUpdate(ChangeLogItem item, HashMap<String, Object> data, Connection c) throws Exception{
		StringBuilder sb = new StringBuilder();
		sb.append("UPDATE " + item.getTableName());
		sb.append(" SET ");
		
		List<Object> params = new ArrayList<Object>();
		for(Entry<String, Object> dataitem : data.entrySet()){
			String colName = dataitem.getKey();
			Object obj = dataitem.getValue();	
			sb.append(colName + " = ?, ");
			params.add(obj);
		}
		sb.deleteCharAt(sb.length() - 1);
		sb.deleteCharAt(sb.length() - 1);
		
		sb.append(" WHERE " + item.getFieldName1() + " = ?");
		if (item.getFieldName2() != null){
			sb.append(" AND " + item.getFieldName2()  + " = ?");
		}
		
		PreparedStatement ps = c.prepareStatement(sb.toString());
		for (int i = 1; i <= params.size(); i ++){
			ps.setObject(i, params.get(i-1));
		}
		ps.setObject(params.size() + 1, item.getKey1());
		if (item.getKey2() != null){
			ps.setObject(params.size() + 2, item.getKey2());
		}else if (item.getKey2String() != null){
			ps.setString(params.size() + 2, item.getKey2String());
		}
		int cnt = ps.executeUpdate();
		if (cnt != 1){
			throw new SQLException("Invalid number of rows updated.");
		}
	}
	
	@Override
	protected void processDataInsert(ChangeLogItem item, HashMap<String, Object> data,Connection c) throws Exception{
		StringBuilder sb = new StringBuilder();
		sb.append("INSERT INTO " + item.getTableName() + "(");
		
		StringBuilder values = new StringBuilder();
		values.append("VALUES(");
		
		List<Object> params = new ArrayList<Object>();
		for(Entry<String, Object> dataitem : data.entrySet()){
			String colName = dataitem.getKey();
			Object obj = dataitem.getValue();	
			sb.append(colName + ",");
			values.append("?,");
			params.add(obj);
		}
		sb.deleteCharAt(sb.length() - 1);
		values.deleteCharAt(values.length() - 1);
		
		sb.append(") ");
		sb.append(values.toString());
		sb.append(")");
		PreparedStatement ps = c.prepareStatement(sb.toString() );
		for (int i = 1; i <= params.size(); i ++){
			ps.setObject(i, params.get(i-1));
		}
		int cnt = ps.executeUpdate();
		if (cnt != 1){
			throw new SQLException("Invalid number of rows updated.");
		}
	}
}
