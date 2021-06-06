package org.wcs.smart.i2.migrate.intelligence;

import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.i2.model.IntelProfile;
import org.wcs.smart.i2.model.IntelRecordSource;
import org.wcs.smart.i2.model.IntelRecordSourceAttribute;

public class IntelMappingRecord {

	private ConservationArea ca;
	
	private IntelligenceSource smart6source;
	
	private IntelProfile profile;
	private IntelRecordSource rsource;
	
	private IntelRecordSourceAttribute fromDateMapping;
	private IntelRecordSourceAttribute toDateMapping;
	
	public IntelMappingRecord(ConservationArea ca, IntelligenceSource source) {
		this.ca = ca;
		this.smart6source = source;
	}
	
	public ConservationArea getConservationArea() {
		return this.ca;
	}
	
	public IntelligenceSource getSmart6Source() {
		return this.smart6source;
	}
	
	public IntelProfile getProfile() {
		return this.profile;
	}
	public void setProfile(IntelProfile profile) {
		this.profile = profile;
	}
	public IntelRecordSource getRecordSource() {
		return this.rsource;
	}
	public void setRecordSource(IntelRecordSource source) {
		this.rsource = source;
	}
	public IntelRecordSourceAttribute getFromDateMapping() {
		return this.fromDateMapping;
	}
	public void setFromDateMapping(IntelRecordSourceAttribute fromDateMapping) {
		this.fromDateMapping = fromDateMapping;
	}
	public IntelRecordSourceAttribute getToDateMapping() {
		return this.toDateMapping;
	}
	public void setToDateMapping(IntelRecordSourceAttribute toDateMapping) {
		this.toDateMapping = toDateMapping;
	}
}
