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

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.connect.api.Uploader;
import org.wcs.smart.connect.datastore.DataStoreManager;
import org.wcs.smart.connect.model.WorkItem;
import org.wcs.smart.connect.model.WorkItem.Status;
import org.wcs.smart.connect.model.WorkItem.Type;
import org.wcs.smart.connect.uploader.sync.ChangeLogManager;

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
	
	public CleanUpJob(SessionFactory sessionFactory){
		this.sessionFactory = sessionFactory;
	}
	
	@Override
	public void run() {
		logger.log(Level.FINEST, "Running cleanup job: " + (new Date()).toString());
		
		syncDownloadAvailableHrs = getEnvironmentVariable(EnvironmentVariables.Variable.SYNC_DOWNLOAD_AVAILABLE);
		caExportAvailableDays = getEnvironmentVariable(EnvironmentVariables.Variable.CA_EXPORT_AVAILABLE);
		changeLogCleanUpDays = getEnvironmentVariable(EnvironmentVariables.Variable.CHANGELOG_CLEAN_UP_DAYS);
		
		try{
			cleanUp();
		}catch (Exception ex){
			logger.log(Level.SEVERE, "Error running cleanup task:" + ex.getMessage(), ex);
		}		
	}
	
	private Integer getEnvironmentVariable(EnvironmentVariables.Variable variable){
		Integer value = null;
		try{
			value = (Integer)EnvironmentVariables.INSTANCE.getEnvironmentVairable(variable);
		}catch (Exception ex){
			logger.log(Level.WARNING, "Value not found for environment variable:" + variable, ex);
		}
		return value;
	}
	
	private void cleanUp(){
		Session s = sessionFactory.openSession();
		try{
			//list all files in uploads directory			
			Path uploadDir = DataStoreManager.INSTANCE.getRootDirectory().toPath().resolve(Uploader.DATASTORE_DIR);
			try(DirectoryStream<Path> stream = Files.newDirectoryStream(uploadDir)){
				for (Path path : stream){
					checkAndDelete(s, path);
				}
			}catch (Exception ex){
				logger.log(Level.WARNING, "Unable to list files in uploads directory for cleaning.", ex);
			}
			
			//ca export directory
			Path caExportDirectory = DataStoreManager.INSTANCE.getRootDirectory().toPath().resolve(DataStoreManager.CA_EXPORT_LOCATION);
			try(DirectoryStream<Path> stream = Files.newDirectoryStream(caExportDirectory)){
				for (Path path : stream){
					checkAndDelete(s, path);
				}
			}catch (Exception ex){
				logger.log(Level.WARNING, "Unable to list files in uploads directory for cleaning.", ex);
			}
			
			//tempDir
			Path tempDir = DataStoreManager.INSTANCE.getTemporaryDirectory().toPath();
			try(DirectoryStream<Path> stream = Files.newDirectoryStream(tempDir)){
				for (Path path : stream){
					if (!Files.isDirectory(path)){
						//only check files here as directories as likely being used
						//to process item
						checkAndDelete(s, path);
					}
				}
			}catch (Exception ex){
				logger.log(Level.WARNING, "Unable to list files in uploads directory for cleaning.", ex);
			}
			
			//delete any work items
			cleanUpWorkItems(s);
			
			//clean up change log items
			cleanUpChangeLog(s);
		}finally{
			s.close();
		}
	}
	
	/*
	 * delete items from the workitem table.
	 */
	private void cleanUpWorkItems(Session s){
		Integer days = null;
		try{
			days = (Integer)EnvironmentVariables.INSTANCE.getEnvironmentVairable(EnvironmentVariables.Variable.WORK_HISTORY_ITEM_AVAILABLE);
		}catch (Exception ex){
			logger.log(Level.WARNING, "Value not found for environment variable:" + EnvironmentVariables.Variable.WORK_HISTORY_ITEM_AVAILABLE.key, ex);
		}
		if (days == null || days <= 0) return;
		
		s.beginTransaction();
		try{
			Query q = s.createQuery("DELETE FROM WorkItem where startTime < :starttime");
			
			Date d = new Date((new Date()).getTime() - days * 24l * 60 * 60 *1000);
			q.setParameter("starttime", d);
			q.executeUpdate();
			
			s.getTransaction().commit();
		}catch (Exception ex){
			s.getTransaction().rollback();
			logger.log(Level.WARNING, "Unable to clean up work item table.", ex);
		}
	}
	
	/*
	 * finds the work item related to the given file and deletes
	 * it if the file can be deleted
	 */
	@SuppressWarnings("unchecked")
	private void checkAndDelete(Session s, Path p){
		String localFilename = DataStoreManager.INSTANCE.getRootDirectory().toPath().relativize(p).toString();
		List<WorkItem> items = s.createCriteria(WorkItem.class)
				.add(Restrictions.eq("localFilename", localFilename))
				.list();
		
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
				logger.log(Level.WARNING, "Unable to cleanup file: " + p.toString(), ex);
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
			Long maxRevision = ChangeLogManager.INSTANCE.getLastRevision(session, lastDate);
			ChangeLogManager.INSTANCE.deleteItems(session, maxRevision);
			session.getTransaction().commit();
		}catch (Exception ex){
			logger.log(Level.SEVERE, "Could not clean up change log.", ex.getMessage());
			session.getTransaction().rollback();
		}
	}

}
