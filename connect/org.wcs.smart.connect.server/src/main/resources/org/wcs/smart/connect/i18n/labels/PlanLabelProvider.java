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
package org.wcs.smart.connect.i18n.labels;

import java.util.Locale;

import org.wcs.smart.connect.i18n.Messages;
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

	@Override
	public String getLabel(Object item, Locale l) {
		if (item == NumericPlanTarget.TargetType.DISTANCE) return Messages.getString("PlanLabelProvider.DistanceTraveledTargetOp", l); //$NON-NLS-1$
		if (item == NumericPlanTarget.TargetType.PATROL_HOURS) return Messages.getString("PlanLabelProvider.PatrolHoursTargetOp", l); //$NON-NLS-1$
		if (item == NumericPlanTarget.TargetType.PATROL_DAYS) return Messages.getString("PlanLabelProvider.PatrolDaysTargetOp", l); //$NON-NLS-1$
		if (item == NumericPlanTarget.TargetType.PATROL_MANHOURS) return Messages.getString("PlanLabelProvider.ManHoursTargetOp", l); //$NON-NLS-1$
		
		if (item == NumericPlanTarget.Unit.KM) return Messages.getString("PlanLabelProvider.KMUnits", l); //$NON-NLS-1$
		if (item == NumericPlanTarget.Unit.HOURS) return Messages.getString("PlanLabelProvider.HoursUnit", l); //$NON-NLS-1$
		if (item == NumericPlanTarget.Unit.DAYS) return Messages.getString("PlanLabelProvider.DaysUnit", l); //$NON-NLS-1$
		
		if (item == Plan.PlanType.CA) return Messages.getString("PlanLabelProvider.CaPlanType", l); //$NON-NLS-1$
		if (item == Plan.PlanType.STATION) return Messages.getString("PlanLabelProvider.StationPlanType", l); //$NON-NLS-1$
		if (item == Plan.PlanType.TEAM) return Messages.getString("PlanLabelProvider.TeamPlanType", l); //$NON-NLS-1$
		if (item == Plan.PlanType.PATROL) return Messages.getString("PlanLabelProvider.PatorlPlanType", l); //$NON-NLS-1$
				
		if (item == PlanTargetStatus.Status.COMPLETE) return Messages.getString("PlanLabelProvider.CompleteStatus", l); //$NON-NLS-1$
		if (item == PlanTargetStatus.Status.INCOMPLETE) return  Messages.getString("PlanLabelProvider.IncompleteStatus", l); //$NON-NLS-1$
		if (item == PlanTargetStatus.Status.UNKNOWN) return Messages.getString("PlanLabelProvider.UnknownStatus", l);  //$NON-NLS-1$
		
		if (item == AdministrativePlanTarget.SUMMARY_KEY){
			return Messages.getString("PlanLabelProvider.AdminTargetLabelStatus", l); //$NON-NLS-1$
		}
		if (item == NumericPlanTarget.SUMMARY_KEY){
			return Messages.getString("PlanLabelProvider.NumericTargetLabelStatus", l); //$NON-NLS-1$
		}
		if (item == SpatialPlanTarget.SUMMARY_KEY){
			return Messages.getString("PlanLabelProvider.SpatialTargetLabelStatus", l); //$NON-NLS-1$
		}
		if (item instanceof PlanPatrolQueryOption){
			return  Messages.getString("PlanLabelProvider.PartofPlanQueryOp", l); //$NON-NLS-1$
		}
		return null;
	}
}
