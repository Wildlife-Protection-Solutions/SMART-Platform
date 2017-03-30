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

import org.hibernate.Session;
import org.wcs.smart.i2.internal.Messages;
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
			return " ( cast(" + fieldName + " as date) <= '" + (new java.sql.Date(filter[1].getTime())).toString()+ "' ) "; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}else if (filter[0] != null && filter[1] == null){
			return " ( cast(" + fieldName + " as date) >= '" + (new java.sql.Date(filter[0].getTime())).toString()+ "' ) "; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}else if (filter[0] != null && filter[1] != null){
			return " ( cast(" + fieldName + " as date) >= '" + (new java.sql.Date(filter[0].getTime())).toString() + "'  AND cast(" + fieldName + " as date) <= '" + (new java.sql.Date(filter[1].getTime())).toString()  + "' ) "; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		}
		return null;
	}
	
	
	public static String operatorToSql(Operator op) throws Exception{
		switch(op){
		case AND:
			return "and"; //$NON-NLS-1$
		case BETWEEN:
			return "between"; //$NON-NLS-1$
		case BRACKETS:
			return "()"; //$NON-NLS-1$
		case BRACKET_CLOSE:
			return ")"; //$NON-NLS-1$
		case BRACKET_OPEN:
			return "("; //$NON-NLS-1$
		case EQUALS:
			return "="; //$NON-NLS-1$
		case GREATERTHAN:
			return ">"; //$NON-NLS-1$
		case GREATERTHANEQUALS:
			return ">="; //$NON-NLS-1$
		case LESSTHAN:
			return "<"; //$NON-NLS-1$
		case LESSTHANEQUALS:
			return "<="; //$NON-NLS-1$
		case NOT:
			return "not"; //$NON-NLS-1$
		case NOTEQUALS:
			return "<>"; //$NON-NLS-1$
		case NOT_BETWEEN:
			return "not between"; //$NON-NLS-1$
		case OR:
			return "or"; //$NON-NLS-1$
		case STR_CONTAINS:
			return "like"; //$NON-NLS-1$
		case STR_EQUALS:
			return "="; //$NON-NLS-1$
		case STR_NOTCONTAINS:
			return "not like"; //$NON-NLS-1$
		default:
			break;
			
		}
		throw new Exception(MessageFormat.format(Messages.SqlGenerator_OpNotSupported, op.getKey()));
	}
	
	public static void switchTables(String tempTable, String obsTable, boolean locationIndex, boolean observationIndex, Session s){
		StringBuilder sql = new StringBuilder();
		sql.append("DROP TABLE " + obsTable); //$NON-NLS-1$
		logString(sql.toString());
		s.createSQLQuery(sql.toString()).executeUpdate();
		
		sql = new StringBuilder();
		sql.append("RENAME TABLE " + tempTable + " TO " + obsTable); //$NON-NLS-1$ //$NON-NLS-2$
		logString(sql.toString());
		s.createSQLQuery(sql.toString()).executeUpdate();
		
		if (locationIndex){
			sql = new StringBuilder();
			sql.append("CREATE INDEX location_uuid_idx on " + obsTable + " (location_uuid)"); //$NON-NLS-1$ //$NON-NLS-2$
			logString(sql.toString());
			s.createSQLQuery(sql.toString()).executeUpdate();
		}
		if (observationIndex){
			sql = new StringBuilder();
			sql.append("CREATE INDEX observation_uuid_idx on " + obsTable + " (observation_uuid)"); //$NON-NLS-1$ //$NON-NLS-2$
			logString(sql.toString());
			s.createSQLQuery(sql.toString()).executeUpdate();
		}
	}
	
	
	public static void logString(String string){
		//TODO:
		System.out.println(string);
	}
}
