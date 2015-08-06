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
package org.wcs.smart.patrol.query.model;


import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.wcs.smart.patrol.query.parser.IPatrolQueryOption;
import org.wcs.smart.query.model.filter.date.DayDateGroupBy;
import org.wcs.smart.query.model.filter.date.IDateGroupBy;
import org.wcs.smart.query.model.filter.date.MonthDateGroupBy;
import org.wcs.smart.query.model.filter.date.YearDateGroupBy;

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


	
	/**
	 * Options for computing encounter rates
	 */
	public final static PatrolValueOption[] GRID_ENCOUNTER_RATE_OPTIONS = {
		PatrolValueOption.DISTANCE,
		PatrolValueOption.NUM_PATROLS,
		PatrolValueOption.NUM_DAYS
	};

	
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

	public static IDateGroupBy[] DATE_GROUBY_OPS = new IDateGroupBy[]{
		DayDateGroupBy.INSTANCE,
		MonthDateGroupBy.INSTANCE,
		YearDateGroupBy.INSTANCE
	};

	
	public static final boolean isGroupByFilterValueItem(PatrolValueOption option){
		if ((option == PatrolValueOption.NUM_PATROLS_TOTAL)
			|| 	(option == PatrolValueOption.NUM_DAYS_TOTAL)
			|| 	(option == PatrolValueOption.DISTANCE_TOTAL)
			|| 	(option == PatrolValueOption.NUM_HOURS_TOTAL)
			|| 	(option == PatrolValueOption.MAN_HOURS_TOTAL)
			|| 	(option == PatrolValueOption.MAN_DAYS_TOTAL)){
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

