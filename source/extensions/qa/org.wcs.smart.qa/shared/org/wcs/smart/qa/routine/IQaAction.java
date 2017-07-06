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
package org.wcs.smart.qa.routine;

import java.util.List;
import java.util.Locale;

import org.eclipse.swt.graphics.Image;
import org.wcs.smart.qa.model.QaError;

/**
 * An action that can be applied to a QA result item.
 * 
 * @author Emily
 *
 */
public interface IQaAction {

	public static final String DELETE_ACTION_ID = "org.wcs.smart.qa.action.delete"; //$NON-NLS-1$
	
	/**
	 * Perform the action on the set of items.  If supportsMultiple is true
	 * this list should only have a single item in it.
	 * 
	 * List or errors may contain items that you cannot perform and action on.  In
	 * these cases you should skip this item and continue.
	 * 
	 * @param items
	 * @return true if any of the items have been modified
	 */
	public boolean doAction(List<QaError> items);
	
	/**
	 * @return if the action can be applied to multiple items at once.  For example
	 * delete actions could be applied to multiple actions, but goto source
	 * do not. 
	 */
	public boolean supportsMultiple();
		
	/**
	 * 
	 * @return id of the qa action
	 */
	public String getId();
	
	/**
	 * 
	 * @param l
	 * @return the name of the qa action
	 */
	public String getName(Locale l);
	
	/**
	 * 
	 * @return the image for the action; can return null if no image applicable
	 */
	public default Image getImage() {
		return null;
	}
}
