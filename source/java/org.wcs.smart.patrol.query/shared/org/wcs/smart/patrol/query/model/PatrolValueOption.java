package org.wcs.smart.patrol.query.model;

import java.util.Locale;

import org.wcs.smart.SmartContext;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.PatrolLegMember;
import org.wcs.smart.patrol.model.Track;
import org.wcs.smart.patrol.ui.IQueryPatrolLabelProvider;

/**
 * Represents the possible patrol values for summary
 * queries.
 * 
 * @author egouge
 * @since 1.0.0
 */
public enum PatrolValueOption {

	NUM_PATROLS("numpatrols", Patrol.class), //$NON-NLS-1$
	NUM_DAYS("numdays", Patrol.class), //$NON-NLS-1$
	NUM_NIGHTS("numnights", Patrol.class), //$NON-NLS-1$
	DISTANCE("distance", Track.class), //$NON-NLS-1$
	NUM_FIELDHOURS("numhours", PatrolLegDay.class), //$NON-NLS-1$
	NUM_PATROLHOURS("numpatrolhours", PatrolLegDay.class), //$NON-NLS-1$
	NUM_MEMBERS("nummembers", PatrolLegMember.class), //$NON-NLS-1$
	MAN_HOURS("manhours", PatrolLegDay.class), //$NON-NLS-1$
	MAN_DAYS("mandays", Patrol.class), //$NON-NLS-1$
	
	NUM_PATROLS_TOTAL("totalnumpatrols", Patrol.class),  //$NON-NLS-1$
	NUM_DAYS_TOTAL("totalnumdays", Patrol.class),  //$NON-NLS-1$
	DISTANCE_TOTAL("totaldistance", Track.class),  //$NON-NLS-1$
	NUM_FIELDHOURS_TOTAL("totalnumhours", PatrolLegDay.class),  //$NON-NLS-1$
	NUM_PATROLHOURS_TOTAL("totalpatrolhours", PatrolLegDay.class),  //$NON-NLS-1$
	MAN_HOURS_TOTAL("totalmanhours", PatrolLegDay.class),  //$NON-NLS-1$
	MAN_DAYS_TOTAL("totalmandays", Patrol.class);  //$NON-NLS-1$

	String key;		//unique key
	Class<?> clazz; //class that contains the value variable
	
	PatrolValueOption(String queryKey, Class<?> clazz){
		this.key = queryKey;
		this.clazz = clazz;
	}
	
	public String getGuiName(Locale l){
		return SmartContext.INSTANCE.getClass(IQueryPatrolLabelProvider.class).getLabel(this, l);
	}
			
	public String getKeyPart(){
		return this.key;
	}
	
	public Class<?> getOptionClass(){
		return this.clazz;
	}
}

