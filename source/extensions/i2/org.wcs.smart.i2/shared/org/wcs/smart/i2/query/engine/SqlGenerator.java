package org.wcs.smart.i2.query.engine;

import java.text.MessageFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.core.runtime.dynamichelpers.IFilter;
import org.wcs.smart.i2.query.Operator;
import org.wcs.smart.i2.query.observation.filter.IQueryFilter;
import org.wcs.smart.i2.query.observation.filter.NotFilter;

public class SqlGenerator {

	/**
	 * Creates a temporary query table 
	 * 
	 * @return
	 */
	private static AtomicLong tableCnter = new AtomicLong();
	public static synchronized String createTempTableName(){
		return "query_temp_i2_" + tableCnter.incrementAndGet();//$NON-NLS-1$ 
	}
	
	public static String generateDateClause(Date[] filter, String fieldName){
		if (filter[0] == null && filter[1] != null){
			return " ( cast(" + fieldName + " as date) <= '" + (new java.sql.Date(filter[1].getTime())).toString()+ "' ) ";
		}else if (filter[0] != null && filter[1] == null){
			return " ( cast(" + fieldName + " as date) >= '" + (new java.sql.Date(filter[0].getTime())).toString()+ "' ) ";
		}else if (filter[0] != null && filter[1] != null){
			return " ( cast(" + fieldName + " as date) >= '" + (new java.sql.Date(filter[0].getTime())).toString() + "'  AND cast(" + fieldName + " as date) <= '" + (new java.sql.Date(filter[1].getTime())).toString()  + "' ) ";
		}
		return null;
	}
	
	
	public static String operatorToSql(Operator op) throws Exception{
		switch(op){
		case AND:
			return "and";
		case BETWEEN:
			return "between";
		case BRACKETS:
			return "()";
		case BRACKET_CLOSE:
			return "(";
		case BRACKET_OPEN:
			return ")";
		case EQUALS:
			return "=";
		case GREATERTHAN:
			return ">";
		case GREATERTHANEQUALS:
			return ">=";
		case LESSTHAN:
			return "<";
		case LESSTHANEQUALS:
			return "<=";
		case NOT:
			return "not";
		case NOTEQUALS:
			return "<>";
		case NOT_BETWEEN:
			return "not between";
		case OR:
			return "or";
		case STR_CONTAINS:
			return "like";
		case STR_EQUALS:
			return "=";
		case STR_NOTCONTAINS:
			return "not like";
		default:
			break;
			
		}
		throw new Exception(MessageFormat.format("Operator {0}  not supported.", op.getKey()));
	}
	
	public static void logString(String string){
		System.out.println(string);
	}
}
