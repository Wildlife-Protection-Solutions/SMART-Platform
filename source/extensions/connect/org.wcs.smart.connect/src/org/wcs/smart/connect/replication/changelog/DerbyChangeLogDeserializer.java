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
package org.wcs.smart.connect.replication.changelog;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.hibernate.Session;
import org.wcs.smart.util.UuidUtils;

public class DerbyChangeLogDeserializer extends ChangeLogDeserializer{

	public DerbyChangeLogDeserializer(Path changeLogFile) {
		super(changeLogFile);
	}

	@Override
	public void processFile(final Session session) throws Exception{
		session.createSQLQuery("SET CONSTRAINTS ALL DEFERRED").executeUpdate();
		super.processFile(session);
	}
	
	public boolean shouldProcess(ChangeLogItem it){
		if (ChangeLogTableManager.INSTANCE.constains(session, it)){
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
		ChangeLogTableManager.INSTANCE.addItem(s, item);
	}
	
	@Override
	protected void processDataDelete(ChangeLogItem item, Connection c) throws SQLException{
		
		StringBuilder sb = new StringBuilder();
		sb.append("DELETE FROM " + item.getTableName());
		sb.append(" WHERE " + item.getFieldName1() + " = ?");
		if (item.getFieldName2() != null){
			sb.append(" AND " + item.getFieldName2()  + " = ?");
		}
		PreparedStatement ps = c.prepareStatement(sb.toString());
		ps.setBytes(1, UuidUtils.uuidToByte(item.getKey1()));
		if (item.getKey2() != null){
			ps.setBytes(2, UuidUtils.uuidToByte(item.getKey2()));	
		}else if (item.getKey2String() != null){
			ps.setString(2, item.getKey2String());
		}
		int up = ps.executeUpdate();
		//not a valid check as item may been previously removed
		if (up != 1){
			throw new SQLException("Invalid number of row deleted.");
		}
	}
	
	@Override
	protected void processDataUpdate(ChangeLogItem item, HashMap<String, Object> data, Connection c) throws ClassNotFoundException, IOException, SQLException{
		StringBuilder sb = new StringBuilder();
		sb.append("UPDATE " + item.getTableName());
		sb.append("SET ");
		
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
		int cnt = ps.executeUpdate();
		if (cnt != 1){
			throw new SQLException("Invalid number of rows updated.");
		}
	}
	
	@Override
	protected void processDataInsert(ChangeLogItem item, HashMap<String, Object> data, Connection c) throws ClassNotFoundException, IOException, SQLException{
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
