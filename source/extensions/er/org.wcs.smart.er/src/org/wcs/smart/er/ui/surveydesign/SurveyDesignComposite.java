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
package org.wcs.smart.er.ui.surveydesign;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.hibernate.Session;
import org.wcs.smart.er.model.SurveyDesign;

/**
 * A GUI component that represents an attribute of
 * a survey design.
 * 
 * @author Emily
 *
 */
public abstract class SurveyDesignComposite {

	private List<ISurveyDesignListener> changeListeners;
	
	public SurveyDesignComposite(){
		changeListeners = new ArrayList<ISurveyDesignListener>();
	}
	
	/**
	 * Adds a change listener
	 * @param listener
	 */
	public void addChangeListener(ISurveyDesignListener listener){
		changeListeners.add(listener);
	}
	
	/**
	 * Fire change listeners.  Implementors should fire
	 * this event when any gui element is modified.
	 */
	protected void fireChangeListeners(){
		for (ISurveyDesignListener listener: changeListeners){
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
	 * from the survey design.
	 * 
	 * @param design
	 * @parma session
	 */
	public abstract void init(SurveyDesign design, Session session);
	
	/**
	 * Updates the survey design with info from
	 * the gui.
	 * 
	 * @param design
	 */
	public abstract void updateDesign(SurveyDesign design);
	
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
	
}
