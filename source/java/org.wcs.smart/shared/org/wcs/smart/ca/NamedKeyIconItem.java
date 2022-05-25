package org.wcs.smart.ca;

import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;

import org.wcs.smart.ca.icon.Icon;

/**
 * Adds an icon to a named key item. Assumes the field
 * is icon_uuid.
 * 
 * @author Emily
 *
 */
@MappedSuperclass
public class NamedKeyIconItem extends NamedKeyItem {

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
