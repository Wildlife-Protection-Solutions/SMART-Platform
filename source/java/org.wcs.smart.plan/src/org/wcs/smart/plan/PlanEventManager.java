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
package org.wcs.smart.plan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.wcs.smart.plan.model.Plan;

/**
 * Event manager for managing Plan events.
 * 
 * @author elitvin
 * @author jeffloun
 * @since 1.0.0
 */
public class PlanEventManager {

	private final static PlanEventManager INSTANCE = new PlanEventManager();
	
	/**
	 * event type when the patrol/plan link is changed
	 */
	public static final int PATROL_PLAN_ATTRIBUTE = 4;
	
	//registered listeners
	private HashMap<EventType, List<IPlanEventListener>> listeners = new HashMap<EventType, List<IPlanEventListener>>();
	
	
	/**
	 * Types of Plan events that can be listened to
	 * 
	 *
	 */
	public static enum EventType {
		PLAN_ADDED,
		PLAN_DELETED,
		PLAN_MODIFIED
	}

	/**
	 * 
	 * @return the Plan event manager instance
	 */
	public static PlanEventManager getInstance(){
		return INSTANCE;
	}
	
	/**
	 * Fires a Plan added event
	 */
	public void planAdded(Plan plan){
		fireListeners(EventType.PLAN_ADDED, -1, plan);
	}
		
	/**
	 * Fires a Plan deleted event
	 */
	public void PlanDeleted(Plan source){
		fireListeners(EventType.PLAN_DELETED, -1, source);
	}
	
	/**
	 * Fires a Plan changed event
	 */
	public void planChanged(int attributeChanged, Plan source){
		fireListeners(EventType.PLAN_MODIFIED, attributeChanged, source);
	}
	
	private void fireListeners(EventType type, int attributeChange, Plan source){
		if (listeners.get(type) != null){
			//prevents concurrentmodificationexceptions
			ArrayList<IPlanEventListener> localListeners = new ArrayList<IPlanEventListener>();
			localListeners.addAll(listeners.get(type));
			for (IPlanEventListener listener : localListeners){
				try{
					listener.eventFired(attributeChange, source);
				}catch (Exception ex){
					SmartPlanPlugIn.displayLog("Error firing event listeners.  It is recommended you restart the application." + ex.getLocalizedMessage(), ex);
				}
			}
		}
	}
	
	/**
	 * Adds a Plan listener for a particular Plan event
	 * 
	 * @param type Plan event type
	 * @param listener listener
	 */
	public void addListener(EventType type, IPlanEventListener listener){
		if (listeners.get(type) == null){
			listeners.put(type, new ArrayList<IPlanEventListener>());
		}
		listeners.get(type).add(listener);
	}
	
	/**
	 * Removes a Plan listener for a particular Plan event
	 * 
	 * @param type Plan event type
	 * @param listener listener
	 */
	public void removeListener(EventType type, IPlanEventListener listener){
		List<IPlanEventListener> lists = listeners.get(type);
		if (lists != null){
			lists.remove(listener);
		}
	}
	
	/**
	 * Plan event listener.  This listener
	 * is used for all Plan events.
	 *
	 */
	public interface IPlanEventListener{
		/**
		 * attributeChanged - is the type of attribute modified
		 * 
		 * @param attributeChanged  -1 if Plan added or removed
		 * @param source the source object of the event.  Either a Plan object
		 */
		public void eventFired(int attributeChanged, Plan source);
	}
	
}
