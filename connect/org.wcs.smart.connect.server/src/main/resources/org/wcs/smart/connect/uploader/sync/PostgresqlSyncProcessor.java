package org.wcs.smart.connect.uploader.sync;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.connect.ZipUtil;
import org.wcs.smart.connect.model.ConservationAreaInfo;
import org.wcs.smart.connect.replication.changelog.ChangeLogItem;
import org.wcs.smart.connect.replication.changelog.ChangeLogItem.Action;
import org.wcs.smart.connect.uploader.PackageMetadata;
import org.wcs.smart.util.UuidUtils;

public class PostgresqlSyncProcessor {
	
	private Path zipFile;
	private Session session;
	private ConservationAreaInfo info;
	private PackageMetadata metadata;
	
	public PostgresqlSyncProcessor(Path file, ConservationAreaInfo info, Session session){
		this.zipFile = file;
		this.info = info;
		this.session = session;
	}
	
	public void processFile() throws Exception{
		//create a temporary location to unzip file
		Path tempDir = ZipUtil.createTemporaryDirectory().toPath();		
		try{
			//unzip file
			ZipUtil.unzipFolder(zipFile.toFile(), tempDir.toFile());

			Path metadataFile = null;
			Path changeLogFile = null;
			try(DirectoryStream<Path> files = Files.newDirectoryStream(tempDir)){
				for (Path file : files){
					if (file.getFileName().toString().endsWith(".changelog.metadata")){
						metadataFile = file;
					}else if (file.getFileName().toString().endsWith(".changelog")){
						changeLogFile = file;
					}
				}
			}
			if (metadataFile == null){
				throw new Exception("Invalid sync package, no metadata file provided.");
			}
			if (changeLogFile == null){
				throw new Exception("Invalid sync package, no change log file provided.");
			}
			//check metadata
			metadata = PackageMetadata.readMeadata(metadataFile);
			//check ca
			if (!info.getUuid().equals(metadata.getConservationArea())){
				throw new Exception("Conservation area uuids do not match");
			}
			//check version
			if (!info.getVersion().equals(metadata.getVersion())){
				throw new Exception("Conservation area versions do not match");
			}
			
			//check revision
			long serverRevision = ChangeLogManager.INSTANCE.getLastRevision(session, info.getUuid());
			if (metadata.getServerRevision() > serverRevision ){
				throw new Exception("Invalid server revision.  Cannot sync package");
			}
			if (metadata.getServerRevision() < serverRevision){
				throw new Exception("Local copy not up-to-date.  You must download and apply changes from the server before you can upload your changes.");
			}
			
			
			//check plugin version
			Query q = session.createSQLQuery("SELECT plugin_id, version FROM connect.connect_plugin_version");
			List<Object[]> plugins = q.list();
			HashMap<String, String> dbVersions = new HashMap<String, String>();
			for (Object[] plugin : plugins){
				dbVersions.put((String)plugin[0], (String)plugin[1]);
			}
			for(String pluginid : metadata.getPlugins()){
				String version = metadata.getPluginVersion(pluginid);
				String dbVersion = dbVersions.get(pluginid);
				if (dbVersion == null){
					throw new Exception("The connect server does not have the plugin " + pluginid + " installed. You cannot sync without this plugin installed.");
				}
				if (!version.equals(dbVersion)){
					throw new Exception("The connect server has different version for plugin " + pluginid + ". (server: " + dbVersion + " / client:" + version);
				}
			}
			
			
			//apply change log
			applyChangeLog(changeLogFile);
		
		}finally{
			FileUtils.deleteDirectory(tempDir.toFile());
		}
	}
	
	private void applyChangeLog(final Path file) throws Exception{
		session.doWork(new Work() {
			@Override
			public void execute(Connection connection) throws SQLException {
				
				try(InputStream fin = Files.newInputStream(file);
						ObjectInputStream oin = new ObjectInputStream(fin)){
					int size = oin.readInt();
				
					for (int i = 0; i < size; i ++){
						ChangeLogItem it = (ChangeLogItem) oin.readObject();
	
						if (it.getAction() == Action.DELETE){
							processDelete(it, connection);
						}else if (it.getAction() == Action.INSERT){
							processInsert(it, oin, connection);
						}else if (it.getAction() == Action.UPDATE){
							processUpdate(it, oin, connection);
						}else{
							//filestore type do nothing with database
						}
						
						ChangeLogManager.INSTANCE.insertItem(session, it);
					}
				}catch (Exception ex){
					throw new SQLException (ex);
				}
			}
		});
	}
	
	
	private void processDelete(ChangeLogItem item, Connection c) throws Exception{
		
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
	
	private void processUpdate(ChangeLogItem item, ObjectInputStream is, Connection c) throws Exception{
		StringBuilder sb = new StringBuilder();
		sb.append("UPDATE " + item.getTableName());
		sb.append(" SET ");
		
		int numCols = is.readInt();
		List<Object> params = new ArrayList<Object>();
		for (int i = 0; i < numCols; i ++){
			String name = (String) is.readObject();
			int type = is.readInt();
			sb.append(name + " = ?, ");
			
			if (type == Types.BLOB){
				long length = is.readLong();
				byte[] data = new byte[(int)length];
				is.readFully(data);
				params.add(data);
			}else if (type == Types.BINARY){
				//TODO: I don't think we can guarentee a binary is a uuid
				byte[] uuid = (byte[]) is.readObject();
				params.add(UuidUtils.byteToUUID(uuid));
			}else{
				params.add(is.readObject());
			}
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
	
	private void processInsert(ChangeLogItem item, ObjectInputStream is, Connection c) throws Exception{
		StringBuilder sb = new StringBuilder();
		sb.append("INSERT INTO " + item.getTableName() + "(");
		
		StringBuilder values = new StringBuilder();
		values.append("VALUES(");
		
		int numCols = is.readInt();
		List<Object> params = new ArrayList<Object>();
		for (int i = 0; i < numCols; i ++){
			String name = (String) is.readObject();
			int type = is.readInt();
			sb.append(name + ",");
			values.append("?,");
			
			if (type == Types.BLOB){
				long length = is.readLong();
				byte[] data = new byte[(int)length];
				is.readFully(data);
				params.add(data);
			}else if (type == Types.BINARY){
				//TODO: I don't think we can guarentee a binary is a uuid
				byte[] uuid = (byte[]) is.readObject();
				params.add(UuidUtils.byteToUUID(uuid));
			}else{
				params.add(is.readObject());
			}
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
