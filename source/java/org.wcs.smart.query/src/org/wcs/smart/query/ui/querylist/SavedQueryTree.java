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
package org.wcs.smart.query.ui.querylist;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.IQueryFolderListener;
import org.wcs.smart.query.IQueryHibernateManager;
import org.wcs.smart.query.QueryHibernateManager;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.event.IQueryListener;
import org.wcs.smart.query.event.QueryEventManager;
import org.wcs.smart.query.event.QueryListenerAdapter;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryFolder;
import org.wcs.smart.query.ui.editor.QueryEditorInput;

/**
 * A tree that represents the list of saved 
 * queries.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class SavedQueryTree {

	private static SavedQueryTree instance = null;
	
	private List<QueryFolder> folders = null;
	private HashMap<UUID, List<QueryEditorInput>> queries = null;
	
	
	private List<ISourceChangedListener> listeners = new ArrayList<ISourceChangedListener>();
	
	private Job loadQueriesJob = new Job(Messages.SavedQueryTree_LoadingJobName){
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			Session s = HibernateManager.openSession();
			s.beginTransaction();
			try{
				folders = QueryHibernateManager.getInstance().getQueryFolders(s, true);
				queries = QueryHibernateManager.getInstance().getQueryProxies(s);
			}catch (Exception ex){
				QueryPlugIn.displayLog(Messages.SavedQueryTree_ErrorLoadingQueries + ex.getLocalizedMessage(), ex);
			}finally{
				s.getTransaction().rollback();
				s.close();
			}
			return Status.OK_STATUS;
		}
	};
	
	/**
	 * query folder listener that listens for query change events
	 * and updates the query tree source as required.  It
	 * then fires change events.
	 * 
	 */
	private IQueryListener listener = new QueryListenerAdapter() {
		
		@Override
		public void folderModified(int eventType, Object object) {
		
			if (eventType == IQueryListener.FOLDER_DELETED){
				QueryFolder folder = (QueryFolder)object;
				if (folder.getParentFolder() == null){
					for (QueryFolder f: folders){
						f.getChildren().remove(folder);
					}			
				}
			}
			fireListeners(eventType, object);
		}
		
		@Override
		public void queryModified(int eventType, Object object) {
			if (eventType == IQueryListener.QUERY_DELETED){
				QueryEditorInput query = (QueryEditorInput)object;
				for (List<QueryEditorInput> list : queries.values()){
					list.remove(query);
				}
			}else if (eventType == IQueryListener.QUERY_SAVED){
				//update the name
				Query query = (Query)object;				
				for (List<QueryEditorInput> list : queries.values()){
					for (QueryEditorInput input : list){
						if ( input.getUuid().equals(query.getUuid()) ){
							input.setQueryName(query.getName());
							break;
						}
					}
				}
			}else if (eventType == IQueryListener.QUERY_ADDED){
				Query query = (Query)object;			
				UUID key = null;
				if (query.getFolder() != null){
					key = query.getFolder().getUuid();
				}else if (query.getIsShared()){
					key = IQueryHibernateManager.CA_QUERY_KEY;
				}else{
					key = IQueryHibernateManager.USER_QUERY_KEY;
				}
				List<QueryEditorInput> ins = queries.get(key);
				if (ins == null){
					ins = new ArrayList<QueryEditorInput>();
					queries.put(key, ins);
				}
				object = new QueryEditorInput(query);
				ins.add((QueryEditorInput)object);
			}
			fireListeners(eventType, object);
		}	
	};

	
	/**
	 * Fires all listeners
	 * @param eventType
	 * @param object
	 */
	private void fireListeners(int eventType, Object object){
		for (ISourceChangedListener listener: listeners){
			listener.sourceChanged(eventType, object);
		}
	}
	
	/**
	 * Adds a source changed listener
	 * @param listener
	 */
	public void addListener(ISourceChangedListener listener){
		this.listeners.add(listener);
	}
	
	/**
	 * Removes a source changed listener
	 * @param listener
	 */
	public void removeListener(ISourceChangedListener listener){
		this.listeners.remove(listener);
	}
	
	/**
	 * Creates a new query tree
	 */
	private SavedQueryTree(){
		QueryEventManager.getInstance().addListener(listener);
	}
	
	/**
	 * Disposed of the query tree
	 */
	public void dispose(){
		QueryEventManager.getInstance().removeListener(listener);
	}
	
	/**
	 * @return the instance of this class
	 */
	public static synchronized SavedQueryTree getInstance(){
		if (instance == null){
			instance = new SavedQueryTree();
		}
		return instance;
	}
	
	/**
	 * @return the existing folders
	 */
	public List<QueryFolder> getFolders(){
		if (folders == null){
			loadData();
		}
		return folders;		
	}
	
	/**
	 * loads the folders and queries from the database
	 */
	private synchronized void loadData(){
		if (folders == null || queries == null){
			loadQueriesJob.schedule();
			try{
				loadQueriesJob.join();
			}catch (Exception ex){
				QueryPlugIn.displayLog(Messages.SavedQueryTree_CouldNotLoadQueries, ex);
			}
		}
	}
	
	/**
	 * @return the saved queries; a map of the hashcode of the uuid array of a query folder 
	 * mapped to set of query input of the query
	 */
	public HashMap<UUID, List<QueryEditorInput>> getQueries(){
		if (queries == null){
			loadData();
		}
		return queries;
	}
	
	/**
	 * Listener fired when the saved query tree source is changed
	 * 
	 * @author egouge
	 * @since 1.0.0
	 */
	interface ISourceChangedListener{
		/**
		 * 
		 * @param eventType the type of event that caused the change 
		 * @see IQueryFolderListener
		 * @param object the object involved in the change
		 */
		void sourceChanged(int eventType, Object object);
	}
}
