package org.wcs.smart.observation.model;

import java.time.LocalDateTime;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.UuidItem;


@Entity
@Table(name="smart.data_link")
public class DataLink extends UuidItem{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private String dataType;
	private UUID providerId;
	private UUID smartId;
	private LocalDateTime lastUpdated;
	private ConservationArea ca;

	public DataLink() {
		modified();
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="ca_uuid", referencedColumnName="uuid")
	public ConservationArea getConservationArea() {
		return this.ca;
	}
	
	public void setConservationArea(ConservationArea ca){
		this.ca = ca;
	}
	
	@Column(name="data_type")
	public String getDataType() {
		return this.dataType;
	}
	
	public void setDataType(String dataType) {
		this.dataType = dataType;
		modified();
	}
	
	@Column(name="provider_id")
	public UUID getProviderId() {
		return this.providerId;
	}
	
	public void setProviderId(UUID provider) {
		this.providerId = provider;
	}
	
	@Column(name="smart_id")
	public UUID getSmartId() {
		return this.smartId;
	}
	
	public void setSmartId(UUID smartId) {
		this.smartId = smartId;
		modified();
	}
	
	@Column(name="last_modified")
	public LocalDateTime getLastModified() {
		return this.lastUpdated;
	}
	
	public void setLastModified(LocalDateTime lastModified) {
		this.lastUpdated = lastModified;
	}
	
	@Transient
	private void modified() {
		this.lastUpdated = LocalDateTime.now();
	}
	
}
