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
package org.wcs.smart.connect.internal.server.replication;

import java.io.IOException;
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
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.internal.Messages;
import org.wcs.smart.connect.model.ChangeLogItem;
import org.wcs.smart.connect.model.ChangeLogItem.Action;
import org.wcs.smart.connect.model.ChangeLogItem.Source;
import org.wcs.smart.connect.model.ConnectSyncHistoryRecord;
import org.wcs.smart.connect.model.ConnectSyncHistoryRecord.Type;
import org.wcs.smart.connect.replication.changelog.ChangeLogDeserializer;
import org.wcs.smart.connect.replication.changelog.ConflictException;
import org.wcs.smart.util.UuidUtils;

/**
 * Derserialize and apply the change log file.
 * 
 */
public class DerbyChangeLogDeserializer extends ChangeLogDeserializer{

	private Long lastUploadRevision = null;
	private ConservationArea ca;
	
	public DerbyChangeLogDeserializer(Path changeLogFile, Path changeLogFilestoreDir, ConservationArea ca) {
		super(changeLogFile, changeLogFilestoreDir);
		this.ca = ca;
	}

	/**
	 * Processes the change log file, updating the database as necessary.
	 */
	@Override
	public void processFile(final Session session) throws Exception{
		session.createSQLQuery("SET CONSTRAINTS ALL DEFERRED").executeUpdate(); //$NON-NLS-1$
		
		ConnectSyncHistoryRecord lastUpload = SyncHistoryManager.INSTANCE.getLastNonErrorSyncRecord(session, ca, Type.UPLOAD);
		lastUploadRevision = lastUpload == null ? -1 : lastUpload.getEndRevision();
		
		super.processFile(session);
	}
	
