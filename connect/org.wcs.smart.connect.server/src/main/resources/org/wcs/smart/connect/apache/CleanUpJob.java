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
package org.wcs.smart.connect.apache;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.servlet.ServletContext;

import org.apache.commons.io.FileUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.wcs.smart.cipher.EncryptUtils;
import org.wcs.smart.connect.api.DataQueue;
import org.wcs.smart.connect.api.ReportApi;
import org.wcs.smart.connect.api.Uploader;
import org.wcs.smart.connect.api.noa.GlobalForestWatchNoa;
import org.wcs.smart.connect.dataqueue.ServerDataQueueItem;
import org.wcs.smart.connect.datastore.DataStoreManager;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.connect.model.ConservationAreaInfo;
import org.wcs.smart.connect.model.WorkItem;
import org.wcs.smart.connect.model.WorkItem.Status;
import org.wcs.smart.connect.model.WorkItem.Type;
import org.wcs.smart.connect.uploader.sync.ChangeLogManager;
import org.wcs.smart.hibernate.QueryFactory;

/**
 * Remove items from filestore.
 * 
 * @author Emily
 *
 */
public class CleanUpJob implements Runnable {

	private final Logger logger = Logger.getLogger(CleanUpJob.class.getName());
		
	private SessionFactory sessionFactory;
	
	private Integer syncDownloadAvailableHrs = null;
	private Integer caExportAvailableDays = null;
	private Integer changeLogCleanUpDays = null;
	private Integer gfwCleanUpDays = null;
	private ServletContext context;
	
	public CleanUpJob(SessionFactory sessionFactory, ServletContext context){
		this.sessionFactory = sessionFactory;
		this.context = context;
	}
	
	@Override
	public void run() {
		logger.log(Level.FINEST, "Running cleanup job: " + (new Date()).toString()); //$NON-NLS-1$
		
		syncDownloadAvailableHrs = getEnvironmentVariable(EnvironmentVariables.Variable.SYNC_DOWNLOAD_AVAILABLE);
		caExportAvailableDays = getEnvironmentVariable(EnvironmentVariables.Variable.CA_EXPORT_AVAILABLE);
		changeLogCleanUpDays = getEnvironmentVariable(EnvironmentVariables.Variable.CHANGELOG_CLEAN_UP_DAYS);
		gfwCleanUpDays = getEnvironmentVariable(EnvironmentVariables.Variable.GFW_CLEAN_UP_DAYS);
		
		try{
			cleanUp();
		}catch (Exception ex){
			logger.log(Level.SEVERE, "Error running cleanup task:" + ex.getMessage(), ex); //$NON-NLS-1$
		}		
	}
	
	private Integer getEnvironmentVariable(EnvironmentVariables.Variable variable){
		Integer value = null;
		try{
			value = (Integer)EnvironmentVariables.INSTANCE.getEnvironmentVariable(variable);
		}catch (Exception ex){
			logger.log(Level.WARNING, "Value not found for environment variable:" + variable, ex); //$NON-NLS-1$
		}
		return value;
	}
	
	private void cleanUp(){
		try(Session s = sessionFactory.openSession()){
			//list all files in uploads directory			
			Path uploadDir = DataStoreManager.INSTANCE.getRootDirectory().resolve(Uploader.DATASTORE_DIR);
			try(DirectoryStream<Path> stream = Files.newDirectoryStream(uploadDir)){
				for (Path path : stream){
					checkAndDelete(s, path);
				}
			}catch (Exception ex){
				logger.log(Level.WARNING, "Unable to list files in uploads directory for cleaning.", ex); //$NON-NLS-1$
			}
			
			//ca export directory
			Path caExportDirectory = DataStoreManager.INSTANCE.getRootDirectory().resolve(DataStoreManager.CA_EXPORT_LOCATION);
			if (Files.exists(caExportDirectory)) {
				try(DirectoryStream<Path> stream = Files.newDirectoryStream(caExportDirectory)){
					for (Path path : stream){
						checkAndDelete(s, path);
					}
				}catch (Exception ex){
					logger.log(Level.WARNING, "Unable to list files in uploads directory for cleaning.", ex); //$NON-NLS-1$
				}
			}
			
			//tempDir
			try {
				Path tempDir = DataStoreManager.INSTANCE.getTemporaryDirectory();
				try(DirectoryStream<Path> stream = Files.newDirectoryStream(tempDir)){
					for (Path path : stream){
						if (!Files.isDirectory(path)){
							//only check files here as directories as likely being used
							//to process item
							checkAndDelete(s, path);
						}
					}
				}
			}catch (Exception ex){
				logger.log(Level.WARNING, "Unable to list files in uploads directory for cleaning.", ex); //$NON-NLS-1$
			}
			
			//delete any work items
			cleanUpWorkItems(s);
			
			//clean up change log items
			cleanUpChangeLog(s);
			
			//clean up data queue items
			cleanUpDataQueue(s);
			
			//clean up temporary tables
			cleanupQueryTempTables(s);
			
			//clean up gfw files logged
			cleanUpGlobalForestWatchFiles();
		}
		
		cleanReportImages();
		cleanTemporaryFiles();
	}
	
