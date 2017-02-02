/*
 * Copyright (C) 2016 Wildlife Conservation Society
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

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.List;

import javax.imageio.ImageIO;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.NamedKeyItem;

/**
 * Represents a source option for records
 * @author Emily
 *
 */
@Entity
@Table(name="smart.i_recordsource")
public class IntelRecordSource extends NamedKeyItem{

	private ConservationArea ca;
	private byte[] icon;
	private List<IntelRecordSourceAttribute> attributes;
	
	/**
	 * Get the conservation_area.
	 * 
	 * @return conservation_area
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="ca_uuid", referencedColumnName="uuid")
	public ConservationArea getConservationArea() {
		return this.ca;
	}
	
	/**
	 * Set the conservation_area.
	 * 
	 * @param ca
	 *            conservation_area
	 */
	public void setConservationArea(ConservationArea ca) {
		this.ca = ca;
	}
	
	/**
	 * Get the valid attributes for this source type
	 * @return
	 */
	@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "source")
	@OrderBy("seq_order asc")
	public List<IntelRecordSourceAttribute> getAttributes(){
		return this.attributes;
	}
	
	/**
	 * Sets the valid attributes for this source type
	 * @param attributes
	 */
	public void setAttributes(List<IntelRecordSourceAttribute> attributes){
		this.attributes = attributes;
	}
	
	/**
	 * Get the icon.
	 * 
	 * @return icon
	 */
	@Lob
	@Column(name="icon")
	public byte[] getIcon() {
		return this.icon;
	}
	/**
	 * Set the icon.
	 * 
	 * @param icon
	 *            icon
	 */
	public void setIcon(byte[] icon) {
		this.icon = icon;
	}
	
	@Transient
	public BufferedImage getIconAsImage() throws Exception{
		if (getIcon() == null) return null;
		try(ByteArrayInputStream in = new ByteArrayInputStream(getIcon())){
			return ImageIO.read(in);
		}
	}
}
