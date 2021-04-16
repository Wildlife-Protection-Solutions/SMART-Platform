package org.wcs.smart.query.model.filter;

public enum FilterType {
	OBSERVATION("observation"),  //$NON-NLS-1$
	GROUP("obsgroup"), //$NON-NLS-1$
	WAYPOINT("waypoint"); //$NON-NLS-1$

	private String key;
	
	FilterType(String key){
		this.key = key;
	}
	public String getKey(){
		return this.key;
	}		
	public static FilterType parse(String type){
		if (type.equals(WAYPOINT.key)) return WAYPOINT;
		if (type.equals(GROUP.key)) return GROUP;
		return OBSERVATION;
	}
}
