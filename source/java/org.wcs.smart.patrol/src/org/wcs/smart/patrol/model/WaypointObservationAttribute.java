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
package org.wcs.smart.patrol.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;


/**
 * Waypoint observation attribute.
 * @author Emily
 * @since 1.0.0
 */
@Entity
@Table(name="smart.wp_observation_attributes")
public class WaypointObservationAttribute {
	private byte[] uuid;
	
	private WaypointObservation observation;
	private Attribute attribute;
	private AttributeListItem listItem;
	private AttributeTreeNode nodeItem;
	private String sValue;
	private Double dValue;
	
	public WaypointObservationAttribute(){
		
	}
	
	@Id
	@GeneratedValue(generator="uuid")
	@GenericGenerator(name= "uuid", strategy="uuid2")
	public byte[] getUuid() {
		return uuid;
	}
	public void setUuid(byte[] uuid) {
		this.uuid = uuid;
	}
	
	@ManyToOne(fetch = FetchType.LAZY)
	public WaypointObservation getObservation(){
		return this.observation;
	}
	public void setObservation(WaypointObservation observation){
		this.observation = observation;
	}
	
	@ManyToOne(fetch =FetchType.LAZY)
	public Attribute getAttribute(){
		return this.attribute;
	}
	public void setAttribute(Attribute attribute){
		this.attribute = attribute;
	}
	@ManyToOne(fetch =FetchType.LAZY)
	public AttributeListItem getAttributeListItem(){
		return this.listItem;
	}
	public void setAttributeListItem(AttributeListItem item){
		this.listItem = item;
	}
	@ManyToOne(fetch =FetchType.LAZY)
	public AttributeTreeNode getAttributeTreeNode(){
		return this.nodeItem;
	}
	public void setAttributeTreeNode(AttributeTreeNode node){
		this.nodeItem = node;
	}
	
	@Column(name="string_value")
	public String getStringValue(){
		return this.sValue;
	}
	public void setStringValue(String value){
		this.sValue = value;
	}
	
	@Column(name="number_value")
	public Double getNumberValue(){
		return this.dValue;
	}
	public void setNumberValue(Double value){
		this.dValue = value;
	}
}
