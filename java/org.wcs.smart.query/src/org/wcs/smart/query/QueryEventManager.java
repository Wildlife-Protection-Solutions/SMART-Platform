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

import org.wcs.smart.query.model.WaypointQuery;

/**
 * Query event manager for managing query change events.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class QueryEventManager {

	private static QueryEventManager instance = null;
	
	private ArrayList<QueryChangedListener> listeners = new ArrayList<QueryChangedListener>();
	
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
	 * Fires all query changed listeners.
	 * 
	 * @param query the query that was modified
	 */
	public void fireQueryChangedListeners(WaypointQuery query){
		for (QueryChangedListener listener: listeners){
			listener.queryChanged(query);
			
		}
	}
	
	/**
	 * Adds a query change listener 
	 * @param listener the listener
	 */
	public void addQueryChangedEvent(QueryChangedListener listener){
		listeners.add(listener);
	}
	
	/**
	 * Removes a query change listener
	 * @param listener the listener to remove
	 */
	public void removeQueryChangedEvent(QueryChangedListener listener){
		listeners.remove(listener);
	}
}
