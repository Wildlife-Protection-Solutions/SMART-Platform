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
package org.wcs.smart.plan.internal;

import java.util.Locale;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.wcs.smart.plan.IPlanLabelProvider;
import org.wcs.smart.plan.SmartPlanPlugIn;
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

	public static final String ADMIN_TARGET_GUI_NAME = Messages.AdministrativePlanTarget_GuiName;
	
	public static final String NUMERIC_TARGET_GUI_NAME = Messages.NumericPlanTarget_GuiName;

	@Override
	public String getLabel(Object item, Locale l) {
		if (item == NumericPlanTarget.TargetType.DISTANCE) return Messages.NumericPlanTarget_TargetType_DistanceTraveled;
		if (item == NumericPlanTarget.TargetType.PATROL_HOURS) return Messages.NumericPlanTarget_TargetType_PatrolHours;
		if (item == NumericPlanTarget.TargetType.PATROL_DAYS) return Messages.NumericPlanTarget_TargetType_PatrolDays;
		if (item == NumericPlanTarget.TargetType.PATROL_MANHOURS) return Messages.NumericPlanTarget_TargetType_PatrolManHours;
		
		if (item == NumericPlanTarget.Unit.KM) return Messages.NumericPlanTarget_km;
		if (item == NumericPlanTarget.Unit.HOURS) return Messages.NumericPlanTarget_hours;
		if (item == NumericPlanTarget.Unit.DAYS) return Messages.NumericPlanTarget_days;
		
		if (item == Plan.PlanType.CA) return Messages.PlanType_ConservationArea;
		if (item == Plan.PlanType.STATION) return Messages.PlanType_Station;
		if (item == Plan.PlanType.TEAM) return Messages.PlanType_Team;
		if (item == Plan.PlanType.PATROL) return Messages.PlanType_Patrol;
				
		if (item == PlanTargetStatus.Status.COMPLETE) return Messages.PlanTargetStatus_Complete;
		if (item == PlanTargetStatus.Status.INCOMPLETE) return  Messages.PlanTargetStatus_Incomplete;
		if (item == PlanTargetStatus.Status.UNKNOWN) return Messages.PlanTargetStatus_Unknown; 
		
		if (item == AdministrativePlanTarget.SUMMARY_KEY){
			return Messages.PlanLabelProvider_AdminSummary;
		}
		if (item == NumericPlanTarget.SUMMARY_KEY){
			return Messages.PlanLabelProvider_NumericSummary;
		}
		if (item == SpatialPlanTarget.SUMMARY_KEY){
			return Messages.PlanLabelProvider_SpatialSummary;
		}
		if (item instanceof PlanPatrolQueryOption){
			return  Messages.PlanPatrolQueryOption_Name;
		}
		return null;
	}

	
	public static ImageDescriptor getImage(Plan.PlanType item){
		if (item == Plan.PlanType.CA) return SmartPlanPlugIn.getDefault().getImageRegistry().getDescriptor(SmartPlanPlugIn.CA_PLAN_ICON);
		if (item == Plan.PlanType.STATION) return SmartPlanPlugIn.getDefault().getImageRegistry().getDescriptor(SmartPlanPlugIn.STATION_PLAN_ICON);
		if (item == Plan.PlanType.TEAM) return SmartPlanPlugIn.getDefault().getImageRegistry().getDescriptor(SmartPlanPlugIn.TEAM_PLAN_ICON);
		if (item == Plan.PlanType.PATROL) return  SmartPlanPlugIn.getDefault().getImageRegistry().getDescriptor(SmartPlanPlugIn.PATROL_PLAN_ICON);
		return null;
	}
	
	public static Image getImage(PlanTargetStatus.Status item){
		if (item == PlanTargetStatus.Status.COMPLETE) return SmartPlanPlugIn.getDefault().getImageRegistry().get(SmartPlanPlugIn.STATUS_COMPLETE);
		if (item == PlanTargetStatus.Status.INCOMPLETE) return  SmartPlanPlugIn.getDefault().getImageRegistry().get(SmartPlanPlugIn.STATUS_INCOMPLETE);
		return null;		
	}
}
