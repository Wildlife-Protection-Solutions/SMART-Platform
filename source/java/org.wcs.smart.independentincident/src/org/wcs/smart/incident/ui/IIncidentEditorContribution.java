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
package org.wcs.smart.incident.ui;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.wcs.smart.observation.model.Waypoint;

/**
 * Contribution for the Other tab of the mission editor.
 * 
 * @author Emily
 *
 */
public interface IIncidentEditorContribution {

	/**
	 * Extension id
	 */
	public static final String EXTENSION_ID = "org.wcs.smart.incident.contribution"; //$NON-NLS-1$
	
	/**
	 * Creates the control content.  Each content is
	 * placed within a toolkit section.
	 * 
	 * @param toolkit
	 * @param parent
	 * @param canEdit true if the patrol can be edited, false otherwise
	 * @return
	 */
	Composite createControl(FormToolkit toolkit, Composite parent, boolean canEdit);
	
	/**
	 * 
	 * Displayed in the header section of the
	 * toolkit section.
	 * 
	 * @return the name of the contribution
	 */
	String getName();
	
	/**
	 * Sets the mission associated with the current editor.
	 * 
	 * @param patrol
	 */
	void setIncident(Waypoint incident);
}
