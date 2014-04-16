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
package org.wcs.smart.patrol.internal.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.hibernate.Session;
import org.wcs.smart.patrol.model.Patrol;

/**
 * A composite that can be used both in the create patrol wizard pages
 * and in a dialog that allows users to change the values.
 * 
 * @author Emily
 * @since 1.0.0
 */
public abstract class PatrolItemComposite {
	
	private List<IPatrolItemChangeListener> listeners = new ArrayList<IPatrolItemChangeListener>();
	protected String errorMessage = null;
	
	/**
	 * Creates the component.
	 * 
	 * @param parent parent component
	 * @param style style
	 * @return composite of the created component
	 */
	public abstract Composite createComponent(Composite parent, int style);
	
	/**
	 * Updates the component values with the values from the patrol. 
	 * The current session is also provided to default or options
	 * can be loaded from the database. 
	 * 
	 * @param p
	 * @param session
	 */
	public abstract void setValues(Patrol p, Session session) ;

	/**
	 * Update the patrol with the values provided
	 * by the user.
	 * <p>The open database session can be used to validate
	 * objects against the database if required.</p>
	 * 
	 * @param p patrol object to update
	 * @param session current database connection; not in transaction
	 * @return <code>true</code> if successfully updated <code>false</code> if could not update patrol
	 * 
	 */
	public abstract boolean updatePatrol(Patrol p, Session session);
	
	/**
	 * 
	 * @return the composite title
	 */
	public abstract String getTitle();
	
	/**
	 * Adds a listener to fire when component changes are made.
	 * 
	 * @param listener
	 */
	public void addChangeListener(IPatrolItemChangeListener listener){
		this.listeners.add(listener);
		
	}
	/**
	 * Removes a listener added using the addChangeListener
	 * @param listener
	 */
	public void removeChangeListener(IPatrolItemChangeListener listener){
		this.listeners.remove(listener);
	}
	
	/**
	 * Fires all registered listeners
	 */
	protected void fireChangeListeners(){
		for(IPatrolItemChangeListener listener : listeners){
			listener.itemChanged();
		}
	}
	
	/**
	 * <p>The error message should be set before listenered are fired.
	 * </p>
	 * @return the current error message associated with the composite; null if no error message
	 */
	public String getErrorMessage(){
		return errorMessage;
	}
	/**
	 * 
	 * @param errorMessage error message associated with the composite; null if not error message
	 */
	public void setErrorMessage(String errorMessage){
		this.errorMessage = errorMessage;
	}
	
	/**
	 * This is used when firing events.  This identifies what attribute 
	 * is being modified. 
	 * @return the attribute being modified.
	 */
	public abstract int getAttribute();
	
	/**
	 * 
	 * @return the initial size for the dialog or null
	 * if use default
	 */
	public Point getInitialSize(){
		return null;
	}
}


