/*
 * Copyright (C) 2012 Wildlife Conservation Society
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
package org.wcs.smart.er.hibernate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class SurveyMissionProxy {
	
	public enum Type {MISSION, SURVEY};
	
	private UUID uuid;
	private String id;
	
	private String designName;
	private UUID designUuid;
	private List<SurveyMissionProxy> missions;
	
	private LocalDate missionStart;
	private LocalDate missionEnd;
	
	private SurveyMissionProxy parent;
	private Type type;
	/**
	 * Constructor
	 */
	public SurveyMissionProxy(String id, UUID uuid, String designName, UUID designUuid) {
		this.uuid = uuid;
		this.id = id;
		this.designName = designName;
		this.designUuid = designUuid;
		this.type = Type.SURVEY;
		this.parent = null;
		this.missions = new ArrayList<>();
	}
	
	public UUID getUuid() {
		return uuid;
	}
	
	public String getId() {
		return id;
	}
	
	public String getDesignName() {
		return designName;
	}
	
	public UUID getDesignUuid() {
		return designUuid;
	}
	
	public Type getType() {
		return this.type;
	}
	public void addMission(String id, UUID uuid, LocalDate start, LocalDate end) {
		SurveyMissionProxy mission = new SurveyMissionProxy(id, uuid, null, null);
		mission.type = Type.MISSION;
		mission.missionEnd = end;
		mission.missionStart = start;
		mission.parent = this;
		this.missions.add(mission);
	}
	
	public LocalDate getStartDate() {
		return this.missionStart;
	}
	
	public LocalDate getEndDate() {
		return this.missionEnd;
	}
	
	public SurveyMissionProxy getParent() {
		return parent;
	}
	
	public List<SurveyMissionProxy> getMissions(){
		return this.missions;
	}
	
	@Override
	public boolean equals(Object other) {
		if (other == null) return false;
		if (other == this) return true;
		if (getClass() != other.getClass()) return false;
		
		return Objects.equals(uuid, ((SurveyMissionProxy)other).uuid);
	}
	
	@Override
	public int hashCode() {
		return uuid.hashCode();
	}
}
