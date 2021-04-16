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
package org.wcs.smart.er.query.engine;

import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.MissionAttribute;
import org.wcs.smart.er.model.MissionAttributeListItem;
import org.wcs.smart.er.model.MissionDay;
import org.wcs.smart.er.model.MissionMember;
import org.wcs.smart.er.model.MissionProperty;
import org.wcs.smart.er.model.MissionPropertyValue;
import org.wcs.smart.er.model.MissionTrack;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.model.SamplingUnitAttribute;
import org.wcs.smart.er.model.SamplingUnitAttributeListItem;
import org.wcs.smart.er.model.SamplingUnitAttributeValue;
import org.wcs.smart.er.model.Survey;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.model.SurveyWaypoint;
import org.wcs.smart.er.query.filter.SurveyDesignFilter;
import org.wcs.smart.query.common.engine.AbstractQueryEngine;
import org.wcs.smart.query.common.engine.IFilterProcessor;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.FilterType;

/**
 * Query engine for executing 
 * queries using derby.
 * 
 * @author Emily
 * @since 1.0.0
 */
public abstract class DerbySurveyQueryEngine extends AbstractQueryEngine {
	

	protected SurveyDesignFilter designFilter;
	
	static {
		tablePrefix.put(SurveyDesign.class, "sd"); //$NON-NLS-1$
		tablePrefix.put(Survey.class, "s"); //$NON-NLS-1$
		tablePrefix.put(Mission.class, "m"); //$NON-NLS-1$
		tablePrefix.put(MissionDay.class, "md"); //$NON-NLS-1$
		tablePrefix.put(MissionAttribute.class, "ma"); //$NON-NLS-1$
		tablePrefix.put(MissionAttributeListItem.class, "mali"); //$NON-NLS-1$
		tablePrefix.put(MissionProperty.class, "mp"); //$NON-NLS-1$
		tablePrefix.put(MissionTrack.class, "t"); //$NON-NLS-1$
		tablePrefix.put(MissionMember.class, "mm"); //$NON-NLS-1$
		tablePrefix.put(MissionPropertyValue.class, "mpv"); //$NON-NLS-1$
		tablePrefix.put(SurveyWaypoint.class, "sw"); //$NON-NLS-1$
		tablePrefix.put(SamplingUnit.class, "su"); //$NON-NLS-1$
		tablePrefix.put(SamplingUnitAttribute.class, "sua"); //$NON-NLS-1$
		tablePrefix.put(SamplingUnitAttributeValue.class, "suav"); //$NON-NLS-1$
		tablePrefix.put(SamplingUnitAttributeListItem.class, "suli"); //$NON-NLS-1$
	}

	
	/**
	 * Maps hibernate classes to database table names
	 */
	static {
		tableNames.put(SurveyDesign.class, "smart.survey_design"); //$NON-NLS-1$
		tableNames.put(Survey.class, "smart.survey"); //$NON-NLS-1$
		tableNames.put(Mission.class, "smart.mission"); //$NON-NLS-1$
		tableNames.put(MissionDay.class, "smart.mission_day"); //$NON-NLS-1$
		tableNames.put(MissionAttribute.class, "smart.mission_attribute"); //$NON-NLS-1$
		tableNames.put(MissionAttributeListItem.class, "smart.mission_attribute_list"); //$NON-NLS-1$
		tableNames.put(MissionProperty.class, "smart.mission_property"); //$NON-NLS-1$
		tableNames.put(MissionTrack.class, "smart.mission_track"); //$NON-NLS-1$
		tableNames.put(MissionMember.class, "smart.mission_member"); //$NON-NLS-1$
		tableNames.put(MissionPropertyValue.class, "smart.mission_property_value"); //$NON-NLS-1$
		tableNames.put(SurveyWaypoint.class, "smart.survey_waypoint"); //$NON-NLS-1$
		tableNames.put(SamplingUnit.class, "smart.sampling_unit"); //$NON-NLS-1$
		tableNames.put(SamplingUnitAttribute.class, "smart.sampling_unit_attribute"); //$NON-NLS-1$
		tableNames.put(SamplingUnitAttributeValue.class, "smart.sampling_unit_attribute_value"); //$NON-NLS-1$
		tableNames.put(SamplingUnitAttributeListItem.class, "smart.sampling_unit_attribute_list"); //$NON-NLS-1$
	}

	
	/**
	 * Creates the filter processor based on the query filter type
	 * 
	 * @param filterType
	 * @param queryDataTable
	 * @return
	 */
	@Override
	public IFilterProcessor getFilterProcessor(FilterType filterType, 
			String queryDataTable,
			Query query){
		if (filterType == FilterType.OBSERVATION){
			return new FilterProcessor(queryDataTable, this, designFilter, query);
		}else if (filterType == FilterType.GROUP){
			return new WaypointGroupFilterProcessor(queryDataTable, this, designFilter, query);
		}else{
			return new WaypointFilterProcessor(queryDataTable, this, designFilter, query);
		}
	}
	
}
