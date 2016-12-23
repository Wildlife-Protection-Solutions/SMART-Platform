package org.wcs.smart.i2.ui.views.query;

import java.util.List;

import org.wcs.smart.i2.ui.views.query.dropitem.DropItem;

public abstract class FilterItem {

	private String name;
	
	public FilterItem(String name){
		this.name = name;
	}
	
	public String getName(){
		return name;
	}
	
	public abstract FilterItem getParent();
	
	public abstract List<FilterItem> getChildren();
	
	public abstract DropItem[] asDropItem();
	
}
