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
package org.wcs.smart.query;

import java.util.ArrayList;

import org.wcs.smart.query.model.Query;

/**
 * Query event manager for managing query change events.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class QueryEventManager {

	private static QueryEventManager instance = null;
	
	private ArrayList<IQueryListener> listeners = new ArrayList<IQueryListener>();
	private ArrayList<IQueryFolderListener> folderListeners = new ArrayList<IQueryFolderListener>();
	
	
	/**
	 * @return the query event manager
	 */
	public static QueryEventManager getInstance(){
		if (instance == null){
			instance = new QueryEventManager();
		}
		return instance;
	}
	
	/**
	 * Creates a new query event manager
	 */
	private QueryEventManager(){
	}
	
	/**
	 * Fires the query changed method of all
	 * query listeners
	 * 
	 * @param query the query that was modified
	 */
	public void fireQueryChangedListeners(Query query){
		for (IQueryListener listener: listeners){
			listener.queryChanged(query);
			
		}
	}
	
	/**
	 * Fires all query run event on all query listeners.
	 * 
	 * @param query the query that is to be run 
	 */
	public void fireQueryRunListeners(Query query){
		for (IQueryListener listener: listeners){
			listener.queryRun(query);
			
		}
	}
	
	/**
	 * Fires all query changed listeners.
	 * 
	 * @param query the query that was modified
	 */
	public void fireFolderChangedListeners(int eventType, Object folder){
		for (IQueryFolderListener listener: folderListeners){
			listener.folderChanged(eventType, folder);
			
		}
	}
	
	
	/**
	 * Adds a query change listener 
	 * @param listener the listener
	 */
	public void addQueryChangedEvent(IQueryListener listener){
		listeners.add(listener);
	}
	
	/**
	 * Removes a query change listener
	 * @param listener the listener to remove
	 */
	public void removeQueryChangedEvent(IQueryListener listener){
		listeners.remove(listener);
	}
	
	/**
	 * Adds a query folder listener 
	 * @param listener the listener
	 */
	public void addQueryFolderListener(IQueryFolderListener listener){
		folderListeners.add(listener);
	}
	
	/**
	 * Removes a query folder listener
	 * @param listener the listener to remove
	 */
	public void removeQueryFolderListener(IQueryFolderListener listener){
		folderListeners.remove(listener);
	}
}
