/*
 * Copyright (C) 2021 Wildlife Conservation Society
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
package org.wcs.smart;

import org.eclipse.swt.widgets.Composite;
import org.hibernate.Session;

/**
 * Contribution to the ID Pattern Preference page for
 * defining patterns for generating ids for items in the system (incident id,
 * patrol ids etc.);
 * 
 * @author Emily
 *
 */
public interface IdGeneratorContribution {

	/**
	 * Initialize the UI elements 
	 * @param session
	 */
	public void initComponent(Session session);
	
	/**
	 * create the ui elements
	 * @param parent
	 * @return
	 */
	public Composite createComposite(Composite parent);
	
	/**
	 * Save the configuration to the database.  Return false
	 * if configuration cannot be saved to the database otherwise
	 * returns true.
	 * 
	 * @param session
	 * @return
	 */
	public boolean save(Session session);
}
