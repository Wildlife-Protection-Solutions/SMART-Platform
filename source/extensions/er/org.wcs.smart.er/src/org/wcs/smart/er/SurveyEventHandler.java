package org.wcs.smart.er;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SurveyEventHandler {

	public enum  EventType{
			SURVEY_DESIGN_ADDED,
			SURVEY_DESIGN_MODIFIED,
			SURVEY_DESIGN_DELETED,
			SURVEY_ADDED,
			SURVEY_MODIFIED,
			SURVEY_DELETED,
			MISSION_ADDED,
			MISSION_MODIFIED,
			MISSION_DELETED
	};
	
	private HashMap<EventType, List<ISurveyEventListener>> events;
	
	private static SurveyEventHandler instance;
	
	public static SurveyEventHandler getInstance(){
		if (instance == null){
			instance = new SurveyEventHandler();
		}
		return instance;
	}
	
	private SurveyEventHandler(){
		events = new HashMap<EventType, List<ISurveyEventListener>>();
	}
	/**
	 * @param type
	 * @param listener
	 */
	public void addListener(EventType type, ISurveyEventListener listener){
		List<ISurveyEventListener> list = events.get(type);
		if (list == null){
			list = new ArrayList<ISurveyEventListener>();
			events.put(type, list);
		}
		list.add(listener);
	}
	
	public void removeListener(EventType type, ISurveyEventListener listener) {
		List<ISurveyEventListener> list = events.get(type);
		if (list != null) {
			list.remove(listener);
		}
	}

	public void fireEvent(EventType type, Object object){
		List<ISurveyEventListener> l = events.get(type);
		if (l != null){
			for (ISurveyEventListener listener: l){
				listener.event(object);
			}
		}
	}

}
