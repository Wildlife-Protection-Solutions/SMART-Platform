/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.event.model;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.wcs.smart.observation.model.WaypointObservation;

/**
 * Action type represents the type of action to perform.  These can have a variety
 * of parameters as required.
 * 
 * @author Emily
 *
 */
public interface IActionType {

	public static final String EXTENSION_ID = "org.wcs.smart.event.actiontype"; //$NON-NLS-1$
	
	public static final int MAX_KEY_LENGTH = 128;
	
	/**
	 * A unique identifier for the action type.  
	 * Maximum string length is 128 
	 * @return
	 */
	public String getKey();
	
	/**
	 * Get the name of the action type for the given locale
	 * 
	 * @param l
	 * @return
	 */
	public String getName(Locale l);
	
	/**
	 * The action type description
	 * @param l
	 * @return
	 */
	public String getDescription(Locale l);
	
	/**
	 * 
	 * @return must not return null; but can return an empty list is no parameters supported
	 */
	public List<IActionParameter> getActionParameters();
	
	public void performAction(EAction action, EFilter filter, WaypointObservation data);
	
}
