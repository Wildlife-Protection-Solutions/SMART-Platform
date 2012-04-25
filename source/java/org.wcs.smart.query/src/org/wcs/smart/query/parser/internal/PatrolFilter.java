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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.SimpleListItem;
import org.wcs.smart.ca.Station;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegMember;
import org.wcs.smart.patrol.model.PatrolMandate;
import org.wcs.smart.patrol.model.PatrolTransportType;
import org.wcs.smart.patrol.model.PatrolType;
import org.wcs.smart.patrol.model.Team;
import org.wcs.smart.query.model.QueryHibernateManager;
import org.wcs.smart.query.ui.formulaDnd.DropItem;
import org.wcs.smart.query.ui.formulaDnd.DropItemFactory;
import org.wcs.smart.query.ui.formulaDnd.ListItem;
import org.wcs.smart.util.SmartUtils;

/**
 * A patrol filter.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class PatrolFilter implements IFilter {

	
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
	
	public static final int PATROL_FILTER_TYPE_STRING = 1;
	public static final int PATROL_FILTER_TYPE_BOOLEAN = 2;
	public static final int PATROL_FILTER_TYPE_UUID = 3;
	/**
	 * Valid patrol filter options.
	 */
	public enum PatrolFilterOption{
		ID("Patrol ID", "id", "id", PATROL_FILTER_TYPE_STRING, Patrol.class, Patrol.class),
		ARMED("Armed", "armed", "is_armed", PATROL_FILTER_TYPE_BOOLEAN, Patrol.class, null),
		STATION("Station", "station", "station_uuid", PATROL_FILTER_TYPE_UUID, Patrol.class, Station.class),
		TEAM("Team", "team", "team_uuid", PATROL_FILTER_TYPE_UUID, Patrol.class, Team.class),
		EMPLOYEE("Patrol Member", "member", "employee_uuid", PATROL_FILTER_TYPE_UUID, PatrolLegMember.class, Employee.class),
		LEADER("Patrol Leader", "leader", "employee_uuid", PATROL_FILTER_TYPE_UUID, PatrolLegMember.class, Employee.class),
		PILOT("Patrol Pilot", "pilot", "employee_uuid", PATROL_FILTER_TYPE_UUID, PatrolLegMember.class, Employee.class),
		MANDATE("Mandate", "mandate", "mandate_uuid", PATROL_FILTER_TYPE_UUID, Patrol.class, PatrolMandate.class),
		PATROLTYPE("Patrol Type", "patroltype", "patrol_type", PATROL_FILTER_TYPE_STRING, Patrol.class, null),
		TRANSPORT("Transport Type", "transport", "transport_uuid", PATROL_FILTER_TYPE_UUID, PatrolLeg.class, PatrolTransportType.class);
		
		//TODO: add objective & objective rating
		String key;
		String columnName;
		int type; //string = 1, boolean = 2, uuid = 3
		Class<?> clazz;
		String guiName;
		Class<?> sourceClazz;
		
		PatrolFilterOption(String guiName, String queryKey, String columnName, int type, Class<?> clazz, Class<?> sourceClazz){
			this.guiName = guiName;
			this.key = queryKey;
			this.columnName = columnName;
			this.type = type;
			this.clazz = clazz;
			this.sourceClazz = sourceClazz;
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
		public int getType(){
			return this.type;
		}

		
		public String[] getNames(Session session, byte[] uuid){
			
			List data = session.createCriteria(sourceClazz).add(Restrictions.eq("uuid", uuid)).list();
			if (data.size() == 0){
				//nothing found 
				return null;
			}else if (data.size() > 1){
				//more than one thing found; this should never happen
				return null;
			}else{
				Object x = data.get(0);
				if (x instanceof SimpleListItem){
					return new String[]{((SimpleListItem)x).findName(SmartDB.getCurrentConservationArea().getDefaultLanguage())};
				}else if (x instanceof Employee){
					Employee e = (Employee)x;
					return new String[]{e.getId(), e.getGivenName(), e.getFamilyName()};
				}
			}
			return null;
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
	 * @see org.wcs.smart.query.parser.internal.IFilter#asString()
	 */
	@Override
	public String asString(){
		if (value == null){
			return patrolKey;
		}else{
			return patrolKey + " " + op.asSmartValue() + " " + value;
		}
	}
	
		
	/**
	 * @see org.wcs.smart.query.parser.internal.IFilter#hasEmployeeFilter()
	 */
	@Override
	public boolean hasEmployeeFilter() {
		return  (patrolKey.equalsIgnoreCase(PatrolFilterOption.EMPLOYEE.key) ||
				patrolKey.equalsIgnoreCase(PatrolFilterOption.LEADER.key) ||
				patrolKey.equalsIgnoreCase(PatrolFilterOption.PILOT.key));
	}
	
	/**
	 * @see org.wcs.smart.query.parser.internal.IFilter#hasCategoryFilter()
	 */
	@Override
	public boolean hasCategoryFilter() {
		return false;
	}
	
	/**
	 * @see org.wcs.smart.query.parser.internal.IFilter#hasAttributeFilter()
	 */
	@Override
	public boolean hasAttributeFilter() {
		return false;
	}
	
	
	public PatrolFilterOption getPatrolType(){
		String patrolItem = patrolKey.split(":")[1];
		PatrolFilterOption option = keyToColumnMap.get(patrolItem);
		return option;
	}
	
	public String getValue(){
		return  SmartUtils.stripQuotes((String)value) ;
	}
	
	/**
	 * @see org.wcs.smart.query.parser.internal.IFilter#asSql(java.util.HashMap)
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

		if (option.type == PATROL_FILTER_TYPE_STRING){
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
		}else if (option.type == PATROL_FILTER_TYPE_BOOLEAN){
			//boolean
			String x = prefix + "." + option.columnName ; //+ " = 'true'" ;
			return x;
		}else if (option.type == PATROL_FILTER_TYPE_UUID){
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
	 * @see org.wcs.smart.query.parser.internal.IFilter#getAttributeFilters(java.util.HashSet)
	 */
	@Override
	public void getAttributeFilters(HashSet<AttributeInfo> attributes) {
	}
	
	/**
	 * 
	 */
	@Override
	public DropItem[] getDropItems(Session session) throws Exception{
		PatrolFilterOption filterType = keyToColumnMap.get(patrolKey.split(":")[1]);
		DropItem it = DropItemFactory.INSTANCE.createPatrolDropItem(filterType);
		
		String value1 = null;
		if (value != null){
			value1 = SmartUtils.stripQuotes((String)value);
		}
		if (filterType == PatrolFilterOption.ID){
			it.initializeData(new String[]{op.getGuiValue(), value1});
		}else if (filterType == PatrolFilterOption.MANDATE){
			ListItem m = QueryHibernateManager.getPatrolMandate(session, value1);
			it.initializeData(m);
		}else if (filterType == PatrolFilterOption.STATION){
			ListItem m = QueryHibernateManager.getStation(session, value1);
			it.initializeData(m);
		}else if (filterType == PatrolFilterOption.TEAM){
			ListItem m = QueryHibernateManager.getTeam(session, value1);
			it.initializeData(m);
		}else if (filterType == PatrolFilterOption.TRANSPORT){
			ListItem m = QueryHibernateManager.getTransportType(session, value1);
			it.initializeData(m);
			
		}else if (filterType == PatrolFilterOption.PATROLTYPE){
			PatrolType.Type t = PatrolType.Type.valueOf( value1 );
			ListItem m = new ListItem(null, t.getGuiName(), t.name());
			it.initializeData(m);
		}else if (filterType == PatrolFilterOption.EMPLOYEE ||
				filterType == PatrolFilterOption.LEADER ||
						filterType == PatrolFilterOption.PILOT
				){
			ListItem m = QueryHibernateManager.getEmployee(session, value1);
			it.initializeData(m);
			
		}
		
		return new DropItem[]{it};
	}
	
	/**
	 * @see org.wcs.smart.query.parser.internal.IFilter#getChildren()
	 */
	@Override
	public List<IFilter> getChildren() {
		return null;
	}
}
