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

	NUM_PATROLS("numpatrols", Patrol.class, false), //$NON-NLS-1$
	NUM_DAYS("numdays", Patrol.class, true), //$NON-NLS-1$
	NUM_NIGHTS("numnights", Patrol.class, true), //$NON-NLS-1$
	DISTANCE("distance", Track.class, false), //$NON-NLS-1$
	NUM_FIELDHOURS("numhours", PatrolLegDay.class, true), //$NON-NLS-1$
	NUM_PATROLHOURS("numpatrolhours", PatrolLegDay.class, true), //$NON-NLS-1$
	NUM_MEMBERS("nummembers", PatrolLegMember.class, false), //$NON-NLS-1$
	MAN_HOURS("manhours", PatrolLegDay.class, true), //$NON-NLS-1$
	MAN_DAYS("mandays", Patrol.class, true), //$NON-NLS-1$
	
	NUM_PATROLS_TOTAL("totalnumpatrols", Patrol.class, false),  //$NON-NLS-1$
	NUM_DAYS_TOTAL("totalnumdays", Patrol.class, true),  //$NON-NLS-1$
	DISTANCE_TOTAL("totaldistance", Track.class, false),  //$NON-NLS-1$
	NUM_FIELDHOURS_TOTAL("totalnumhours", PatrolLegDay.class, true),  //$NON-NLS-1$
	NUM_PATROLHOURS_TOTAL("totalpatrolhours", PatrolLegDay.class, true),  //$NON-NLS-1$
	MAN_HOURS_TOTAL("totalmanhours", PatrolLegDay.class, true),  //$NON-NLS-1$
	MAN_DAYS_TOTAL("totalmandays", Patrol.class, true);  //$NON-NLS-1$

	String key;		//unique key
	Class<?> clazz; //class that contains the value variable
	
	boolean hasNoDataOption;
	
	PatrolValueOption(String queryKey, Class<?> clazz, boolean hasNoDataOption){
		this.key = queryKey;
		this.clazz = clazz;
		this.hasNoDataOption = hasNoDataOption;
	}
	
	public boolean hasNoDataOption() {
		return this.hasNoDataOption;
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

