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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.model.Patrol;

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
	 * Deletes a given patrol.  Once the patrol is deleted
	 * a patrol delete event is fired
	 * 
	 * @param ca the conservation area to delete
	 * @param monitor the progress monitor; cannot be null
	 * @throws Exception if conservation area not deleted
	 */
	public void deletePatrol(byte[] patrolUuid, IProgressMonitor monitor) throws Exception{
		
		int work = 1;
		for (ArrayList<IPatrolDeleteHandler> data : deleteHandlers.values()){
			work += data.size();
		}
		
		monitor.beginTask(Messages.PatrolManager_Progress_DeletingPatrol, work);
		monitor.worked(0);
		Session session = HibernateManager.openSession();
		Patrol patrol = (Patrol)session.load(Patrol.class, patrolUuid);
		
		session.beginTransaction();
		try{
			File fileStore = new File(SmartDB.getCurrentConservationArea().getFileDataStoreLocation() + File.separator + patrol.getPatrolDatastorePath());
			
			runDeleteHandlers(patrol, session, monitor);
			monitor.subTask(Messages.PatrolManager_Progress_SubDeletingPatrol);
			session.delete(patrol);
			session.getTransaction().commit();
			monitor.worked(1);
			
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
		}finally{
			session.close();
		}
		
		PatrolEventManager.getInstance().patrolDeleted(patrol);
	}
	
	/**
	 * Runs all the delete handlers in the order provided.
	 * 
	 */
	private void runDeleteHandlers(Patrol patrol, Session session, IProgressMonitor monitor) throws Exception{
		ArrayList<Integer> items = new ArrayList<Integer> ();
		items.addAll(deleteHandlers.keySet());
		Collections.sort(items);
		for(int i = items.size() -1; i >= 0; i --){
			ArrayList<IPatrolDeleteHandler> listeners = deleteHandlers.get(items.get(i));
			for (IPatrolDeleteHandler listener : listeners){
				listener.beforeDelete(patrol, session, monitor);
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
