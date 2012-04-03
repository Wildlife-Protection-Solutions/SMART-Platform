/*
 * Copyright (C) 2012 Wildlife Conservation Society
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
package org.wcs.smart.query.parser.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.wcs.smart.SmartUtils;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.PatrolLegMember;
import org.wcs.smart.patrol.model.PatrolMandate;
import org.wcs.smart.patrol.model.PatrolType;

/**
 * TODO Purpose of 
 * <p>
 * <ul>
 * <li></li>
 * </ul>
 * </p>
 * @author Emily
 * @since 1.0.0
 */
public class PatrolFilter implements Filter {

	private String patrolKey = null;
	private Operator op= null;
	private Object value= null;

	
	private enum PatrolFilterOption{
		ID("id", "id", 1, Patrol.class),
		ARMED("armed", "is_armed", 2, Patrol.class),
		STATION("station", "station", 3, Patrol.class),
		TEAM("team", "team", 3, Patrol.class),
		EMPLOYEE("member", "id.member", 3, PatrolLegMember.class),
		LEADER("leader", "id.member", 3, PatrolLegMember.class),
		PILOT("pilot", "id.member", 3, PatrolLegMember.class),
		MANDATE("mandate", "mandate", 3, Patrol.class),
		PATROLTYPE("patroltype", "patrolType", 1, Patrol.class),
		TRANSPORT("transport", "type", 3, PatrolLeg.class);
		
		
		String key;
		String columnName;
		int type; //string = 1, boolean = 2, uuid = 3
		Class<?> clazz;
		PatrolFilterOption(String queryKey, String columnName, int type, Class<?> clazz){
			this.key = queryKey;
			this.columnName = columnName;
			this.type = type;
			this.clazz = clazz;
		}
		
		boolean isEmployeeItem(){
			return this == EMPLOYEE || this == PILOT || this == LEADER;
		}
		
	}
	
	private static Map<String, PatrolFilterOption> keyToColumnMap;
	static{
		keyToColumnMap = new HashMap<String, PatrolFilterOption>();
		for (int i = 0; i < PatrolFilterOption.values().length ; i ++){
			PatrolFilterOption op = PatrolFilterOption.values()[i];
			keyToColumnMap.put(op.key, op);
		}
		keyToColumnMap = Collections.unmodifiableMap(keyToColumnMap);
	}
	
	public PatrolFilter (String patrolKey, Operator op, Object value){
		this(patrolKey);
		this.op = op;
		this.value = value;
		
		
	}
	public PatrolFilter (String patrolKey){
		this.patrolKey = patrolKey;
	}
	
