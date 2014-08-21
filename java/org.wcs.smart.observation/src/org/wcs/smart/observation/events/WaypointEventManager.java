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
package org.wcs.smart.observation.events;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.wcs.smart.observation.model.Waypoint;

/**
 * Manager for waypoint event.
 * @author Emily
 *
 */
public class WaypointEventManager {

	/**
	 * Various waypoint event types
	 * @author Emily
	 *
	 */
	public static enum EventType{
		WAYPOINT_MODIFIED,
		WAYPOINT_DELETED,
		WAYPOINT_OPTIONS_MODIFIED,
	}

	
	private HashMap<EventType, List<IWaypointEventListener>> listeners = new HashMap<EventType, List<IWaypointEventListener>>();
		
	private static WaypointEventManager instance;
	
	/**
	 * 
	 * @return event manager instance
	 */
	public synchronized static WaypointEventManager getInstance(){
		if (instance == null){
			instance = new WaypointEventManager();
		}
		return instance;
	}
	
	private WaypointEventManager(){
		
	}
	
	/**
	 * Adds a listener
	 * @param type
	 * @param listener
	 */
	public void addListener(EventType type, IWaypointEventListener listener){
		List<IWaypointEventListener> typelisteners = listeners.get(type);
		if (typelisteners == null){
			typelisteners = new ArrayList<IWaypointEventListener>();
			listeners.put(type, typelisteners);
		}
		typelisteners.add(listener);
	}
	
	
	/**
	 * removes a listener
	 * @param type
	 * @param listener
	 */
	public void removeListener(EventType type, IWaypointEventListener listener){
		List<IWaypointEventListener> typelisteners = listeners.get(type);
		if (typelisteners != null){
			typelisteners.remove(listener);
		}
	}
	
	/**
	 * Fires waypoint modified events
	 * @param wp
	 */
	public void waypointModified(Waypoint wp){
		fireListeners(EventType.WAYPOINT_MODIFIED, wp);
	}
	
	/**
	 * Fires waypoint delete events
	 * @param wp
	 */
	public void waypointDeleted(Waypoint wp){
		fireListeners(EventType.WAYPOINT_DELETED,wp);
	}
	
	/**
	 * Fires waypoint options changed event
	 * @param wp
	 */
	public void waypointOptionsModified(){
		fireListeners(EventType.WAYPOINT_OPTIONS_MODIFIED,null);
	}
	
	/*
	 * fires events for given event types
	 */
	private void fireListeners(EventType type, Waypoint wp){
		List<IWaypointEventListener> typelisteners = listeners.get(type);
		if (typelisteners != null){
			for (IWaypointEventListener l : typelisteners){
				l.handleEvent(wp);
			}
		}
	}
	
	
}
