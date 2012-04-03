package org.wcs.smart.query;

import java.util.ArrayList;

import org.wcs.smart.query.model.WaypointQuery;

public class QueryEventManager {

	
	private static QueryEventManager instance = null;
	private ArrayList<QueryChangedListener> listeners = new ArrayList<QueryChangedListener>();
	
	public static QueryEventManager getInstance(){
		if (instance == null){
			instance = new QueryEventManager();
		}
		return instance;
	}
	
	private QueryEventManager(){
		
	}
	
	public void fireQueryChangedListeners(WaypointQuery query){
		for (QueryChangedListener listener: listeners){
			listener.queryChanged(query);
			
		}
	}
	
	public void addQueryChangedEvent(QueryChangedListener listener){
		listeners.add(listener);
	}
	public void removeQueryChangedEvent(QueryChangedListener listener){
		listeners.remove(listener);
	}
}
