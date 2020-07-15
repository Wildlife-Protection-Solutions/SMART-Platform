/*
 * Copyright (C) 2020 Wildlife Conservation Society
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
package org.wcs.smart.incident;

import org.eclipse.swt.graphics.Image;
import org.hibernate.Session;
import org.wcs.smart.observation.model.Waypoint;

/**
 * To be implemented by any plugin which contributes to the Incidents
 * plugin. 
 * 
 * @author Emily
 *
 */
public interface IIncidentProvider {

	/**
	 * 
	 * @return the waypoint source key for the incident provider
	 */
	public String getWaypointSourceKey();
	
	/**
	 * 
	 * @return the name of incident provider
	 */
	public String getName();
	
	/**
	 * 
	 * @return the editor id used to edit the incident 
	 */
	public String getEditorID();
	
	/**
	 * 
	 * @return icon associated with the incident type
	 */
	public Image getImage();
	
	/**
	 * Called after the waypoint is created from the new incident wizard, but before
	 * the transaction is committed.  Allows plugins to contribute other properties
	 * to the waypoints.
	 * 
	 * @param wp
	 * @param session
	 */
	public void waypointCreated(Waypoint wp, Session session);

	/**
	 * 
	 * @return the xml exporter for the given incident type
	 */
	public IIncidentXmlExporter getXmlExporter();
	
	/**
	 * 
	 * @return the xml importer for the given incident ype
	 */
	public IIncidentXmlImporter getXmlImporter();
}
