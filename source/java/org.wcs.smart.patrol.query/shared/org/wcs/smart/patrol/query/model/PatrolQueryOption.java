package org.wcs.smart.patrol.query.model;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ICoreLabelProvider;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.ca.Station;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegMember;
import org.wcs.smart.patrol.model.PatrolMandate;
import org.wcs.smart.patrol.model.PatrolTransportType;
import org.wcs.smart.patrol.model.Team;
import org.wcs.smart.patrol.query.parser.IPatrolQueryOption;
import org.wcs.smart.patrol.ui.IQueryPatrolLabelProvider;

/**
 * Valid patrol filter options.
 */
public enum PatrolQueryOption implements IPatrolQueryOption {
	
	ID("id", "id", Patrol.class, Patrol.class, PatrolQueryOptionType.STRING), //$NON-NLS-2$ //$NON-NLS-1$
	
	ARMED("armed", "is_armed", Patrol.class, null, PatrolQueryOptionType.BOOLEAN), //$NON-NLS-2$ //$NON-NLS-1$
	
	STATION("station", "station_uuid", Patrol.class, Station.class, PatrolQueryOptionType.UUID), //$NON-NLS-2$ //$NON-NLS-1$
	
	TEAM("team", "team_uuid", Patrol.class, Team.class, PatrolQueryOptionType.UUID), //$NON-NLS-2$ //$NON-NLS-1$
	TEAM_KEY("teamkey", "team_uuid", Patrol.class, Team.class, PatrolQueryOptionType.KEY), //$NON-NLS-2$ //$NON-NLS-1$
	
	EMPLOYEE("member", "employee_uuid", PatrolLegMember.class, Employee.class, PatrolQueryOptionType.UUID), //$NON-NLS-2$ //$NON-NLS-1$
	
	LEADER("leader", "employee_uuid", PatrolLegMember.class, Employee.class, PatrolQueryOptionType.UUID), //$NON-NLS-2$ //$NON-NLS-1$
	
	PILOT( "pilot", "employee_uuid", PatrolLegMember.class, Employee.class, PatrolQueryOptionType.UUID), //$NON-NLS-2$ //$NON-NLS-1$
	
	MANDATE("mandate", "mandate_uuid", Patrol.class, PatrolMandate.class, PatrolQueryOptionType.UUID), //$NON-NLS-2$ //$NON-NLS-1$
	MANDATE_KEY("mandatekey", "mandate_uuid", Patrol.class, PatrolMandate.class, PatrolQueryOptionType.KEY), //$NON-NLS-2$ //$NON-NLS-1$
	
	PATROL_TYPE("patroltype", "patrol_type", Patrol.class, null, PatrolQueryOptionType.STRING), //$NON-NLS-2$ //$NON-NLS-1$
	
	PATROL_TRANSPORT_TYPE("transport", "transport_uuid", PatrolLeg.class, PatrolTransportType.class, PatrolQueryOptionType.UUID), //$NON-NLS-2$ //$NON-NLS-1$
	PATROL_TRANSPORT_TYPE_KEY("transportkey", "transport_uuid", PatrolLeg.class, PatrolTransportType.class, PatrolQueryOptionType.KEY), //$NON-NLS-2$ //$NON-NLS-1$
	
	CONSERVATION_AREA("ca", "ca_uuid", Patrol.class, ConservationArea.class, PatrolQueryOptionType.UUID);  //$NON-NLS-1$//$NON-NLS-2$
	
	private String key;			//unique identifier key
	private String columnName;	//column name in database table
	private Class<?> clazz;		//class containing the attribute
	private Class<?> sourceClazz; //class that represents the object
	private PatrolQueryOptionType type; //data type; if boolean, uuid or string field
	
	PatrolQueryOption(String queryKey, 
			String columnName, Class<?> clazz, 
			Class<?> sourceClazz, PatrolQueryOptionType type){

		this.key = queryKey;
		this.columnName = columnName;
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
	public String getGuiName(Locale l){
		return SmartContext.INSTANCE.getClass(IQueryPatrolLabelProvider.class).getLabel(this, l);
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
	public String getName(Session session, UUID uuid, Locale l){
		Object x = getObject(session, uuid);
		if (x != null){
			if (x instanceof NamedItem){
				return ((NamedItem) x).getName();
			}else if (x instanceof Employee){
				return SmartContext.INSTANCE.getClass(ICoreLabelProvider.class).getLabel(x, l);
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
	public String[] getNames(Session session, UUID uuid, Locale l){
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
	public Object getObject(Session session, UUID uuid){
		List<?> data = session.createCriteria(sourceClazz).add(Restrictions.eq("uuid", uuid)).list(); //$NON-NLS-1$
		if (data.size() == 0){
			return null; //nothing found
		}else if (data.size() > 1){
			assert false; //should never happen
		}
		return data.get(0);
	}
	
	
}