package org.wcs.smart.ca;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name="smart.conservation_area_property")
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
