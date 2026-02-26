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
package org.wcs.smart.connect.query.engine.i2;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

import org.hibernate.Session;
import org.hibernate.query.MutationQuery;
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
	
	public static final String QUERY_TEMP_SCHEMA = "query_temp"; //$NON-NLS-1$
	/**
	 * Creates a temporary query table with unique name.   
	 * 
	 * @return
	 */
	public static synchronized String createTempTableName(){
		String tablename = QUERY_TEMP_SCHEMA + ".query_temp_i2_" + tableCnter.incrementAndGet();//$NON-NLS-1$
		return tablename;
	}
	
	/**
	 * Creates a unique index name using the prefix 
	 * @param prefix
	 * @return
	 */
	public static synchronized String createIndexName(String prefix){
		int index = prefix.indexOf('.');
		if (index > 0) {
			prefix = prefix.substring(index+1);
		}
		return prefix + "_" + indexCnter.incrementAndGet() + "_idx";//$NON-NLS-1$ //$NON-NLS-2$ 
	}
	
	
	public static String generateDateClause(LocalDate[] filter, String fieldName){
		if (filter[0] == null && filter[1] != null){
			return " ( cast(" + fieldName + " as date) <= '" + DateTimeFormatter.ISO_LOCAL_DATE.format(filter[1]) + "' ) "; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}else if (filter[0] != null && filter[1] == null){
			return " ( cast(" + fieldName + " as date) >= '" + DateTimeFormatter.ISO_LOCAL_DATE.format(filter[0]) + "' ) "; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}else if (filter[0] != null && filter[1] != null){
			return " ( cast(" + fieldName + " as date) >= '" + DateTimeFormatter.ISO_LOCAL_DATE.format(filter[0]) + "'  AND cast(" + fieldName + " as date) <= '" + DateTimeFormatter.ISO_LOCAL_DATE.format(filter[1]) + "' ) "; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		}
		return null;
	}
	
	public static String generateDateTimeClause(LocalDateTime[] filter, String fieldName){
		if (filter[0] == null && filter[1] != null){
			return " ( " + fieldName + " <= '" + DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(filter[1]) + "' ) "; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}else if (filter[0] != null && filter[1] == null){
			return " ( " + fieldName + " >= '" + DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(filter[0]) + "' ) "; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}else if (filter[0] != null && filter[1] != null){
			return " ( " + fieldName + " between '" + DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(filter[0]) + "'  AND '" + DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(filter[1]) + "' ) "; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
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
		throw new Exception(MessageFormat.format("Operator {0} not supported.", op.getKey())); //$NON-NLS-1$
	}
	
	public static void switchTables(String tempTable, String obsTable, boolean locationIndex, boolean observationIndex, Session s){
		StringBuilder sql = new StringBuilder();
		sql.append("DROP TABLE " + obsTable); //$NON-NLS-1$
		logString(sql.toString());
		s.createNativeMutationQuery(sql.toString()).executeUpdate();
		
		sql = new StringBuilder();
		String newname = obsTable;
		int index = newname.indexOf('.');
		if (index > 0) {
			newname = newname.substring(index+1);
		}
		sql.append("ALTER TABLE " + tempTable + " RENAME TO " + newname); //$NON-NLS-1$ //$NON-NLS-2$
		logString(sql.toString());
		s.createNativeMutationQuery(sql.toString()).executeUpdate();
		
		if (locationIndex){
			sql = new StringBuilder();
			sql.append("CREATE INDEX " + SqlGenerator.createIndexName( obsTable ) + " on " + obsTable + " (location_uuid)"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			logString(sql.toString());
			s.createNativeMutationQuery(sql.toString()).executeUpdate();
		}
		if (observationIndex){
			sql = new StringBuilder();
			sql.append("CREATE INDEX " + SqlGenerator.createIndexName( obsTable ) + " on " + obsTable + " (observation_uuid)"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			logString(sql.toString());
			s.createNativeMutationQuery(sql.toString()).executeUpdate();
		}
	}
	
	
	public static void logString(String string){
		//System.out.println(string);
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

		query.setParameter("value1", dstart); //$NON-NLS-1$
		query.setParameter("value2", dend); //$NON-NLS-1$
	
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
				
		query.setParameter("value1", dtstart); //$NON-NLS-1$
		query.setParameter("value2", dtend); //$NON-NLS-1$
		
		logString(sql.toString());
		query.executeUpdate();
	}
}

