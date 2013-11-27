package org.wcs.smart.incident.event;

import java.util.ArrayList;
import java.util.List;

public class IncidentEventManager {

	public static int INCIDENT_ADDED= 1;
	public static int INCIDENT_MODIFIED = 2;
	public static int INCIDENT_DELETED = 3;
	
	private static IncidentEventManager  instance;
	
	private List<IIncidentListener> listeners = new ArrayList<IIncidentListener>();
	
	public static IncidentEventManager getInstance(){
		if (instance == null){
			instance = new IncidentEventManager();
		}
		return instance;
	}
	
	public void fireEvent(int id, Object source){
		for (IIncidentListener listener : listeners){
			listener.handleEvent(id, source);
		}
	}
	
	public void addListener(IIncidentListener listener){
		listeners.add(listener);
	}
	
	public void removeListener(IIncidentListener listener){
		listeners.remove(listener);
	}
}
