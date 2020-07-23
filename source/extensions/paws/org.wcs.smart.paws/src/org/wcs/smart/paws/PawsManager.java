/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.paws;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.swt.graphics.Image;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.paws.internal.Messages;
import org.wcs.smart.paws.model.PawsConfiguration;
import org.wcs.smart.paws.model.PawsParameter;
import org.wcs.smart.paws.model.PawsRun;
import org.wcs.smart.paws.model.PawsService;
import org.wcs.smart.query.QueryTypeManager;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.util.SmartUtils;


/**
 * Supporting functions for managing PAWS objects 
 * 
 * @author Emily
 *
 */
public enum PawsManager {
	INSTANCE;
	
	public String createLabel(Query q) {
		return q.getName() + " (" + QueryTypeManager.INSTANCE.findQueryType(q.getTypeKey()).getGuiName() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	/**
	 * Overwrites any existing paws server settings with the fixed defaults
	 * @param session
	 */
	public PawsService createDefaultSettings(Session session) {
		PawsService service = QueryFactory.buildQuery(session, PawsService.class, 
				new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).uniqueResult(); //$NON-NLS-1$
		if (service == null) {
			service = new PawsService();
			service.setConservationArea(SmartDB.getCurrentConservationArea());
		}
		service.setApiKey("a2e5c3f769924a1b98189b652e81c9d9");
		service.setTaskApiUrl("https://paws-api-backend-api-mgmt.azure-api.net/taskmanagement/task");
		service.setPawsApiUrl("https://paws-api-backend-api-mgmt.azure-api.net/paws/predict-risk");
		service.setClientId("61619872-cd80-41ec-a799-b7c7fba349ce");
		service.setOAuthUrl("https://login.microsoftonline.com/af58cf6f-6228-4801-8a20-caa33d81cee2/oauth2");
		service.setStorageUrl("https://pawsparkstorage.blob.core.windows.net");
		session.saveOrUpdate(service);
		return service;
	}
	
	/**
	 * Delete a set of configurations
	 * @param configurations
	 * @param broker
	 * @throws Exception
	 */
	public void deleteConfigurations(List<PawsConfiguration> configurations, IEventBroker broker) throws Exception{
		//runs and results are set with configuration set to null
		List<Path> toDelete = new ArrayList<>();
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				for (PawsConfiguration c : configurations) {
					c = session.get(PawsConfiguration.class, c.getUuid());
					if (c != null) {
						toDelete.add(PawsFileManager.INSTANCE.getDirectory(c));
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
				throw new Exception(Messages.PawsManager_DeleteConfigError + "\n\n" + ex.getMessage(), ex);  //$NON-NLS-1$
			}
		}
		for (Path p : toDelete) {
			if (Files.exists(p)) {
				//delete directory and all files
				try {
					Files.walkFileTree(p, new FileVisitor<Path>() {
						@Override
						public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
								throws IOException {
							return FileVisitResult.CONTINUE;
						}

						@Override
						public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
							Files.delete(file);
							return FileVisitResult.CONTINUE;
						}

						@Override
						public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
							return FileVisitResult.CONTINUE;
						}

						@Override
						public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
							Files.delete(dir);
							return FileVisitResult.CONTINUE;
						}
					});
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
						toDelete.add(PawsFileManager.INSTANCE.getDirectory(c));
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
				throw new Exception(Messages.PawsManager_DeleteRunError + "\n\n" + ex.getMessage(), ex);  //$NON-NLS-1$
			}
		}
		for (Path p : toDelete) {
			if (Files.exists(p)) {
				//delete directory and all files
				try {
					SmartUtils.deleteDirectory(p);
				}catch (IOException ex) {
					PawsPlugIn.displayLog(ex.getMessage(), ex);
				}
				
			}
			
		}
		//delete all the files in the directories
		broker.post(PawsEvent.PAWS_RUN_DELETE, runs);
	}
	
	public void validateConfiguration(PawsConfiguration config) throws Exception{
		try(Session session = HibernateManager.openSession()){
			config = session.get(PawsConfiguration.class, config.getUuid());
			if (config.getParameters() == null) throw new Exception(Messages.PawsManager_GridParametersRequired);

//			PawsParameter pp = config.findParameter(PawsParameter.FixedParameter.GRID_CRS.name());
//			if (pp == null || pp.getValue() == null || pp.getValue().isEmpty() ) throw new Exception("Grid coordinate reference system required");
	
			PawsParameter pp = config.findParameter(PawsParameter.FixedParameter.GRID_SIZE.name());
			if (pp == null || pp.getValue() == null || pp.getValue().isEmpty() ) throw new Exception(Messages.PawsManager_GridSizeRequired);
	
			pp = config.findParameter(PawsParameter.FixedParameter.LYR_BOUNDARY.name());
			if (pp == null || pp.getValue() == null || pp.getValue().isEmpty() ) throw new Exception(Messages.PawsManager_CALayerRequired);
		}
	}
	
	public String generateUniqueName(String runname, ConservationArea ca){
		String id = runname;
		int cnt = 1;
		int index = runname.lastIndexOf(' ');
		if (index > 0) {
			String test = runname.substring(index + 1);
			try {
				cnt = Integer.parseInt(test)+1;
				runname = runname.substring(0, index);
				id = runname + " " + (cnt++); //$NON-NLS-1$
			}catch (Exception ex) {
				//fail
			}
		}
		
		try(Session session = HibernateManager.openSession()){
			while(true) {
				if (QueryFactory.buildCountQuery(session, PawsRun.class, 
						new Object[] {"conservationArea", ca}, //$NON-NLS-1$
						new Object[] {"id", id}) > 0) { //$NON-NLS-1$
					
					id = runname + " " + (cnt++);  //$NON-NLS-1$
				}else {
					break;
				}
			}
		}
		return id;
	}
	
	
	public Image getImage(PawsRun.Status status){
		switch(status){
		case UPLOADING_DATA:
		case RUNNING:
		case DOWNLOADING_RESULTS:
		case COMPILING_DATA: return PawsPlugIn.getDefault().getImageRegistry().get(PawsPlugIn.ICON_WORKING);
		case COMPLETE: return PawsPlugIn.getDefault().getImageRegistry().get(PawsPlugIn.ICON_DONE);
		case ERROR: return PawsPlugIn.getDefault().getImageRegistry().get(PawsPlugIn.ICON_ERROR);
		case AUTH_TIMEOUT:return PawsPlugIn.getDefault().getImageRegistry().get(PawsPlugIn.ICON_AUTHTIMEOUT);
		}
		return null;
	}
	
	public String getStatusLabel(PawsRun.Status status) {
		switch(status){
		case UPLOADING_DATA: return "Uploading Data";
		case RUNNING: return "Running";
		case DOWNLOADING_RESULTS: return "Downloading Results";
		case COMPILING_DATA: return "Compiling Data";
		case COMPLETE: return "Complete";
		case ERROR: return "Error";
		case AUTH_TIMEOUT:return "Authentication Timeout";
		}
		return null;
	}
	
	public String getName(PawsParameter.FixedParameter fixedParameter){
		switch(fixedParameter){
//		case GRID_CRS: return "CRS" ;
		case GRID_SIZE: return Messages.PawsManager_GridSize;
		case LYR_BOUNDARY: return Messages.PawsManager_CABoundary;
		case LYR_OTHER: return Messages.PawsManager_OtherFiles;
		case CLASSIFIER_MODEL: return Messages.PawsManager_ClassifierModel;
		case TRAINING_RES: return Messages.PawsManager_TrainingResolution;
		}
		return null;
	}
}