	private void cleanTemporaryFiles() {
		Path uploadDir = DataStoreManager.INSTANCE.getRootDirectory().resolve(EncryptUtils.TEMP_DIR);
		deleteFilesInDir(uploadDir);
	}
	
	private void cleanReportImages() {
		java.nio.file.Path imagesDir = Paths.get(context.getRealPath(ReportApi.REPORT_IMAGES_DIR));
		deleteFilesInDir(imagesDir);
	}
	
	/*
	 * Delete all files in the provided directorys
	 */
	private void deleteFilesInDir(Path dir) {
		if (!Files.exists(dir)) return;
		try(DirectoryStream<Path> stream = Files.newDirectoryStream(dir)){
			for (Path path : stream){
				try{
					if (Files.isDirectory(path)){
						FileUtils.deleteDirectory(path.toFile());
					}else{
						Files.delete(path);
					}
				}catch (Exception ex){
					logger.log(Level.WARNING, "Unable to cleanup file: " + path.toString(), ex); //$NON-NLS-1$
				}
			}
		}catch (Exception ex){
			logger.log(Level.WARNING, "Unable to list files in directory for cleaning.", ex); //$NON-NLS-1$
		}
	}
	
	/*
	 * delete items from the workitem table.
	 */
	private void cleanUpWorkItems(Session s){
		Integer days = null;
		try{
			days = (Integer)EnvironmentVariables.INSTANCE.getEnvironmentVariable(EnvironmentVariables.Variable.WORK_HISTORY_ITEM_AVAILABLE);
		}catch (Exception ex){
			logger.log(Level.WARNING, "Value not found for environment variable:" + EnvironmentVariables.Variable.WORK_HISTORY_ITEM_AVAILABLE.key, ex); //$NON-NLS-1$
		}
		if (days == null || days <= 0) return;
		
		s.beginTransaction();
		try{
			Query<?> q = s.createQuery("DELETE FROM WorkItem where startTime < :starttime"); //$NON-NLS-1$
			
			Date d = new Date((new Date()).getTime() - days * 24l * 60 * 60 *1000);
			q.setParameter("starttime", d); //$NON-NLS-1$
			q.executeUpdate();
			
			s.getTransaction().commit();
		}catch (Exception ex){
			logger.log(Level.WARNING, "Unable to clean up work item table.", ex); //$NON-NLS-1$
			s.getTransaction().rollback();
			
		}
	}
	
	/*
	 * finds the work item related to the given file and deletes
	 * it if the file can be deleted
	 */
	private void checkAndDelete(Session s, Path p){
		String localFilename = DataStoreManager.INSTANCE.getRootDirectory().relativize(p).toString();
		
		List<WorkItem> items = QueryFactory.buildQuery(s, WorkItem.class, "localFilename", localFilename).list(); //$NON-NLS-1$
		
		boolean delete = true;
		for (WorkItem i : items){
			if (!canDelete(i)){
				delete = false;
				break;
			}
		}
		
		if (delete){
			try{
				if (Files.isDirectory(p)){
					FileUtils.deleteDirectory(p.toFile());
				}else{
					Files.delete(p);
				}
			}catch (Exception ex){
				logger.log(Level.WARNING, "Unable to cleanup file: " + p.toString(), ex); //$NON-NLS-1$
			}
		}
	}
	
