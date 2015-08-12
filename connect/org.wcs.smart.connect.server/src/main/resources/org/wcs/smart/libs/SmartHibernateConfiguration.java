package org.wcs.smart.libs;

import org.wcs.smart.ca.Agency;
import org.wcs.smart.ca.Area;
import org.wcs.smart.ca.BasemapDefinition;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.Label;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.NamedDescriptionItem;
import org.wcs.smart.ca.NamedDescriptionKeyItem;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.ca.NamedKeyItem;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.ca.Rank;
import org.wcs.smart.ca.SmartStyle;
import org.wcs.smart.ca.Station;
import org.wcs.smart.ca.UuidItem;
import org.wcs.smart.ca.datamodel.Aggregation;
import org.wcs.smart.ca.datamodel.AggregationLabel;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.ca.datamodel.DmObject;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.CmAttributeItem;
import org.wcs.smart.dataentry.model.CmAttributeListItem;
import org.wcs.smart.dataentry.model.CmAttributeOption;
import org.wcs.smart.dataentry.model.CmAttributeTreeNode;
import org.wcs.smart.dataentry.model.CmNode;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.entity.model.Entity;
import org.wcs.smart.entity.model.EntityAttribute;
import org.wcs.smart.entity.model.EntityAttributeValue;
import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.entity.query.model.EntityGriddedQuery;
import org.wcs.smart.entity.query.model.EntityObservationQuery;
import org.wcs.smart.entity.query.model.EntitySummaryQuery;
import org.wcs.smart.entity.query.model.EntityWaypointQuery;
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
import org.wcs.smart.er.model.SurveyDesignProperty;
import org.wcs.smart.er.model.SurveyDesignSamplingUnitAttribute;
import org.wcs.smart.er.model.SurveyWaypoint;
import org.wcs.smart.er.query.model.MissionQuery;
import org.wcs.smart.er.query.model.MissionTrackQuery;
import org.wcs.smart.er.query.model.SurveyGriddedQuery;
import org.wcs.smart.er.query.model.SurveyObservationQuery;
import org.wcs.smart.er.query.model.SurveySummaryQuery;
import org.wcs.smart.er.query.model.SurveyWaypointQuery;
import org.wcs.smart.intelligence.model.Informant;
import org.wcs.smart.intelligence.model.Intelligence;
import org.wcs.smart.intelligence.model.IntelligenceAttachment;
import org.wcs.smart.intelligence.model.IntelligencePoint;
import org.wcs.smart.intelligence.model.IntelligenceSource;
import org.wcs.smart.intelligence.query.model.IntelligenceRecordQuery;
import org.wcs.smart.intelligence.query.model.IntelligenceSummaryQuery;
import org.wcs.smart.observation.model.ObservationAttachment;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointAttachment;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.observation.query.model.ObsObservationQuery;
import org.wcs.smart.observation.query.model.ObservationGriddedQuery;
import org.wcs.smart.observation.query.model.ObservationSummaryQuery;
import org.wcs.smart.observation.query.model.ObservationWaypointQuery;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.PatrolLegMember;
import org.wcs.smart.patrol.model.PatrolMandate;
import org.wcs.smart.patrol.model.PatrolTransportType;
import org.wcs.smart.patrol.model.PatrolWaypoint;
import org.wcs.smart.patrol.model.ScreenOption;
import org.wcs.smart.patrol.model.ScreenOptionUuid;
import org.wcs.smart.patrol.model.Team;
import org.wcs.smart.patrol.model.Track;
import org.wcs.smart.patrol.query.model.PatrolGriddedQuery;
import org.wcs.smart.patrol.query.model.PatrolObservationQuery;
import org.wcs.smart.patrol.query.model.PatrolQuery;
import org.wcs.smart.patrol.query.model.PatrolSummaryQuery;
import org.wcs.smart.patrol.query.model.PatrolWaypointQuery;
import org.wcs.smart.plan.model.AdministrativePlanTarget;
import org.wcs.smart.plan.model.NumericPlanTarget;
import org.wcs.smart.plan.model.PatrolPlan;
import org.wcs.smart.plan.model.Plan;
import org.wcs.smart.plan.model.PlanTarget;
import org.wcs.smart.plan.model.SpatialPlanTarget;
import org.wcs.smart.plan.model.SpatialPlanTargetPoint;
import org.wcs.smart.query.common.model.GriddedQuery;
import org.wcs.smart.query.common.model.ObservationQuery;
import org.wcs.smart.query.common.model.SummaryQuery;
import org.wcs.smart.query.common.model.WaypointQuery;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryFolder;
import org.wcs.smart.query.model.StyledQuery;

