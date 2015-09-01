package org.wcs.smart.connect.hibernate;

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
import org.wcs.smart.intelligence.model.PatrolIntelligence;
import org.wcs.smart.intelligence.query.model.IntelligenceRecordQuery;
import org.wcs.smart.intelligence.query.model.IntelligenceSummaryQuery;
import org.wcs.smart.observation.model.ObservationAttachment;
import org.wcs.smart.observation.model.ObservationOptions;
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
import org.wcs.smart.patrol.model.PatrolType;
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
import org.wcs.smart.report.model.Report;
import org.wcs.smart.report.model.ReportFolder;
import org.wcs.smart.report.model.ReportQuery;

public enum SmartTable {

	EMPLOYEE(Employee.class, null),
	ADMINISTRATIVEPLANTARGET(AdministrativePlanTarget.class,".plan.conservationArea"),
	AGENCY(Agency.class,null),
	AGGREGATION(Aggregation.class,null),
	AGGREGATIONLABEL(AggregationLabel.class,null),
	AREA(Area.class,null),
	ATTRIBUTE(Attribute.class,null),
	ATTRIBUTELISTITEM(AttributeListItem.class,".attribute.conservationArea"),
	ATTRIBUTETREENODE(AttributeTreeNode.class,".attribute.conservationArea"),
	BASEMAPDEFINITION(BasemapDefinition.class,null),
	CATEGORY(Category.class,null),
	CATEGORYATTRIBUTE(CategoryAttribute.class,".id.category.conservationArea"),
	CMATTRIBUTE(CmAttribute.class,".node.model.conservationArea"),
	CMATTRIBUTEITEM(CmAttributeItem.class,null),
	CMATTRIBUTELISTITEM(CmAttributeListItem.class,".configurableModel.conservationArea"),
	CMATTRIBUTEOPTION(CmAttributeOption.class,".cmAttribute.node.model.conservationArea"),
	CMATTRIBUTETREENODE(CmAttributeTreeNode.class,".configurableModel.conservationArea"),
	CMNODE(CmNode.class,".model.conservationArea"),
	CONFIGURABLEMODEL(ConfigurableModel.class,null),
	CONSERVATIONAREA(ConservationArea.class,".uuid"),
	DMOBJECT(DmObject.class,null),
	ENTITY(Entity.class,".entityType.conservationArea"),
	ENTITYATTRIBUTE(EntityAttribute.class,".entityType.conservationArea"),
	ENTITYATTRIBUTEVALUE(EntityAttributeValue.class,".id.entity.entityType.conservationArea"),
	ENTITYGRIDDEDQUERY(EntityGriddedQuery.class,null),
	ENTITYOBSERVATIONQUERY(EntityObservationQuery.class,null),
	ENTITYSUMMARYQUERY(EntitySummaryQuery.class,null),
	ENTITYTYPE(EntityType.class,null),
	ENTITYWAYPOINTQUERY(EntityWaypointQuery.class,null),
	GRIDDEDQUERY(GriddedQuery.class,null),
	INFORMANT(Informant.class,null),
	INTELLIGENCE(Intelligence.class,null),
	INTELLIGENCEATTACHMENT(IntelligenceAttachment.class,".intelligence.conservationArea"),
	INTELLIGENCEPOINT(IntelligencePoint.class,".intelligence.conservationArea"),
	INTELLIGENCERECORDQUERY(IntelligenceRecordQuery.class,null),
	INTELLIGENCESOURCE(IntelligenceSource.class,null),
	INTELLIGENCESUMMARYQUERY(IntelligenceSummaryQuery.class,null),
	LABEL(Label.class,".id.language.ca"),
	LANGUAGE(Language.class,null),
	MISSION(Mission.class,".survey.surveyDesign.conservationArea"),
	MISSIONATTRIBUTE(MissionAttribute.class,null),
	MISSIONATTRIBUTELISTITEM(MissionAttributeListItem.class,".attribute.conservationArea"),
	MISSIONDAY(MissionDay.class,".mission.survey.surveyDesign.conservationArea"),
	MISSIONMEMBER(MissionMember.class,".id.member.conservationArea"),
	MISSIONPROPERTY(MissionProperty.class,".id.surveyDesign.conservationArea"),
	MISSIONPROPERTYVALUE(MissionPropertyValue.class,".id.mission.survey.surveyDesign.conservationArea"),
	MISSIONQUERY(MissionQuery.class,null),
	MISSIONTRACK(MissionTrack.class,".missionDay.mission.survey.surveyDesign.conservationArea"),
	MISSIONTRACKQUERY(MissionTrackQuery.class,null),
	NAMEDDESCRIPTIONITEM(NamedDescriptionItem.class,null),
	NAMEDDESCRIPTIONKEYITEM(NamedDescriptionKeyItem.class,null),
	NAMEDITEM(NamedItem.class,null),
	NAMEDKEYITEM(NamedKeyItem.class,null),
	NUMERICPLANTARGET(NumericPlanTarget.class,".plan.conservationArea"),
	OBSERVATIONATTACHMENT(ObservationAttachment.class,".observation.waypoint.conservationArea"),
	OBSERVATIONGRIDDEDQUERY(ObservationGriddedQuery.class,null),
	OBSERVATIONOPTIONS(ObservationOptions.class,".uuid"),
	OBSERVATIONQUERY(ObservationQuery.class,null),
	OBSERVATIONSUMMARYQUERY(ObservationSummaryQuery.class,null),
	OBSERVATIONWAYPOINTQUERY(ObservationWaypointQuery.class,null),
	OBSOBSERVATIONQUERY(ObsObservationQuery.class,null),
	PATROL(Patrol.class,null),
	PATROLGRIDDEDQUERY(PatrolGriddedQuery.class,null),
	PATROLLEG(PatrolLeg.class,".patrol.conservationArea"),
	PATROLLEGDAY(PatrolLegDay.class,".patrolLeg.patrol.conservationArea"),
	PATROLLEGMEMBER(PatrolLegMember.class,".id.patrolLeg.patrol.conservationArea"),
	PATROLMANDATE(PatrolMandate.class,null),
	PATROLOBSERVATIONQUERY(PatrolObservationQuery.class,null),
	PATROLPLAN(PatrolPlan.class,".id.plan.conservationArea"),
	PATROLQUERY(PatrolQuery.class,null),
	PATROLSUMMARYQUERY(PatrolSummaryQuery.class,null),
	PATROLTYPE(PatrolType.class, ".id.conservationArea"),
	PATROLTRANSPORTTYPE(PatrolTransportType.class,null),
	PATROLWAYPOINT(PatrolWaypoint.class,".id.waypoint.conservationArea"),
	PATROLWAYPOINTQUERY(PatrolWaypointQuery.class,null),
	PATROLINTELLIGENCE(PatrolIntelligence.class,".id.patrol.conservationArea"),
	PLAN(Plan.class,null),
	PLANTARGET(PlanTarget.class,null),
	PROJECTION(Projection.class,null),
	QUERY(Query.class,null),
	QUERYFOLDER(QueryFolder.class,null),
	RANK(Rank.class,".agency.conservationArea"),
	REPORT(Report.class, null),
	REPORTFOLDER(ReportFolder.class, null),
	REPORTQUERY(ReportQuery.class, ".id.report.conservationArea"),
	SAMPLINGUNIT(SamplingUnit.class,".surveyDesign.conservationArea"),
	SAMPLINGUNITATTRIBUTE(SamplingUnitAttribute.class,".conservationArea"),
	SAMPLINGUNITATTRIBUTELISTITEM(SamplingUnitAttributeListItem.class,".attribute.conservationArea"),
	SAMPLINGUNITATTRIBUTEVALUE(SamplingUnitAttributeValue.class,".id.samplingUnitAttribute.conservationArea"),
	SCREENOPTION(ScreenOption.class,null),
	SCREENOPTIONUUID(ScreenOptionUuid.class,".screenOption.conservationArea"),
	SMARTSTYLE(SmartStyle.class,null),
	SPATIALPLANTARGET(SpatialPlanTarget.class,".plan.conservationArea"),
	SPATIALPLANTARGETPOINT(SpatialPlanTargetPoint.class,".planTarget.plan.conservationArea"),
	STATION(Station.class,null),
	STYLEDQUERY(StyledQuery.class,null),
	SUMMARYQUERY(SummaryQuery.class,null),
	SURVEY(Survey.class,".surveyDesign.conservationArea"),
	SURVEYDESIGN(SurveyDesign.class,null),
	SURVEYDESIGNPROPERTY(SurveyDesignProperty.class,".surveyDesign.conservationArea"),
	SURVEYDESIGNSAMPLINGUNITATTRIBUTE(SurveyDesignSamplingUnitAttribute.class,null),
	SURVEYGRIDDEDQUERY(SurveyGriddedQuery.class,null),
	SURVEYOBSERVATIONQUERY(SurveyObservationQuery.class,null),
	SURVEYSUMMARYQUERY(SurveySummaryQuery.class,null),
	SURVEYWAYPOINT(SurveyWaypoint.class,".id.waypoint.conservationArea"),
	SURVEYWAYPOINTQUERY(SurveyWaypointQuery.class,null),
	TEAM(Team.class,null),
	TRACK(Track.class,".patrolLegDay.patrolLeg.patrol.conservationArea"),
	UUIDITEM(UuidItem.class,null),
	WAYPOINT(Waypoint.class,null),
	WAYPOINTATTACHMENT(WaypointAttachment.class,".waypoint.conservationArea"),
	WAYPOINTOBSERVATION(WaypointObservation.class,".waypoint.conservationArea"),
	WAYPOINTOBSERVATIONATTRIBUTE(WaypointObservationAttribute.class,".id.observation.waypoint.conservationArea"),
	WAYPOINTQUERY(WaypointQuery.class, null);
	
	public Class<?> hibernateClass;
	public String caProperty;
	
	SmartTable(Class<?> clazz, String caLink){
		this.hibernateClass = clazz;
		this.caProperty = caLink;
	}
}
