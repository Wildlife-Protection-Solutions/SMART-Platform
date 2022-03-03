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
package org.wcs.smart.incident;

import java.util.Locale;

import org.eclipse.swt.graphics.Image;
import org.hibernate.Session;
import org.wcs.smart.incident.ui.IncidentEditor;
import org.wcs.smart.incident.xml.IncidentExporter;
import org.wcs.smart.incident.xml.IncidentImporter;
import org.wcs.smart.observation.WaypointSourceEngine;
import org.wcs.smart.observation.model.Waypoint;

/**
 * Incident provider for SMART Integrate Incidents
 * @author Emily
 *
 */
public class IntegrateIncidentProvider implements IIncidentProvider {

	@Override
	public String getWaypointSourceKey() {
		return IntegrateIncidentSource.KEY;
	}
	
	@Override
	public String getName() {
		return WaypointSourceEngine.INSTANCE.getSource(IntegrateIncidentSource.KEY).getName(Locale.getDefault());
	}

	@Override
	public Image getImage() {
		return IncidentPlugIn.getDefault().getImageRegistry().get(IncidentPlugIn.INTEGRATE_ICON);
	}


	@Override
	public String getEditorID() {
		return IncidentEditor.ID;
	}

	@Override
	public void waypointCreated(Waypoint wp, Session session) {

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