	public String asString(){
		if (value == null){
			return patrolKey;
		}else{
			return patrolKey + " " + op.asString() + " " + value;
		}
	}
	
	
	public static PatrolFilter createStringFilter(String key, Operator op, Object value){
		return new PatrolFilter(key, op, value);
	}
	public static PatrolFilter createBooleanFilter(String key){
		return new PatrolFilter(key);
	}

	
	/* (non-Javadoc)
	 * @see org.wcs.smart.query.parser.internal.Filter#asSql(java.util.HashMap)
	 */
	@Override
	public String asHql(HashMap<Class<?>, String> tableMapping, HashMap<String, Object> parameters) {
		String patrolItem = patrolKey.split(":")[1];
		PatrolFilterOption option = keyToColumnMap.get(patrolItem);
		
		if (option.isEmployeeItem()){
			return employeeOptionAsHql(tableMapping, parameters, option);
			
		}
		
		
		String prefix = tableMapping.get(option.clazz);
		if (prefix == null){
			throw new IllegalStateException("Patrol prefix could not be determined for type " + option.key);
		}

		if (option.type == 1){
			//string
			String key1 = "p" + parameters.size();
			String value1 = SmartUtils.stripQuotes((String)value);	
			
			if (option == PatrolFilterOption.PATROLTYPE){
				String x = prefix + "." + option.columnName + " = :" + key1;
				parameters.put(key1, PatrolType.Type.valueOf(value1));
				return x;
			}else{
				if (op == Operator.STR_CONTAINS || op == Operator.STR_NOTCONTAINS){
					value1 = "%" + value1 + "%";
				}
				String x = prefix + "." + option.columnName + " " + op.asHql() + " :" + key1;
				parameters.put(key1, value1);
				return x;
			}
			
		}else if (option.type == 2){
			//boolean
			String x = prefix + "." + option.columnName + " = 'true'" ;
			return x;
		}else if (option.type == 3){
			//uuid
			try{
				String key1 = "p" + parameters.size();
				byte[] value1 = SmartUtils.decodeHex(SmartUtils.stripQuotes((String)value));
				String x = prefix + "." + option.columnName + ".uuid = :" + key1;
				parameters.put(key1, value1);
				return x;
			}catch (Exception ex){
				throw new IllegalStateException(ex);
			}
			
		}
		return "";
	}
	private String employeeOptionAsHql(HashMap<Class<?>, String> tableMapping,
			HashMap<String, Object> parameters, PatrolFilterOption option) {
		try {
			String prefix = tableMapping.get(PatrolLeg.class);

			String key1 = "p" + parameters.size();
			byte[] value1 = SmartUtils.decodeHex(SmartUtils.stripQuotes((String) value));
			String x = prefix + " IN ( select id.patrolLeg.uuid from " 
					+ PatrolLegMember.class.getSimpleName()
					+ " where ";
			if (option == PatrolFilterOption.LEADER) {
				x += " isLeader = 'true' AND ";
			} else if (option == PatrolFilterOption.PILOT) {
				x += " isPilot = 'true' AND ";
			}
			x += " id.member.uuid = :" + key1 + ")";
			parameters.put(key1, value1);
			return x;
		} catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}
	
	
	/* (non-Javadoc)
	 * @see org.wcs.smart.query.parser.internal.Filter#hasAttributeTreeItemFilter()
	 */
	@Override
	public boolean hasAttributeTreeItemFilter() {
		return false;
	}
	/* (non-Javadoc)
	 * @see org.wcs.smart.query.parser.internal.Filter#hasAttributeListItemFilter()
	 */
	@Override
	public boolean hasAttributeListItemFilter() {
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.wcs.smart.query.parser.internal.Filter#hasEmployeeFilter()
	 */
	@Override
	public boolean hasEmployeeFilter() {
		return  (patrolKey.equalsIgnoreCase(PatrolFilterOption.EMPLOYEE.key) ||
				patrolKey.equalsIgnoreCase(PatrolFilterOption.LEADER.key) ||
				patrolKey.equalsIgnoreCase(PatrolFilterOption.PILOT.key));
	}
	/* (non-Javadoc)
	 * @see org.wcs.smart.query.parser.internal.Filter#hasCategoryFilter()
	 */
	@Override
	public boolean hasCategoryFilter() {
		return false;
	}
	/* (non-Javadoc)
	 * @see org.wcs.smart.query.parser.internal.Filter#hasAttributeFilter()
	 */
	@Override
	public boolean hasAttributeFilter() {
		return false;
	}
	
	
	
	/* (non-Javadoc)
	 * @see org.wcs.smart.query.parser.internal.Filter#asSql(java.util.HashMap)
	 */
	@Override
	public String asSql(HashMap<Class<?>, String> tableMapping) {
		String patrolItem = patrolKey.split(":")[1];
		PatrolFilterOption option = keyToColumnMap.get(patrolItem);
		
		if (option.isEmployeeItem()){
			return employeeOptionAsSql(tableMapping, option);
			
		}		
		String prefix = tableMapping.get(option.clazz);
		if (prefix == null){
			throw new IllegalStateException("Patrol prefix could not be determined for type " + option.key);
		}

		if (option.type == 1){
			
			if (option == PatrolFilterOption.PATROLTYPE){
				String x = prefix + "." + option.columnName + " = '" + ((String)value) + "'";
				return x;
			}else{
				String value1 = SmartUtils.stripQuotes((String)value);
				if (op == Operator.STR_CONTAINS || op == Operator.STR_NOTCONTAINS){
					value1 = "%" + value1 + "%";
				}
				String x = prefix + "." + option.columnName + " " + op.asSql() + " '" + value1 + "'";
				return x;
			}
			
		}else if (option.type == 2){
			//boolean
			String x = prefix + "." + option.columnName ; //+ " = 'true'" ;
			return x;
		}else if (option.type == 3){
			//uuid
			try{
				byte[] value1 = SmartUtils.decodeHex(SmartUtils.stripQuotes((String)value));
				String value2 = Arrays.toString(value1).replaceAll(" ","").replaceAll("[", "").replaceAll("]", "").replaceAll(",", "");			
				String x = prefix + "." + option.columnName + ".uuid = x'" + value2 + "'";
				return x;
			}catch (Exception ex){
				throw new IllegalStateException(ex);
			}
			
		}
		return "";
	}
	private String employeeOptionAsSql(HashMap<Class<?>, String> tableMapping,
			PatrolFilterOption option) {
		try {
			String prefix = tableMapping.get(PatrolLeg.class);

			byte[] value1 = SmartUtils.decodeHex(SmartUtils.stripQuotes((String) value));
			String x = prefix + " IN ( select id.patrolLeg.uuid from " 
					+ PatrolLegMember.class.getSimpleName()
					+ " where ";
			if (option == PatrolFilterOption.LEADER) {
//				x += " isLeader = 'true' AND ";
				x += " isLeader  AND ";
			} else if (option == PatrolFilterOption.PILOT) {
				//x += " isPilot = 'true' AND ";
				x += " isPilot AND ";
			}
			String value2 = Arrays.toString(value1).replaceAll(" ","").replaceAll("[", "").replaceAll("]", "").replaceAll(",", "");			
			x += " id.member.uuid = x'" + value2 + "')";
			
			return x;
		} catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}
}
