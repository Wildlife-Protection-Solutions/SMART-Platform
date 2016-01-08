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
package org.wcs.smart.patrol;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.ObservationHibernateManager;
import org.wcs.smart.observation.model.ObservationOptions;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.PatrolWaypoint;
import org.wcs.smart.util.UuidUtils;

/**
 * Patrol Manager for deleting patrols.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class PatrolManager {

	/*
	 * Map of delete listeners
	 */
	private HashMap<Integer, ArrayList<IPatrolDeleteHandler>> deleteHandlers = new HashMap<Integer, ArrayList<IPatrolDeleteHandler>>();
	
	private static PatrolManager instance;
	/**
	 * 
	 * @return the instance of the conservation area manager
	 */
	public static PatrolManager getInstance(){
		if (instance == null){
			instance = new PatrolManager();
		}
		return instance;
	}
	
	private PatrolManager(){}
	
	
	/**
	 * 
	 * @param patrol the patrol to be edited
	 * @param ops current patrol options {@link ObservationHibernateManager#getPatrolOptions(org.wcs.smart.ca.ConservationArea, Session)}
	 * @return null if the patrol can be edited, otherwise a string
	 * that described reason why can't be edited.
	 */
	public String canEdit(Patrol patrol, ObservationOptions ops){
		if (ops.getEditTime() == null || ops.getEditTime() < 0){
			return null;
		}else if (patrol.getStartDate() == null){
			return null;
		}else if (SmartDB.getCurrentEmployee().getSmartUserLevel() == Employee.SmartUserLevel.DATA_ENTRY ||
				SmartDB.getCurrentEmployee().getSmartUserLevel() == Employee.SmartUserLevel.ANALYST 
				){
			Date d = new Date();
			d.setTime( d.getTime() - (long)ops.getEditTime() * 24 * 60 * 60 * 1000 );
			if (patrol.getStartDate().after(d)){
				return null;
			}else{
				return MessageFormat.format(Messages.PatrolEditor_EditError_PatrolToOld, new Object[]{ops.getEditTime() }) ;
			}
		}else{
			return null;
		}
	}
	
	/**
	 * Deletes a given patrol.  Once the patrol is deleted
	 * a patrol delete event is fired
	 * 
	 * @param ca the conservation area to delete
	 * @param monitor the progress monitor; cannot be null
	 * @throws Exception if conservation area not deleted
	 */
	public boolean deletePatrol(UUID patrolUuid, IProgressMonitor monitor) throws Exception{
		
		int work = 1;
		for (ArrayList<IPatrolDeleteHandler> data : deleteHandlers.values()){
			work += data.size();
		}
		
		monitor.beginTask(MessageFormat.format(Messages.PatrolManager_Progress_DeletingPatrol1, new Object[]{UuidUtils.uuidToString(patrolUuid)}), work);
		monitor.worked(0);
		Patrol patrol = null;
		Session session = HibernateManager.openSession();
		
		try{
			patrol = (Patrol)session.load(Patrol.class, patrolUuid);
			monitor.setTaskName(MessageFormat.format(Messages.PatrolManager_Progress_DeletingPatrol1, new Object[]{patrol.getId()}));
			
			// ensure can edit patrol 
			String canEdit = canEdit(patrol, ObservationHibernateManager.getPatrolOptions(SmartDB.getCurrentConservationArea(), session));
			if (canEdit != null){
				throw new Exception(canEdit);
			}
			session.beginTransaction();
			try{
				File fileStore = new File(SmartDB.getCurrentConservationArea().getFileDataStoreLocation() + File.separator + patrol.getPatrolDatastorePath());
			
				if (!runDeleteHandlers(patrol, session, monitor)){
					return false;
				}
				monitor.subTask(Messages.PatrolManager_Progress_SubDeletingPatrol);
				
				//waypoint deletion is not cascaded; we must delete this explicitly
				for (PatrolLeg pl : patrol.getLegs()){
					for (PatrolLegDay pld : pl.getPatrolLegDays()){
						for (PatrolWaypoint pw : pld.getWaypoints()){
							session.delete(pw.getWaypoint());
						}
					}
				}
				session.delete(patrol);
				session.getTransaction().commit();
				monitor.worked(1);
			
				runAfterDeleteHandlers(patrol, monitor);
			
				if (fileStore.exists()){
					monitor.subTask(Messages.PatrolManager_Progress_RemovingFileStore);
					try{
						FileUtils.forceDelete(fileStore);
					}catch(Exception ex){
						SmartPatrolPlugIn.displayLog(Messages.PatrolManager_Error_CouldNotDeleteFilestore + fileStore.getAbsolutePath(), ex);
					}
				}
				monitor.worked(1);

			}catch (Exception ex){
				session.getTransaction().rollback();
				throw ex;
			}
		}finally{
			session.close();
		}
	
		if (patrol != null){
			PatrolEventManager.getInstance().patrolDeleted(patrol);
		}
		monitor.done();
		
		return true;
	}
	
	/**
	 * Runs all the delete handlers in the order provided.
	 * 
	 */
	private boolean runDeleteHandlers(Patrol patrol, Session session, IProgressMonitor monitor) throws Exception{
		ArrayList<Integer> items = new ArrayList<Integer> ();
		items.addAll(deleteHandlers.keySet());
		Collections.sort(items);
		for(int i = items.size() -1; i >= 0; i --){
			ArrayList<IPatrolDeleteHandler> listeners = deleteHandlers.get(items.get(i));
			for (IPatrolDeleteHandler listener : listeners){
				boolean canDelete = listener.beforeDelete(patrol, session, monitor);
				if (!canDelete){
					return false;
				}
				monitor.worked(1);
			}
		}
		return true;
	}

	/**
	 * Runs all the delete handlers in the order provided.
	 * 
	 */
	private void runAfterDeleteHandlers(Patrol patrol, IProgressMonitor monitor) throws Exception{
		ArrayList<Integer> items = new ArrayList<Integer> ();
		items.addAll(deleteHandlers.keySet());
		Collections.sort(items);
		for(int i = items.size() -1; i >= 0; i --){
			ArrayList<IPatrolDeleteHandler> listeners = deleteHandlers.get(items.get(i));
			for (IPatrolDeleteHandler listener : listeners){
				listener.afterDelete(patrol, monitor);
				monitor.worked(1);
			}
		}
	}
	
	/**
	 * <code>deleteOrder</code> is the order in which items should be deleted.  
	 * Items with higher delete
	 * orders are executed before items with lower delete order.
	 * <p>
	 * ConservationArea have a delete order of 0 and are the last items deleted.
	 * </p>
	 * 
	 * @param listener
	 * @param deleteOrder  a number greater than 0
	 */
	public void addDeleteHandler(IPatrolDeleteHandler listener, int deleteOrder){
		assert deleteOrder > 0;
		
		ArrayList<IPatrolDeleteHandler> listeners = deleteHandlers.get(deleteOrder);
		if (listeners == null){
			listeners = new ArrayList<IPatrolDeleteHandler>();
			deleteHandlers.put(deleteOrder, listeners);
		}
		listeners.add(listener);
	}
}
