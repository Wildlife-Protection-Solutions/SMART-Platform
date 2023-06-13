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

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.hibernate.Session;
import org.hibernate.query.NativeQuery;
import org.wcs.smart.connect.ZipUtil;
import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.connect.model.ConnectSyncHistoryRecord;
import org.wcs.smart.connect.model.ConservationAreaInfo;
import org.wcs.smart.connect.model.WorkItem;
import org.wcs.smart.connect.replication.metadata.MetadataPackager;
import org.wcs.smart.connect.replication.metadata.PackageMetadata;
import org.wcs.smart.connect.uploader.ca.CaProcessorUtils;
import org.wcs.smart.util.ZipUtilCommon;

import jakarta.persistence.Tuple;


/**
 * A postgresql specific processor for processing a change
 * log file.  This unzips the change log package, validates the
 * metadata, applies the changes and returns. 
 *  
 * @author Emily
 *
 */
public class PostgresqlSyncProcessor {
	
	private final Logger logger = Logger.getLogger(PostgresqlSyncProcessor.class.getName());
	
	private Path zipFile;
	private Session session;
	private ConservationAreaInfo info;
	private PackageMetadata metadata;
	private WorkItem item;
	
	public PostgresqlSyncProcessor(Path file, ConservationAreaInfo info, Session session, WorkItem item){
		this.zipFile = file;
		this.info = info;
		this.session = session;
		this.item = item;
	}
	
	public void processFile() throws Exception{
		
		//create a temporary location to unzip file
		Path tempDir = ZipUtil.createTemporaryDirectory();		
		try{
			//unzip file
			ZipUtilCommon.unzipFolder(zipFile, tempDir);

			Path metadataFile = null;
			Path changeLogFile = null;
			Path filestoreDir = tempDir.resolve(ConnectSyncHistoryRecord.PACKAGE_FILESTORE_DIR);
			try(DirectoryStream<Path> files = Files.newDirectoryStream(tempDir)){
				for (Path file : files){
					if (file.getFileName().toString().endsWith(".changelog.metadata")){ //$NON-NLS-1$
						metadataFile = file;
					}else if (file.getFileName().toString().endsWith(".changelog")){ //$NON-NLS-1$
						changeLogFile = file;
					}
				}
			}
			if (metadataFile == null){
				throw new Exception(Messages.getString("PostgresqlSyncProcessor_NoMetadataFile", item.getLocale())); //$NON-NLS-1$
			}
			if (changeLogFile == null){
				throw new Exception(Messages.getString("PostgresqlSyncProcessor_NoChangeLogFile", item.getLocale())); //$NON-NLS-1$
			}
			//check metadata
			metadata = MetadataPackager.INSTANCE.readMetadata(metadataFile);
			//check ca
			if (!info.getUuid().equals(metadata.getConservationArea())){
				throw new Exception(Messages.getString("PostgresqlSyncProcessor_CaUuidError", item.getLocale())); //$NON-NLS-1$
			}
			//check version
			if (!info.getVersion().equals(metadata.getVersion())){
				throw new Exception(Messages.getString("PostgresqlSyncProcessor_CaVersionError", item.getLocale())); //$NON-NLS-1$
			}
			
			//check revision
			long serverRevision = ChangeLogManager.INSTANCE.getLastRevision(session, info.getUuid());
			if (metadata.getServerRevision() > serverRevision ){
				throw new Exception(Messages.getString("PostgresqlSyncProcessor_InvalidServerRevision", item.getLocale())); //$NON-NLS-1$
			}
			if (metadata.getServerRevision() < serverRevision){
				throw new Exception(Messages.getString("PostgresqlSyncProcessor_LocalCopyNotUpToDate", item.getLocale())); //$NON-NLS-1$
			}
			
			
			//check plugin version
			NativeQuery<Tuple> q = session.createNativeQuery("SELECT plugin_id, version FROM connect.connect_plugin_version", Tuple.class); //$NON-NLS-1$
			List<Tuple> plugins = q.list();
			HashMap<String, String> dbVersions = new HashMap<String, String>();
			for (Tuple plugin : plugins){
				dbVersions.put((String)plugin.get(0), (String)plugin.get(1));
			}
			
			for(String pluginid : metadata.getPluginVersions().keySet()){
				String version = metadata.getPluginVersion(pluginid);
				String dbVersion = dbVersions.get(pluginid);
				if (dbVersion == null){
					throw new Exception(MessageFormat.format(Messages.getString("PostgresqlSyncProcessor.MissingPlugin", item.getLocale()), pluginid)); //$NON-NLS-1$
				}
				if (!version.equals(dbVersion)){
					throw new Exception(MessageFormat.format(Messages.getString("PostgresqlSyncProcessor.InvalidPluginVersion", item.getLocale()), pluginid, dbVersion, version)); //$NON-NLS-1$
				}
			}
			
			applyChangeLog(changeLogFile, filestoreDir);

			//update info label
			CaProcessorUtils.updateCaLabel(session, info);
		}finally{
			try {
				FileUtils.deleteDirectory(tempDir.toFile());
			}catch (Exception ex) {
				logger.log(Level.SEVERE, ex.getMessage(), ex);
			}
		}
	}

	private void applyChangeLog(Path changelogFile, Path changelogFilestore) throws Exception{
		PostgresqlChangeLogDeserializer processor = new PostgresqlChangeLogDeserializer(changelogFile, changelogFilestore);
		
		//disable writing to change log table; all changes written to tables will
		//not be written to change log table. We will write those changes once
		//we turn back on the triggers
		try {
			//we need to lock the entire database here as this disables all triggers and at this point
			//only the Conservation Area is locked
			ChangeLogManager.INSTANCE.disableChangeTracking(info, session);
			//apply change log
			processor.processFile(session);
			ChangeLogManager.INSTANCE.enableChangeTracking(info, session);
		}catch (Exception ex) {
			session.getTransaction().rollback();
			session.beginTransaction();
			try {
				ChangeLogManager.INSTANCE.enableChangeTracking(info, session);	
			}catch (Exception ex2) {
				logger.log(Level.SEVERE, ex2.getMessage(), ex2);	
			}
			throw ex;
		}
		
		//write all change log 
		processor.writeToChangeLog(session);
	}

}
