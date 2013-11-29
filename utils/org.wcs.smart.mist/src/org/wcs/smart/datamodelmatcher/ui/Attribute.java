package org.wcs.smart.datamodelmatcher.ui;

import org.wcs.smart.internal.ca.datamodel.xml.generate.AttributeType;

public class Attribute {
	private String name;
	private String key;
	private AttributeType attributetype;
	
	public Attribute(String name , String key, AttributeType attributeType){
		this.name = name;
		this.key = key;
		this.attributetype = attributeType;
	}
	
	public String getKey(){
		return key;
	}
	public String getName(){
		return name;
	}
	
	public String getText(){
		return name;
	}
	
	public AttributeType getAttributeType(){
		return attributetype;
	}
}
