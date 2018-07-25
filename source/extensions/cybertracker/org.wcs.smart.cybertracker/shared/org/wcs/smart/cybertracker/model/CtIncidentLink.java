package org.wcs.smart.cybertracker.model;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.wcs.smart.ca.UuidItem;
import org.wcs.smart.observation.model.Waypoint;

/**
 * Links cybertracker incident ids to waypoint uuids;
 * 
 * @author Emily
 *
 */
@Entity
@Table(name = "smart.ct_incident_link")
public class CtIncidentLink extends UuidItem {
	
	private UUID incidentGroupId;
	
	private Waypoint waypoint;
	
	private Integer lastObservationId;
	
	@Column(name="ct_group_id")
	public UUID getIncidentGroupId() {
		return this.incidentGroupId;
	}
	
	public void setIncidentGroupId(UUID incidentGroupId) {
		this.incidentGroupId = incidentGroupId;
	}
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="wp_uuid", referencedColumnName="uuid")
	public Waypoint getWaypoint() {
		return this.waypoint;
	}
	
	public void setWaypoint(Waypoint waypoint) {
		this.waypoint = waypoint;
	}
	
	@Column(name="last_cnt")
	public Integer getLastObservationCounter() {
		return this.lastObservationId;
	}
	
	public void setLastObservationCounter(Integer counter) {
		this.lastObservationId = counter;
	}
}
