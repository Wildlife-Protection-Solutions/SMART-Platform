/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
	NUM_CUSTOM("numcustom", Patrol.class, false), //$NON-NLS-1$
	DISTANCE("distance", Track.class, false), //$NON-NLS-1$
	AREA_BUFFER("areabuffer", Track.class, false), //$NON-NLS-1$
	NUM_FIELDHOURS("numhours", PatrolLegDay.class, true), //$NON-NLS-1$
	NUM_PATROLHOURS("numpatrolhours", PatrolLegDay.class, true), //$NON-NLS-1$
	PATROLHOURS_TRACK("numtrackpatrolhours", PatrolLegDay.class, false), //$NON-NLS-1$
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

