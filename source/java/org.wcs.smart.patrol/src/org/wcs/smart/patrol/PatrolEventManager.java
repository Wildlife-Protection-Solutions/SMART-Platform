package org.wcs.smart.patrol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Event manager for managing patrol events.
 * 
 * @author Emily
 *
 */
public class PatrolEventManager {

	private final static PatrolEventManager INSTANCE = new PatrolEventManager();
	
	//registered listeners
	private HashMap<EventType, List<IPatrolEventListener>> listeners = new HashMap<EventType, List<IPatrolEventListener>>();
	
	/**
	 * Types of patrol events that can be listened to
	 * 
	 *
	 */
	public static enum EventType{
		PATROL_ADDED,
		PATROL_DELETED,
		PATROL_MODIFIED
	}
	
	/**
	 * 
	 * @return the patrol event manager instance
	 */
	public static PatrolEventManager getInstance(){
		return INSTANCE;
	}
	
	/**
	 * Fires a patrol added event
	 */
	public void patrolAdded(){
		fireListeners(EventType.PATROL_ADDED);
	}
	
	/**
	 * Fires a patrol deleted event
	 */
	public void patrolDeleted(){
		fireListeners(EventType.PATROL_DELETED);
	}
	
	/**
	 * Fires a patrol changed event
	 */
	public void patrolChanged(){
		fireListeners(EventType.PATROL_MODIFIED);
	}
	
	private void fireListeners(EventType type){
		if (listeners.get(type) != null){
			for (IPatrolEventListener listener : listeners.get(type)){
				listener.eventFired();
			}
		}
	}
	
	/**
	 * Adds a patrol listener for a particular patrol event
	 * 
	 * @param type patrol event type
	 * @param listener listener
	 */
	public void addListener(EventType type, IPatrolEventListener listener){
		if (listeners.get(type) == null){
			listeners.put(type, new ArrayList<IPatrolEventListener>());
		}
		listeners.get(type).add(listener);
	}
	
	/**
	 * Patrol event listener.  This listener
	 * is used for all patrol events.
	 *
	 */
	public interface IPatrolEventListener{
		public void eventFired();
	}
}
