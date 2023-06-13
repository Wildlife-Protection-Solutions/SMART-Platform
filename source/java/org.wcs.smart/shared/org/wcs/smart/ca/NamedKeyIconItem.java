package org.wcs.smart.ca;

import org.wcs.smart.ca.icon.Icon;

import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;

/**
 * Adds an icon to a named key item. Assumes the field
 * is icon_uuid.
 * 
 * @author Emily
 *
 */
@MappedSuperclass
public class NamedKeyIconItem extends NamedKeyItem implements IconItem{

	private static final long serialVersionUID = 1L;

	protected Icon icon;
	
	/**
	 * The icon associated with the data model element
	 * @return
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="icon_uuid", referencedColumnName="uuid")
	public Icon getIcon() {
		return icon;
	}
	
	public void setIcon(Icon icon) {
		this.icon = icon;
	}
}
