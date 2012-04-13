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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.wcs.smart.SmartUtils;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegMember;

/**
 * A patrol filter.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class PatrolFilter implements Filter {

	
	/**
	 * Creates a patrol filter
	 * 
	 * @param key patrol filter key
	 * @param op patrol filter operator 
	 * @param value patrol filter value
	 * @return
	 */
	public static PatrolFilter createStringFilter(String key, Operator op, Object value){
		return new PatrolFilter(key, op, value);
	}
	
	/**
	 * Creates a patrol filter for a boolean patrol filter option
	 * 
	 * @param key the patrol key 
	 * @return
	 */
	public static PatrolFilter createBooleanFilter(String key){
		return new PatrolFilter(key);
	}
	/**
	 * Valid patrol filter options.
	 */
	public enum PatrolFilterOption{
		ID("Patrol ID", "id", "id", 1, Patrol.class),
		ARMED("Armed", "armed", "is_armed", 2, Patrol.class),
		STATION("Station", "station", "station_uuid", 3, Patrol.class),
		TEAM("Team", "team", "team_uuid", 3, Patrol.class),
		EMPLOYEE("Patrol Member", "member", "employee_uuid", 3, PatrolLegMember.class),
		LEADER("Patrol Leader", "leader", "employee_uuid", 3, PatrolLegMember.class),
		PILOT("Patrol Pilot", "pilot", "employee_uuid", 3, PatrolLegMember.class),
		MANDATE("Mandate", "mandate", "mandate_uuid", 3, Patrol.class),
		PATROLTYPE("Patrol Type", "patroltype", "patrol_type", 1, Patrol.class),
		TRANSPORT("Transport Type", "transport", "transport_uuid", 3, PatrolLeg.class);
		
		//TODO: add objective & objective rating
		String key;
		String columnName;
		int type; //string = 1, boolean = 2, uuid = 3
		Class<?> clazz;
		String guiName;
		
		PatrolFilterOption(String guiName, String queryKey, String columnName, int type, Class<?> clazz){
			this.guiName = guiName;
			this.key = queryKey;
			this.columnName = columnName;
			this.type = type;
			this.clazz = clazz;
		}
		
		boolean isEmployeeItem(){
			return this == EMPLOYEE || this == PILOT || this == LEADER;
		}
		
		public String getGuiName(){
			return this.guiName;
		}
		
		public String getKeyPart(){
			return this.key;
		}

	}

	private String patrolKey = null;
	private Operator op= null;
	private Object value= null;
	
	/*
	 * Maps of patrol filter keys to the patrol filter option
	 */
	private static Map<String, PatrolFilterOption> keyToColumnMap;
	static {
		keyToColumnMap = new HashMap<String, PatrolFilterOption>();
		for (int i = 0; i < PatrolFilterOption.values().length; i++) {
			PatrolFilterOption op = PatrolFilterOption.values()[i];
			keyToColumnMap.put(op.key, op);
		}
		keyToColumnMap = Collections.unmodifiableMap(keyToColumnMap);
	}
	
	/**
	 * Creates a new patrol filter
	 * @param patrolKey patrol key
	 * @param op operator
	 * @param value filter value
	 */
	public PatrolFilter (String patrolKey, Operator op, Object value){
		this(patrolKey);
		this.op = op;
		this.value = value;
	}
	
	
	/**
	 * Creates a new patrol filter
	 * @param patrolKey patrol filter key
	 */
	private PatrolFilter (String patrolKey){
		this.patrolKey = patrolKey;
	}
	
	/**
	 * @see org.wcs.smart.query.parser.internal.Filter#asString()
	 */
	@Override
	public String asString(){
		if (value == null){
			return patrolKey;
		}else{
			return patrolKey + " " + op.asString() + " " + value;
		}
	}
	
		
	/**
	 * @see org.wcs.smart.query.parser.internal.Filter#hasEmployeeFilter()
	 */
	@Override
	public boolean hasEmployeeFilter() {
		return  (patrolKey.equalsIgnoreCase(PatrolFilterOption.EMPLOYEE.key) ||
				patrolKey.equalsIgnoreCase(PatrolFilterOption.LEADER.key) ||
				patrolKey.equalsIgnoreCase(PatrolFilterOption.PILOT.key));
	}
	
	/**
	 * @see org.wcs.smart.query.parser.internal.Filter#hasCategoryFilter()
	 */
	@Override
	public boolean hasCategoryFilter() {
		return false;
	}
	
	/**
	 * @see org.wcs.smart.query.parser.internal.Filter#hasAttributeFilter()
	 */
	@Override
	public boolean hasAttributeFilter() {
		return false;
	}
	
	/**
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
				String x = prefix + "." + option.columnName + " = '" + SmartUtils.stripQuotes((String)value) + "'";
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
				String value2 = SmartUtils.stripQuotes((String)value);
				String x = prefix + "." + option.columnName + " = x'" + value2 + "'";
				return x;
			}catch (Exception ex){
				throw new IllegalStateException(ex);
			}
			
		}
		return "";
	}
	
	
	/**
	 * Add employee filter 
	 * @param tableMapping
	 * @param option
	 * @return
	 */
	private String employeeOptionAsSql(HashMap<Class<?>, String> tableMapping,
			PatrolFilterOption option) {
		try {
			String prefix = tableMapping.get(PatrolLeg.class);
			String x = prefix + ".uuid IN ( select patrol_leg_uuid from smart.patrol_leg_members " 
					+ " where ";
			if (option == PatrolFilterOption.LEADER) {
				x += " is_leader  AND ";
			} else if (option == PatrolFilterOption.PILOT) {
				x += " is_pilot AND ";
			}
			String value2 = SmartUtils.stripQuotes((String)value);			
			x += " employee_uuid = x'" + value2 + "')";
			return x;
		} catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}
	
	
	/**)
	 * @see org.wcs.smart.query.parser.internal.Filter#getAttributeFilters(java.util.HashSet)
	 */
	@Override
	public void getAttributeFilters(HashSet<AttributeInfo> attributes) {
	}
}
