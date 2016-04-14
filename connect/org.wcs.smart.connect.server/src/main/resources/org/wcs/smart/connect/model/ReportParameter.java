package org.wcs.smart.connect.model;

import java.util.ArrayList;
import java.util.List;

public class ReportParameter {

	public enum Type {
		DATE,
		TIME,
		DATETIME,
		DOUBLE,
		INTEGER,
		STRING,
		BOOLEAN,
		GROUP,
		OTHER;
	}
	private String name;
	private String displayText;
	private Object defaultValue;
	private Type type;
	private List<ReportParameter> kids;
	
	public ReportParameter(){
		kids = new ArrayList<ReportParameter>();
	}
	
	public ReportParameter(Type type, String name, String displayText, Object defaultValue){
		this();
		this.type = type;
		this.name = name;
		this.displayText = displayText;
		this.defaultValue = defaultValue;
	}
	
	public String getName(){
		return this.name;
	}
	
	public void setName(String name){
		this.name = name;
	}
	
	public Type getType(){
		return this.type;
	}
	
	public void setType(Type type){
		this.type = type;
	}
	
	public String getDisplayText(){
		return this.displayText;
	}
	public void setDisplayText(String displayText){
		this.displayText = displayText;
	}
	
	public Object getDefaultValue(){
		return this.defaultValue;
	}
	public void setDefaultValue(Object defaultValue){
		this.defaultValue = defaultValue;
	}
	
	public List<ReportParameter> getChildren(){
		return this.kids;
	}
	public void setChildren(List<ReportParameter> kids){
		this.kids = kids;
	}
}
