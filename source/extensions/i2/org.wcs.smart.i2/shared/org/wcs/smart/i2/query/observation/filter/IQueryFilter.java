package org.wcs.smart.i2.query.observation.filter;

import java.text.SimpleDateFormat;

public interface IQueryFilter {

	public static String DATE_FORMAT_STR = "yyyy-MM-dd";
	
	public enum FilterType{
		OBSERVATION("observation"),
		WAYPOINT("waypoint");
		
		String key;
		FilterType(String key){
			this.key = key;	
		}
		
		public String getKey(){
			return this.key;
		}
		
		public static FilterType parse(String key){
			for (FilterType t : FilterType.values()){
				if (t.key.equalsIgnoreCase(key)) return t;
			}
			throw new IllegalStateException(key + " invalid filter type");
		}
	}
	
}
