package org.wcs.smart.data.oda.smart.impl;

public class GeometryColumn {

	private String label;
	
	private String key;
	
	public GeometryColumn(String label, String key){
		this.label = label;
		this.key = key;
	}
	
	public String getKey(){
		return this.key;
	}
	
	public String getLabel(){
		return this.label;
	}
}