public enum SmartHibernateConfiguration {

	INSTANCE;
	
	public Class<?>[] getTables(){
		return new Class[]{
			Employee.class,
			Language.class,
			Agency.class,
			Rank.class,
			ConservationArea.class,
			QueryFolder.class,
			Label.class,
			NamedItem.class,
			UuidItem.class,
			Query.class,
			PatrolObservationQuery.class,
			Station.class,
			Team.class,
			CmAttributeOption.class,
			Entity.class,
			Informant.class,
			Mission.class,
			MissionDay.class,
			MissionTrack.class,
			Area.class,
			BasemapDefinition.class,
			CmAttribute.class,
			CmAttributeItem.class,
			CmAttributeTreeNode.class,
			CmNode.class,
			ConfigurableModel.class,
			Intelligence.class,
			NamedDescriptionItem.class,
			NamedKeyItem.class,
			Plan.class,
			SmartStyle.class,
			CmAttributeListItem.class,
			DmObject.class,
			Attribute.class,
			AttributeListItem.class,
			AttributeTreeNode.class,
			Category.class,
			EntityAttribute.class,
			EntityType.class,
			IntelligenceSource.class,
			MissionAttribute.class,
			MissionAttributeListItem.class,
			NamedDescriptionKeyItem.class,
			PatrolMandate.class,
			PatrolTransportType.class,
			SamplingUnitAttribute.class,
			SamplingUnitAttributeListItem.class,
			SurveyDesign.class,
			IntelligenceSummaryQuery.class,
			StyledQuery.class,
			GriddedQuery.class,
			EntityGriddedQuery.class,
			ObservationGriddedQuery.class,
			PatrolGriddedQuery.class,
			SurveyGriddedQuery.class,
			IntelligenceRecordQuery.class,
			MissionQuery.class,
			MissionTrackQuery.class,
			ObservationQuery.class,
			PatrolQuery.class,
			WaypointQuery.class,
			EntityObservationQuery.class,
			ObsObservationQuery.class,
			PatrolObservationQuery.class,
			SurveyObservationQuery.class,
			EntityWaypointQuery.class,
			ObservationWaypointQuery.class,
			PatrolWaypointQuery.class,
			SurveyWaypointQuery.class,
			SummaryQuery.class,
			EntitySummaryQuery.class,
			ObservationSummaryQuery.class,
			PatrolSummaryQuery.class,
			SurveySummaryQuery.class,
			ObservationAttachment.class,
			Patrol.class,
			PatrolLeg.class,
			PatrolLegDay.class,
			Projection.class,
			SamplingUnit.class,
			ScreenOption.class,
			ScreenOptionUuid.class,
			Survey.class,
			SurveyDesignProperty.class,
			Track.class,
			Waypoint.class,
			WaypointAttachment.class,
			WaypointObservation.class,
			Aggregation.class,
			EntityAttributeValue.class,
			MissionMember.class,
			PatrolLegMember.class,
			MissionPropertyValue.class,
			MissionDay.class,
			PatrolWaypoint.class,
			SurveyWaypoint.class,
			IntelligenceAttachment.class,
			IntelligencePoint.class,
			PatrolPlan.class,
			PlanTarget.class,
			AdministrativePlanTarget.class,
			SpatialPlanTarget.class,
			SpatialPlanTargetPoint.class,
			NumericPlanTarget.class,
			CategoryAttribute.class,
			MissionProperty.class,
			SurveyDesignSamplingUnitAttribute.class,
			AggregationLabel.class,
			SamplingUnitAttributeValue.class,
			WaypointObservationAttribute.class,
			
			
		
			
		};
	}
}
