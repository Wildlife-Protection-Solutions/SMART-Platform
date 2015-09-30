package org.wcs.smart.connect.downloader.sync;

import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.wcs.smart.connect.database.LockManager;
import org.wcs.smart.connect.downloader.ca.CaExporterJob;
import org.wcs.smart.connect.model.ConservationAreaInfo;
import org.wcs.smart.connect.model.WorkItem;
import org.wcs.smart.connect.model.WorkItem.Status;

public class CaChangeLogPackageJob implements Runnable {
	private final Logger logger = Logger.getLogger(CaExporterJob.class.getName());
	
	private ConservationAreaInfo info = null;
	private WorkItem item = null;
	private SessionFactory factory;
	private Path destFile = null;
	private String fileurl;
	private Long startRevision;
	
	public CaChangeLogPackageJob(ConservationAreaInfo info, 
			WorkItem item, 
			String fileurl,
			Long startRevision,
			SessionFactory factory){
		this.info = info;
		this.item = item;
		this.fileurl = fileurl;
		this.factory = factory;
		this.startRevision = startRevision;
	}

	@Override
	public void run() {
		Session s = factory.openSession();
		
		try{
			//lock database for conservation area
			LockManager.INSTANCE.lockDatabase(s, item.getConservationAreaInfo());
		}catch (Exception ex){
			logger.log(Level.SEVERE, "Could not lock database to create conservation change log package. " + item.getUuid());
			
			//set error status
			s.beginTransaction();
			item.setStatus(org.wcs.smart.connect.model.WorkItem.Status.ERROR);
			item.setMessage(MessageFormat.format("Error processing item {0}: {1}.", item.getUuid().toString(), ex.getMessage()));
			s.getTransaction().commit();
			
			return;
		}
		
		try{
			ChangeLogPackager packer = new ChangeLogPackager(s, info.getUuid(), startRevision);
			Path file = packer.createPackage();
		
			s.beginTransaction();
			item.setStatus(Status.COMPLETE);
			item.setMessage("{\"file_url\": " + "\"" + fileurl + "\"}");
			item.setLocalFilename(file.toString());
			s.saveOrUpdate(item);
			s.getTransaction().commit();
			
		}catch (Exception ex){
			logger.log(Level.SEVERE, "Error creating change package for conservation area. " + ex.getMessage(), ex);
			
			//TODO: do we want to close and reopen this session here?
			try{
				s.beginTransaction();
				item.setStatus(Status.ERROR);
				item.setMessage("{\"error\": " + "\"Could not create change log package. " + ex.getMessage() + "\"}");
				s.saveOrUpdate(item);
				s.getTransaction().commit();
			}catch (Exception ex2){
				logger.log(Level.SEVERE, "Error updating change log status. " + ex2.getMessage(), ex2);
			}
		}finally{
			
			try{
				LockManager.INSTANCE.releaseDatabase(s, item.getConservationAreaInfo());
			}catch (Exception ex){
				logger.log(Level.SEVERE, "Could not release database lock after creating conservation area download package. " + item.getUuid());
			}
			if (s.isOpen())	s.close();
		}
	}
	
	
}
