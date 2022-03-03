package org.wcs.smart.nunavut;

import java.util.ArrayList;

public class MysqlObservation {

	public enum valueType {
        NUMERIC,
        TEXT,
        LIST;
    }
	private String categoryKey;
	private ArrayList<String> attrs = new ArrayList<String>();
	private ArrayList<String> values = new ArrayList<String>();
	private ArrayList<valueType> valueTypes = new ArrayList<valueType>();
	
	
	public String getCategoryKey() {
		return categoryKey;
	}
	public void setCategoryKey(String categoryKey) {
		this.categoryKey = categoryKey;
	}
	public ArrayList<String> getAttrs() {
		return attrs;
	}
	public void setAttrs(ArrayList<String> attrs) {
		this.attrs = attrs;
	}
	public ArrayList<String> getValues() {
		return values;
	}
	public void setValues(ArrayList<String> values) {
		this.values = values;
	}
	public ArrayList<valueType> getValueTypes() {
		return valueTypes;
	}
	public void setValueTypes(ArrayList<valueType> valueTypes) {
		this.valueTypes = valueTypes;
	}
	
}
