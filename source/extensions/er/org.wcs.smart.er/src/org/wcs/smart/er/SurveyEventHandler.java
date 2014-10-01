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
package org.wcs.smart.er;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Event handler for survey objects.
 * 
 * @author Emily
 *
 */
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
		List<ISurveyEventListener> l = new ArrayList<ISurveyEventListener>();
		if (events.get(type) == null) return;
		l.addAll(events.get(type));
		for (ISurveyEventListener listener: l){
			listener.event(object);
		}
	}

}
