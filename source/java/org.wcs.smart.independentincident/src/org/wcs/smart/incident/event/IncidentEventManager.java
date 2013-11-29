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
package org.wcs.smart.incident.event;

import java.util.ArrayList;
import java.util.List;

/**
 * Event manager for incidents.
 * 
 * @author Emily
 *
 */
public class IncidentEventManager {

	/**
	 * Incident added event
	 */
	public static int INCIDENT_ADDED= 1;
	
	/**
	 * Incident modified event
	 */
	public static int INCIDENT_MODIFIED = 2;
	
	/**
	 * Incident removed event
	 */
	public static int INCIDENT_DELETED = 3;
	
	private static IncidentEventManager  instance;
	
	private List<IIncidentListener> listeners = new ArrayList<IIncidentListener>();
	
	/**
	 * 
	 * @return manager incident
	 */
	public static IncidentEventManager getInstance(){
		if (instance == null){
			instance = new IncidentEventManager();
		}
		return instance;
	}
	
	/**
	 * Fires an event 
	 * @param id the event id
	 * @param source the source object.  This should be a
	 * Waypoint or IncidentEditorInput object
	 */
	public void fireEvent(int id, Object source){
		for (IIncidentListener listener : listeners){
			listener.handleEvent(id, source);
		}
	}
	
	/**
	 * Adds a listener
	 * @param listener
	 */
	public void addListener(IIncidentListener listener){
		listeners.add(listener);
	}
	
	/**
	 * Removes a listener
	 * @param listener
	 */
	public void removeListener(IIncidentListener listener){
		listeners.remove(listener);
	}
}
