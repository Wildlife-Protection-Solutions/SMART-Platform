package org.wcs.smart.ca.icon;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.NamedKeyItem;

@Entity
@Table(name="smart.iconset")

public class IconSet extends NamedKeyItem{

	//these are the default icons set keys
	public static final String[] FIXED_KEYS = new String[] {"black", "line", "color"};
	
	private ConservationArea ca;
	private boolean isDefaut;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="ca_uuid", referencedColumnName="uuid")
	public ConservationArea getConservationArea() {
		return this.ca;
	}
	
	public void setConservationArea(ConservationArea ca){
		this.ca = ca;
	}
	
	@Transient
	public boolean isDefault() {
		return this.getIsDefault();
	}
	
	@Column(name="is_default")
	public boolean getIsDefault() {
		return this.isDefaut;
	}
	
	public void setIsDefault(boolean isDefault) {
		this.isDefaut = isDefault;
	}
}
