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
package org.wcs.smart.er.ui.mision;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.hibernate.Session;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.ui.ISurveyListener;

/**
 * A mission component composite for mission properties.
 * 
 * @author Emily
 *
 */
public abstract class MissionComposite {

	private List<ISurveyListener> changeListeners;
	
	public MissionComposite(){
		changeListeners = new ArrayList<ISurveyListener>();
	}
	
	/**
	 * Adds a change listener
	 * @param listener
	 */
	public void addChangeListener(ISurveyListener listener){
		changeListeners.add(listener);
	}
	
	/**
	 * Fire change listeners.  Implementors should fire
	 * this event when any gui element is modified.
	 */
	protected void fireChangeListeners(){
		for (ISurveyListener listener: changeListeners){
			listener.compositeModified();
		}
	}
	
	/**
	 * Creates the gui elements.
	 * @param parent
	 * @return
	 */
	public abstract Control createControl(Composite parent);
	
	/**
	 * Initializes the gui elements with data
	 * from the mission.
	 * 
	 * @param mission
	 */
	public abstract void init(Mission mission, Session session);
	
	/**
	 * Updates the mission with info from
	 * the gui.
	 * 
	 * @param mission
	 */
	public abstract void updateDesign(Mission mission);
	
	/**
	 * 
	 * @return <code>true</code> if all widgets in composite are valid
	 */
	public abstract boolean isValid();
	
	/**
	 * 
	 * @return widget title
	 */
	public abstract String getTitle();
	
	/**
	 * 
	 * @return descriptions of widgets represented by composite
	 */
	public abstract String getDescription();
	
	/**
	 * This returns null, sub classes can overwrite.
	 * @return the error message to display in the wizard dialog
	 * 
	 */
	public String getErrorMessage(){
		return null;
	}
}
