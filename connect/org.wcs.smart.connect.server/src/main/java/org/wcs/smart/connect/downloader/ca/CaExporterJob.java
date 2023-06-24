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
package org.wcs.smart.connect.downloader.ca;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.codec.digest.DigestUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.export.ICaDataExportEngine;
import org.wcs.smart.connect.database.LockManager;
import org.wcs.smart.connect.datastore.DataStoreManager;
import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.connect.model.ConservationAreaInfo;
import org.wcs.smart.connect.model.WorkItem;
import org.wcs.smart.connect.model.WorkItem.Status;
import org.wcs.smart.connect.model.WorkItem.Type;
import org.wcs.smart.connect.uploader.sync.ChangeLogManager;
import org.wcs.smart.util.UuidUtils;

import jakarta.xml.bind.DatatypeConverter;

/**
 * Main process for exporting a conservation area.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class CaExporterJob implements Runnable {
	private final Logger logger = Logger.getLogger(CaExporterJob.class.getName());
	
	private ConservationAreaInfo info = null;
	private WorkItem item = null;
	private SessionFactory factory;
	private Path destFile = null;
	private String fileurl;
	
	public CaExporterJob(ConservationAreaInfo info, 
			WorkItem item, 
			String fileurl,
			SessionFactory factory){
		this.info = info;
		this.item = item;
		this.fileurl = fileurl;
		this.factory = factory;
	}

	private void updateItemStatus(String messagee, int percentComplete) {
		try (Session s = factory.openSession()){
			try {
				s.beginTransaction();
				item.setMessage(messagee);
				item.setPercentComplete(percentComplete);
				s.merge(item);
				s.getTransaction().commit();
			}catch (Exception ex) {
				if (s.getTransaction().isActive()) s.getTransaction().rollback();
				logger.log(Level.WARNING, ex.getMessage());
			}
		}
	}

	@Override
	public void run() {
		try(Session s = factory.openSession()){
			
			try{
				//lock database for conservation area
				LockManager.INSTANCE.lockDatabase(s, item.getConservationAreaInfo());
			}catch (Exception ex){
				logger.log(Level.SEVERE, "Could not lock database to create Conservation Area download package. " + item.getUuid()); //$NON-NLS-1$
				
				//set error status
				s.beginTransaction();
				item.setStatus(org.wcs.smart.connect.model.WorkItem.Status.ERROR);
				item.setMessage(MessageFormat.format("Error processing item {0}: {1}.", item.getUuid().toString(), ex.getMessage())); //$NON-NLS-1$
				s.getTransaction().commit();
				
				return;
			}
			
			try{
				s.beginTransaction();
				try{
					long revision = ChangeLogManager.INSTANCE.getLastRevision(s, info.getUuid());
				
					String filename = UuidUtils.uuidToString(info.getUuid())
						 +"." + UuidUtils.uuidToString(info.getVersion())  //$NON-NLS-1$
						 + "." + String.valueOf(revision) //$NON-NLS-1$
						 + ".zip"; //$NON-NLS-1$
				
					if (item.getType() == Type.RECOVERY_CA) {
						filename = UuidUtils.uuidToString(info.getUuid())
								+ "." + UuidUtils.uuidToString(item.getUuid()) //$NON-NLS-1$
								+ ".recovery.zip"; //$NON-NLS-1$
					}
					
					destFile = DataStoreManager.INSTANCE
						.getRootDirectory()
						.resolve(DataStoreManager.CA_EXPORT_LOCATION)
						.resolve(filename);
					if (!Files.exists(destFile.getParent())){
						Files.createDirectories(destFile.getParent());
					}
					
					item.setLocalFilename(DataStoreManager.INSTANCE.getRootDirectory().relativize(destFile).toString());
					
					if (Files.exists(destFile)){
						
						item.setStatus(Status.COMPLETE);
						item.setMessage("{\"file_url\": " + "\"" + fileurl + "\"}"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						s.merge(item);
						s.getTransaction().commit();
					}else{
						try{
							if (item.getType() == Type.DOWN_CA) {
								export(s, info.getUuid(), destFile);
							}else if (item.getType() == Type.RECOVERY_CA) {
								exportRecoveryPackage(s,  info.getUuid(), destFile, item.getData());
							}else {
								throw new Exception(MessageFormat.format("Work item type {0} not supported.", item.getType())); //$NON-NLS-1$
							}
							item.setStatus(Status.COMPLETE);
							item.setMessage("{\"file_url\": " + "\"" + fileurl + "\"}"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							s.merge(item);
							s.getTransaction().commit();
						}catch (Exception ex){
							logger.log(Level.SEVERE, "Error exporting Conservation Area data. " + ex.getMessage(), ex); //$NON-NLS-1$
							
							//open a new transaction to update db state
							s.getTransaction().rollback();
							
							s.beginTransaction();
							item.setStatus(Status.ERROR);
							item.setMessage("{\"error\": \"" + MessageFormat.format(Messages.getString("CaExporterJob.caExportError", item.getLocale()), ex.getMessage()) + "\"}"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							s.merge(item);
							s.getTransaction().commit();
						}
					}
				}catch (Exception ex){
					logger.log(Level.SEVERE, "Error exporting Conservation Area data. " + ex.getMessage(), ex); //$NON-NLS-1$
				}
			}finally{
				try{
					LockManager.INSTANCE.releaseDatabase(s, item.getConservationAreaInfo());
				}catch (Exception ex){
					logger.log(Level.SEVERE, "Could not release database lock after creating Conservation Area download package. " + item.getUuid()); //$NON-NLS-1$
				}
				
			}
		}
	}
	
	
	/**
	 * Exports the current conservation area to the given file.
	 * @param destFile output file
	 * @param monitor progress monitor
	 * 
	 * 
	 */
	public void export(Session session, UUID caUuid, Path destFile) throws Exception{
	
		ConservationArea ca = (ConservationArea) session.get(ConservationArea.class, caUuid);
		
		ICaDataExportEngine engine = null;
		try{
			//export ca
			engine = new PostgresqlCaDataExportEngine(ca, session);
			(new PostgresqlExporters()).exportAll(engine);
			engine.createExportFile(destFile, null);

		}finally{
			if (engine != null) engine.cleanUp();
		}
	
	}
	
	/**
	 * Exports the current conservation area to the given file.
	 * @param destFile output file
	 * @param monitor progress monitor
	 * 
	 * 
	 */
	public void exportRecoveryPackage(Session session, UUID caUuid, Path destFile, String data) throws Exception{
	
		ConservationArea ca = (ConservationArea) session.get(ConservationArea.class, caUuid);
		
		Path caDir = DataStoreManager.INSTANCE.getConservationAreaFullPath(info);
		
		Set<Path> foundFiles = new HashSet<>();
		
		List<Path> filesToDelete = new ArrayList<>();
		
		updateItemStatus("processing filestore", 1);
		//parse hashes from desktop file
		HashMap<Path, String> filehashes = new HashMap<>();
		Path desktopdata = DataStoreManager.INSTANCE.getRootDirectory().resolve(data);
		try(BufferedReader reader = Files.newBufferedReader(desktopdata, StandardCharsets.UTF_8)){
			String line = null;
			while((line = reader.readLine()) != null) {
				int index = line.lastIndexOf(ICaDataExportEngine.DATA_SEPARATOR);
				String file = line.substring(0, index);
				String hash = line.substring(index+1);
				
				Path thisfile = DataStoreManager.INSTANCE.getConservationAreaFullPath(info).resolve(file);
				filehashes.put(thisfile,  hash);
			}
		}
		updateItemStatus("processing filestore", 3);
		
		final MessageDigest digest = MessageDigest.getInstance(ICaDataExportEngine.RECOVERY_HASH_ALGORITHM);

		Path caRootPath = DataStoreManager.INSTANCE.getConservationAreaFullPath(info);
		Path exportFilestoreDir = Paths.get(ICaDataExportEngine.FILESTORE_DIR);
		
		HashMap<Path, Path> filesToInclude = new HashMap<>();
		
		try(Stream<Path> spath = Files.walk(caDir)){
			List<Path> files = spath.filter(p->!Files.isDirectory(p)).collect(Collectors.toList());
			for (Path p : files) {
				if (filehashes.containsKey(p)) {
					foundFiles.add(p);					
					byte[] hash = DigestUtils.digest(digest, p);
					String hex = DatatypeConverter.printHexBinary(hash);
				
					if (!hex.equals(filehashes.get(p))){
						//hash is different so we want to reset this file				
						filesToInclude.put(p,  exportFilestoreDir.resolve( caRootPath.relativize(p.getParent()) )  );	
					}
				}else {
					//file doesn't exist
					filesToInclude.put(p,  exportFilestoreDir.resolve( caRootPath.relativize(p.getParent()) )  );	
				}					
			}
		}
		for (Path p : filehashes.keySet()) {
			if (!foundFiles.contains(p)) {
				filesToDelete.add(p);
			}
		}
		updateItemStatus("processing filestore", 10);

		ICaDataExportEngine engine = null; 
		try{
			//export ca
			engine = new PostgresqlCaDataExportEngine(ca, session);
			
			//create a file of files to delete
			Path temp = engine.getWorkingLocation().resolve(ICaDataExportEngine.DELETEFILES_FILENAME);
			try(BufferedWriter writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)){
				for (Path p : filesToDelete) {
					writer.write(caDir.relativize(p).toString());
					writer.write(ICaDataExportEngine.FILE_SEPARATOR);
				}
			}
		
			updateItemStatus("exporting database", 50);
			//export the data
			(new PostgresqlExporters()).exportDatabaseOnly(engine);
			
			//add additional files to exporter
			for (Entry<Path, Path> p : filesToInclude.entrySet()) {
				engine.addPath(p.getKey(), p.getValue());
			}
			
			updateItemStatus("creating export file", 80);
			engine.createExportFile(destFile, null);

			updateItemStatus("export complete", 100);
		}finally{
			if (engine != null) engine.cleanUp();
		}
	
	}
}
