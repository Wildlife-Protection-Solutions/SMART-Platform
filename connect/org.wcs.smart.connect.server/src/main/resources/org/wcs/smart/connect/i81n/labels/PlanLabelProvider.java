/*
 * Copyright (C) 2015 Wildlife Conservation Society
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
package org.wcs.smart.connect.i81n.labels;

import java.util.Locale;

import org.wcs.smart.plan.IPlanLabelProvider;
import org.wcs.smart.plan.model.AdministrativePlanTarget;
import org.wcs.smart.plan.model.NumericPlanTarget;
import org.wcs.smart.plan.model.Plan;
import org.wcs.smart.plan.model.PlanTargetStatus;
import org.wcs.smart.plan.model.SpatialPlanTarget;
import org.wcs.smart.plan.query.PlanPatrolQueryOption;

/**
 * Implementation of plan label provider.
 * 
 * @author Emily
 *
 */
public class PlanLabelProvider implements IPlanLabelProvider {

	public static final String ADMIN_TARGET_GUI_NAME = "Administrative";
	
	public static final String NUMERIC_TARGET_GUI_NAME = "Numeric";

	@Override
	public String getLabel(Object item, Locale l) {
		if (item == NumericPlanTarget.TargetType.DISTANCE) return "Distance Travelled";
		if (item == NumericPlanTarget.TargetType.PATROL_HOURS) return "Patrol Hours";
		if (item == NumericPlanTarget.TargetType.PATROL_DAYS) return "Patrol Days";
		if (item == NumericPlanTarget.TargetType.PATROL_MANHOURS) return "Patrol Man-Hours";
		
		if (item == NumericPlanTarget.Unit.KM) return "km";
		if (item == NumericPlanTarget.Unit.HOURS) return "hours";
		if (item == NumericPlanTarget.Unit.DAYS) return "days";
		
		if (item == Plan.PlanType.CA) return "Conservation Area Plan";
		if (item == Plan.PlanType.STATION) return "Station Plan";
		if (item == Plan.PlanType.TEAM) return "Team Plan";
		if (item == Plan.PlanType.PATROL) return "Patrol Plan";
				
		if (item == PlanTargetStatus.Status.COMPLETE) return "Complete";
		if (item == PlanTargetStatus.Status.INCOMPLETE) return  "Incomplete";
		if (item == PlanTargetStatus.Status.UNKNOWN) return "Unknown"; 
		
		if (item == AdministrativePlanTarget.SUMMARY_KEY){
			return "[Admin] {0}";
		}
		if (item == NumericPlanTarget.SUMMARY_KEY){
			return "[Numeric] {0}";
		}
		if (item == SpatialPlanTarget.SUMMARY_KEY){
			return "[Spatial] {0} ({1} point(s))";
		}
		if (item instanceof PlanPatrolQueryOption){
			return  "Part of Plan";
		}
		return null;
	}
}
