package org.wcs.smart.i2.query.observation.filter;

import org.wcs.smart.ca.Area;

public class AreaFilter implements IQueryFilter {

	public static AreaFilter create(String key){
		String[] bits = key.split(":");
		return new AreaFilter(Area.AreaType.valueOf(bits[1]), bits[2]);
	}
	
	private Area.AreaType areaType;
	private String areaKey;
	
	public AreaFilter(Area.AreaType areaType, String areaKey){
		this.areaType = areaType;
		this.areaKey = areaKey;
	}
	
	public Area.AreaType getType(){
		return this.areaType;
	}
	
	public String getKey(){
		return this.areaKey;
	}
	
	
}
