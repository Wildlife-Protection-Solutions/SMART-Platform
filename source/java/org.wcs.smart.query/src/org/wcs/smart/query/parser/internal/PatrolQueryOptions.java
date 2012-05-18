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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.measure.unit.BaseUnit;
import javax.measure.unit.Unit;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.graphics.Image;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.SimpleListItem;
import org.wcs.smart.ca.Station;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.PatrolHibernateManager;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.PatrolLegMember;
import org.wcs.smart.patrol.model.PatrolMandate;
import org.wcs.smart.patrol.model.PatrolTransportType;
import org.wcs.smart.patrol.model.PatrolType;
import org.wcs.smart.patrol.model.Team;
import org.wcs.smart.patrol.model.Track;
import org.wcs.smart.query.QueryHibernateManager;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.model.ListItem;
import org.wcs.smart.util.SmartUtils;

/**
 * Class that defines Patrol based options for
 * queries.  This includes both values for summary queries
 * and filters for summary and waypoint queries.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class PatrolQueryOptions {

	
	/**
	 * Patrol group by options for summary queries.
	 */
	public final static PatrolQueryOption[] PATROL_GROUBY_OPTIONS = {
			PatrolQueryOption.ID, 
			PatrolQueryOption.STATION, 
			PatrolQueryOption.TEAM, 
			PatrolQueryOption.MANDATE, 
			PatrolQueryOption.PATROL_TYPE, 
			PatrolQueryOption.PATROL_TRANSPORT_TYPE, 
			PatrolQueryOption.LEADER, 
			PatrolQueryOption.EMPLOYEE
	};
	
	/**
	 * Patrol filter options for summary and waypoint queries
	 */
	public final static PatrolQueryOption[] PATROL_FILTER_OPTIONS = {
			PatrolQueryOption.ID,
			PatrolQueryOption.ARMED, 
			PatrolQueryOption.STATION, 
			PatrolQueryOption.TEAM, 
			PatrolQueryOption.MANDATE, 
			PatrolQueryOption.PATROL_TYPE, 
			PatrolQueryOption.PATROL_TRANSPORT_TYPE, 
			PatrolQueryOption.LEADER,
			PatrolQueryOption.PILOT,
			PatrolQueryOption.EMPLOYEE
	};
	
	/**
	 * Represents the possible patrol values for summary
	 * queries.
	 * 
	 * @author egouge
	 * @since 1.0.0
	 */
	public enum PatrolValueOption{
		NUM_PATROLS("Number of Patrols", "numpatrols", Patrol.class),
		NUM_DAYS("Number of Days", "numdays", Patrol.class),
		NUM_NIGHTS("Number of Nights", "numnights", Patrol.class),
		DISTANCE("Distance (km)", "distance", Track.class),
		NUM_HOURS("Number of Hours", "numhours", PatrolLegDay.class),
		NUM_MEMBERS("Number of Employees", "nummembers", PatrolLegMember.class),
		MAN_HOURS("Person - Hours", "manhours", PatrolLegDay.class),
		MAN_DAYS("Person - Days", "mandays", PatrolLegDay.class);
		
		String key;		//unique key
		String guiName; //display name
		Class<?> clazz; //class that contains the value variable
		
		PatrolValueOption(String guiName, String queryKey, Class<?> clazz){
			this.guiName = guiName;
			this.key = queryKey;
			this.clazz = clazz;
		}
		
		public String getGuiName(){
			return this.guiName;
		}
				
		public String getKeyPart(){
			return this.key;
		}
		
		public Class<?> getOptionClass(){
			return this.clazz;
		}
	}
	
	/**
	 * Finds a particular patrol filter option based
	 * on the key.
	 *  
	 * @param key the patrol filter key
	 * @return
	 */
	public static final PatrolQueryOption findPatrolQueryOption(String key){
		return keyToColumnMap.get(key);
	}
	
	/*
	 * Maps of patrol filter keys to the patrol filter option
	 */
	private static Map<String, PatrolQueryOption> keyToColumnMap;
	static {
		keyToColumnMap = new HashMap<String, PatrolQueryOption>();
		for (int i = 0; i < PatrolQueryOption.values().length; i++) {
			PatrolQueryOption op = PatrolQueryOption.values()[i];
			keyToColumnMap.put(op.getKey(), op);
		}
		keyToColumnMap = Collections.unmodifiableMap(keyToColumnMap);
	}
	
	/**
	 * 
	 * @param option
	 * @return the image that represnts a particular date
	 * group by option
	 */
	public static Image getImage(DateGroupByOption option){
		if (option == DateGroupByOption.DAY){
			return JFaceResources.getImageRegistry().get(QueryPlugIn.CALENDAR_DAY_ICON);
		}else if (option == DateGroupByOption.MONTH){
			return JFaceResources.getImageRegistry().get(QueryPlugIn.CALENDAR_MONTH_ICON);
//		}else if (option == DateGroupByOption.WEEK){
//			return JFaceResources.getImageRegistry().get(QueryPlugIn.CALENDAR_WEEK_ICON);
		}else if (option == DateGroupByOption.YEAR){
			return JFaceResources.getImageRegistry().get(QueryPlugIn.CALENDAR_YEAR_ICON);
		}
		return null;
	}
	
	
	/**
	 * @param option
	 * @return the image that represents a particular 
	 * patrol filter option
	 */
	public static Image getImage(PatrolQueryOption option){
		if (option == PatrolQueryOption.ARMED){
			return JFaceResources.getImageRegistry().get(SmartPatrolPlugIn.PATROL_ARMED_ICON);
		}else if (option == PatrolQueryOption.TEAM){
			return JFaceResources.getImageRegistry().get(SmartPatrolPlugIn.PATROL_TEAM_ICON);
		}else if (option == PatrolQueryOption.STATION){
			return JFaceResources.getImageRegistry().get(SmartPlugIn.STATION_ICON);
		}else if (option == PatrolQueryOption.MANDATE){
			return JFaceResources.getImageRegistry().get(SmartPatrolPlugIn.PATROL_MANDATE_ICON);
		}else if (option == PatrolQueryOption.LEADER){
			return JFaceResources.getImageRegistry().get(SmartPatrolPlugIn.PATROL_LEADER_ICON);
		}else if (option == PatrolQueryOption.PILOT){
			return JFaceResources.getImageRegistry().get(SmartPatrolPlugIn.PATROL_PILOT_ICON);
		}else if (option == PatrolQueryOption.EMPLOYEE){
			return JFaceResources.getImageRegistry().get(SmartPatrolPlugIn.PATROL_MEMBER_ICON);
		}else if (option == PatrolQueryOption.PATROL_TYPE){
			return JFaceResources.getImageRegistry().get(SmartPatrolPlugIn.PATROL_ICON);
		}else if (option == PatrolQueryOption.PATROL_TRANSPORT_TYPE){
			return JFaceResources.getImageRegistry().get(SmartPatrolPlugIn.GROUND_PATROL_ICON);
		}else {
			return JFaceResources.getImageRegistry().get(SmartPatrolPlugIn.PATROL_ICON);
		}
	}
	
	/**
	 * Possible patrol filter option typs.
	 * @author egouge
	 * @since 1.0.0
	 */
	public enum PatrolQueryOptionType{
		BOOLEAN, UUID, STRING
	}
	
	/**
	 * Valid patrol filter options.
	 */
	public enum PatrolQueryOption{
		
		ID("Patrol ID", "id", "id", Patrol.class, Patrol.class, PatrolQueryOptionType.STRING),
		
		ARMED("Armed", "armed", "is_armed", Patrol.class, null, PatrolQueryOptionType.BOOLEAN),
		
		STATION("Station", "station", "station_uuid", Patrol.class, Station.class, PatrolQueryOptionType.UUID),
		
		TEAM("Team", "team", "team_uuid", Patrol.class, Team.class, PatrolQueryOptionType.UUID),
		
		EMPLOYEE("Patrol Member", "member", "employee_uuid", PatrolLegMember.class, Employee.class, PatrolQueryOptionType.UUID),
		
		LEADER("Patrol Leader", "leader", "employee_uuid", PatrolLegMember.class, Employee.class, PatrolQueryOptionType.UUID),
		
		PILOT("Patrol Pilot", "pilot", "employee_uuid", PatrolLegMember.class, Employee.class, PatrolQueryOptionType.UUID),
		
		MANDATE("Mandate", "mandate", "mandate_uuid", Patrol.class, PatrolMandate.class, PatrolQueryOptionType.UUID),
		
		PATROL_TYPE("Patrol Type", "patroltype", "patrol_type", Patrol.class, null, PatrolQueryOptionType.STRING),
		
		PATROL_TRANSPORT_TYPE("Transport Type", "transport", "transport_uuid", PatrolLeg.class, PatrolTransportType.class, PatrolQueryOptionType.UUID);
		
		
		private String key;			//unique identifier key
		private String columnName;	//column name in database table
		private Class<?> clazz;		//class containing the attribute
		private String guiName;		//display name
		private Class<?> sourceClazz; //class that represents the object
		private PatrolQueryOptionType type; //data type; if boolean, uuid or string field
		
		PatrolQueryOption(String guiName, String queryKey, 
				String columnName, Class<?> clazz, 
				Class<?> sourceClazz, PatrolQueryOptionType type){
			this.guiName = guiName;
			this.key = queryKey;
			this.columnName = columnName;
//			this.type = type;
			this.clazz = clazz;
			this.sourceClazz = sourceClazz;
			this.type = type;
		}
		
		/**
		 * @return <code>true</code> if this option involved employees
		 */
		public boolean isEmployeeItem(){
			return this == EMPLOYEE || this == PILOT || this == LEADER;
		}
		
		/**
		 * @return gui name
		 */
		public String getGuiName(){
			return this.guiName;
		}
		
		/**
		 * @return the option key
		 */
		public String getKey(){
			return this.key;
		}
		
		/**
		 * @return the database column name
		 */
		public String getColumnName(){
			return this.columnName;
		}
		
		/**
		 * @return option type
		 */
		public PatrolQueryOptionType getType(){
			return this.type;
		}
		
		/**
		 * 
		 * @return the class this patrol option is an attribute of
		 */
		public Class<?> getPatrolAttributeClass(){
			return this.clazz;
		}

		/**
		 * @return the class that represents the option
		 */
		public Class<?> getSourceClass(){
			return this.sourceClazz;
		}
		
		/**
		 * Given a particular uuid (key) determine the string
		 * name for the given option.
		 * 
		 * @param session
		 * @param uuid
		 * @return
		 */
		public String getName(Session session, byte[] uuid){
			Object x = getObject(session, uuid);
			if (x != null){
				if (x instanceof SimpleListItem){
					return ((SimpleListItem) x).getName();
				}else if (x instanceof Employee){
					return ((Employee) x).getLabel();
				}
			}
			return null;
		}
		
		/**
		 * Return an array of names that represent
		 * the uuid for a given option.
		 * 
		 * @param session
		 * @param uuid
		 * @return if this option represents employee then
		 * an array of the employeeid, givenname, familyname is returned,
		 * otherwise a simple element array with the object 
		 * name is returned.
		 */
		public String[] getNames(Session session, byte[] uuid){
			Object x = getObject(session, uuid);
			if (x != null){
				if (x instanceof SimpleListItem){
					return new String[]{((SimpleListItem)x).findName(SmartDB.getCurrentConservationArea().getDefaultLanguage())};
				}else if (x instanceof Employee){
					Employee e = (Employee)x;
					return new String[]{e.getId(), e.getGivenName(), e.getFamilyName()};
				}
			}
			return null;
		}
		
		/**
		 * Give a particular uuid return the source 
		 * object (returns a Team, Station etc. object)
		 * @param session
		 * @param uuid
		 * @return
		 */
		public Object getObject(Session session, byte[] uuid){
			List<?> data = session.createCriteria(sourceClazz).add(Restrictions.eq("uuid", uuid)).list();
			if (data.size() == 0){
				return null; //nothing found
			}else if (data.size() > 1){
				assert false; //should never happen
			}
			return data.get(0);
		}
		
		/**
		 * Given a set of keys (hex encoded uuids or string keys), returns
		 * a list of listitems that represent the objects
		 * with the given keys.
		 * 
		 * @param session
		 * @param keys
		 * @return
		 */
		public List<ListItem> getValues(Session session, String[] keys){
			List<ListItem> results = new ArrayList<ListItem>();
			if (type == PatrolQueryOptionType.UUID){
				
				
				byte[][] uuidkeys = new byte[keys.length][];
				try {
					for (int i = 0; i < keys.length; i++) {
						uuidkeys[i] = SmartUtils.decodeHex(keys[i]);
					}
				} catch (Exception ex) {
					QueryPlugIn.log(
							"Could not get values for given patrol filer."
									+ ex.getMessage(), ex);
					return results;
				}
				List<?> data = session.createCriteria(sourceClazz).add(Restrictions.in("uuid", uuidkeys)).list();
				
				for (Iterator<?> iterator = data.iterator(); iterator.hasNext();) {
					Object object = (Object) iterator.next();
					if (object instanceof SimpleListItem){
						results.add(new ListItem(((SimpleListItem) object).getUuid(), ((SimpleListItem) object).getName()));
					}else if (object instanceof Employee){
						Employee e = (Employee)object;
						results.add(new ListItem(e.getUuid(), e.getLabel()));
					}	
				}
				
			}else if (type == PatrolQueryOptionType.STRING){
				if (this == ID){
					List<?> data = session.createCriteria(sourceClazz).add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())).add(Restrictions.in(columnName, keys)).list();
					for (Iterator<?> iterator = data.iterator(); iterator.hasNext();) {
						Object object = (Object) iterator.next();
						if (object instanceof Patrol){
							results.add(new ListItem(((Patrol) object).getUuid(), ((Patrol) object).getId()));
						}
					}
				}else if (this == PATROL_TYPE){
					for (int i = 0; i < keys.length; i ++){
						results.add(new ListItem(null, PatrolType.Type.valueOf(keys[i]).getGuiName(), keys[i]));
					}
				}
			}
			return results;
		}
		
		/**
		 * @param session
		 * @return a list of listitems that represent all
		 * active values for a given object 
		 */
		public List<ListItem> getAllActiveValues(Session session){
			ArrayList<ListItem> items = new ArrayList<ListItem>();
			if (this == ID){
				//TODO: why uuid is not used here
				List<String> pids = QueryHibernateManager.getPatrolIds(session);
				for (String pid : pids){
					items.add(new ListItem(null, pid, pid));
				}
				
			}else if (this == STATION){
				List<Station> stations = PatrolHibernateManager.getActiveStations(SmartDB.getCurrentConservationArea(), session);
				for (Station s : stations){
					items.add(new ListItem(s.getUuid(), s.getName()));
				}
			}else if (this == TEAM){
				List<Team> teams = PatrolHibernateManager.getActiveTeams(SmartDB.getCurrentConservationArea(), session);
				for (Team t : teams){
					items.add(new ListItem(t.getUuid(), t.getName()));
				}
			}else if (this == MANDATE){
				List<PatrolMandate> mandates= PatrolHibernateManager.getActiveMandates(SmartDB.getCurrentConservationArea(), session);
				for (PatrolMandate t : mandates){
					items.add(new ListItem(t.getUuid(), t.getName()));
				}
			}else if (this == PATROL_TYPE){
				List<PatrolType> types= PatrolHibernateManager.getActivePatrolTypes(SmartDB.getCurrentConservationArea(), session);
				for (PatrolType t : types){
					items.add(new ListItem(null, t.getType().getGuiName(), t.getType().name() ));
				}
			}else if (this == PATROL_TRANSPORT_TYPE){
				List<PatrolTransportType> types= PatrolHibernateManager.getActivePatrolTransporationTypes(SmartDB.getCurrentConservationArea(), session);
				for (PatrolTransportType t : types){
					items.add(new ListItem(t.getUuid(), t.getName()));
				}
			}else if (this == LEADER ||
					this == PILOT || 
					this == EMPLOYEE){
				List<Employee> employees = PatrolHibernateManager.getActiveEmployees(SmartDB.getCurrentConservationArea(), session);
				for (Employee t : employees){
					items.add(new ListItem(t.getUuid(), t.getLabel() ));
				}
			}
			return items;
		}
	}
	
	
	/**
	 * Summary date group by options
	 * @author egouge
	 * @since 1.0.0
	 */
	public enum DateGroupByOption{
		
		DAY("Day", "day"),
	//	WEEK("Week", "week"),
		MONTH("Month", "month"),
		YEAR("Year", "year");
		
		private String key;
		private String guiName;
		
		private DateGroupByOption(String guiName, String key){
			this.guiName = guiName;
			this.key = key;
		}
		
		/**
		 * @return unique key represting option
		 */
		public String getKey(){
			return this.key;
		}
		
		/**
		 * @return display name
		 */
		public String getGuiName(){
			return this.guiName;
		}
	}
	
	/**
	 * Given a key returns the associated date group by option
	 * @param key
	 * @return
	 */
	public static DateGroupByOption findDateGroupByOption(String key){
		for (int i = 0; i < DateGroupByOption.values().length; i ++){
			if (DateGroupByOption.values()[i].key.equals(key)){
				return DateGroupByOption.values()[i];
			}
		}
		return null;
	}
	
	/**
	 * Given a key returns the associated patrol value option
	 * @param key
	 * @return
	 */
	public static PatrolValueOption findPatrolValueItem(String key) {
		for (int i = 0; i < PatrolValueOption.values().length; i++) {
			if (PatrolValueOption.values()[i].getKeyPart().equals(key)) {
				return PatrolValueOption.values()[i];
			}
		}
		return null;
	}
}
