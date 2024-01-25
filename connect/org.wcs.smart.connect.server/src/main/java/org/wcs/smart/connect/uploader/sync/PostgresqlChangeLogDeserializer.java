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
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent.Kind;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.hibernate.Session;
import org.wcs.smart.SmartContext;
import org.wcs.smart.connect.model.ChangeLogItem;
import org.wcs.smart.connect.model.ConservationAreaInfo;
import org.wcs.smart.connect.replication.changelog.ChangeLogDeserializer;

/**
 * Postgresql specific change log deserializer. Deserializes a change log
 * file and applies each change to the database.
 * 
 * @author Emily
 *
 */
public class PostgresqlChangeLogDeserializer extends ChangeLogDeserializer {

	private List<ChangeLogItem> newItems;
	private ConservationAreaInfo ca;
	
	
	private List<Object[]> fileEvents;
	
	
	public PostgresqlChangeLogDeserializer(ConservationAreaInfo ca, Path changeLogFile, Path changeLogFilestore) {
		super(changeLogFile, changeLogFilestore);
		newItems =  new ArrayList<>();
		this.ca = ca;
		this.fileEvents = new ArrayList<>();
	}

	@Override
	public void processFile(final Session session) throws Exception{
		session.createNativeMutationQuery("SET CONSTRAINTS ALL DEFERRED").executeUpdate(); //$NON-NLS-1$
		super.processFile(session);

		
		//wait until all filestore events have been processed  
		//if we don't do this there is a possibility the filestore 
		//watcher get re-enabled for the CA before the events are processing
		//causing the event to be logged as a "new change to the file"
		//which leads to conflicts with syncing 
		//wait a maximum of 1 second for each file so we don't end up
		//locking
		for (Object[] events : fileEvents) {
			int cnt = 0;
			while(cnt < 10 &&
					!ChangeLogManager.INSTANCE.checkStatus(ca, (Path)events[0], (Kind<?>)events[1])) {				
				Thread.sleep(100);
				cnt ++;
			}
			if (cnt >= 10) {
				Logger.getLogger(PostgresqlChangeLogDeserializer.class.getName())
					.log(Level.WARNING, "Waited longer than 1 second for file event to occurr."); //$NON-NLS-1$

			}
		}
		
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
		//add to list of events
		fileEvents.add(new Object[] {toPath, StandardWatchEventKinds.ENTRY_DELETE});		
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

			Files.move(fromPath, toPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			//add to list of events
			fileEvents.add(new Object[] {toPath, StandardWatchEventKinds.ENTRY_CREATE});			
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
		//add item to list of changes; we will write these changes later
		//after we turn back on track changes
		newItems.add(item);
		
	}

	/**
	 * Writes all changes to change log table.
	 * 
	 * @param s
	 */
	public void writeToChangeLog(Session s) {
		for (ChangeLogItem i  : newItems) ChangeLogManager.INSTANCE.insertItem(s, i);
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
			sb.append(colName + " = "); //$NON-NLS-1$
			if (obj instanceof LocalDateTime ){
				//do this to avoid time zone issues
				DateTimeFormatter sdf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"); //$NON-NLS-1$
				params.add(sdf.format((LocalDateTime)obj));
				sb.append("cast(? as timestamp), "); //$NON-NLS-1$
			}else if (obj instanceof LocalTime) {
				DateTimeFormatter sdf = DateTimeFormatter.ofPattern("HH:mm:ss.SSS"); //$NON-NLS-1$
				params.add(sdf.format((LocalTime)obj));
				sb.append("cast(? as time without time zone),"); //$NON-NLS-1$
			}else{
				sb.append("?, "); //$NON-NLS-1$
				params.add(obj);
			}
		}
		sb.deleteCharAt(sb.length() - 1);
		sb.deleteCharAt(sb.length() - 1);
		
		sb.append(" WHERE " + item.getFieldName1() + " = ?"); //$NON-NLS-1$ //$NON-NLS-2$
		if (item.getFieldName2() != null){
			sb.append(" AND " + item.getFieldName2()  + " = ?"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		//System.out.println(sb.toString());
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
			
			if (obj instanceof LocalDateTime ){
				//do this to avoid time zone issues
				DateTimeFormatter sdf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"); //$NON-NLS-1$
				params.add(sdf.format((LocalDateTime)obj));
				values.append("cast(? as timestamp),"); //$NON-NLS-1$
			}else if (obj instanceof LocalTime) {
				DateTimeFormatter sdf = DateTimeFormatter.ofPattern("HH:mm:ss.SSS"); //$NON-NLS-1$
				params.add(sdf.format((LocalTime)obj));
				values.append("cast(? as time without time zone),"); //$NON-NLS-1$
			}else{
				values.append("?,"); //$NON-NLS-1$
				params.add(obj);
			}
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
