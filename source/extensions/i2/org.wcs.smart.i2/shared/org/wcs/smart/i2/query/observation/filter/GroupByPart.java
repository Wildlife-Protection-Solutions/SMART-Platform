package org.wcs.smart.i2.query.observation.filter;

import java.util.ArrayList;
import java.util.List;

public class GroupByPart {

	public static GroupByPart parse(List<GroupByItem> items) {
		GroupByPart p = new GroupByPart();
		for (GroupByItem i : items)p.addItem(i);
		return p;
	}
	
	private List<GroupByItem> items = new ArrayList<>();
	
	public GroupByPart() {
		
	}
	
	public List<GroupByItem> getItems(){
		return this.items;
	}
	
	public void addItem(GroupByItem item) {
		this.items.add(item);
	}
}