	private boolean canDelete(WorkItem item){
		//keep these until processing complete
		if (item.getType() == Type.UP_CA || item.getType() == Type.UP_SYNC){
			if (item.getStatus() == Status.COMPLETE || item.getStatus() == Status.ERROR){
				return true;
			}
			return false;
		}
	
		//keep these for 6 months
		if (item.getType() == Type.DOWN_CA){
			if (item.getStatus() == Status.COMPLETE || item.getStatus() == Status.ERROR){
				return checkCaExport(item.getStartTime());
			}else{
				return false;
			}
		}
		
		//keep these for 24 hours
		if (item.getType() == Type.DOWN_SYNC){
			if (item.getStatus() == Status.COMPLETE){
				return checkSyncDownload(item.getStartTime());
			}else if (item.getStatus() == Status.ERROR){
				return true;
			}else{
				return false;
			}
		}
		return false;
	}
	
	/**
	 * 
	 * @param d
	 * @return true if file should be deleted; false otherwise
	 */
	private boolean checkCaExport(Date d){
		if (caExportAvailableDays == null || caExportAvailableDays <= 0) return false;
		
		Date now = new Date();
		if (now.getTime() - d.getTime() > caExportAvailableDays * 24l * 60 * 60 * 1000){
			return true;
		}
		return false;
	}
	
	/**
	 * 
	 * @param d
	 * @return true if file should be deleted; false otherwise
	 */
	private boolean checkSyncDownload(Date d){
		if (syncDownloadAvailableHrs == null || syncDownloadAvailableHrs <= 0) return false;
		Date now = new Date();
		if (now.getTime() - d.getTime() > syncDownloadAvailableHrs * 60l * 60 * 1000){
			return true;
		}
		return false;
	}

	
	/**
	 * Clean up items in the change log table.
	 * @param session
	 */
	private void cleanUpChangeLog(Session session){
		if (changeLogCleanUpDays == null || changeLogCleanUpDays <= 0) return;
		
		Date lastDate = new Date((new Date()).getTime() - changeLogCleanUpDays * 24l * 60 *60 *1000);
		session.beginTransaction();
		try{
			for (ConservationAreaInfo ca : HibernateManager.getConservationAreaInfos(session)){
				Long maxRevision = ChangeLogManager.INSTANCE.getLastRevision(session, lastDate, ca.getUuid());
				ChangeLogManager.INSTANCE.deleteItems(session, maxRevision, ca.getUuid());
			}
			session.getTransaction().commit();
		}catch (Exception ex){
			logger.log(Level.SEVERE, "Could not clean up change log.", ex.getMessage()); //$NON-NLS-1$
			session.getTransaction().rollback();
		}
	}

