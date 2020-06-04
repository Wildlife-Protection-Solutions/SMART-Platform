package org.wcs.smart.incident;

import org.eclipse.swt.graphics.Image;
import org.hibernate.Session;
import org.wcs.smart.incident.ui.IncidentEditor;
import org.wcs.smart.observation.model.Waypoint;

public class IndependentIncidentProvider implements IIncidentProvider {

	@Override
	public String getWaypointSourceKey() {
		return IndepedentIncidentSource.KEY;
	}
	
	@Override
	public String getName() {
		return "Independent Incident";
	}

	@Override
	public Image getImage() {
		return IncidentPlugIn.getDefault().getImageRegistry().get(IncidentPlugIn.INCIDENT_ICON);
	}


	@Override
	public String getEditorID() {
		return IncidentEditor.ID;
	}

	@Override
	public void waypointCreated(Waypoint wp, Session session) {
		
	}

}
