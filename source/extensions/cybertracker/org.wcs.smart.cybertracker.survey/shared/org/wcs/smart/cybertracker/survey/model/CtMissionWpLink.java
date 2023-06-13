/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.cybertracker.survey.model;

import java.util.UUID;

import org.wcs.smart.ca.UuidItem;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointObservationGroup;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * Link between CyberTracker observation groups and
 * SMART waypoints/groups (supports sub-incidents)
 * 
 * @author Emily
 * @since 7.0.0
 *
 */
@Entity
@Table(name="ct_mission_wplink", schema="smart")
public class CtMissionWpLink extends UuidItem{

	private static final long serialVersionUID = 1L;

	private CtMissionLink link;
	
	private UUID ctRootId;
	private UUID ctGroupId;
	
	private UUID waypointUuid;
	private UUID obsGroupUuid;
	
	@Transient
	private Waypoint wp;
	@Transient
	private WaypointObservationGroup group;
	
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="ct_mission_link_uuid", referencedColumnName="ct_uuid")
	public CtMissionLink getLink() {
		return this.link;
	}
	public void setLink(CtMissionLink link) {
		this.link = link;
	}
	
	@Column(name="ct_root_id")
	public UUID getCtRootId() {
		return ctRootId;
	}
	public void setCtRootId(UUID ctRootId) {
		this.ctRootId = ctRootId;
	}
	
	@Column(name="ct_group_id")
	public UUID getCtGroupId() {
		return ctGroupId;
	}
	public void setCtGroupId(UUID ctGroupId) {
		this.ctGroupId = ctGroupId;
	}
	
	/**
	 * Should not be used directly; use getWaypoint instead
	 * @return
	 */
	@Column(name="wp_uuid")
	public UUID getWaypointUuid() {
		return waypointUuid;
	}
	
	/**
	 * Should not be used directly; call setWaypoint instead
	 * @return
	 */	
	public void setWaypointUuid(UUID waypointUuid) {
		this.waypointUuid = waypointUuid;
	}
	
	/**
	 * Should not be used directly; call getObservationGroup instead
	 * @return
	 */	
	@Column(name="obs_group_uuid")
	public UUID getObservationGroupUuid() {
		return obsGroupUuid;
	}
	
	/**
	 * Should not be used directly; call setObservationGroup instead
	 * @return
	 */	
	public void setObservationGroupUuid(UUID obsGroupUuid) {
		this.obsGroupUuid = obsGroupUuid;
	}
	
	/**
	 * Sets the waypoint to link to
	 * @param wp
	 */
	public void setWaypoint(Waypoint wp) {
		this.wp = wp;
		setWaypointUuid(wp.getUuid());
	}
	
	@Transient
	public Waypoint getWaypoint() {
		if (this.wp != null) return this.wp;
		Waypoint tmp = new Waypoint();
		tmp.setUuid(getWaypointUuid());
		return tmp;
	}
	
	public void setObservationGroup(WaypointObservationGroup group) {
		this.group = group;
		setObservationGroupUuid(group == null ? null : group.getUuid());
	}
	
	@Transient
	public WaypointObservationGroup getObservationGroup() {
		if (this.group != null) return this.group;
		WaypointObservationGroup tmp = new WaypointObservationGroup();
		tmp.setUuid(getObservationGroupUuid());
		return tmp;
	}
	
	
}
