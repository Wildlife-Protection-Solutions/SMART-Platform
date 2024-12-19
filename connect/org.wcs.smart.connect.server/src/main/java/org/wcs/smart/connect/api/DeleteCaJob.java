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
package org.wcs.smart.connect.api;

import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.wcs.smart.connect.datastore.DataStoreManager;
import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.connect.model.ConservationAreaInfo;
import org.wcs.smart.connect.model.CyberTrackerNavigationLayer;
import org.wcs.smart.connect.model.CyberTrackerPackage;
import org.wcs.smart.connect.model.WorkItem;
import org.wcs.smart.connect.query.QueryManager;
import org.wcs.smart.connect.uploader.sync.ChangeLogManager;
import org.wcs.smart.hibernate.QueryFactory;

/**
 * Job to delete a Conservation Area.  This has a lock so only
 * one Conservation Area is deleted at a time.
 * @author Emily
 *
 */
public class DeleteCaJob implements Runnable {

	private final Logger logger = Logger.getLogger(DeleteCaJob.class.getName());

	/**
	 * Lock to only delete one conservation area at a time
	 */
	private static final Object LOCK = new Object();
	
	private ConservationAreaInfo serverDelete;
	private SessionFactory factory;
	private boolean deleteAll;
	private Locale l;

	public DeleteCaJob(ConservationAreaInfo toDelete, boolean deleteAll, SessionFactory factory, Locale l) {
		this.serverDelete = toDelete;
		this.factory = factory;
		this.deleteAll = deleteAll;
		this.l = l;
	}

	@Override
	public void run() {
		synchronized (LOCK) {
			
			try(Session session = factory.openSession()){
				session.beginTransaction();
				try {
					Set<Path> filesToDelete = new HashSet<>();
					
					UUID caUuidToDelete = serverDelete.getUuid();
					serverDelete = (ConservationAreaInfo) session.get(ConservationAreaInfo.class, caUuidToDelete);
					if (serverDelete == null) {
						//conservation area not found
						return;
					}
				
					//need to do some of the (deleteall) work before the desktop data is gone
					if (deleteAll){
						// delete query actions associated with any query from the CA being deleted
						QueryManager.INSTANCE.removeAccessToQueriesFromCa(serverDelete.getUuid(), session);
						
						//ConservationArea ca = session.get(ConservationArea.class, serverDelete.getUuid());

						//package files 
						List<CyberTrackerNavigationLayer> navlayers = QueryFactory.buildQuery(session, CyberTrackerNavigationLayer.class, 
								new Object[] {"conservationArea", serverDelete}).list(); //$NON-NLS-1$
						for (CyberTrackerNavigationLayer layer : navlayers) {
							java.nio.file.Path toDelete = DataStoreManager.INSTANCE.getRootDirectory()
									.resolve(CyberTracker.CT_NAVIGATION_DATASTORE_LOCATION).resolve(layer.getFilename());
							filesToDelete.add(toDelete);
							session.remove(layer);
						}
						
						List<CyberTrackerPackage> ctpackages = QueryFactory.buildQuery(session, CyberTrackerPackage.class, 
								new Object[] {"conservationArea", serverDelete}).list(); //$NON-NLS-1$
						for (CyberTrackerPackage layer : ctpackages) {
							java.nio.file.Path toDelete = DataStoreManager.INSTANCE.getRootDirectory()
									.resolve(CyberTracker.CT_PACKAGE_DATASTORE_LOCATION).resolve(layer.getFilename());
							filesToDelete.add(toDelete);
							session.remove(layer);
						}
						
						//workitem files
						List<WorkItem> workitems = QueryFactory.buildQuery(session, WorkItem.class, 
								new Object[] {"conservationAreaInfo", serverDelete}).list(); //$NON-NLS-1$
						for (WorkItem workitem : workitems) {
							if (workitem.getLocalFilename() != null && !workitem.getLocalFilename().isEmpty()) {
								java.nio.file.Path toDelete = DataStoreManager.INSTANCE.getFile(workitem.getLocalFilename());
								filesToDelete.add(toDelete);
							}
							session.remove(workitem);
						}
						
				
					}
	
					//disable change tracking while we delete the Conservation Area
					ChangeLogManager.INSTANCE.disableChangeTracking(serverDelete, session);
				
					//delete desktop data
					String query = "DELETE FROM smart.conservation_area WHERE uuid = :uuid"; //$NON-NLS-1$
					session.createNativeMutationQuery(query)
						.setParameter("uuid", serverDelete.getUuid())//, PostgresUUIDType.INSTANCE) //$NON-NLS-1$
						.executeUpdate();
			
					//delete plugin data
					session.createMutationQuery("DELETE FROM CaPluginVersion WHERE id.conservationAreaUuid = :ca") //$NON-NLS-1$
								.setParameter("ca", serverDelete.getUuid()) //$NON-NLS-1$
								.executeUpdate();
			
					//delete change log data
					ChangeLogManager.INSTANCE.deleteItems(session, serverDelete.getUuid());
						
					if (deleteAll){
							
						//delete actions associated with resource
						session.createMutationQuery("DELETE FROM SmartUserAction WHERE resource = :ca") //$NON-NLS-1$
							.setParameter("ca", serverDelete.getUuid()) //$NON-NLS-1$
							.executeUpdate();
						
						session.createMutationQuery("DELETE FROM WorkItemSummary WHERE conservationAreaInfo = :ca") //$NON-NLS-1$
						.setParameter("ca", serverDelete) //$NON-NLS-1$
						.executeUpdate();
						
						//delete server only data
						session.remove(serverDelete);
					}else{
						serverDelete.setStatus(ConservationAreaInfo.Status.NODATA);
						serverDelete.setVersion(null);
					}
						
					session.getTransaction().commit();
					
					for (Path toDelete : filesToDelete) {
						if (Files.exists(toDelete)) {
							try {
								Files.delete(toDelete);
							}catch (Exception ex) {
								logger.warning(MessageFormat.format("Could not delete file: {0}", toDelete.toString()));  //$NON-NLS-1$
							}
						}
					}
					//delete all ca data and findstore
					try{
						DataStoreManager.INSTANCE.deleteDirectory(caUuidToDelete);
					}catch (Exception ex){
						logger.severe(Messages.getString("ConservationAreas.CouldNotDeleteFilestore", l)); //$NON-NLS-1$
					}
				}catch (Throwable ex) {
					if (session.getTransaction().isActive()) session.getTransaction().rollback();

					ex.printStackTrace();
					logger.log(Level.SEVERE, "Error occurred while deleting Conservation Area: " + serverDelete.getLabel() + "; " + ex.getMessage(), ex); //$NON-NLS-1$ //$NON-NLS-2$
					try {
						session.beginTransaction();
						serverDelete.setStatus(ConservationAreaInfo.Status.ERROR);
						session.merge(serverDelete);
						session.getTransaction().commit();
					} catch (Exception ex2) {
						logger.log(Level.SEVERE, "Error occurred while deleting Conservation Area: " + serverDelete.getLabel() + "; " + ex2.getMessage(), ex2); //$NON-NLS-1$ //$NON-NLS-2$
					}
				}finally {
					//re-enable triggers
					try {
						session.beginTransaction();
						ChangeLogManager.INSTANCE.enableChangeTracking(serverDelete, session);
						session.getTransaction().commit();
					} catch (Exception ex) {
						logger.log(Level.SEVERE, "Error occurred while re-enabling change tracker after deleting Conservation Area: " + serverDelete.getLabel() + "; " + ex.getMessage(), ex); //$NON-NLS-1$ //$NON-NLS-2$
					}
				}		
			}
		}
	}
	
}