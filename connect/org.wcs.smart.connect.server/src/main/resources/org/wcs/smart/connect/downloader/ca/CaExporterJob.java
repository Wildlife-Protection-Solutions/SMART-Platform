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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.export.ICaDataExportEngine;
import org.wcs.smart.connect.ZipUtil;
import org.wcs.smart.connect.database.LockManager;
import org.wcs.smart.connect.datastore.DataStoreManager;
import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.connect.model.ConservationAreaInfo;
import org.wcs.smart.connect.model.WorkItem;
import org.wcs.smart.connect.model.WorkItem.Status;
import org.wcs.smart.connect.uploader.sync.ChangeLogManager;
import org.wcs.smart.util.UuidUtils;

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


	@Override
	public void run() {
		Session s = factory.openSession();
		
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
			
				destFile = DataStoreManager.INSTANCE
					.getRootDirectory().toPath()
					.resolve(DataStoreManager.CA_EXPORT_LOCATION)
					.resolve(filename);
				if (!Files.exists(destFile.getParent())){
					Files.createDirectories(destFile.getParent());
				}
				
				item.setLocalFilename(DataStoreManager.INSTANCE.getRootDirectory().toPath().relativize(destFile).toString());
				
				if (Files.exists(destFile)){
					
					item.setStatus(Status.COMPLETE);
					item.setMessage("{\"file_url\": " + "\"" + fileurl + "\"}"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					s.saveOrUpdate(item);
					s.getTransaction().commit();
				}else{
					try{
						export(s, info.getUuid(), destFile);
						item.setStatus(Status.COMPLETE);
						item.setMessage("{\"file_url\": " + "\"" + fileurl + "\"}"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						s.saveOrUpdate(item);
						s.getTransaction().commit();
					}catch (Exception ex){
						logger.log(Level.SEVERE, "Error exporting Conservation Area data. " + ex.getMessage(), ex); //$NON-NLS-1$
						
						//open a new transaction to update db state
						s.getTransaction().rollback();
						s.beginTransaction();
						s.saveOrUpdate(item);
						item.setStatus(Status.ERROR);
						item.setMessage("{\"error\": \"" + MessageFormat.format(Messages.getString("CaExporterJob.caExportError", item.getLocale()), ex.getMessage()) + "\"}"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
			if (s.isOpen())	s.close();
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
		
		Path tempDir = new File(DataStoreManager.INSTANCE.getTemporaryDirectory().getAbsoluteFile(), System.nanoTime()+"").toPath(); //$NON-NLS-1$
		if (!Files.exists(tempDir)){
			Files.createDirectories(tempDir);
		}
		try{
			//export ca
			ICaDataExportEngine engine = new PostgresqlCaDataExportEngine(tempDir.toFile(), ca, session);
			(new PostgresqlExporters()).exportAll(engine);
			
			//zip
			ZipUtil.createZip(tempDir.toFile().listFiles(), destFile.toFile());
		}finally{
			FileUtils.deleteDirectory(tempDir.toFile());
		}
	
	}
}
