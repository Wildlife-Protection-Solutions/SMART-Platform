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
package org.wcs.smart.entity.ui.newwizard;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.hibernate.Session;
import org.wcs.smart.entity.model.EntityType;

/**
 * Incident field composite.  A super class for incident field
 * composites so they can be used in the wizard or edit dialog.
 *  
 * @author Emily
 *
 */
public abstract class AbstractEntityComposite {

	private List<Listener> listeners = new ArrayList<Listener>();
	
	/**
	 * if set to true listeners will not be fired 
	 */
	protected boolean initializing = false;
	
	/**
	 * Add a change listener
	 * @param listener
	 */
	public void addChangeListener(Listener listener){
		listeners.add(listener);
	}
	
	/**
	 * Remove a change listener
	 * @param listener
	 */
	public void removeChangeListener(Listener listener){
		listeners.remove(listener);
	}
	
	/**
	 * Fires change listener.  Listeners are only fired
	 * if initializing field is set to false;
	 * @param e
	 */
	public void fireChange(Event e){
		if (initializing) return;
		for (Listener l : listeners){
			l.handleEvent(e);
		}
	}
	
	/**
	 * 
	 * @return the panel gui name
	 */
	public abstract String getName();
	
	/**
	 * 
	 * @return the panel description
	 */
	public abstract String getDescription();
	
	/**
	 * Validates fields in the panel and returns
	 * a string describing the error or null if no
	 * errors found. 
	 * 
	 */
	public abstract String validate(); 
	
	/**
	 * Creates the composite.
	 * @param parent
	 * @return
	 */
	public abstract Composite createComposite(Composite parent);
	
	/**
	 * Updates the incident with the values from the composite.
	 * 
	 * @param incident
	 */
	public abstract void updateEntityType(EntityType entityType);
	
	/**
	 * Initializes the composite fields with the values
	 * from the incident.
	 *  
	 * @param incident
	 * @param session
	 */
	public abstract void initFields(EntityType entityType, Session session);
}
