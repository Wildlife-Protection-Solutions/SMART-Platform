package org.wcs.smart.incident;

import org.eclipse.swt.graphics.Image;
import org.hibernate.Session;
import org.wcs.smart.observation.model.Waypoint;

public interface IIncidentProvider {

	public String getWaypointSourceKey();
	
	public String getName();
	
	public String getEditorID();
	
	public Image getImage();
	
	public void waypointCreated(Waypoint wp, Session session);


}
