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
package org.wcs.smart.ca;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.internal.Messages;

/**
 * This is a manager for conservation areas.
 * 
 * <p>
 * Additional plug-ins that add conservation area
 * specific information should implement a {@link ICaDeleteHandler} 
 * and register it with this manager.
 * </p> 
 * 
 * @see ICaDeleteHandler 
 * 
 * @author Emily
 * @since 1.0.0
 */
public class ConservationAreaManager {

	/*
	 * Map of delete listeners
	 */
	private HashMap<Integer, ArrayList<ICaDeleteHandler>> deleteListeners = new HashMap<Integer, ArrayList<ICaDeleteHandler>>();
	/*
	 * List of area listeners
	 */
	private List<IAreaModifiedListener> areaListeners = new ArrayList<IAreaModifiedListener>();
	private List<IEmployeeListener> employeeListener = new ArrayList<IEmployeeListener>();
	private List<IProjectionListener> projectionListener = new ArrayList<IProjectionListener>();
	
	private static ConservationAreaManager instance = null;
	
	/**
	 * 
	 * @return the instance of the conservation area manager
	 */
	public static ConservationAreaManager getInstance(){
		if (instance == null){
			instance = new ConservationAreaManager();
		}
		return instance;
	}
	
	private ConservationAreaManager(){}
	
	/**
	 * Deletes a given conservation area.  Once the conservation
	 * area is deleted the application restarts.
	 * 
	 * @param ca the conservation area to delete
	 * @param monitor the progress monitor; cannot be null
	 * @throws Exception if conservation area not deleted
	 */
	public void deleteConservationArea(ConservationArea ca, IProgressMonitor monitor) throws Exception{
		
		int work = 1;
		for (ArrayList<ICaDeleteHandler> data : deleteListeners.values()){
			work += data.size();
		}
		
		monitor.beginTask(Messages.ConservationAreaManager_Progress_DeleteCa, work);
		monitor.worked(0);
		Session session = HibernateManager.openSession();
		session.update(ca);
		
		session.beginTransaction();
		try{
			final File fileStore = new File(ca.getFileDataStoreLocation());
			
			runDeleteHandlers(ca, session, monitor);
			monitor.subTask(Messages.ConservationAreaManager_Progress_DeleteCa);
			session.delete(ca);
			session.getTransaction().commit();
			monitor.worked(1);
			
			if (fileStore.exists()){
				monitor.subTask(Messages.ConservationAreaManager_Progress_RemoveFileStore);
				try{
					FileUtils.forceDelete(fileStore);
				}catch(final Exception ex){
					Display.getDefault().syncExec(new Runnable(){
						@Override
						public void run() {
							SmartPlugIn.displayLog(Display.getDefault().getActiveShell(), 
								Messages.ConservationAreaManager_Error_DeletingFilestore + fileStore.getAbsolutePath(), ex);
						}});
				}
			}
			monitor.worked(1);
			monitor.subTask(Messages.ConservationAreaManager_Progress_Restarting);
		}catch (Exception ex){
			session.getTransaction().rollback();
			throw ex;
		}finally{
			session.close();
		}
		//logout
		Display.getDefault().syncExec(new Runnable(){
			@Override
			public void run() {
				PlatformUI.getWorkbench().restart();
			}});
	}
	
	/**
	 * Runs all the delete handlers in the order provided.
	 * 
	 */
	private void runDeleteHandlers(ConservationArea ca, Session session, IProgressMonitor monitor) throws Exception{
		ArrayList<Integer> items = new ArrayList<Integer> ();
		items.addAll(deleteListeners.keySet());
		Collections.sort(items);
		for(int i = items.size() -1; i >= 0; i --){
			ArrayList<ICaDeleteHandler> listeners = deleteListeners.get(items.get(i));
			for (ICaDeleteHandler listener : listeners){
				listener.beforeDelete(ca, session, monitor);
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
	public void addDeleteHandler(ICaDeleteHandler listener, int deleteOrder){
		assert deleteOrder > 0;
		
		ArrayList<ICaDeleteHandler> listeners = deleteListeners.get(deleteOrder);
		if (listeners == null){
			listeners = new ArrayList<ICaDeleteHandler>();
			deleteListeners.put(deleteOrder, listeners);
		}
		listeners.add(listener);
	}
	
	
	
	/**
	 * Adds an listener that is fired when areas
	 * are modified.
	 * 
	 * @param listener
	 */
	public void addAreaChangeListener(IAreaModifiedListener listener){
		areaListeners.add(listener);
	}
	
	/**
	 * Removes an area change listener
	 * 
	 * @param listener
	 */
	public void removeAreaChangeListener(IAreaModifiedListener listener){
		areaListeners.remove(listener);
	}
	
	/**
	 * Fires all area changes listeners 
	 * 
	 * @param type the type of area modified.
	 */
	public void fireAreaChanged(Area.AreaType type){
		for(IAreaModifiedListener listener : areaListeners){
			listener.areasUpdated(type);
		}
	}
	
	/**
	 * Add employee listener
	 * @param listener
	 */
	public void addEmployeeListener(IEmployeeListener listener){
		employeeListener.add(listener);
	}
	
	/**
	 * Removes an employee listener
	 * @param listener
	 */
	public void removeEmployeeListener(IEmployeeListener listener){
		employeeListener.remove(listener);
	}
	
	/**
	 * Fires employee before delete event 
	 * @param toDelete employee to delete
	 * @param session current session with open transaction
	 */
	public void fireEmployeeBeforeDelete(Employee toDelete, Session session) {
		for(IEmployeeListener listener : employeeListener){
			listener.beforeDelete(toDelete, session);
		}
	}
	
	
	/**
	 * Adds a projection list listener for changes to the conservation
	 * area projection list.
	 * 
	 * @param listener project list listener
	 */
	public void addProjectListListener(IProjectionListener listener){
		projectionListener.add(listener);
	}
	
	/**
	 * Removes an projection list listener.
	 * 
	 * @param listener
	 */
	public void removeProjectionListnListener(IProjectionListener listener){
		projectionListener.remove(listener);
	}
	
	/**
	 * Fires project list listeners after the project list
	 * has been modified and modifications saved to the 
	 * database.
	 */
	public void fireProjectionListModified() {
		for(IProjectionListener listener : projectionListener){
			listener.projectionsModified();
		}
	}
}
