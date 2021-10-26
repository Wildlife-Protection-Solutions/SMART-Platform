package org.wcs.smart.connect.model;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.Table;
import javax.persistence.Transient;

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
	
	private CyberTrackerApiKeyPk id = new CyberTrackerApiKeyPk();
	
	private String key;

	@EmbeddedId
	public CyberTrackerApiKeyPk getId() {
		return this.id;
	}
	
	public void setId(CyberTrackerApiKeyPk id) {
		this.id = id;
	}

	
	@Column(name="api_key")
	public String getApiKey() {
		return this.key;
	}
	
	public void setApiKey(String key) {
		this.key = key;
	}
	
	
	@Transient
	public ConservationAreaInfo getConservationArea() {
		return id.getConservationArea();
	}
	
	@Transient
	public void setConservationArea(ConservationAreaInfo ca) {
		id.setConservationArea(ca);
	}
	
	@Transient
	public void setType(Type type) {
		this.id.setType(type);
	}
	
	@Embeddable
	private static class CyberTrackerApiKeyPk implements Serializable{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		private Type type;
		private ConservationAreaInfo ca;

		public CyberTrackerApiKeyPk(){
		}

		
		@Column(name="key_type")
		@Enumerated(EnumType.STRING)
		public Type getType() {
			return type;
		}
		
		public void setType(Type type) {
			this.type = type;
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
		
		@Override
		public boolean equals(Object other){
			if (other instanceof CyberTrackerApiKeyPk){
				CyberTrackerApiKeyPk pk2 = (CyberTrackerApiKeyPk)other;
				return Objects.equals(this.ca, pk2.ca) && Objects.equals(this.type, pk2.type);
			}
			return false;
		}
		
		@Override
		public int hashCode(){
			return Objects.hash(this.ca, this.type);
		}
	}
}
