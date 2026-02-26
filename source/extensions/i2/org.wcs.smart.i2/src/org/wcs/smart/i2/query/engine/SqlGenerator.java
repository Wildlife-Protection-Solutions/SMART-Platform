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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

import org.hibernate.Session;
import org.hibernate.query.MutationQuery;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.query.Operator;
import org.wcs.smart.i2.query.observation.filter.IQueryFilter;
import org.wcs.smart.i2.query.observation.filter.SystemAttributeFilter;
import org.wcs.smart.i2.query.observation.filter.SystemAttributeFilter.SystemAttribute;

/**
 * SQL Generation utilities for query engines
 * 
 * @author Emily
 *
 */
public class SqlGenerator {

	private static AtomicLong tableCnter = new AtomicLong();
	private static AtomicLong indexCnter = new AtomicLong();
	
	/**
	 * Creates a temporary query table with unique name.   
	 * 
	 * @return
	 */
	public static synchronized String createTempTableName(){
		String tname = "query_temp_i2_" + tableCnter.incrementAndGet();//$NON-NLS-1$
		logString("CREATE TABLE: " + tname); //$NON-NLS-1$
		return tname;
	}
	
	/**
	 * Creates a unique index name using the prefix 
	 * @param prefix
	 * @return
	 */
	public static synchronized String createIndexName(String prefix){
		return prefix + "_" + indexCnter.incrementAndGet() + "_idx";//$NON-NLS-1$ //$NON-NLS-2$ 
	}
	
	
	public static String generateDateClause(LocalDate[] filter, String fieldName){
		if (filter[0] == null && filter[1] != null){
			return " ( cast(" + fieldName + " as date) <= '" + filter[1].toString()+ "' ) "; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}else if (filter[0] != null && filter[1] == null){
			return " ( cast(" + fieldName + " as date) >= '" + filter[0].toString()+ "' ) "; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}else if (filter[0] != null && filter[1] != null){
			return " ( cast(" + fieldName + " as date) >= '" + filter[0].toString() + "'  AND cast(" + fieldName + " as date) <= '" + filter[1].toString()  + "' ) "; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		}
		return null;
	}
	
	
	/**
	 * Called when the fieldName value is stored in utc and the filters are localdatetime in UTC
	 * 
	 * @param filter
	 * @param fieldName
	 * @return
	 */
	//for these utc fields I created a UTC drop down item which display the date/time to the user
	//in there local timezone, but stores the utc value of this in the filter. This ensures connect
	//will produce the same results.
	public static String generateSystemDateClauseUtc(LocalDateTime[] filter, String fieldName){
				
		String[] dts = new String[] {null, null};
		
		if (filter[0] != null) {
			dts[0] = DateTimeFormatter.ofPattern(IQueryFilter.DATETIME_FORMAT_STR).format(filter[0]);
		}
		if (filter[1] != null){
			dts[1] = DateTimeFormatter.ofPattern(IQueryFilter.DATETIME_FORMAT_STR).format(filter[1]);
		}
		
		
		if (filter[0] == null && filter[1] != null){
			return " ( " + fieldName + " <= '" + dts[1] + "' ) "; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}else if (filter[0] != null && filter[1] == null){
			return " ( " + fieldName + " >= '" + dts[0] + "' ) "; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}else if (filter[0] != null && filter[1] != null){
			return " ( " + fieldName + " between '" + dts[0] + "' and '" + dts[1]  + "' ) "; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ 
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
		s.createNativeMutationQuery(sql.toString()).executeUpdate();
		
		sql = new StringBuilder();
		sql.append("RENAME TABLE " + tempTable + " TO " + obsTable); //$NON-NLS-1$ //$NON-NLS-2$
		logString(sql.toString());
		s.createNativeMutationQuery(sql.toString()).executeUpdate();
		
		if (locationIndex){
			sql = new StringBuilder();
			sql.append("CREATE INDEX " + obsTable + "_location_uuid_idx on " + obsTable + " (location_uuid)"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			logString(sql.toString());
			s.createNativeMutationQuery(sql.toString()).executeUpdate();
		}
		if (observationIndex){
			sql = new StringBuilder();
			sql.append("CREATE INDEX " + obsTable + "_observation_uuid_idx on " + obsTable + " (observation_uuid)"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			logString(sql.toString());
			s.createNativeMutationQuery(sql.toString()).executeUpdate();
		}
	}
	
	
	public static void logString(String string){
		if (Intelligence2PlugIn.LOG_QUERY) System.out.println(string);
	}
	
	
	/**
	 * 
	 * @param sql MUST include "smart.i_entity e" in the from clause
	 * @param filter
	 * @param session
	 * @throws Exception
	 */
	public static void processEntitySystemDateFilter(StringBuilder sql, SystemAttributeFilter filter, Session session) throws Exception {
		//these dates are stored as utc and the parameters needs to reflect this	
		if (filter.getAttribute() == SystemAttribute.ENTITY_DATE_CREATED) {
			sql.append(" e.date_created "); //$NON-NLS-1$
		}else if (filter.getAttribute() == SystemAttribute.ENTITY_DATE_MODIFIED) {
			sql.append(" e.date_modified "); //$NON-NLS-1$
		}			
		sql.append(operatorToSql(filter.getOperator()));
		sql.append(" :value1 and :value2"); //$NON-NLS-1$
	
		MutationQuery query = session.createNativeMutationQuery(sql.toString());
		
		LocalDateTime dstart = filter.getDateTimeValues()[0];
		LocalDateTime dend= filter.getDateTimeValues()[1];
		
		logString((DateTimeFormatter.ofPattern(IQueryFilter.DATETIME_FORMAT_STR)).format(dstart));
		logString((DateTimeFormatter.ofPattern(IQueryFilter.DATETIME_FORMAT_STR)).format(dend));
				
		query.setParameter("value1", (DateTimeFormatter.ofPattern(IQueryFilter.DATETIME_FORMAT_STR)).format(dstart) ); //$NON-NLS-1$
		query.setParameter("value2", (DateTimeFormatter.ofPattern(IQueryFilter.DATETIME_FORMAT_STR)).format(dend) ); //$NON-NLS-1$
		
	
		logString(sql.toString());
		query.executeUpdate();
	}
	
	/**
	 * 
	 * @param sql MUST include "smart.i_record r" in the from clause
	 * @param filter
	 * @param session
	 * @throws Exception
	 */
	public static void processRecordSystemDateFilter(StringBuilder sql, SystemAttributeFilter filter, Session session ) throws Exception{
			
		if (filter.getAttribute() == SystemAttribute.RECORD_DATE_CREATED) {
			sql.append(" r.date_created "); //$NON-NLS-1$
		}else if (filter.getAttribute() == SystemAttribute.RECORD_DATE_MODIFIED) {
			sql.append(" r.last_modified_date "); //$NON-NLS-1$
		}
		sql.append(operatorToSql(filter.getOperator()));
		sql.append(" :value1 and :value2"); //$NON-NLS-1$
	
		MutationQuery query = session.createNativeMutationQuery(sql.toString());
		
		LocalDateTime dtstart = filter.getDateTimeValues()[0];
		LocalDateTime dtend= filter.getDateTimeValues()[1];
		
		logString((DateTimeFormatter.ofPattern(IQueryFilter.DATETIME_FORMAT_STR)).format(dtstart));
		logString((DateTimeFormatter.ofPattern(IQueryFilter.DATETIME_FORMAT_STR)).format(dtend));
				
		query.setParameter("value1", (DateTimeFormatter.ofPattern(IQueryFilter.DATETIME_FORMAT_STR)).format(dtstart) ); //$NON-NLS-1$
		query.setParameter("value2", (DateTimeFormatter.ofPattern(IQueryFilter.DATETIME_FORMAT_STR)).format(dtend) ); //$NON-NLS-1$
		
	
		logString(sql.toString());
		query.executeUpdate();
	}
}
