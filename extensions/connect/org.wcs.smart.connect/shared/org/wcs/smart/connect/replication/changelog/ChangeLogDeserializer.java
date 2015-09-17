package org.wcs.smart.connect.replication.changelog;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.UUID;

import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.connect.replication.changelog.ChangeLogItem.Action;
import org.wcs.smart.util.UuidUtils;

public abstract class ChangeLogDeserializer {

	protected Path changeLogFile;
	protected Session session;
	
	public ChangeLogDeserializer(Path changeLogFile){
		this.changeLogFile = changeLogFile;
	}
	
	
	public void processFile(final Session session) throws Exception{
		this.session = session;
		session.doWork(new Work() {
			@Override
			public void execute(Connection connection) throws SQLException {
				
				try(InputStream fin = Files.newInputStream(changeLogFile);
						ObjectInputStream oin = new ObjectInputStream(fin)){
					
					int size = oin.readInt();
				
					for (int i = 0; i < size; i ++){
						ChangeLogItem it = (ChangeLogItem) oin.readObject();
	
						if (!shouldProcess(it)){
							if (it.getAction() == Action.INSERT ||
									it.getAction() == Action.UPDATE){
								//read the remaining data and ingore
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
							processFileUpdate(it, connection);
						}else if(it.getAction() == Action.FS_INSERT){
							processFileInsert(it, connection);
						}
						saveItem(it, session);
					}
				}catch (Exception ex){
					throw new SQLException (ex);
				}
			}
		});
	}
	
	protected abstract boolean shouldProcess(ChangeLogItem item);

	private HashMap<String, Object> readObject(ObjectInputStream is) throws Exception{
		int numCols = is.readInt();
		HashMap<String, Object> data = new HashMap<String, Object>();
	
		for (int i = 0; i < numCols; i ++){
			String colName = (String) is.readObject();
			int type = is.readInt();
		
			if (type == Types.BLOB ||
					type == Types.BINARY){
				long length = is.readLong();
				byte[] bytes = new byte[(int)length];
				is.readFully(bytes);
				data.put(colName, bytes);
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
	
	protected abstract void processFileUpdate(ChangeLogItem item, Connection c) throws Exception;
		
	protected abstract void processFileInsert(ChangeLogItem item, Connection c) throws Exception;
}
