package org.wcs.smart.connect.model;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.Table;

@Entity
@Table(name="connect.ct_api_key")
public class CyberTrackerApiKey {

	/**
	 * We have separate keys for private (patrol/survey etc) packages
	 * and public (community) packages 
	 * 
	 * @author Emily
	 *
	 */
	public enum Type{
		PRIVATE,
		PUBLIC
	};
	
	private String key;
	private UUID cauuid;
	private ConservationAreaInfo ca;
	private Type type;
	
	@Column(name="key_type")
	@Enumerated(EnumType.STRING)
	public Type getType() {
		return type;
	}
	
	public void setType(Type type) {
		this.type = type;
	}
	
	@Column(name="api_key")
	public String getApiKey() {
		return this.key;
	}
	
	public void setApiKey(String key) {
		this.key = key;
	}
	
	@Id
	@Column(name="ca_uuid")
	public UUID getUuid() {
		return this.cauuid;
	}
	public void setUuid(UUID cauuid) {
		this.cauuid = cauuid;
	}
	
	@MapsId
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name="ca_uuid", referencedColumnName="ca_uuid")
	public ConservationAreaInfo getConservationArea() {
		return this.ca;
	}
	
	public void setConservationArea(ConservationAreaInfo ca) {
		this.ca = ca;
	}
}
