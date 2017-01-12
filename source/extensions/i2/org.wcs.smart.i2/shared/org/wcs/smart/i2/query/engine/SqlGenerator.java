/*
 * Copyright (C) 2016 Wildlife Conservation Society
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.wcs.smart.i2.query.engine;

import java.text.MessageFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

import org.wcs.smart.i2.query.Operator;

/**
 * SQL Generation utilities for query engines
 * 
 * @author Emily
 *
 */
public class SqlGenerator {

	private static AtomicLong tableCnter = new AtomicLong();
	
	/**
	 * Creates a temporary query table with unique name.   
	 * 
	 * @return
	 */
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
