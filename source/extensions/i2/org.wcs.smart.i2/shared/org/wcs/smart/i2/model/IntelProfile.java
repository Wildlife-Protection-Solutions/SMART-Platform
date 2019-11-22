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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.NamedKeyItem;

/**
 * Model class for profile 
 * @author Emily
 *
 */
@Entity
@Table(name="smart.i_profile_config")
public class IntelProfile extends NamedKeyItem {

	private static final long serialVersionUID = 1L;
	
	private Integer color;
	private ConservationArea ca;
	
	private Set<IntelEntityType> etypes;
	private Set<IntelRecordSource> rsources;

	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="ca_uuid", referencedColumnName="uuid")
	public ConservationArea getConservationArea() {
		return this.ca;
	}
	
	public void setConservationArea(ConservationArea ca){
		this.ca = ca;
	}
	

	@ManyToMany(mappedBy="profiles")
	public Set<IntelEntityType> getEntityTypes(){
		return this.etypes;
	}
	
	public void setEntityTypes(Set<IntelEntityType> etypes) {
		this.etypes = etypes;
	}
	
	@ManyToMany(mappedBy="profiles")
	public Set<IntelRecordSource> getRecordSources(){
		return this.rsources;
	}
	
	public void setRecordSources(Set<IntelRecordSource> sources) {
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
