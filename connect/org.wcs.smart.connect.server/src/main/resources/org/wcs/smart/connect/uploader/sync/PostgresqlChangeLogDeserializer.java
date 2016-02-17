/*
 * Copyright (C) 2015 Wildlife Conservation Society
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
package org.wcs.smart.connect.uploader.sync;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.hibernate.Session;
import org.wcs.smart.SmartContext;
import org.wcs.smart.connect.model.ChangeLogItem;
import org.wcs.smart.connect.replication.changelog.ChangeLogDeserializer;

/**
 * Postgresql specific change log deserializer.  Deserializes a change log
 * file and applies each change to the database.
 * 
 * @author Emily
 *
 */
public class PostgresqlChangeLogDeserializer extends ChangeLogDeserializer {

	public PostgresqlChangeLogDeserializer(Path changeLogFile, Path changeLogFilestore) {
		super(changeLogFile, changeLogFilestore);
	}

	@Override
	public void processFile(final Session session) throws Exception{
		session.createSQLQuery("SET CONSTRAINTS ALL DEFERRED").executeUpdate(); //$NON-NLS-1$
		super.processFile(session);
	}
	
	/**
	 * This just checks for existence of the item already.  The assumption
	 * is that there will no conflicts and the local desktop uploading
	 * changes has already confirmed there should be no conflicts.
	 */
	@Override
	protected boolean shouldProcess(ChangeLogItem it, Path changeLogPackage ) {
		if (ChangeLogManager.INSTANCE.constains(session, it)){
			//we already have this item
			return false;
		}
		return true;
	}
	
	@Override
	protected void processFileDelete(ChangeLogItem item, Connection c)
			throws Exception {
		Path toPath = FileSystems.getDefault().getPath(SmartContext.INSTANCE.getFilestoreLocation(), item.getFileName());
		if (Files.isDirectory(toPath)){
			FileUtils.deleteDirectory(toPath.toFile());
		}else{
			Files.deleteIfExists(toPath);
		}
		
	}

	@Override
	protected void processFileInsert(ChangeLogItem item, Path changeLogFilestore, Connection c)
			throws Exception {
		Path toPath = FileSystems.getDefault().getPath(SmartContext.INSTANCE.getFilestoreLocation(), item.getFileName());
		Path fromPath = changeLogFilestore.resolve(item.getFileName());
		if (!Files.exists(fromPath)){
			//source path doesn't exist; probably deleted last so ignore this
			return;
		}
		if (Files.isDirectory(fromPath)){
			Files.createDirectories(toPath);
		}else{
			//ensure all parent directories are created
			Files.createDirectories(toPath.getParent());
			//delete existing file
			if (!Files.isDirectory(toPath) && Files.exists(toPath)){
				Files.delete(toPath);
			}
			//copy file
			Files.copy(fromPath, toPath);
			
		}
	}

	@Override
	protected void processFileUpdate(ChangeLogItem item, Path changeLogFilestore,  Connection c)
			throws Exception {
		//same as insert
		processFileInsert(item, changeLogFilestore, c);
	}

	@Override
	protected void saveItem(ChangeLogItem item, Session s) throws Exception {
		ChangeLogManager.INSTANCE.insertItem(s, item);
	}

	@Override
	protected void processDataDelete(ChangeLogItem item, Connection c) throws Exception{
		StringBuilder sb = new StringBuilder();
		sb.append("DELETE FROM " + item.getTableName()); //$NON-NLS-1$
		sb.append(" WHERE " + item.getFieldName1() + " = ?"); //$NON-NLS-1$ //$NON-NLS-2$
		if (item.getFieldName2() != null){
			sb.append(" AND " + item.getFieldName2()  + " = ?"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		PreparedStatement ps = c.prepareStatement(sb.toString());
		ps.setObject(1, item.getKey1());
		if (item.getKey2() != null){
			ps.setObject(2, item.getKey2());	
		}else if (item.getKey2String() != null){
			ps.setString(2, item.getKey2String());
		}
		ps.executeUpdate();
	}
	
	@Override
	protected void processDataUpdate(ChangeLogItem item, HashMap<String, Object> data, Connection c) throws Exception{
		StringBuilder sb = new StringBuilder();
		sb.append("UPDATE " + item.getTableName()); //$NON-NLS-1$
		sb.append(" SET "); //$NON-NLS-1$
		
		List<Object> params = new ArrayList<Object>();
		for(Entry<String, Object> dataitem : data.entrySet()){
			String colName = dataitem.getKey();
			Object obj = dataitem.getValue();	
			sb.append(colName + " = ?, "); //$NON-NLS-1$
			params.add(obj);
		}
		sb.deleteCharAt(sb.length() - 1);
		sb.deleteCharAt(sb.length() - 1);
		
		sb.append(" WHERE " + item.getFieldName1() + " = ?"); //$NON-NLS-1$ //$NON-NLS-2$
		if (item.getFieldName2() != null){
			sb.append(" AND " + item.getFieldName2()  + " = ?"); //$NON-NLS-1$ //$NON-NLS-2$
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
			throw new SQLException("Invalid number of rows updated."); //$NON-NLS-1$
		}
	}
	
	@Override
	protected void processDataInsert(ChangeLogItem item, HashMap<String, Object> data,Connection c) throws Exception{
		StringBuilder sb = new StringBuilder();
		sb.append("INSERT INTO " + item.getTableName() + "("); //$NON-NLS-1$ //$NON-NLS-2$
		
		StringBuilder values = new StringBuilder();
		values.append("VALUES("); //$NON-NLS-1$
		
		List<Object> params = new ArrayList<Object>();
		for(Entry<String, Object> dataitem : data.entrySet()){
			String colName = dataitem.getKey();
			Object obj = dataitem.getValue();	
			sb.append(colName + ","); //$NON-NLS-1$
			values.append("?,"); //$NON-NLS-1$
			params.add(obj);
		}
		sb.deleteCharAt(sb.length() - 1);
		values.deleteCharAt(values.length() - 1);
		
		sb.append(") "); //$NON-NLS-1$
		sb.append(values.toString());
		sb.append(")"); //$NON-NLS-1$
		PreparedStatement ps = c.prepareStatement(sb.toString() );
		for (int i = 1; i <= params.size(); i ++){
			ps.setObject(i, params.get(i-1));
		}
		int cnt = ps.executeUpdate();
		if (cnt != 1){
			throw new SQLException("Invalid number of rows updated."); //$NON-NLS-1$
		}
	}
}
