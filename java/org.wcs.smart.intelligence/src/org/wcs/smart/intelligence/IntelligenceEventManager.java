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
package org.wcs.smart.intelligence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.wcs.smart.intelligence.model.Intelligence;

/**
 * Event manager for managing intelligence events.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class IntelligenceEventManager {

	private final static IntelligenceEventManager INSTANCE = new IntelligenceEventManager();
	
	//registered listeners
	private HashMap<EventType, List<IIntelligenceEventListener>> listeners = new HashMap<EventType, List<IIntelligenceEventListener>>();
	
	
	/**
	 * Types of Intelligence events that can be listened to
	 * 
	 *
	 */
	public static enum EventType {
		INTELLIGENCE_ADDED,
		INTELLIGENCE_DELETED,
		INTELLIGENCE_MODIFIED
	}

	/**
	 * 
	 * @return the Intelligence event manager instance
	 */
	public static IntelligenceEventManager getInstance(){
		return INSTANCE;
	}
	
	/**
	 * Fires a Intelligence added event
	 */
	public void intelligenceAdded(Intelligence Intelligence){
		fireListeners(EventType.INTELLIGENCE_ADDED, -1, Intelligence);
	}
	
//	/**
//	 * Fires a Intelligence added event
//	 */
//	public void intelligenceSaved(Intelligence Intelligence){
//		fireListeners(EventType.INTELLIGENCE_SAVED, -1, Intelligence);
//	}
	
	/**
	 * Fires a Intelligence deleted event
	 */
	public void intelligenceDeleted(Intelligence source){
		fireListeners(EventType.INTELLIGENCE_DELETED, -1, source);
	}
	
	/**
	 * Fires a Intelligence changed event
	 */
	public void intelligenceChanged(int attributeChanged, Intelligence source){
		fireListeners(EventType.INTELLIGENCE_MODIFIED, attributeChanged, source);
	}
	
	private void fireListeners(EventType type, int attributeChange, Intelligence source){
		if (listeners.get(type) != null){
			//prevents concurrentmodificationexceptions
			ArrayList<IIntelligenceEventListener> localListeners = new ArrayList<IIntelligenceEventListener>();
			localListeners.addAll(listeners.get(type));
			for (IIntelligenceEventListener listener : localListeners){
				listener.eventFired(attributeChange, source);
			}
		}
	}
	
	/**
	 * Adds a Intelligence listener for a particular Intelligence event
	 * 
	 * @param type Intelligence event type
	 * @param listener listener
	 */
	public void addListener(EventType type, IIntelligenceEventListener listener){
		if (listeners.get(type) == null){
			listeners.put(type, new ArrayList<IIntelligenceEventListener>());
		}
		listeners.get(type).add(listener);
	}
	
	/**
	 * Removes a Intelligence listener for a particular Intelligence event
	 * 
	 * @param type Intelligence event type
	 * @param listener listener
	 */
	public void removeListener(EventType type, IIntelligenceEventListener listener){
		List<IIntelligenceEventListener> lists = listeners.get(type);
		if (lists != null){
			lists.remove(listener);
		}
	}
	
	/**
	 * Intelligence event listener.  This listener
	 * is used for all Intelligence events.
	 *
	 */
	public interface IIntelligenceEventListener{
		/**
		 * attributeChanged - is the type of attribute modified
		 * 
		 * @param attributeChanged  -1 if Intelligence added or removed
		 * @param source the source object of the event.  Either a Intelligence object
		 */
		public void eventFired(int attributeChanged, Intelligence source);
	}
	
}
