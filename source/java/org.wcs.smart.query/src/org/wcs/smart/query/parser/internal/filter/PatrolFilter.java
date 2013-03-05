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
package org.wcs.smart.query.parser.internal.filter;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolType;
import org.wcs.smart.query.QueryHibernateManager;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.ListItem;
import org.wcs.smart.query.parser.PatrolQueryOptions;
import org.wcs.smart.query.parser.PatrolQueryOptions.PatrolQueryOption;
import org.wcs.smart.query.parser.PatrolQueryOptions.PatrolQueryOptionType;
import org.wcs.smart.query.ui.formulaDnd.DropItem;
import org.wcs.smart.query.ui.formulaDnd.DropItemFactory;
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
	

	private String patrolKey = null;
	private Operator op= null;
	private Object value= null;
	private PatrolQueryOption option ;

	
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
		
		String patrolItem = patrolKey.split(":")[1]; //$NON-NLS-1$
		option = PatrolQueryOptions.findPatrolQueryOption(patrolItem);
	}
	
	/**
	 * @see org.wcs.smart.query.parser.internal.filter.IFilter#asString()
	 */
	@Override
	public String asString(){
		if (value == null){
			return patrolKey;
		}else{
			return patrolKey + " " + op.asSmartValue() + " " + value;  //$NON-NLS-1$  //$NON-NLS-2$
		}
	}
	
		
	/**
	 * @see org.wcs.smart.query.parser.internal.filter.IFilter#hasEmployeeFilter()
	 */
	@Override
	public boolean hasEmployeeFilter() {
		return  (patrolKey.equalsIgnoreCase(PatrolQueryOption.EMPLOYEE.getKey()) ||
				patrolKey.equalsIgnoreCase(PatrolQueryOption.LEADER.getKey()) ||
				patrolKey.equalsIgnoreCase(PatrolQueryOption.PILOT.getKey()));
	}
	
	/**
	 * @see org.wcs.smart.query.parser.internal.filter.IFilter#hasCategoryFilter()
	 */
	@Override
	public boolean hasCategoryFilter() {
		return false;
	}
	
	/**
	 * @see org.wcs.smart.query.parser.internal.filter.IFilter#hasAttributeFilter()
	 */
	@Override
	public boolean hasAttributeFilter() {
		return false;
	}
	
	
	/**
	 * @return the patrol filter type
	 */
	public PatrolQueryOption getPatrolOption(){
		return option;
	}
	
	/**
	 * <p>Quotes have been removed</p>
	 * @return the filter value as a string
	 */
	public String getValue(){
		return  SmartUtils.stripQuotes((String)value) ;
	}
	
	/**
	 * The value to set the filter to (without quotes).
	 * @param value
	 */
	public void setValue(String value){
		this.value = "\"" + value + "\""; //$NON-NLS-1$ //$NON-NLS-2$
		
	}
	/**
	 * @see org.wcs.smart.query.parser.internal.filter.IFilter#asSql(java.util.HashMap)
	 */
	@Override
	public String asSql(HashMap<Class<?>, String> tableMapping) {	
		if (option.isEmployeeItem()){
			return employeeOptionAsSql(tableMapping, option);
			
		}		
		String prefix = tableMapping.get(option.getPatrolAttributeClass());
		if (prefix == null){
			throw new IllegalStateException(MessageFormat.format(
					Messages.PatrolFilter_InvalidPrefix, new Object[]{ option.getKey()}));
		}

		if (option.getType() == PatrolQueryOptionType.STRING){
			if (option == PatrolQueryOption.PATROL_TYPE){
				String x = prefix + "." + option.getColumnName() + " = '" + SmartUtils.stripQuotes((String)value) + "'"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				return x;				
			}else{
				String value1 = SmartUtils.stripQuotes((String)value);
				if (op == Operator.STR_CONTAINS || op == Operator.STR_NOTCONTAINS){
					value1 = "%" + value1 + "%"; //$NON-NLS-1$ //$NON-NLS-2$
				}
				String x = "LOWER(" + prefix + "." + option.getColumnName() + ") " + op.asSql() + " '" + value1.toLowerCase() + "'"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
				return x;
			}
		}else if (option.getType() == PatrolQueryOptionType.BOOLEAN){
			//boolean
			String x = prefix + "." + option.getColumnName() ; //+ " = 'true'" ; //$NON-NLS-1$
			return x;
		}else if (option.getType() == PatrolQueryOptionType.UUID){
			//uuid
			try{
				String value2 = SmartUtils.stripQuotes((String)value);
				String x = prefix + "." + option.getColumnName() + " = x'" + value2 + "'"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				return x;
			}catch (Exception ex){
				throw new IllegalStateException(ex);
			}
			
		}
		return ""; //$NON-NLS-1$
	}
	
	
	/**
	 * Add employee filter 
	 * @param tableMapping
	 * @param option
	 * @return
	 */
	private String employeeOptionAsSql(HashMap<Class<?>, String> tableMapping,
			PatrolQueryOption option) {
		try {
			String prefix = tableMapping.get(PatrolLeg.class);
			String x = prefix + ".uuid IN ( select patrol_leg_uuid from smart.patrol_leg_members "  //$NON-NLS-1$
					+ " where "; //$NON-NLS-1$
			if (option == PatrolQueryOption.LEADER) {
				x += " is_leader  AND "; //$NON-NLS-1$
			} else if (option == PatrolQueryOption.PILOT) {
				x += " is_pilot AND "; //$NON-NLS-1$
			}
			String value2 = SmartUtils.stripQuotes((String)value);			
			x += " employee_uuid = x'" + value2 + "')"; //$NON-NLS-1$ //$NON-NLS-2$
			return x;
		} catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}
	
	
	/**)
	 * @see org.wcs.smart.query.parser.internal.filter.IFilter#getAttributeFilters(java.util.HashSet)
	 */
	@Override
	public void getAttributeFilters(HashSet<AttributeInfo> attributes) {
	}
	
	/**
	 * 
	 */
	@Override
	public DropItem[] getDropItems(Session session) throws Exception{
		DropItem it = DropItemFactory.INSTANCE.createPatrolFilterDropItem(option);
		
		String value1 = null;
		if (value != null){
			value1 = SmartUtils.stripQuotes((String)value);
		}
		if (option == PatrolQueryOption.ID){
			it.initializeData(new String[]{op.getGuiValue(), value1});
		}else if (option == PatrolQueryOption.MANDATE){
			ListItem m = QueryHibernateManager.getInstance().getPatrolMandate(session, value1);
			it.initializeData(m);
		}else if (option == PatrolQueryOption.STATION){
			ListItem m = QueryHibernateManager.getInstance().getStation(session, value1);
			it.initializeData(m);
		}else if (option == PatrolQueryOption.TEAM){
			ListItem m = QueryHibernateManager.getInstance().getTeam(session, value1);
			it.initializeData(m);
		}else if (option == PatrolQueryOption.PATROL_TRANSPORT_TYPE){
			ListItem m = QueryHibernateManager.getInstance().getTransportType(session, value1);
			it.initializeData(m);
			
		}else if (option == PatrolQueryOption.PATROL_TYPE){
			PatrolType.Type t = PatrolType.Type.valueOf( value1 );
			ListItem m = new ListItem(null, t.getGuiName(), t.name());
			it.initializeData(m);
		}else if (option == PatrolQueryOption.EMPLOYEE ||
				option == PatrolQueryOption.LEADER ||
						option == PatrolQueryOption.PILOT
				){
			ListItem m = QueryHibernateManager.getInstance().getEmployee(session, value1);
			it.initializeData(m);
			
		}
		
		return new DropItem[]{it};
	}
	
	/**
	 * @see org.wcs.smart.query.parser.internal.filter.IFilter#getChildren()
	 */
	@Override
	public List<IFilter> getChildren() {
		return null;
	}
}
