package org.wcs.smart.paws;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.paws.model.PawsConfiguration;
import org.wcs.smart.paws.model.PawsRun;
import org.wcs.smart.util.UuidUtils;


public enum PawsManager {
	INSTANCE;
	
	public void deleteConfigurations(List<PawsConfiguration> configurations, IEventBroker broker) throws Exception{
		
		List<Path> toDelete = new ArrayList<>();
		try(Session session = HibernateManager.openSession()){
			//TODO: figure out what we do with the results (set configuration to null?)
			session.beginTransaction();
			try {
				for (PawsConfiguration c : configurations) {
					c = session.get(PawsConfiguration.class, c.getUuid());
					if (c != null) {
						toDelete.add(getDirectory(c));
						session.delete(c);
					}
				}
				session.getTransaction().commit();
			}catch (Exception ex) {
				try {
					session.getTransaction().rollback();
				}catch (Exception ex2) {
					PawsPlugIn.log(ex2.getMessage(), ex2);
				}
				throw new Exception("Unable to delete selected configurations." + "\n\n" + ex.getMessage(), ex);
			}
		}
		for (Path p : toDelete) {
			if (Files.exists(p)) {
				//delete directory and all files
				try {
					FileUtils.deleteDirectory(p.toFile());
				}catch (IOException ex) {
					PawsPlugIn.displayLog(ex.getMessage(), ex);
				}
				
			}
			
		}
		//delete all the files in the directories
		broker.post(PawsEvent.PAWS_CONFIG_DELETE, configurations);
	}
	
	public void deleteRun(List<PawsRun> runs, IEventBroker broker) throws Exception{
		
		List<Path> toDelete = new ArrayList<>();
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				for (PawsRun c : runs) {
					c = session.get(PawsRun.class, c.getUuid());
					if (c != null) {
						toDelete.add(getDirectory(c));
						session.delete(c);
					}
				}
				session.getTransaction().commit();
			}catch (Exception ex) {
				try {
					session.getTransaction().rollback();
				}catch (Exception ex2) {
					PawsPlugIn.log(ex2.getMessage(), ex2);
				}
				throw new Exception("Unable to delete selected runs." + "\n\n" + ex.getMessage(), ex);
			}
		}
		for (Path p : toDelete) {
			if (Files.exists(p)) {
				//delete directory and all files
				try {
					FileUtils.deleteDirectory(p.toFile());
				}catch (IOException ex) {
					PawsPlugIn.displayLog(ex.getMessage(), ex);
				}
				
			}
			
		}
		//delete all the files in the directories
		broker.post(PawsEvent.PAWS_RUN_DELETE, runs);
	}
	
	public Path getDirectory(PawsConfiguration config) {
		Path ds = Paths.get(config.getConservationArea().getFileDataStoreLocation())
				.resolve(PawsPlugIn.PAWS_DIR)
				.resolve(UuidUtils.uuidToString(config.getUuid()));
		return ds;
	}
	
	public Path getDirectory(PawsRun config) {
		Path ds = Paths.get(config.getConservationArea().getFileDataStoreLocation())
				.resolve(PawsPlugIn.PAWS_DIR)
				.resolve(UuidUtils.uuidToString(config.getUuid()));
		return ds;
	}
}
