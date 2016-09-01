package org.wcs.smart.i2.model;

import java.util.List;
import java.util.Locale;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.eclipse.swt.graphics.Image;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.wcs.smart.SmartContext;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.NamedKeyItem;
import org.wcs.smart.i2.IIntelligenceLabelProvider;

/**
 * Model class of intelligence attribute for entity types
 * or relationship types
 * 
 */
@Entity
@Table(name="smart.i_attribute")
public class IntelAttribute extends NamedKeyItem{

	public enum IAttributeType{
		NUMERIC,
		TEXT, 
		BOOLEAN,
		LIST,
		DATE;
		
		public Image getImage(){
			String key = null;
			if (this == NUMERIC){
				key = SmartPlugIn.ATTRIBUTE_NUMBER_ICON;
			}else if (this == TEXT){
				key = SmartPlugIn.ATTRIBUTE_TEXT_ICON;
			}else if (this == BOOLEAN){
				key = SmartPlugIn.ATTRIBUTE_BOOLEAN_ICON;
			}else if (this == LIST){
				key = SmartPlugIn.ATTRIBUTE_LIST_ICON;
			}else if (this == DATE){
				key = SmartPlugIn.ATTRIBUTE_DATE_ICON;
			}
			if (key == null) return null;
			return SmartPlugIn.getDefault().getImageRegistry().get(key);
		}
		
		public String getGuiName(Locale l){
			IIntelligenceLabelProvider provider = SmartContext.INSTANCE.getClass(IIntelligenceLabelProvider.class);
			return provider.getLabel(this, l);
		}
	}
	
	private IAttributeType type;
	private ConservationArea ca;

	private List<IntelAttributeListItem> listItems;
	
	/**
	 * Constructor.
	 */
	public IntelAttribute() {
	}


	/**
	 * Get the type.
	 * 
	 * @return type
	 */
	@Enumerated(EnumType.STRING)
	@Column(name="type")
	public IAttributeType getType() {
		return this.type;
	}
	
	/**
	 * Set the type.
	 * 
	 * @param type
	 *            type
	 */
	public void setType(IAttributeType type) {
		this.type = type;
	}


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
	 * Only valid for list attributes.
	 * 
	 * @return set of valid list elements
	 */
	@OneToMany(fetch=FetchType.LAZY, mappedBy="attribute", cascade={CascadeType.ALL}, orphanRemoval=true)
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public List<IntelAttributeListItem> getAttributeList(){
		return this.listItems;
	}
	/**
	 * Only valid for list attributes.
	 * 
	 * @param attributeList the set of valid list elements
	 */
	public void setAttributeList(List<IntelAttributeListItem> listItems){
		this.listItems = listItems;
	}
}
