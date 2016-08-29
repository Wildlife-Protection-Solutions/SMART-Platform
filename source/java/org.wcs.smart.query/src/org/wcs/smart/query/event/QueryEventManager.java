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
package org.wcs.smart.query.event;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryFolder;
/**
 * Query event manager for managing query events.
 * @author Emily
 *
 */
public class QueryEventManager {

	private static QueryEventManager instance = new QueryEventManager();
	
	private List<IQueryListener> listeners = Collections.synchronizedList(new ArrayList<IQueryListener>());
	
	private QueryEventManager(){
		
	}
	
	public static QueryEventManager getInstance(){
		return instance;
	}
	
	/**
	 * adds a listener
	 * @param listener
	 */
	public void addListener(IQueryListener listener){
		listeners.add(listener);
	}
	
	/**
	 * removes a listener
	 * @param listener
	 */
	public void removeListener(IQueryListener listener){
		listeners.remove(listener);
	}
	
	private Collection<IQueryListener> getListeners(){
		synchronized (listeners) {
			return new ArrayList<IQueryListener>(listeners);
		}
	}
	/**
	 * fires run query event
	 * @param query
	 */
	public void fireRunQuery(Query query){
		for (IQueryListener l : getListeners()){
			l.queryRun(query);
		}	
	}
	
	/**
	 * fire refresh query listener
	 */
	public void fireRefreshQuery(Query query){
		for (IQueryListener l : getListeners()){
			l.queryRefreshed(query);
		}
		
	}
	
	/**
	 * Fire before delete listened
	 * @param query
	 * @param session
	 * @return
	 */
	public boolean fireBeforeDelete(Query query, Session session){
		for (IQueryListener l : getListeners()){
			if (!l.beforeDelete(query, session)){
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Fire before save listener 
	 * @param query
	 * @param session
	 * @return
	 */
	public boolean fireBeforeSave(Query query, Session session){
		for (IQueryListener l : getListeners()){
			if (!l.beforeSave(query, session)){
				return false;
			}
		}
		return true;
	}
	

	
	/**
	 * fire query folder added
	 * @param folder
	 */
	public void fireFolderAdded(QueryFolder folder){
		for (IQueryListener l : getListeners()){
			l.folderModified(IQueryListener.FOLDER_ADDED, folder);
		}
	}
	
	/**
	 * fire query folder deleted
	 * @param folder
	 */
	public void fireFolderDeleted(QueryFolder folder){
		for (IQueryListener l : getListeners()){
			l.folderModified(IQueryListener.FOLDER_DELETED, folder);
		}
	}
	
	/**
	 * fire query folder renamed
	 * @param folder
	 */
	public void fireFolderRenamed(QueryFolder folder){
		for (IQueryListener l : getListeners()){
			l.folderModified(IQueryListener.FOLDER_RENAMED, folder);
		}
	}
	
	/**
	 * fire query deleted event
	 * @param object query or queryeditorinput
	 */
	public void fireQueryDeleted(Object object){
		for (IQueryListener l : getListeners()){
			l.queryModified(IQueryListener.QUERY_DELETED, object);
		}
	}
	
	/**
	 * fire query added event
	 * @param object
	 */
	public void fireQueryAdded(Object object){
		for (IQueryListener l : getListeners()){
			l.queryModified(IQueryListener.QUERY_ADDED, object);
		}
	}
	
	/**
	 * fire query saved event
	 * @param object
	 */
	public void fireQuerySaved(Object object){
		for (IQueryListener l : getListeners()){
			l.queryModified(IQueryListener.QUERY_SAVED, object);
		}
	}
	
	/**
	 * fire query name modified event
	 * @param query
	 */
	public void fireQueryNameModified(Query query){
		for (IQueryListener l : getListeners()){
			l.queryModified(IQueryListener.QUERY_NAME_MODIFIED, query);
		}
	}
	
	/**
	 * fire query definition modified event
	 * @param query
	 */
	public void fireQueryDefinitionModified(Query query){
		for (IQueryListener l : getListeners()){
			l.queryModified(IQueryListener.QUERY_DEFINITION_MODIFIED, query);
		}
	}
}
