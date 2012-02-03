package org.wcs.smart.patrol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class PatrolEventManager {

	private final static PatrolEventManager INSTANCE = new PatrolEventManager();
	
	private HashMap<EventType, List<IPatrolEventListener>> listeners = new HashMap<EventType, List<IPatrolEventListener>>();
	
	public static enum EventType{
		PATROL_ADDED,
		PATROL_DELETED,
		PATROL_MODIFIED
	}
	
	public static PatrolEventManager getInstance(){
		return INSTANCE;
	}
	
	public void patrolAdded(){
		fireListeners(EventType.PATROL_ADDED);
	}
	
	public void patrolDeleted(){
		fireListeners(EventType.PATROL_DELETED);
	}
	
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
	
	
	public void addListener(EventType type, IPatrolEventListener listener){
		if (listeners.get(type) == null){
			listeners.put(type, new ArrayList<IPatrolEventListener>());
		}
		listeners.get(type).add(listener);
	}
	
	public interface IPatrolEventListener{
		public void eventFired();
	}
}
