package org.wcs.smart.connect.dataqueue.cybertracker.patrol.model;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.wcs.smart.patrol.model.PatrolLeg;

@Entity
@Table(name="smart.ct_patrol_link")
public class CtPatrolLink {

	private UUID ctUuid;
	
	private PatrolLeg patrolLeg;
	
	private String deviceId;
	
	@Id
	@Column(name="ct_uuid")
	public UUID getCtUuid(){
		return this.ctUuid;
	}
	
	public void setCtUuid(UUID ctUuid){
		this.ctUuid = ctUuid;
	}
	
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="patrol_leg_uuid")
	public PatrolLeg getPatrolLeg(){
		return this.patrolLeg;
	}
	
	public void setPatrolLeg(PatrolLeg patrolLeg){
		this.patrolLeg = patrolLeg;
	}
	
	@Column(name="ct_device_id")
	public String getDeviceId(){
		return this.deviceId;
	}
	
	public void setDeviceId(String deviceId){
		this.deviceId = deviceId;
	}
}