	@SuppressWarnings("unchecked")
	public boolean shouldProcess(ChangeLogItem it, Path changeLogPackage) throws ConflictException{
		if (ChangeLogTableManager.INSTANCE.constains(session, it)){
			//we already have this item
			return false;
		}

		try{
			//conflict checking deleting filestore items
			if (it.getAction() == Action.FS_DELETE){
				Criteria c = session.createCriteria(ChangeLogItem.class);
				c.add(Restrictions.eq("fileName", it.getFileName())); //$NON-NLS-1$
				c.add(Restrictions.gt("revision", lastUploadRevision)); //$NON-NLS-1$
				c.add(Restrictions.eq("source", Source.LOCAL)); //$NON-NLS-1$
				List<ChangeLogItem> changes = c.list();
				
				//if there is anything other than a delete throw a conflict exception
				//if we both deleted the same thing we will not throw a conflict
				for (ChangeLogItem i : changes){
					if (i.getAction() != Action.FS_DELETE){
						throw new ConflictExceptionImpl(it);					
					}
				}
				return true;
			}
			
			//conflict checking updating filestore items
			if (it.getAction() == Action.FS_UPDATE){
				//in the logging we do not log updates to directories so this must be a file
				//any any other file modification/addition/deletion should cause a conflict
				Criteria c = session.createCriteria(ChangeLogItem.class);
				c.add(Restrictions.eq("fileName", it.getFileName())); //$NON-NLS-1$
				c.add(Restrictions.gt("revision", lastUploadRevision)); //$NON-NLS-1$
				c.add(Restrictions.eq("source", Source.LOCAL)); //$NON-NLS-1$
				c.setProjection(Projections.rowCount());
				Long cnt = (Long) c.uniqueResult();
				if (cnt > 0){
					throw new ConflictExceptionImpl(it);
				}
				
				return true;
			}
			
			if (it.getAction() ==  Action.FS_INSERT){
				Criteria c = session.createCriteria(ChangeLogItem.class);
				c.add(Restrictions.eq("fileName", it.getFileName())); //$NON-NLS-1$
				c.add(Restrictions.gt("revision", lastUploadRevision)); //$NON-NLS-1$
				c.add(Restrictions.eq("source", Source.LOCAL)); //$NON-NLS-1$
				
				//if we both create the same directory this should not be a conflict
				Path fromPath = changeLogPackage.resolve(it.getFileName());
				if (Files.exists(fromPath)){
					if (Files.isDirectory(fromPath)){
						//if its a directory and all other changes are also create changes
						//we are ok to continue
						List<ChangeLogItem> others = (List<ChangeLogItem>) c.list();
						for (ChangeLogItem o : others){
							if(o.getAction() != Action.FS_INSERT){
								throw new ConflictExceptionImpl(it);
							}
						}
						return true;
					}
				}
				
				//check for conflicts
				c.setProjection(Projections.rowCount());
				Long cnt = (Long) c.uniqueResult();
				if (cnt > 0){
					throw new ConflictExceptionImpl(it);
				}
				
				return true;
			}
		}catch (ConflictException i){
			if (!it.getFileName().endsWith(".qix")) throw i; //$NON-NLS-1$
			if (it.getAction() != Action.FS_UPDATE) throw i; 

			//this are udig index files and are likely to be modified
			//only care if updates
			if (it.getAction() ==  Action.FS_UPDATE){
				ConnectPlugIn.log("The qix file is has been modified on client and server causing potential conflict.  This conflict is ignored and file is overwriteen as this is likely a shp index file.", i);
				//do the action anyways
			}
			
		}

		// Conflict checking data records
		Criteria c = session.createCriteria(ChangeLogItem.class);
		c.add(Restrictions.eq("tableName", it.getTableName())); //$NON-NLS-1$
		c.add(Restrictions.eq("conservationArea", it.getConservationArea())); //$NON-NLS-1$
		if (it.getFieldName1() != null){
			c.add(Restrictions.eq("fieldName1", it.getFieldName1())); //$NON-NLS-1$
			c.add(Restrictions.eq("key1", it.getKey1())); //$NON-NLS-1$
		}
		if (it.getFieldName2() != null){
			c.add(Restrictions.eq("fieldName2", it.getFieldName2())); //$NON-NLS-1$
			if (it.getKey2() != null){
				c.add(Restrictions.eq("key2", it.getKey2())); //$NON-NLS-1$
				c.add(Restrictions.isNull("key2String")); //$NON-NLS-1$
			}else if (it.getKey2String() != null){
				c.add(Restrictions.eq("key2String", it.getKey2String())); //$NON-NLS-1$
				c.add(Restrictions.isNull("key2")); //$NON-NLS-1$
			}else{
				//this case should not happen
				throw new IllegalStateException("Invalid change log record.  Second key table provide but value not set"); //$NON-NLS-1$
			}
		}
		c.add(Restrictions.gt("revision", lastUploadRevision)); //$NON-NLS-1$
		c.add(Restrictions.eq("source", Source.LOCAL)); //$NON-NLS-1$

		c.setProjection(Projections.rowCount());
		Long cnt = (Long) c.uniqueResult();
		if (cnt > 0){
			throw new ConflictExceptionImpl(it);
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
	protected void processFileInsert(ChangeLogItem item, Path packageFilestoreDir, Connection c)
			throws Exception {
		Path toPath = FileSystems.getDefault().getPath(SmartContext.INSTANCE.getFilestoreLocation(), item.getFileName());
		Path fromPath = packageFilestoreDir.resolve(item.getFileName());
		if (!Files.exists(fromPath)){
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
	protected void processFileUpdate(ChangeLogItem item, Path packageFilestoreDir, Connection c)
			throws Exception {
		//same as insert
		processFileInsert(item, packageFilestoreDir, c);
	}

	@Override
	protected void saveItem(ChangeLogItem item, Session s) throws Exception {
		//it is not necessary to save this change;  there is no way we can download it twice
		//ChangeLogTableManager.INSTANCE.addItem(s, item);
	}
	
	@Override
	protected void processDataDelete(ChangeLogItem item, Connection c) throws SQLException{
		
		StringBuilder sb = new StringBuilder();
		sb.append("DELETE FROM " + item.getTableName()); //$NON-NLS-1$
		sb.append(" WHERE " + item.getFieldName1() + " = ?"); //$NON-NLS-1$ //$NON-NLS-2$
		if (item.getFieldName2() != null){
			sb.append(" AND " + item.getFieldName2()  + " = ?"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		PreparedStatement ps = c.prepareStatement(sb.toString());
		ps.setBytes(1, UuidUtils.uuidToByte(item.getKey1()));
		if (item.getKey2() != null){
			ps.setBytes(2, UuidUtils.uuidToByte(item.getKey2()));	
		}else if (item.getKey2String() != null){
			ps.setString(2, item.getKey2String());
		}
		ps.executeUpdate();
	}
	
	@Override
	protected void processDataUpdate(ChangeLogItem item, HashMap<String, Object> data, Connection c) throws ClassNotFoundException, IOException, SQLException{
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
			if (params.get(i-1) instanceof UUID){
				ps.setBytes(i,  UuidUtils.uuidToByte(((UUID)params.get(i-1))) );
			}else{
				ps.setObject(i, params.get(i-1));
			}
		}
		ps.setObject(params.size() + 1, UuidUtils.uuidToByte(item.getKey1()));
		if (item.getKey2() != null){
			ps.setObject(params.size() + 2, UuidUtils.uuidToByte(item.getKey2()));
		}else if (item.getKey2String() != null){
			ps.setString(params.size() + 2, item.getKey2String());
		}
		
		int cnt = ps.executeUpdate();
		if (cnt != 1){
			throw new SQLException(Messages.DerbyChangeLogDeserializer_InvalidNumberOfRows);
		}
	}
	
	@Override
	protected void processDataInsert(ChangeLogItem item, HashMap<String, Object> data, Connection c) throws ClassNotFoundException, IOException, SQLException{
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
			if (params.get(i-1) instanceof UUID){
				ps.setBytes(i,  UuidUtils.uuidToByte(((UUID)params.get(i-1))) );
			}else{
				ps.setObject(i, params.get(i-1));
			}
		}
		int cnt = ps.executeUpdate();
		if (cnt != 1){
			throw new SQLException(Messages.DerbyChangeLogDeserializer_InvalidNumberOfRows);
		}
	}
}
