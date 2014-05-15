package org.wcs.smart.patrol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.wcs.smart.observation.events.WaypointEventManager;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolWaypoint;

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
		PATROL_MODIFIED, 
		PATROL_SAVED,
		WAYPOINT_MODIFIED,
		WAYPOINT_DELETED
	}
	
	/**
	 * Patrol station attribute
	 */
	public static int PATROL_STATION = 1;
	/**
	 * Patrol team attribute
	 */
	public static int PATROL_TEAM = 2;
	/**
	 * Patrol objective attribute
	 */
	public static int PATROL_OBJECTIVE = 3;
	/**
	 * Patrol leg changed; this includes changes to transportation type,
	 * employees, or dates
	 */
	public static int PATROL_DATES_LEG = 4;
	
	/**
	 * Patrol waypoints.   Fired when waypoints associated with any leg
	 * is modified.
	 */
	public static int PATROL_WAYPOINTS = 5;
	
	/**
	 * Patrol tracks.  Fired when track associated with any leg
	 * is modified.
	 */
	public static int PATROL_TRACKS = 6;
	
	/**
	 * Patrol armed attribute
	 */
	public static int PATROL_ARMED = 7;
	/**
	 * Patrol mandate object
	 */
	public static int PATROL_MANDATE = 7;
	
	/**
	 * Patrol comment object
	 */
	public static int PATROL_COMMENT = 8;
	
	/**
	 * Patrol id
	 */
	public static int PATROL_ID = 9;
	
	/**
	 * Waypoint ID - applicable for only
	 * EventType.WAYPOINT_DELETED and WAYPOINT_MODIFIED
	 */
	public static int WAYPOINT = 10;
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
	public void patrolAdded(Patrol patrol){
		fireListeners(EventType.PATROL_ADDED, -1, patrol);
	}
	
	/**
	 * Fires a patrol added event
	 * @param patrol the patrol saved
	 * @param legsModified if the dates of the patrol or the number of
	 * legs have been modified in any way.
	 */
	public void patrolSaved(Patrol patrol, boolean legsModified){
		if (legsModified){
			fireListeners(EventType.PATROL_SAVED, PATROL_DATES_LEG, patrol);
		}else{
			fireListeners(EventType.PATROL_SAVED, -1, patrol);
		}
	}
	
	/**
	 * Fires a patrol deleted event
	 */
	public void patrolDeleted(Patrol patrol){
		fireListeners(EventType.PATROL_DELETED, -1, patrol);
	}
	
	/**
	 * Fires a patrol changed event
	 */
	public void patrolChanged(int attributeChanged, Object source){
		fireListeners(EventType.PATROL_MODIFIED, attributeChanged, source);
	}
	
	
	/**
	 * Fires a waypoint modified event.
	 * @param waypoint
	 */
	public void waypointModified(PatrolWaypoint waypoint){
		fireListeners(EventType.WAYPOINT_MODIFIED, WAYPOINT, waypoint);
		WaypointEventManager.getInstance().waypointModified(waypoint.getWaypoint());
	}
	/**
	 * Fires a waypoint deleted event.
	 * @param waypoint
	 */
	public void waypointDeleted(PatrolWaypoint waypoint){
		fireListeners(EventType.WAYPOINT_DELETED, WAYPOINT, waypoint);
		WaypointEventManager.getInstance().waypointDeleted(waypoint.getWaypoint());
	}
	
	
	private void fireListeners(EventType type, int attributeChange, Object source){
		if (listeners.get(type) != null){
			//prevents concurrentmodificationexceptions
			ArrayList<IPatrolEventListener> localListeners = new ArrayList<IPatrolEventListener>();
			localListeners.addAll(listeners.get(type));
			for (IPatrolEventListener listener : localListeners){
				listener.eventFired(attributeChange, source);
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
	 * Removes a patrol listener for a particular patrol event
	 * 
	 * @param type patrol event type
	 * @param listener listener
	 */
	public void removeListener(EventType type, IPatrolEventListener listener){
		List<IPatrolEventListener> lists = listeners.get(type);
		if (lists != null){
			lists.remove(listener);
		}
	}
	
	/**
	 * Patrol event listener.  This listener
	 * is used for all patrol events.
	 *
	 */
	public interface IPatrolEventListener{
		/**
		 * attributeChanged - is the type of attribute modified
		 * 
		 * @param attributeChanged  -1 if patrol added or removed
		 * @param source the source object of the event.  Either a Patrol object or PatrolLegDay object
		 */
		public void eventFired(int attributeChanged, Object source);
	}
}