	/**
	 * Clean up items in the change log table.
	 * @param session
	 */
	private void cleanUpGlobalForestWatchFiles(){
		if (gfwCleanUpDays == null || gfwCleanUpDays <= 0) return;
		
		Path gfwPath = DataStoreManager.INSTANCE.getRootDirectory().resolve(GlobalForestWatchNoa.LOG_DIRECTORY);
		if (!Files.exists(gfwPath)) return;
		
		Date lastDate = new Date((new Date()).getTime() - gfwCleanUpDays * 24l * 60 *60 *1000);
		try {
			Files.walkFileTree(gfwPath, new FileVisitor<Path>() {
					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
						String fileName = file.getFileName().toString();
						int firstIndex = fileName.indexOf('.');
						int lastIndex = fileName.indexOf('.', firstIndex+1);
						if (firstIndex < 0 || lastIndex < 0)  return null;
						
						String datetime = fileName.substring(firstIndex+1, lastIndex);
						try {
							SimpleDateFormat df = new SimpleDateFormat(GlobalForestWatchNoa.DATE_FORMAT);
							Date dd = df.parse(datetime);
							
							if (dd.before(lastDate)) {
								Files.delete(file);
							}
						}catch (Exception ex) {
							logger.log(Level.WARNING,  "Error cleaning up gfw directory: " + ex.getMessage(), ex); //$NON-NLS-1$
						}
						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
						return FileVisitResult.CONTINUE;
					}
					@Override
					public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
						return FileVisitResult.CONTINUE;
					}
					@Override
					public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
						return FileVisitResult.CONTINUE;
				}});
		} catch (IOException ex) {
			logger.log(Level.WARNING,  "Error cleaning up gfw directory: " + ex.getMessage(), ex); //$NON-NLS-1$
		}
	}
	
	/*
	 * delete items from the data queue table and associated files
	 */
	private void cleanUpDataQueue(Session s){
		Integer days = null;
		try{
			days = getEnvironmentVariable(EnvironmentVariables.Variable.DATA_QUEUE_CLEAN_UP_DAYS);
		}catch (Exception ex){
			logger.log(Level.WARNING, "Value not found for environment variable:" + EnvironmentVariables.Variable.WORK_HISTORY_ITEM_AVAILABLE.key, ex); //$NON-NLS-1$
		}
		if (days != null && days > 0){
			//remove all items
			List<Path> filesToDelete = new ArrayList<>();
			Date lastDate = new Date((new Date()).getTime() - days * 24l * 60 *60 *1000);
			s.beginTransaction();
			try{
				CriteriaBuilder cb = s.getCriteriaBuilder();
				CriteriaQuery<ServerDataQueueItem> c = cb.createQuery(ServerDataQueueItem.class);
				Root<ServerDataQueueItem> from = c.from(ServerDataQueueItem.class);
				c.where(cb.and(
						cb.lessThanOrEqualTo(from.get("uploadedDate"), lastDate), //$NON-NLS-1$
						from.get("status").in(ServerDataQueueItem.Status.COMPLETE, ServerDataQueueItem.Status.ERROR) //$NON-NLS-1$
						));
				List<ServerDataQueueItem> toDelete = s.createQuery(c).list();
				
				for (ServerDataQueueItem delete : toDelete){
					Path fToDelete = DataStoreManager.INSTANCE.getFile(delete.getFile());
					filesToDelete.add(fToDelete);
					s.delete(delete);
				}
		
				s.getTransaction().commit();
			}catch (Exception ex){
				s.getTransaction().rollback();
				logger.log(Level.WARNING, "Unable to clean up data queue items.", ex); //$NON-NLS-1$
			}
			//delete associated files
			for (Path f : filesToDelete){
				try{
					Files.deleteIfExists(f);
				}catch (Exception ex){
					logger.log(Level.WARNING, MessageFormat.format("Unable to delete data queue file: {0}.", f.toString()), ex); //$NON-NLS-1$
				}
			}
		}
		//delete any files that are in the data queue folder that are not 
		//the data queue table; there should never be any, but if something goes wrong this will
		//clean up the files eventually
		Set<String> allFiles = new HashSet<String>();
		s.beginTransaction();
		try{
			List<?> files = s.createQuery("SELECT file FROM ServerDataQueueItem").list(); //$NON-NLS-1$
			for (Object x : files) allFiles.add((String)x);
			s.getTransaction().commit();
		}catch (Exception ex){
			s.getTransaction().rollback();
			logger.log(Level.WARNING, "Unable to clean up data queue items.", ex); //$NON-NLS-1$
		}
		Path dataqueueDir = DataStoreManager.INSTANCE.getFile(DataQueue.FILE_STORE_LOCATION);
		Path root = DataStoreManager.INSTANCE.getRootDirectory();
		if (Files.exists(dataqueueDir)) {
			try(DirectoryStream<Path> files = Files.newDirectoryStream(dataqueueDir)){
				for (Path f : files){
					//find any matching item in the data queue
					String check = root.relativize(f).toString();
					if (!allFiles.contains(check)){
						//delete
						try{
							Files.deleteIfExists(f);
						}catch(Exception ex){
							logger.log(Level.WARNING, MessageFormat.format("Unable to delete data queue file that is not associated with any files: {0}.", f.toString()), ex);		 //$NON-NLS-1$
						}
					}
				}
			}catch (Exception ex){
				logger.log(Level.WARNING, "Unable to delete data queue files that are not associated with any files.", ex); //$NON-NLS-1$
			}
		}
	}
	
	/*
	 * delete all query temp tables
	 */
	private void cleanupQueryTempTables(Session s){
		String query = "select table_schema || '.' || table_name from information_schema.tables where table_schema = 'query_temp' and table_name like 'query\\_temp\\_%'"; //$NON-NLS-1$
		
		s.beginTransaction();
		try{
			@SuppressWarnings("unchecked")
			List<String> tablesToDrop = s.createNativeQuery(query).list();
			for (String table : tablesToDrop){
				try{
					s.createNativeQuery("DROP TABLE " + table).executeUpdate(); //$NON-NLS-1$
				}catch (Exception ex){
					logger.log(Level.WARNING, "Unable to drop temporary query table : " + table); //$NON-NLS-1$
				}
			}
		}finally{
			s.getTransaction().commit();
		}
		
	}
}
