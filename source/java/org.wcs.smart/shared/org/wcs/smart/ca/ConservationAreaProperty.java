package org.wcs.smart.ca;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name="conservation_area_property", schema="smart")
public class ConservationAreaProperty extends UuidItem{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private ConservationArea conservationArea;
	private String propertyKey;
	private String propertyValue;
	
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="ca_uuid", referencedColumnName="uuid")
	public ConservationArea getConservationArea() {
		return this.conservationArea;
	}
	
	public void setConservationArea(ConservationArea ca){
		this.conservationArea = ca;
	}
	
	@Column(name="pkey")
	public String getKey() {
		return this.propertyKey;
	}
	
	public void setKey(String key) {
		this.propertyKey = key;
	}
	
	@Column(name = "value")
	public String getValue() {
		return this.propertyValue;
	}
	
	public void setValue(String value) {
		this.propertyValue = value;
	}
	
	
}
