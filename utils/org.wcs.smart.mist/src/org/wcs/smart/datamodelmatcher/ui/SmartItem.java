package org.wcs.smart.datamodelmatcher.ui;

public class SmartItem {

	private String category;
	
	public SmartItem(String category){
		this.category = category;
	}
	public SmartItem() {
		category = new String();
	}
	public String getText(){
		return category;
	}
	
	public void setCategory(String s){
		this.category = s;
	}
	
}
