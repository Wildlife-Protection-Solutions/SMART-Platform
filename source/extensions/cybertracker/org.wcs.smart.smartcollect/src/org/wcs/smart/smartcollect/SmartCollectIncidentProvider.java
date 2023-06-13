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
package org.wcs.smart.smartcollect;

import org.eclipse.swt.graphics.Image;
import org.hibernate.Session;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.incident.IIncidentProvider;
import org.wcs.smart.incident.IIncidentXmlExporter;
import org.wcs.smart.incident.IIncidentXmlImporter;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.smartcollect.internal.Messages;
import org.wcs.smart.smartcollect.model.SmartCollectWaypoint;
import org.wcs.smart.smartcollect.model.SmartCollectWaypointSource;
import org.wcs.smart.smartcollect.ui.SmartCollectIncidentEditor;
import org.wcs.smart.smartcollect.xml.IncidentExporter;
import org.wcs.smart.smartcollect.xml.IncidentImporter;

/**
 * Incident provider for SMARTCollect incidents
 *  
 * @author Emily
 *
 */
public class SmartCollectIncidentProvider implements IIncidentProvider {


	@Override
	public String getWaypointSourceKey() {
		return SmartCollectWaypointSource.KEY;
	}

	@Override
	public String getName() {
		return Messages.SmartCollectIncidentProvider_SmartCollectIncident;
	}

	@Override
	public String getEditorID() {
		return SmartCollectIncidentEditor.ID;
	}

	@Override
	public Image getImage() {
		return SmartCollectPlugIn.getDefault().getImageRegistry().get(SmartCollectPlugIn.SMARTCOLLECT_ICON);
	}

	@Override
	public void waypointCreated(Waypoint wp, Session session) {
		SmartCollectWaypoint cm = new SmartCollectWaypoint();
		cm.setSource(SmartDB.getCurrentEmployee().getSmartUserId() + " [SMART]"); //$NON-NLS-1$
		cm.setWaypoint(wp);
		session.persist(cm);
	}

	@Override
	public IIncidentXmlExporter getXmlExporter() {
		return IncidentExporter.INSTANCE;
	}

	@Override
	public IIncidentXmlImporter getXmlImporter() {
		return IncidentImporter.INSTANCE;		
	}

}
