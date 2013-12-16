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
package org.wcs.smart.entity.event;

import java.util.ArrayList;
import java.util.List;

/**
 * Event manager for incidents.
 * 
 * @author Emily
 *
 */
public class EntityEventManager {

	/**
	 * Entity type added event
	 */
	public static int ENTITY_TYPE_ADDED = 1;

	/**
	 * Entity type modified event
	 */
	public static int ENTITY_TYPE_MODIFIED = 2;
	
	/**
	 * Entity type deleted event
	 */
	public static int ENTITY_TYPE_DELETED = 3;

	
	/**
	 * Entity added 
	 */
	public static int ENTITY_ADDED = 4;
	
	/**
	 * Entity modified 
	 */
	public static int ENTITY_MODIFIED = 5;
	
	/**
	 * Entity removed
	 */
	public static int ENTITY_DELETED = 6;
	
	
	
	private static EntityEventManager  instance;
	
	private List<IEntityListener> listeners = new ArrayList<IEntityListener>();
	
	/**
	 * 
	 * @return manager incident
	 */
	public static EntityEventManager getInstance(){
		if (instance == null){
			instance = new EntityEventManager();
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
		for (IEntityListener listener : listeners){
			listener.handleEvent(id, source);
		}
	}
	
	/**
	 * Adds a listener
	 * @param listener
	 */
	public void addListener(IEntityListener listener){
		listeners.add(listener);
	}
	
	/**
	 * Removes a listener
	 * @param listener
	 */
	public void removeListener(IEntityListener listener){
		listeners.remove(listener);
	}
}
