package org.wcs.smart.connect.uploader.sync;

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.connect.ZipUtil;
import org.wcs.smart.connect.model.ConservationAreaInfo;
import org.wcs.smart.connect.uploader.PackageMetadata;

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
		PostgresqlChangeLogDeserializer processor = new PostgresqlChangeLogDeserializer(file);
		processor.processFile(session);
	}
	
}
