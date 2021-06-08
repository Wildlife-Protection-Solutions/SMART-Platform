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
package org.wcs.smart.i2.migrate.intelligence;

import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.i2.model.IntelProfile;
import org.wcs.smart.i2.model.IntelRecordSource;
import org.wcs.smart.i2.model.IntelRecordSourceAttribute;

/**
 * Mapping record to map between a SMART 6 intelligence record source
 * and a SMART 7 profile record source.
 * 
 * @author Emily
 *
 */
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
