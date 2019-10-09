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
package org.wcs.smart.cybertracker.patrol.model;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.wcs.smart.ca.UuidItem;

/**
 * Link between CyberTracker observation groups and
 * SMART waypoints/groups (supports sub-incidents)
 * 
 * @author Emily
 * @since 7.0.0
 *
 */
@Entity
@Table(name="smart.ct_patrol_wplink")
public class CtPatrolWpLink extends UuidItem{


	private CtPatrolLink link;
	
	private UUID ctRootId;
	private UUID ctGroupId;
	
	private UUID waypointUuid;
	private UUID obsGroupUuid;
	
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="ct_patrol_link_uuid", referencedColumnName="ct_uuid")
	public CtPatrolLink getLink() {
		return this.link;
	}
	public void setLink(CtPatrolLink link) {
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
	
	@Column(name="wp_uuid")
	public UUID getWaypointUuid() {
		return waypointUuid;
	}
	public void setWaypointUuid(UUID waypointUuid) {
		this.waypointUuid = waypointUuid;
	}
	
	@Column(name="obs_group_uuid")
	public UUID getObservationGroupUuid() {
		return obsGroupUuid;
	}
	public void setObservationGroupUuid(UUID obsGroupUuid) {
		this.obsGroupUuid = obsGroupUuid;
	}
	
	
}
