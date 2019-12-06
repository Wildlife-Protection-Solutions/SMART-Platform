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
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.swt.graphics.Image;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.paws.model.PawsConfiguration;
import org.wcs.smart.paws.model.PawsParameter;
import org.wcs.smart.paws.model.PawsResultManager;
import org.wcs.smart.paws.model.PawsRun;
import org.wcs.smart.query.QueryTypeManager;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.util.UuidUtils;


/**
 * Supporting functions for managing PAWS objects 
 * 
 * @author Emily
 *
 */
public enum PawsManager {
	INSTANCE;
	
	public String createLabel(Query q) {
		return q.getName() + " (" + QueryTypeManager.INSTANCE.findQueryType(q.getTypeKey()).getGuiName() + ")";
	}
	
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
	
	public void validateConfiguration(PawsConfiguration config) throws Exception{
		try(Session session = HibernateManager.openSession()){
			config = session.get(PawsConfiguration.class, config.getUuid());
			if (config.getParameters() == null) throw new Exception("Grid parameters not supplied");
			
//			PawsParameter pp = config.findParameter(PawsParameter.FixedParameter.GRID_BNDS.name());
//			if (pp == null || pp.getValue() == null || pp.getValue().isEmpty() ) throw new Exception("Grid bounds required");
//			
			PawsParameter pp = config.findParameter(PawsParameter.FixedParameter.GRID_CRS.name());
			if (pp == null || pp.getValue() == null || pp.getValue().isEmpty() ) throw new Exception("Grid coordinate reference system required");
	
			pp = config.findParameter(PawsParameter.FixedParameter.GRID_SIZE.name());
			if (pp == null || pp.getValue() == null || pp.getValue().isEmpty() ) throw new Exception("Grid size required");
	
			pp = config.findParameter(PawsParameter.FixedParameter.TIMEZONE.name());
			if (pp == null || pp.getValue() == null || pp.getValue().isEmpty() ) throw new Exception("Timezone required");
	
			pp = config.findParameter(PawsParameter.FixedParameter.LYR_BOUNDARY.name());
			if (pp == null || pp.getValue() == null || pp.getValue().isEmpty() ) throw new Exception("Conservation Area Boundary layer required");
		}
	}
	
	public String generateUniqueName(String runname, ConservationArea ca){
		String id = runname;
		int cnt = 1;
		try(Session session = HibernateManager.openSession()){
			while(true) {
				if (QueryFactory.buildCountQuery(session, PawsRun.class, 
						new Object[] {"conservationArea", ca},
						new Object[] {"id", id}) > 0) {
					id = runname + " " + (cnt++);
				}else {
					break;
				}
			}
		}
		return id;
	}
	public Path getDirectory(PawsConfiguration config) {
		Path ds = Paths.get(config.getConservationArea().getFileDataStoreLocation())
				.resolve(PawsPlugIn.PAWS_DIR)
				.resolve("config")
				.resolve(UuidUtils.uuidToString(config.getUuid()));
		return ds;
	}
	
	public Path getDirectory(PawsRun config) {
		return PawsResultManager.getRunDirectory(config);
	}
	
	public Image getImage(PawsRun.Status status){
		switch(status){
		case UPLOADING_DATA:
		case RUNNING:
		case DOWNLOADING_RESULTS:
		case COMPILING_DATA: return PawsPlugIn.getDefault().getImageRegistry().get(PawsPlugIn.ICON_WORKING);
		case COMPLETE: return PawsPlugIn.getDefault().getImageRegistry().get(PawsPlugIn.ICON_DONE);
		case ERROR: return PawsPlugIn.getDefault().getImageRegistry().get(PawsPlugIn.ICON_ERROR);
		}
		return null;
	}
	
	public String getName(PawsParameter.FixedParameter fixedParameter){
		switch(fixedParameter){
//		case GRID_BNDS: return "Bounds";
		case GRID_CRS: return "CRS" ;
		case GRID_SIZE: return "Grid Size";
		case LYR_BOUNDARY: return "Conservation Area Boundary";
		case LYR_OTHER: return "Shapefiles";
		case TIMEZONE: return "Time Zone";
		case CLASSIFIER_MODEL: return "Classifier Model";
		case TRAINING_RES: return "Temporal Training Resolution (Months)";
		}
		return null;
	}
}
