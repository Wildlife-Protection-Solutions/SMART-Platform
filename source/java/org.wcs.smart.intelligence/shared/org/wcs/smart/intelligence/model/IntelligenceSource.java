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
package org.wcs.smart.intelligence.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.NamedKeyItem;

/**
 * Source types for intelligence.
 * 
 * @author elitvin
 * @since 3.0.0
 */
@Entity
@Table(name = "smart.intelligence_source")
public class IntelligenceSource extends NamedKeyItem {
	
	public static final String PATROL_KEY = "patrol"; //$NON-NLS-1$
	public static final String INFORMANT_KEY = "informant"; //$NON-NLS-1$

	public static final Integer MAX_NAME_LENGTH = 64;
	
	private ConservationArea conservationArea;

	public boolean isActive;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="ca_uuid", referencedColumnName="uuid")
	public ConservationArea getConservationArea() {
		return conservationArea;
	}
	public void setConservationArea(ConservationArea conservationArea) {
		this.conservationArea = conservationArea;
	}
	
	@Column(name="is_active")
	public boolean getIsActive() {
		return this.isActive;
	}
	public void setIsActive(boolean isActive) {
		this.isActive = isActive;
	}
 
	public static boolean isPatrolSource(IntelligenceSource source) {
    	if (source == null)
    		return false;
    	return PATROL_KEY.equals(source.getKeyId());
    }

	public static boolean isInformantSource(IntelligenceSource source) {
    	if (source == null)
    		return false;
    	return INFORMANT_KEY.equals(source.getKeyId());
    }
}
