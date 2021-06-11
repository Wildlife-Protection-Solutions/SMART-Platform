package org.wcs.smart.i2.migrate.entity;

import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelProfile;

public class EntityTypeMappingRecord {

	private EntityTypeItem entitytype;
	private IntelEntityType intelEntityType;
	private IntelProfile profile;
	
	private ConservationArea ca;
	
	public EntityTypeMappingRecord(ConservationArea ca) {
		this.ca = ca;
		
	}

	public ConservationArea getConservationArea() {
		return this.ca;
	}

	public EntityTypeItem getEntitytype() {
		return entitytype;
	}


	public void setEntitytype(EntityTypeItem entitytype) {
		this.entitytype = entitytype;
	}


	public IntelEntityType getIntelEntityType() {
		return intelEntityType;
	}


	public void setIntelEntityType(IntelEntityType intelEntityType) {
		this.intelEntityType = intelEntityType;
	}


	public IntelProfile getProfile() {
		return profile;
	}


	public void setProfile(IntelProfile profile) {
		this.profile = profile;
	}
}
