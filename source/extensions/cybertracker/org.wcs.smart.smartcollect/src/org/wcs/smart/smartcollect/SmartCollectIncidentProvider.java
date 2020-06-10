package org.wcs.smart.smartcollect;

import org.eclipse.swt.graphics.Image;
import org.hibernate.Session;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.incident.IIncidentProvider;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.smartcollect.model.SmartCollectWaypoint;
import org.wcs.smart.smartcollect.model.SmartCollectWaypointSource;
import org.wcs.smart.smartcollect.ui.SmartCollectIncidentEditor;

public class SmartCollectIncidentProvider implements IIncidentProvider {


	@Override
	public String getWaypointSourceKey() {
		return SmartCollectWaypointSource.KEY;
	}

	@Override
	public String getName() {
		return "SMART Collect Incident";
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
		cm.setSource(SmartDB.getCurrentEmployee().getSmartUserId() + " [SMART]");
		cm.setWaypoint(wp);
		session.save(cm);
	}

}
