package org.wcs.smart.i2.ui.views.query;

public class AttributeHeaderFilterItem extends BasicTreeFilterItem {

	private boolean isGroup;
	
	public AttributeHeaderFilterItem(String name, boolean isGroup) {
		super(name);
		this.isGroup = isGroup;
	}
	
	public boolean isGroup(){
		return this.isGroup;
	}

}
