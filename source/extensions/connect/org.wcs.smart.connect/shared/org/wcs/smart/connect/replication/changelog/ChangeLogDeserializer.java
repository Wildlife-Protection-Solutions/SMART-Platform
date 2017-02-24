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

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.UUID;

import org.hibernate.Session;
import org.hibernate.exception.GenericJDBCException;
import org.hibernate.jdbc.Work;
import org.wcs.smart.connect.model.ChangeLogItem;
import org.wcs.smart.connect.model.ChangeLogItem.Action;
import org.wcs.smart.connect.model.ChangeLogItem.Source;

/**
 * Class for deserializing a change log.
 * 
 * @author Emily
 *
 */
public abstract class ChangeLogDeserializer {

	private SimpleDateFormat dateFormatter = new SimpleDateFormat(ChangeLogItemSerializer.DATE_FORMAT_STR); 
	
	protected Path changeLogFile;
	protected Path changeLogFilestoreDir;
	protected Session session;
	
	public ChangeLogDeserializer(Path changeLogFile, Path changeLogFilestoreDir){
		this.changeLogFile = changeLogFile;
		this.changeLogFilestoreDir = changeLogFilestoreDir;
	}
	
	
	public void processFile(final Session session) throws Exception{
		try{
		this.session = session;
		session.doWork(new Work() {
			@Override
			public void execute(Connection connection) throws SQLException {
				
				try(InputStream fin = Files.newInputStream(changeLogFile);
						ObjectInputStream oin = new ObjectInputStream(fin)){
					
					int size = oin.readInt();
					
					for (int i = 0; i < size; i ++){
						ChangeLogItem it = (ChangeLogItem) oin.readObject();
	
						if (!shouldProcess(it, changeLogFilestoreDir)){
							if (it.getAction() == Action.INSERT ||
									it.getAction() == Action.UPDATE){
								//read the remaining data and ignore
								readObject(oin);
							}
							continue;
						}
						
						if (it.getAction() == Action.DELETE){
							processDataDelete(it, connection);
						}else if (it.getAction() == Action.INSERT){
							processDataInsert(it, readObject(oin), connection);
						}else if (it.getAction() == Action.UPDATE){
							processDataUpdate(it, readObject(oin), connection);
						}else if(it.getAction() == Action.FS_DELETE){
							processFileDelete(it, connection);
						}else if(it.getAction() == Action.FS_UPDATE){
							processFileUpdate(it, changeLogFilestoreDir, connection);
						}else if(it.getAction() == Action.FS_INSERT){
							processFileInsert(it, changeLogFilestoreDir, connection);
						}
						it.setSource(Source.SERVER);
						saveItem(it, session);
					}
				}catch (Exception ex){
					throw new SQLException (ex);
				}
			}
		});
		}catch(GenericJDBCException ex){
			//try to find the originating exception
			Throwable t = ex.getCause();
			if (t instanceof SQLException){
				if (t.getCause() instanceof ConflictException){
					throw (ConflictException)t.getCause();
				}
				throw (SQLException)t;
			}
			throw ex;
		}
	}
	
	protected abstract boolean shouldProcess(ChangeLogItem item,  Path changeLogPackage) throws ConflictException;

	
	private HashMap<String, Object> readObject(ObjectInputStream is) throws Exception{
		int numCols = is.readInt();
		HashMap<String, Object> data = new HashMap<String, Object>();
	
		for (int i = 0; i < numCols; i ++){
			String colName = (String) is.readObject();
			int type = is.readInt();
		
			if (type == Types.BLOB ||
					type == Types.BINARY){
				//both (blob and clob) of these are going to cause problems if the 
				//length is greater than integer max value
				long length = is.readLong();
				if (length == 0){
					data.put(colName, null);
				}else{
					byte[] bytes = new byte[(int)length];
					is.readFully(bytes);
					data.put(colName, bytes);
				}
			}else if (type == Types.CLOB){
				long length = is.readLong();
				byte[] cdata = new byte[(int)length];
				is.readFully(cdata);
				data.put(colName, new String(cdata));
			}else if (type == Types.DATE){
				//serialization of dates does not include timezone which causes a problem when deserializing as
				//it assumes the jvms default timezone which causes dates to get shifted
				//so we will serialize and deserialize dates a strings
				Object x = is.readObject();
				if( x == null){
					data.put(colName, null);	
				}else{
					data.put(colName, new java.sql.Date(dateFormatter.parse((String)x).getTime()));
				}
			}else if (type == Types.OTHER){
				//uuid
				UUID uuid = (UUID) is.readObject();
				data.put(colName, uuid);
			}else{
				data.put(colName, is.readObject());
			}
		}
		return data;
	}
	
	protected abstract void saveItem(ChangeLogItem item, Session session) throws Exception;

	protected abstract void processDataDelete(ChangeLogItem item, Connection c) throws Exception;
	
	protected abstract void processDataUpdate(ChangeLogItem item, HashMap<String, Object> data, Connection c) throws Exception;
		
	protected abstract void processDataInsert(ChangeLogItem item, HashMap<String, Object> data, Connection c) throws Exception;
	
	protected abstract void processFileDelete(ChangeLogItem item, Connection c) throws Exception;
	
	protected abstract void processFileUpdate(ChangeLogItem item, Path packageFilestoreDir, Connection c) throws Exception;
		
	protected abstract void processFileInsert(ChangeLogItem item, Path packageFilestoreDir, Connection c) throws Exception;
}
