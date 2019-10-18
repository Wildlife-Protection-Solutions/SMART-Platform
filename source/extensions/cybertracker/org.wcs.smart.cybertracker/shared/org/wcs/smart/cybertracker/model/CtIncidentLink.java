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
import org.wcs.smart.observation.model.WaypointObservationGroup;

/**
 * Links cybertracker incident ids to waypoint uuids;
 * 
 * @author Emily
 *
 */
@Entity
@Table(name = "smart.ct_incident_link")
public class CtIncidentLink extends UuidItem {
	
	private static final long serialVersionUID = 1L;
	
	private UUID ctIncidentGroupId;
	private UUID ctRootId;

	private Waypoint waypoint;
	private WaypointObservationGroup obsGroup;
	
	@Column(name="ct_root_id")
	public UUID getRootId() {
		return this.ctRootId;
	}
	
	public void setRootId(UUID ctRootId) {
		this.ctRootId = ctRootId;
	}
	
	@Column(name="ct_group_id")
	public UUID getIncidentGroupId() {
		return this.ctIncidentGroupId;
	}
	
	public void setIncidentGroupId(UUID incidentGroupId) {
		this.ctIncidentGroupId = incidentGroupId;
	}
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="wp_uuid", referencedColumnName="uuid")
	public Waypoint getWaypoint() {
		return this.waypoint;
	}
	
	public void setWaypoint(Waypoint waypoint) {
		this.waypoint = waypoint;
	}
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="obs_group_uuid", referencedColumnName="uuid")
	public WaypointObservationGroup getObservationGroup() {
		return this.obsGroup;
	}
	
	public void setObservationGroup(WaypointObservationGroup obsGroup) {
		if (obsGroup != null) setWaypoint(obsGroup.getWaypoint());
		this.obsGroup = obsGroup;
	}

}
