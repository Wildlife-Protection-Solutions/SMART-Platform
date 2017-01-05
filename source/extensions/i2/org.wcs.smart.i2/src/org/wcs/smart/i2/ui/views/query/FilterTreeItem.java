package org.wcs.smart.i2.ui.views.query;

import java.util.List;

import org.wcs.smart.i2.ui.views.query.dropitem.DropItem;

public abstract class FilterTreeItem {

	private String name;
	
	/**
	 * Creates a new tree item with the given name
	 * @param name
	 */
	public FilterTreeItem(String name){
		this.name = name;
	}
	
	public String getName(){
		return name;
	}
	
	/**
	 * 
	 * @return the parent tree node
	 */
	public abstract FilterTreeItem getParent();
	
	/**
	 * Gets all children nodes
	 * @return
	 */
	public abstract List<FilterTreeItem> getChildren();
	
	/**
	 * Converts the tree node to a drop item
	 * @return
	 */
	public abstract DropItem[] asDropItem();
	
}
