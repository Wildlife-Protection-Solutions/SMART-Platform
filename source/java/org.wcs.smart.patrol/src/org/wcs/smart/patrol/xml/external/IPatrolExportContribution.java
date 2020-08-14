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
package org.wcs.smart.patrol.xml.external;

import java.util.HashMap;

import org.eclipse.swt.widgets.Composite;

/**
 * UI contribution to patrol export dialog.
 * 
 * All contributions appear above Patrol list and
 * below the include attachments button.
 * 
 * @author Emily
 *
 */
public interface IPatrolExportContribution {

	/**
	 * comp has a grid layout with 3 columns
	 * 
	 * @param comp
	 */
	public void createControls(Composite comp);
	
	/**
	 * 
	 * @return export option values
	 */
	public HashMap<Object, Object> getOptions();
	
	/**
	 * validate ui option
	 * 
	 * @return null if options are valid, description of error otherwise  
	 */
	public String validate();
}
