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
