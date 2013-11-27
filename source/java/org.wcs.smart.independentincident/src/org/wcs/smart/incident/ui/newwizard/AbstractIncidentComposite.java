package org.wcs.smart.incident.ui.newwizard;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.hibernate.Session;
import org.wcs.smart.observation.model.Waypoint;

public abstract class AbstractIncidentComposite {

	private List<Listener> listeners = new ArrayList<Listener>();
	
	public void addChangeListener(Listener listener){
		listeners.add(listener);
	}
	
	public void removeChangeListener(Listener listener){
		listeners.remove(listener);
	}
	
	public void fireChange(Event e){
		for (Listener l : listeners){
			l.handleEvent(e);
		}
	}
	
	
	public abstract String getName();
	
	public abstract String getDescription();
	
	
	public abstract String validate(); 
	
	public abstract Composite createComposite(Composite parent);
	
	public abstract void updateIncident(Waypoint incident);
	
	public abstract void initFields(Waypoint incident, Session session);
}
