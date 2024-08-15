package org.wcs.smart.cybertracker.patrol.model;

import java.util.ArrayList;
import java.util.List;

import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.Station;
import org.wcs.smart.cybertracker.json.JsonImportWarning;
import org.wcs.smart.patrol.meta.PatrolScreenOptionMeta;
import org.wcs.smart.patrol.model.PatrolAttributeValue;
import org.wcs.smart.patrol.model.PatrolMandate;
import org.wcs.smart.patrol.model.PatrolTransportType;
import org.wcs.smart.patrol.model.PatrolType;
import org.wcs.smart.patrol.model.Team;

public class JsonPatrol {
	
	private Station station;
	private Team team;
	private String objective;
	private PatrolMandate mandate;
	private PatrolTransportType patrolTransportType;
	private boolean isArmed = false;
	private String comment;
	private Employee leader;
	private Employee pilot;
	private List<Employee> members;
	
	//used only for gui representation after initial load
	private String ctTransport;
	private String ctStation;
	private String ctTeam;
	private String ctLeader;
	private String ctPilot;
	private List<String> ctMembers;

	private List<PatrolAttributeValue> customAttributes;
	private List<JsonImportWarning> warnings;
	
	public JsonPatrol() {
		
		customAttributes = new ArrayList<>();
		warnings = new ArrayList<>();
	}

	public void addWarning(JsonImportWarning warning) {
		this.warnings.add(warning);
	}
	
	public List<JsonImportWarning> getWarnings(){
		return this.warnings;
	}

	public void addCustomAttributeValue(PatrolAttributeValue v) {
		customAttributes.add(v);
	}
	public List<PatrolAttributeValue> getCustomAttributes(){
		return this.customAttributes;
	}
	
	public Station getStation() {
		return station;
	}

	public Team getTeam() {
		return team;
	}

	public String getObjective() {
		return objective;
	}

	public PatrolMandate getMandate() {
		return mandate;
	}

	public PatrolType getPatrolType() {
		return patrolTransportType != null ? patrolTransportType.getPatrolType() : null;
	}

	public PatrolTransportType getPatrolTransportType() {
		return patrolTransportType;
	}
	
	public boolean isArmed() {
		return isArmed;
	}

	public String getComment() {
		return comment;
	}

	public void setStation(Station station) {
		this.station = station;
	}

	public void setTeam(Team team) {
		this.team = team;
	}

	public void setObjective(String objective) {
		this.objective = objective;
	}

	public void setMandate(PatrolMandate mandate) {
		this.mandate = mandate;
	}

	public void setPatrolTransportType(PatrolTransportType patrolTransportType) {
		this.patrolTransportType = patrolTransportType;
	}
	
	public void setArmed(boolean isArmed) {
		this.isArmed = isArmed;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public Employee getLeader() {
		return leader;
	}

	public void setLeader(Employee leader) {
		this.leader = leader;
	}

	public Employee getPilot() {
		return pilot;
	}

	public void setPilot(Employee pilot) {
		this.pilot = pilot;
	}

	public List<Employee> getMembers() {
		if (members == null)
			members = new ArrayList<Employee>();
		return members;
	}

	public void setMembers(List<Employee> members) {
		this.members = members;
	}

	public String getCtTransport() {
		return ctTransport;
	}

	public void setCtTransport(String ctTransport) {
		this.ctTransport = ctTransport;
	}

	public String getCtStation() {
		return ctStation;
	}

	public void setCtStation(String ctStation) {
		this.ctStation = ctStation;
	}

	public String getCtTeam() {
		return ctTeam;
	}

	public void setCtTeam(String ctTeam) {
		this.ctTeam = ctTeam;
	}

	public String getCtLeader() {
		return ctLeader;
	}

	public void setCtLeader(String ctLeader) {
		this.ctLeader = ctLeader;
	}

	public String getCtPilot() {
		return ctPilot;
	}

	public void setCtPilot(String ctPilot) {
		this.ctPilot = ctPilot;
	}

	public List<String> getCtMembers() {
		if (ctMembers == null)
			ctMembers = new ArrayList<String>();
		return ctMembers;
	}

	public void setCtMembers(List<String> ctMembers) {
		this.ctMembers = ctMembers;
	}
	
	public String getType() {
		return PatrolScreenOptionMeta.PATROL_RESOURCE_ID;
	}

}
