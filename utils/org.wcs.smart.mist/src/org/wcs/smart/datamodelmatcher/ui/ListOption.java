package org.wcs.smart.datamodelmatcher.ui;

public class ListOption {
	private String name;
	private String key;

	public ListOption(String name , String key){
		this.name = name;
		this.key = key;
		
	}
	public String getKey(){
		return key;
	}
	public String getName(){
		return name;
	}
}
