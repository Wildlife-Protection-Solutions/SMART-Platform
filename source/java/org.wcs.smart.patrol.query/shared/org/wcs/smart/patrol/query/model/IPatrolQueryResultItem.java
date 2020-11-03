package org.wcs.smart.patrol.query.model;

import java.time.LocalDate;
import java.util.UUID;

import org.wcs.smart.patrol.model.PatrolType;
import org.wcs.smart.query.common.engine.IResultItem;

public interface IPatrolQueryResultItem extends IResultItem {
	
	public String getPatrolId();
	public String getLeader();
	public String getPilot();
	public LocalDate getPatrolStartDate() ;
	public LocalDate getPatrolEndDate() ;
	public String getStation() ;
	public String getTeam() ;
	public String getObjective() ;
	public String getMandate() ;
	public PatrolType.Type getPatrolType() ;
	public UUID getPatrolUuid() ;
	public boolean isArmed() ;
	public String getPatrolLegId() ;
	public UUID getPatrolLegUuid() ;
	public String getTransportType() ;
	public LocalDate getPatrolLegStartDate();
	public LocalDate getPatrolLegEndDate();
	public String getConservationAreaId();
	public String getConservationAreaName();
	public UUID getConservationAreaUuid();	
	
}
