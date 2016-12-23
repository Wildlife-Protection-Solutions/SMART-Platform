package org.wcs.smart.i2.ui.views.query;


public class DeferredFilterItem extends BasicFilterItem {

	public DeferredFilterItem(String name) {
		super(name);
	}
	
	public boolean requiresLoad(){
		if (kids == null) return true;
		return false;
	}
	public boolean hasChildren(){
		if (kids == null) return true;
		if (kids.isEmpty()) return false;
		return true;
	}
}
