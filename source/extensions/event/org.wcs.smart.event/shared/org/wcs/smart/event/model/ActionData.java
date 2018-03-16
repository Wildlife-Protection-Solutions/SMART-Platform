package org.wcs.smart.event.model;

import java.util.UUID;

public class ActionData {

	private UUID obsUuid;
	
	/**
	 * The observation that triggered the event
	 * @param obsUuid
	 */
	public ActionData(UUID obsUuid) {
		this.obsUuid = obsUuid;
	}
	
	/**
	 * The observation that triggered the event
	 * 
	 * @return
	 */
	public UUID getObservationUuid() {
		return this.obsUuid;
	}
}
