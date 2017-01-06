package org.wcs.smart.i2.query.engine;

import java.util.Date;

import org.wcs.smart.common.filter.DateFilterComposite.DateFilter;

public class SqlGenerator {

	
	public static String generateDateClause(Date[] filter, String fieldName){
		if (filter[0] == null && filter[1] != null){
			return " ( cast(" + fieldName + " as date) <= " + (new java.sql.Date(filter[1].getTime())).toString()+ " ) ";
		}else if (filter[0] != null && filter[1] == null){
			return " ( cast(" + fieldName + " as date) >= " + (new java.sql.Date(filter[0].getTime())).toString()+ " ) ";
		}else if (filter[0] != null && filter[1] != null){
			return " ( cast(" + fieldName + " as date) >= " + (new java.sql.Date(filter[0].getTime())).toString() + "  AND cast(" + fieldName + " as date) <= " + (new java.sql.Date(filter[1].getTime())).toString()  + " ) ";
		}
		return "";
	}
}
