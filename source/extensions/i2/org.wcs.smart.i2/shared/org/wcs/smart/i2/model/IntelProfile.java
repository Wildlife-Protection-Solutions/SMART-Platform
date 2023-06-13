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
package org.wcs.smart.i2.model;

import java.awt.Color;
import java.util.Set;

import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.NamedKeyItem;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * Model class for profile 
 * @author Emily
 *
 */
@Entity
@Table(name="i_profile_config", schema="smart")
public class IntelProfile extends NamedKeyItem {

	private static final long serialVersionUID = 1L;
	
	private Integer color;
	private ConservationArea ca;
	
	private Set<IntelProfileEntityType> etypes;
	private Set<IntelProfileRecordSource> rsources;

	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="ca_uuid", referencedColumnName="uuid")
	public ConservationArea getConservationArea() {
		return this.ca;
	}
	
	public void setConservationArea(ConservationArea ca){
		this.ca = ca;
	}
	

	@OneToMany(fetch = FetchType.LAZY, mappedBy="id.profile", orphanRemoval = true, cascade={CascadeType.ALL})
	public Set<IntelProfileEntityType> getEntityTypes(){
		return this.etypes;
	}
	
	public void setEntityTypes(Set<IntelProfileEntityType> etypes) {
		this.etypes = etypes;
	}
	
	@OneToMany(fetch = FetchType.LAZY, mappedBy="id.profile", orphanRemoval = true, cascade={CascadeType.ALL})
	public Set<IntelProfileRecordSource> getRecordSources(){
		return this.rsources;
	}
	
	public void setRecordSources(Set<IntelProfileRecordSource> sources) {
		this.rsources = sources;
	}
	
	@Column(name="color")
	public Integer getColor() {
		return this.color;
	}
	
	public void setColor(Integer color) {
		this.color = color;
	}
	
	@Transient
	public Color getColorObj() {
		return new Color(color);
	}
	@Transient
	public void setColorObj(Color color) {
		if (color == null) {
			setColor(null);
		}else {
			setColor( color.getRGB() );
		}
	}
	
}
