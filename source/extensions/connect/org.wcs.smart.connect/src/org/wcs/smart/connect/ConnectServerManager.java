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
package org.wcs.smart.connect;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.connect.internal.CaConnectDeleteHandler;
import org.wcs.smart.connect.internal.Messages;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;

/**
 * Manages the connect server instance.  
 * 
 * @author Emily
 *
 */
public enum ConnectServerManager {
	INSTANCE;
	
	
	private List<IConnectServerEventHandler> listeners = new ArrayList<IConnectServerEventHandler>();
	
	/**
	 * Adds a connect server event handler.
	 * 
	 * @param listener
	 */
	public void addHandler(IConnectServerEventHandler listener){
		listeners.add(listener);
	}
	
	/**
	 * Removes server event handler
	 * @param listener
	 */
	public void removeHandler(IConnectServerEventHandler listener){
		listeners.remove(listener);
	}
	
	/*
	 * Runs after delete event for all registered handlers
	 */
	void runAfterDeleteHandlers(Session session) throws Exception{
		for(IConnectServerEventHandler l : listeners){
			l.beforeDelete(session);
		}
	}
	
	/**
	 * Deletes the current connect server configuration and all
	 * associated data.
	 * 
	 * @param monitor
	 * @throws Exception
	 */
	public void deleteConnectServerData(IProgressMonitor monitor) throws Exception{
		monitor.beginTask(Messages.ConnectServerInfoDialog_DeleteServerTaskName, 6);
		
		Session s = HibernateManager.openSession();
		try{
			s.beginTransaction();

			ConservationArea ca = SmartDB.getCurrentConservationArea();
			//delete items from database
			(new CaConnectDeleteHandler()).beforeDelete(ca, s, new SubProgressMonitor(monitor, 6));
		
			//delete items from the datastore
			Path fs = Paths.get(ca.getFileDataStoreLocation(), ConnectDatastore.CONNECT_FILESTORE_DIR);
			if (Files.exists(fs)){
				try{
					FileUtils.forceDelete(fs.toFile());
				}catch (Exception ex){
					ConnectPlugIn.log(ex.getMessage(), ex);
				}
			}
			
			//run any delete handlers
			runAfterDeleteHandlers(s);
		
			s.getTransaction().commit();
		}finally{
			if (s.getTransaction().isActive()){
				s.getTransaction().rollback();
			}
			s.close();
			monitor.done();
		}
	}
	
	/**
	 * Handler for connect server events.
	 * @author Emily
	 *
	 */
	public interface IConnectServerEventHandler{
		/**
		 * Called before the server is deleted.
		 * @param session database session in active transaction
		 */
		public void beforeDelete(Session session) throws Exception;
	}
}
