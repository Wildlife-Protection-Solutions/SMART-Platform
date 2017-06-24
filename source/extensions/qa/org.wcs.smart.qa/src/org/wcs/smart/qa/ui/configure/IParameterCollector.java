/*
 * Copyright (C) 2017 Wildlife Conservation Society
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
package org.wcs.smart.qa.ui.configure;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.wcs.smart.qa.model.QaRoutine;

/**
 * Interface for creating a UI to collect
 * QA Routine parameters for a given QA Routine
 * type.
 * 
 * @author Emily
 *
 */
public abstract class IParameterCollector {

	/**
	 * List of listeners to be fired when a ui
	 * element is modified
	 */
	protected List<Listener> listeners = new ArrayList<>();
	
	/**
	 * Create the ui controls 
	 * 
	 * @param composite
	 */
	public abstract void createUi(Composite composite);
	
	/**
	 *  
	 * @return the current status of ui inputs
	 */
	public abstract boolean isValid();
	
	/**
	 * Initialize the ui controls with values 
	 * from this qa routine.
	 * 
	 * @param routine
	 */
	public abstract void initUi(QaRoutine routine);
	
	/**
	 * Updates the parameters in this qa routine with
	 * the values from the UI
	 * 
	 * @param routine
	 */
	public abstract void updateParameters(QaRoutine routine);
	
	/**
	 * Add a listener to be fired with the ui
	 * control is modified.
	 * 
	 * @param l
	 */
	public void addListener(Listener l){
		this.listeners.add(l);
	}
	
	/**
	 * Removes a listener
	 * @param l
	 */
	public void removeListener(Listener l){
		this.listeners.remove(l);
	}
	
	/**
	 * Fires all listeners
	 */
	protected void fireListeners(){
		Event e = new Event();
		for (Listener l : listeners){
			l.handleEvent(e);
		}
	}
}
