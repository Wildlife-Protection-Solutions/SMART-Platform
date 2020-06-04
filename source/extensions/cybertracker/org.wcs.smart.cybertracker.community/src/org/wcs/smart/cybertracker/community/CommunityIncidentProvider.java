package org.wcs.smart.cybertracker.community;

import org.eclipse.swt.graphics.Image;
import org.hibernate.Session;
import org.wcs.smart.cybertracker.community.model.CommunityWaypoint;
import org.wcs.smart.cybertracker.community.model.CommunityWaypointSource;
import org.wcs.smart.cybertracker.community.ui.CommunityIncidentEditor;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.incident.IIncidentProvider;
import org.wcs.smart.observation.model.Waypoint;

public class CommunityIncidentProvider implements IIncidentProvider {


	@Override
	public String getWaypointSourceKey() {
		return CommunityWaypointSource.KEY;
	}

	@Override
	public String getName() {
		return "Community Incident";
	}

	@Override
	public String getEditorID() {
		return CommunityIncidentEditor.ID;
	}

	@Override
	public Image getImage() {
		return CommunityPlugIn.getDefault().getImageRegistry().get(CommunityPlugIn.COMMUNITY_ICON);
	}

	@Override
	public void waypointCreated(Waypoint wp, Session session) {
		CommunityWaypoint cm = new CommunityWaypoint();
		cm.setSource(SmartDB.getCurrentEmployee().getSmartUserId() + " [SMART]");
		cm.setWaypoint(wp);
		session.save(cm);
	}

}
