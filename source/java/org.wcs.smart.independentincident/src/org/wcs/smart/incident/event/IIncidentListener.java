package org.wcs.smart.incident.event;

public interface IIncidentListener {

	public void handleEvent(int eventType, Object source);
}
