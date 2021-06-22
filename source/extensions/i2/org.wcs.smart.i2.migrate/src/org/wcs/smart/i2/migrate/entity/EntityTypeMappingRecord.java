/*
 * Copyright (C) 2021 Wildlife Conservation Society
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
package org.wcs.smart.i2.migrate.entity;

import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelProfile;

/**
 * Mapping record mapping smart6 entity to profile and smart7 entity
 * type
 * @author Emily
 *
 */
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
