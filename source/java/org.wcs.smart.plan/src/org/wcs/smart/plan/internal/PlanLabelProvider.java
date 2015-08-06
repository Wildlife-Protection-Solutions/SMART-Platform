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

public class PlanLabelProvider implements IPlanLabelProvider {

	public static final String ADMIN_TARGET_GUI_NAME = Messages.AdministrativePlanTarget_GuiName;
	public final static String NUMERIC_TARGET_GUI_NAME = Messages.NumericPlanTarget_GuiName;

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
			return "[Admin] {0}";
		}
		if (item == NumericPlanTarget.SUMMARY_KEY){
			return "[Numeric] {0}";
		}
		if (item == SpatialPlanTarget.SUMMARY_KEY){
			return "[Spatial] {0} ({1} point(s))";
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
