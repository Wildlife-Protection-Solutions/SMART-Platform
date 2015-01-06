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
package org.wcs.smart.patrol.query.parser;


import java.sql.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.graphics.Image;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.NamedItem;
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
import org.wcs.smart.patrol.query.PatrolQueryPlugIn;
import org.wcs.smart.patrol.query.hibernate.MultiCaPatrolQueryHibernateManagerImpl;
import org.wcs.smart.patrol.query.hibernate.PatrolQueryHibernateManager;
import org.wcs.smart.patrol.query.internal.Messages;
import org.wcs.smart.patrol.query.model.PatrolDropItemFactory;
import org.wcs.smart.patrol.query.ui.definition.dropItems.PatrolValueDropItem;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.model.filter.date.DayDateGroupBy;
import org.wcs.smart.query.model.filter.date.IDateGroupBy;
import org.wcs.smart.query.model.filter.date.MonthDateGroupBy;
import org.wcs.smart.query.model.filter.date.YearDateGroupBy;
import org.wcs.smart.query.ui.model.IValueDropItem;
import org.wcs.smart.query.ui.model.ListItem;
import org.wcs.smart.util.SmartUtils;

/**
 * Class that defines Patrol based options for
 * queries.  This includes both values for summary queries
 * and filters for summary and observation queries.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class PatrolQueryOptions {

	/**
	 * Possible date filters for queries
	 * 
	 * @author Emily
	 * @since 1.0.0
	 */
	public static enum DATE_FILTER_OP{
		LAST_30_DAYS(Messages.PatrolQueryOptions_DateOp_Last30, "last30days"), //$NON-NLS-1$
		LAST_60_DAYS(Messages.PatrolQueryOptions_DateOpLast60, "last60days"), //$NON-NLS-1$
		MONTH_TO_DATE(Messages.PatrolQueryOptions_DateOpCurrentMonth, "monthtodate"), //$NON-NLS-1$
		LAST_MONTH(Messages.PatrolQueryOptions_DateOpLastMonth, "lastmonth"), //$NON-NLS-1$
		CURRENT_QUARTER(Messages.PatrolQueryOptions_DateOpCurrentQuarter, "currentquarter"), //$NON-NLS-1$
		LAST_QUARTER(Messages.PatrolQueryOptions_DateOpLastQuarter, "lastquarter"), //$NON-NLS-1$
		YEAR_TO_DATE(Messages.PatrolQueryOptions_DateOpYearToDate, "yeartodate"), //$NON-NLS-1$
		LAST_YEAR(Messages.PatrolQueryOptions_DateOpLastYear, "lastyear"), //$NON-NLS-1$
		ALL(Messages.PatrolQueryOptions_DateOpAll, "alldates"), //$NON-NLS-1$
		CUSTOM(Messages.PatrolQueryOptions_DateOpCustom, "custom"); //$NON-NLS-1$
		
		public String guiName;
		public String key;
		
		private DATE_FILTER_OP(String name, String key){
			this.guiName = name;
			this.key = key;
		}
		/**
		 * 
		 * For applicable date filters it returns an array
		 * of start date and end date.  If the end date is today then 
		 * array of a single date (start date).  If "Custom" null
		 * is returned as cannot determine dates. 
		 * 
		 * @return
		 */
		public Date[] getDates(){
			Calendar cal = Calendar.getInstance();
			java.sql.Date currentDate = new java.sql.Date(cal.getTimeInMillis());

			if (this == LAST_30_DAYS) {
				cal.setTimeInMillis((long) (cal.getTime().getTime() - (30 * 24 * 60 * 60 * 1000.0)));
				java.sql.Date d = new java.sql.Date(cal.getTimeInMillis());
				return new java.sql.Date[] { d, currentDate };
			} else if (this == LAST_60_DAYS) {
				cal.setTimeInMillis((long) (cal.getTime().getTime() - (60 * 24 * 60 * 60 * 1000.0)));
				java.sql.Date d = new java.sql.Date(cal.getTimeInMillis());
				return new java.sql.Date[] { d, currentDate };

			} else if (this == MONTH_TO_DATE) {
				cal.set(Calendar.DAY_OF_MONTH, 1);
				java.sql.Date d = new java.sql.Date(cal.getTimeInMillis());
				return new java.sql.Date[] { d, currentDate };

			} else if (this == YEAR_TO_DATE) {
				cal.set(Calendar.MONTH, 0);
				cal.set(Calendar.DAY_OF_MONTH, 1);
				java.sql.Date d = new java.sql.Date(cal.getTimeInMillis());
				return new java.sql.Date[] { d, currentDate };
			} else if (this == LAST_MONTH){
				int year = cal.get(Calendar.YEAR);
				int month = cal.get(Calendar.MONTH);
				
				int lastyear = year;
				int lastmonth = month;
				lastmonth --;
				if (lastmonth < 0){
					lastmonth = 11;
					lastyear --;
				}
				
				cal.set(Calendar.DAY_OF_MONTH, 1);
				java.sql.Date d1 = new java.sql.Date(cal.getTimeInMillis());
				
				cal.set(Calendar.MONTH, lastmonth);
				cal.set(Calendar.YEAR, lastyear);
				java.sql.Date d2 = new java.sql.Date(cal.getTimeInMillis());
				return new java.sql.Date[]{d2, d1};
			} else if (this == CURRENT_QUARTER){
				int month = ((cal.get(Calendar.MONTH)) / 3) * 3;
				cal.set(Calendar.MONTH, month);
				cal.set(Calendar.DAY_OF_MONTH, 1);
				java.sql.Date d1 = new java.sql.Date(cal.getTimeInMillis());
				
				cal.set(Calendar.MONTH, month + 3);
				java.sql.Date d2 = new java.sql.Date(cal.getTimeInMillis());
				return new java.sql.Date[]{d1, d2};
			} else if (this == LAST_QUARTER){
				int thisQuarter = ((cal.get(Calendar.MONTH)) / 3);
				cal.set(Calendar.MONTH, thisQuarter*3);
				cal.set(Calendar.DAY_OF_MONTH, 1);
				java.sql.Date d2 = new java.sql.Date(cal.getTimeInMillis());
				
				int lastQuarter = thisQuarter;
				int yearOffset = 0;
				lastQuarter = lastQuarter -1;
				if (lastQuarter < 0){
					lastQuarter = 3;
					yearOffset = -1;
				}
				int year = cal.get(Calendar.YEAR);
				cal.set(Calendar.MONTH, lastQuarter * 3);
				cal.set(Calendar.DAY_OF_MONTH,1);
				cal.set(Calendar.YEAR, year + yearOffset );
				
				java.sql.Date d1 = new java.sql.Date(cal.getTimeInMillis());
				
				return new java.sql.Date[]{d1, d2};
			}else if (this == LAST_YEAR){
				int year = cal.get(Calendar.YEAR);
				cal.set(Calendar.YEAR, year-1);
				cal.set(Calendar.MONTH, cal.getActualMinimum(Calendar.MONTH));
				cal.set(Calendar.DAY_OF_MONTH, cal.getActualMinimum(Calendar.DAY_OF_MONTH));
				java.sql.Date d1 = new java.sql.Date(cal.getTimeInMillis());
				
				cal.set(Calendar.YEAR, year);
				cal.set(Calendar.MONTH, cal.getActualMinimum(Calendar.MONTH));
				cal.set(Calendar.DAY_OF_MONTH, cal.getActualMinimum(Calendar.DAY_OF_MONTH));
				java.sql.Date d2 = new java.sql.Date(cal.getTimeInMillis());
				return new java.sql.Date[]{d1, d2};
			}
			return null;
		}
	}
	
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
	 * Patrol group by options for summary queries that can
	 * be shared across conservation areas
	 */
	public final static PatrolQueryOption[] SHARED_PATROL_GROUBY_OPTIONS = {
			PatrolQueryOption.TEAM_KEY,
			PatrolQueryOption.MANDATE_KEY,
			PatrolQueryOption.PATROL_TYPE,
			PatrolQueryOption.PATROL_TRANSPORT_TYPE_KEY,
			PatrolQueryOption.CONSERVATION_AREA
	};
	
	/**
	 * Patrol filter options for summary and observation queries
	 */
	public final static IPatrolQueryOption[] PATROL_FILTER_OPTIONS = {
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
	 * Patrol filter options for summary and observation queries 
	 * that can be shared across conservation areas 
	 */
	public final static PatrolQueryOption[] SHARED_PATROL_FILTER_OPTIONS = {
			PatrolQueryOption.ARMED, 
			PatrolQueryOption.TEAM_KEY,
			PatrolQueryOption.MANDATE_KEY,
			PatrolQueryOption.PATROL_TYPE,
			PatrolQueryOption.PATROL_TRANSPORT_TYPE_KEY
	};
	
	/**
	 * Options for computing encounter rates
	 */
	public final static PatrolValueOption[] SUMMARY_ENCOUNTER_RATE_OPTIONS = {
		PatrolValueOption.DISTANCE,
		PatrolValueOption.DISTANCE_TOTAL,
		PatrolValueOption.NUM_HOURS,
		PatrolValueOption.NUM_HOURS_TOTAL,
		PatrolValueOption.NUM_DAYS,
		PatrolValueOption.NUM_DAYS_TOTAL,
		PatrolValueOption.MAN_DAYS,
		PatrolValueOption.MAN_DAYS_TOTAL,
		PatrolValueOption.MAN_HOURS,
		PatrolValueOption.MAN_HOURS_TOTAL,
		PatrolValueOption.NUM_PATROLS,
		PatrolValueOption.NUM_PATROLS_TOTAL
	};

	public final static IValueDropItem[] SUMMARY_ENCOUNTER_RATE_DROP_OPTIONS;
	static{
		IValueDropItem[] ditems = new IValueDropItem[PatrolQueryOptions.SUMMARY_ENCOUNTER_RATE_OPTIONS.length];
		int i = 0;
		for (PatrolValueOption op : PatrolQueryOptions.SUMMARY_ENCOUNTER_RATE_OPTIONS){
			PatrolValueDropItem item = (PatrolValueDropItem) PatrolDropItemFactory.INSTANCE.createPatrolValueDropItem(op);
			ditems[i++] = item; 
		}	
		SUMMARY_ENCOUNTER_RATE_DROP_OPTIONS = ditems;
	}
	
	/**
	 * Options for computing encounter rates
	 */
	public final static PatrolValueOption[] GRID_ENCOUNTER_RATE_OPTIONS = {
		PatrolValueOption.DISTANCE,
		PatrolValueOption.NUM_PATROLS,
		PatrolValueOption.NUM_DAYS
	};

	public final static IValueDropItem[] GRID_ENCOUNTER_RATE_DROP_OPTIONS;
	static{
		IValueDropItem[] ditems = new IValueDropItem[PatrolQueryOptions.GRID_ENCOUNTER_RATE_OPTIONS.length];
		int i = 0;
		for (PatrolValueOption op : PatrolQueryOptions.GRID_ENCOUNTER_RATE_OPTIONS){
			ditems[i++] = (IValueDropItem) PatrolDropItemFactory.INSTANCE.createPatrolValueDropItem(op);
		}	
		GRID_ENCOUNTER_RATE_DROP_OPTIONS = ditems;
	}
	
	/**
	 * Value options for patrol queries
	 */
	public final static PatrolValueOption[] PATROL_VALUE_OPTIONS = {
		PatrolValueOption.NUM_PATROLS,
		PatrolValueOption.NUM_DAYS,
		PatrolValueOption.NUM_NIGHTS,
		PatrolValueOption.DISTANCE,
		PatrolValueOption.NUM_HOURS,
		PatrolValueOption.NUM_MEMBERS,
		PatrolValueOption.MAN_HOURS,
		PatrolValueOption.MAN_DAYS
	};

	
	/**
	 * Represents the possible patrol values for summary
	 * queries.
	 * 
	 * @author egouge
	 * @since 1.0.0
	 */
	public enum PatrolValueOption{
		NUM_PATROLS(Messages.PatrolQueryOptions_ValueOpNumPatrols, "numpatrols", Patrol.class), //$NON-NLS-1$
		NUM_DAYS(Messages.PatrolQueryOptions_ValueOpNumberDays, "numdays", Patrol.class), //$NON-NLS-1$
		NUM_NIGHTS(Messages.PatrolQueryOptions_ValueOpNumberNights, "numnights", Patrol.class), //$NON-NLS-1$
		DISTANCE(Messages.PatrolQueryOptions_ValueOpDistance, "distance", Track.class), //$NON-NLS-1$
		NUM_HOURS(Messages.PatrolQueryOptions_ValueOpNumberHours, "numhours", PatrolLegDay.class), //$NON-NLS-1$
		NUM_MEMBERS(Messages.PatrolQueryOptions_ValueOpNumEmployees, "nummembers", PatrolLegMember.class), //$NON-NLS-1$
		MAN_HOURS(Messages.PatrolQueryOptions_ValueOpPersonHrs, "manhours", PatrolLegDay.class), //$NON-NLS-1$
		MAN_DAYS(Messages.PatrolQueryOptions_ValueOpPersonDays, "mandays", Patrol.class), //$NON-NLS-1$
		
		NUM_PATROLS_TOTAL(Messages.PatrolQueryOptions_TotalNumPatrols, "totalnumpatrols", Patrol.class),  //$NON-NLS-1$
		NUM_DAYS_TOTAL(Messages.PatrolQueryOptions_TotalNumDays, "totalnumdays", Patrol.class),  //$NON-NLS-1$
		DISTANCE_TOTAL(Messages.PatrolQueryOptions_TotalDistance, "totaldistance", Track.class),  //$NON-NLS-1$
		NUM_HOURS_TOTAL(Messages.PatrolQueryOptions_TotalNumHours, "totalnumhours", PatrolLegDay.class),  //$NON-NLS-1$
		MAN_HOURS_TOTAL(Messages.PatrolQueryOptions_TotalPersonHours, "totalmanhours", PatrolLegDay.class),  //$NON-NLS-1$
		MAN_DAYS_TOTAL(Messages.PatrolQueryOptions_TotalPersonDays, "totalmandays", Patrol.class);  //$NON-NLS-1$

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
		
		public Image getIcon(){
			switch(this){
				case NUM_PATROLS:
				case NUM_PATROLS_TOTAL:
					return PatrolQueryPlugIn.getDefault().getImageRegistry().get(PatrolQueryPlugIn.VALUE_NUM_PATROLS_ICON);
				case NUM_DAYS:
				case NUM_DAYS_TOTAL:
					return QueryPlugIn.getDefault().getImageRegistry().get(QueryPlugIn.VALUE_NUM_DAYS_ICON);
				case NUM_NIGHTS:
					return QueryPlugIn.getDefault().getImageRegistry().get(QueryPlugIn.VALUE_NUM_NIGHTS_ICON);
				case DISTANCE:
				case DISTANCE_TOTAL:
					return QueryPlugIn.getDefault().getImageRegistry().get(QueryPlugIn.VALUE_DISTANCE_ICON);
				case NUM_HOURS:
				case NUM_HOURS_TOTAL:
					return QueryPlugIn.getDefault().getImageRegistry().get(QueryPlugIn.VALUE_NUM_HOURS_ICON);
				case NUM_MEMBERS:
					return PatrolQueryPlugIn.getDefault().getImageRegistry().get(PatrolQueryPlugIn.VALUE_NUM_EMPLOYEES_ICON);
				case MAN_HOURS:
				case MAN_HOURS_TOTAL:
					return PatrolQueryPlugIn.getDefault().getImageRegistry().get(PatrolQueryPlugIn.VALUE_PERSON_HOURS_ICON);
				case MAN_DAYS:
				case MAN_DAYS_TOTAL:
					return PatrolQueryPlugIn.getDefault().getImageRegistry().get(PatrolQueryPlugIn.VALUE_PERSON_DAYS_ICON);
			}
			return null;
		}
	}
	
	
	public static final boolean isGroupByFilterValueItem(PatrolValueOption item){
		switch(item){
			case NUM_PATROLS_TOTAL: 
			case NUM_DAYS_TOTAL: 
			case DISTANCE_TOTAL: 
			case NUM_HOURS_TOTAL:
			case MAN_HOURS_TOTAL:
			case MAN_DAYS_TOTAL:
					return false;
		
		}
		return true;
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
	 * Possible patrol filter option typs.
	 * @author egouge
	 * @since 1.0.0
	 */
	public enum PatrolQueryOptionType{
		BOOLEAN, UUID, STRING,KEY
	}
	
	/**
	 * Valid patrol filter options.
	 */
	public enum PatrolQueryOption implements IPatrolQueryOption {
		
		ID(Messages.PatrolQueryOptions_QueryOpId, "id", "id", Patrol.class, Patrol.class, PatrolQueryOptionType.STRING), //$NON-NLS-2$ //$NON-NLS-1$
		
		ARMED(Messages.PatrolQueryOptions_QueryOpArmed, "armed", "is_armed", Patrol.class, null, PatrolQueryOptionType.BOOLEAN), //$NON-NLS-2$ //$NON-NLS-1$
		
		STATION(Messages.PatrolQueryOptions_QueryOpStation, "station", "station_uuid", Patrol.class, Station.class, PatrolQueryOptionType.UUID), //$NON-NLS-2$ //$NON-NLS-1$
		
		TEAM(Messages.PatrolQueryOptions_QueryOpTeam, "team", "team_uuid", Patrol.class, Team.class, PatrolQueryOptionType.UUID), //$NON-NLS-2$ //$NON-NLS-1$
		TEAM_KEY(Messages.PatrolQueryOptions_QueryOpTeam, "teamkey", "team_uuid", Patrol.class, Team.class, PatrolQueryOptionType.KEY), //$NON-NLS-2$ //$NON-NLS-1$
		
		EMPLOYEE(Messages.PatrolQueryOptions_QueryOpMember, "member", "employee_uuid", PatrolLegMember.class, Employee.class, PatrolQueryOptionType.UUID), //$NON-NLS-2$ //$NON-NLS-1$
		
		LEADER(Messages.PatrolQueryOptions_QueryOpLeader, "leader", "employee_uuid", PatrolLegMember.class, Employee.class, PatrolQueryOptionType.UUID), //$NON-NLS-2$ //$NON-NLS-1$
		
		PILOT(Messages.PatrolQueryOptions_QueryOpPilot, "pilot", "employee_uuid", PatrolLegMember.class, Employee.class, PatrolQueryOptionType.UUID), //$NON-NLS-2$ //$NON-NLS-1$
		
		MANDATE(Messages.PatrolQueryOptions_QueryOpMandate, "mandate", "mandate_uuid", Patrol.class, PatrolMandate.class, PatrolQueryOptionType.UUID), //$NON-NLS-2$ //$NON-NLS-1$
		MANDATE_KEY(Messages.PatrolQueryOptions_QueryOpMandate, "mandatekey", "mandate_uuid", Patrol.class, PatrolMandate.class, PatrolQueryOptionType.KEY), //$NON-NLS-2$ //$NON-NLS-1$
		
		PATROL_TYPE(Messages.PatrolQueryOptions_QueryOpType, "patroltype", "patrol_type", Patrol.class, null, PatrolQueryOptionType.STRING), //$NON-NLS-2$ //$NON-NLS-1$
		
		PATROL_TRANSPORT_TYPE(Messages.PatrolQueryOptions_QueryOpTransportType, "transport", "transport_uuid", PatrolLeg.class, PatrolTransportType.class, PatrolQueryOptionType.UUID), //$NON-NLS-2$ //$NON-NLS-1$
		PATROL_TRANSPORT_TYPE_KEY(Messages.PatrolQueryOptions_QueryOpTransportType, "transportkey", "transport_uuid", PatrolLeg.class, PatrolTransportType.class, PatrolQueryOptionType.KEY), //$NON-NLS-2$ //$NON-NLS-1$
		
		CONSERVATION_AREA(Messages.PatrolQueryOptions_CaGroupByOptionName, "ca", "ca_uuid", Patrol.class, ConservationArea.class, PatrolQueryOptionType.UUID);  //$NON-NLS-1$//$NON-NLS-2$
		
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
		 * @return the image that represents a particular patrol filter option
		 */
		public Image getImage() {
			if (this == PatrolQueryOption.ARMED){
				return SmartPatrolPlugIn.getDefault().getImageRegistry().get(SmartPatrolPlugIn.PATROL_ARMED_ICON);
			}else if (this == PatrolQueryOption.TEAM || this == PatrolQueryOption.TEAM_KEY){
				return SmartPatrolPlugIn.getDefault().getImageRegistry().get(SmartPatrolPlugIn.PATROL_TEAM_ICON);
			}else if (this == PatrolQueryOption.STATION){
				return SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.STATION_ICON);
			}else if (this == PatrolQueryOption.MANDATE || this == MANDATE_KEY){
				return SmartPatrolPlugIn.getDefault().getImageRegistry().get(SmartPatrolPlugIn.PATROL_MANDATE_ICON);
			}else if (this == PatrolQueryOption.LEADER){
				return SmartPatrolPlugIn.getDefault().getImageRegistry().get(SmartPatrolPlugIn.PATROL_LEADER_ICON);
			}else if (this == PatrolQueryOption.PILOT){
				return SmartPatrolPlugIn.getDefault().getImageRegistry().get(SmartPatrolPlugIn.PATROL_PILOT_ICON);
			}else if (this == PatrolQueryOption.EMPLOYEE){
				return SmartPatrolPlugIn.getDefault().getImageRegistry().get(SmartPatrolPlugIn.PATROL_MEMBER_ICON);
			}else if (this == PatrolQueryOption.PATROL_TYPE){
				return SmartPatrolPlugIn.getDefault().getImageRegistry().get(SmartPatrolPlugIn.PATROL_ICON);
			}else if (this == PatrolQueryOption.PATROL_TRANSPORT_TYPE || this == PATROL_TRANSPORT_TYPE_KEY){
				return SmartPatrolPlugIn.getDefault().getImageRegistry().get(SmartPatrolPlugIn.GROUND_PATROL_ICON);
			}else {
				return SmartPatrolPlugIn.getDefault().getImageRegistry().get(SmartPatrolPlugIn.PATROL_ICON);
			}
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
				if (x instanceof NamedItem){
					return ((NamedItem) x).getName();
				}else if (x instanceof Employee){
					return ((Employee) x).getFullLabel();
				}
			}
			return null;
		}
		
		/**
		 * Return an array of names that represent
		 * the uuid for a given option.  Name provided in 
		 * the conservation area default language.
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
				if (x instanceof NamedItem){
					return new String[]{((NamedItem)x).getDefaultName()};
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
			List<?> data = session.createCriteria(sourceClazz).add(Restrictions.eq("uuid", uuid)).list(); //$NON-NLS-1$
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
							Messages.PatrolQueryOptions_ErrorInvalidPatrolFilterValue
									+ ex.getLocalizedMessage(), ex);
					return results;
				}
				Collection<?> data = session.createCriteria(sourceClazz).add(Restrictions.in("uuid", uuidkeys)).list(); //$NON-NLS-1$
				
				for (Iterator<?> iterator = data.iterator(); iterator.hasNext();) {
					Object object = (Object) iterator.next();
					if (object instanceof NamedItem){
						results.add(new ListItem(((NamedItem) object).getUuid(), ((NamedItem) object).getName()));
					}else if (object instanceof Employee){
						Employee e = (Employee)object;
						if (e.getConservationArea().equals(SmartDB.getCurrentConservationArea())){
							results.add(new ListItem(e.getUuid(), e.getFullLabel()));
						}
					}else if (object instanceof ConservationArea){
						ConservationArea ca = (ConservationArea)object;
						results.add(new ListItem(ca.getUuid(), ca.getNameLabel()));
					}else if (object instanceof ListItem){
						results.add((ListItem)object);
					}
				}
			}else if (type == PatrolQueryOptionType.KEY){
				Collection<?> data = null;
				if (SmartDB.isMultipleAnalysis()){
					if (this == TEAM_KEY){
						data = ((MultiCaPatrolQueryHibernateManagerImpl)PatrolQueryHibernateManager.getInstance()).getTeams(session);
					}else if (this == PATROL_TRANSPORT_TYPE_KEY){
						data = ((MultiCaPatrolQueryHibernateManagerImpl)PatrolQueryHibernateManager.getInstance()).getTransportTypes(session);
					}else if (this == MANDATE_KEY){
						data = ((MultiCaPatrolQueryHibernateManagerImpl)PatrolQueryHibernateManager.getInstance()).getMandates(session);
					}
				}
				if (data != null){
					for (Iterator<?> iterator = data.iterator(); iterator.hasNext();) {
						ListItem it = (ListItem) iterator.next();
						if (Arrays.asList(keys).contains(it.getKey())){
							results.add(it);
						}
					}
				}
			}else if (type == PatrolQueryOptionType.STRING){
				if (this == ID){
					List<?> data = session.createCriteria(sourceClazz).add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())).add(Restrictions.in(columnName, keys)).list(); //$NON-NLS-1$
					for (Iterator<?> iterator = data.iterator(); iterator.hasNext();) {
						Object object = (Object) iterator.next();
						if (object instanceof Patrol){
							//results.add(new ListItem(((Patrol) object).getUuid(), ((Patrol) object).getId()));
							results.add(new ListItem( null, ((Patrol) object).getId(),((Patrol) object).getId() ));
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
				List<String> pids = PatrolQueryHibernateManager.getInstance().getPatrolIds(session);
				for (String pid : pids){
					items.add(new ListItem(null, pid, pid));
				}
			}else if (this == CONSERVATION_AREA){
				for (ConservationArea ca : SmartDB.getConservationAreaConfiguration().getConservationAreas()){
					items.add(new ListItem(ca.getUuid(), ca.getNameLabel()));
				}
			}else if (this == STATION){
				List<Station> stations = PatrolHibernateManager.getActiveStations(SmartDB.getCurrentConservationArea(), session);
				for (Station s : stations){
					items.add(new ListItem(s.getUuid(), s.getName()));
				}
			}else if (this == TEAM || this == TEAM_KEY){
				items.addAll(PatrolQueryHibernateManager.getInstance().getActiveTeams(session));
			}else if (this == MANDATE || this == MANDATE_KEY){
				items.addAll(PatrolQueryHibernateManager.getInstance().getActiveMandates(session));
				
			}else if (this == PATROL_TYPE){
				if (SmartDB.isMultipleAnalysis()){
					for (PatrolType.Type t : PatrolType.Type.values()){
						items.add(new ListItem(null, t.getGuiName(), t.name()));
					}
				}else{
					List<PatrolType> types= PatrolHibernateManager.getActivePatrolTypes(SmartDB.getCurrentConservationArea(), session);
					for (PatrolType t : types){
						items.add(new ListItem(null, t.getType().getGuiName(), t.getType().name() ));
					}
				}
			}else if (this == PATROL_TRANSPORT_TYPE || this == PATROL_TRANSPORT_TYPE_KEY){
				items.addAll(PatrolQueryHibernateManager.getInstance().getActiveTransportTypes(session));
			}else if (this == LEADER ||
					this == PILOT || 
					this == EMPLOYEE){
				List<Employee> employees = PatrolHibernateManager.getActiveEmployees(SmartDB.getCurrentConservationArea(), session);
				for (Employee t : employees){
					items.add(new ListItem(t.getUuid(), t.getFullLabel() ));
				}
			}
			Collections.sort(items);
			return items;
		}
		
		@Override
		public ListItem getDefaultListItem() {
			return null;
		}
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
	
	public static IDateGroupBy[] DATE_GROUBY_OPS = new IDateGroupBy[]{
			DayDateGroupBy.INSTANCE,
			MonthDateGroupBy.INSTANCE,
			YearDateGroupBy.INSTANCE
	};
	
}

