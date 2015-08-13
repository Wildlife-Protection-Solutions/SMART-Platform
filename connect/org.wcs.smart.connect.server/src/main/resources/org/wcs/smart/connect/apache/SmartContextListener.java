package org.wcs.smart.connect.apache;

import java.io.File;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.wcs.smart.ICoreLabelProvider;
import org.wcs.smart.SmartContext;
import org.wcs.smart.connect.datastore.DataStoreManager;
import org.wcs.smart.entity.IEntityLabelProvider;
import org.wcs.smart.entity.query.IEntityQueryColumnProvider;
import org.wcs.smart.entity.query.IEntityQueryLabelProvider;
import org.wcs.smart.er.model.IErLabelProvider;
import org.wcs.smart.er.query.ISurveyQueryLabelProvider;
import org.wcs.smart.er.query.model.ISurveyQueryColumnProvider;
import org.wcs.smart.incident.IIncidentLabelProvider;
import org.wcs.smart.intelligence.IIntelligenceLabelProvider;
import org.wcs.smart.intelligence.query.IIntelligenceQueryColumnProvider;
import org.wcs.smart.intelligence.query.IIntelligenceQueryLabelProvider;
import org.wcs.smart.observation.model.IWaypointSourceEngine;
import org.wcs.smart.observation.query.model.columns.IObservationQueryColumnProvider;
import org.wcs.smart.observation.query.view.IObservationQueryLabelProvider;
import org.wcs.smart.patrol.model.IPatrolLabelProvider;
import org.wcs.smart.patrol.query.ext.IPatrolContributionFinder;
import org.wcs.smart.patrol.query.model.IPatrolQueryColumnProvider;
import org.wcs.smart.patrol.ui.IQueryPatrolLabelProvider;
import org.wcs.smart.plan.IPlanLabelProvider;
import org.wcs.smart.query.model.IGridQueryColumnLabelProvider;
import org.wcs.smart.query.model.filter.IOperatorLabelProvider;
import org.wcs.smart.query.model.filter.date.IQueryDateLabelProvider;
import org.wcs.smart.shared.PatrolContributionFinder;
import org.wcs.smart.shared.WaypointSourceEngine;
import org.wcs.smart.shared.labels.EntityLabelProvider;
import org.wcs.smart.shared.labels.EntityQueryLabelProvider;
import org.wcs.smart.shared.labels.ErLabelProvider;
import org.wcs.smart.shared.labels.GridQueryColumnLabelProvider;
import org.wcs.smart.shared.labels.IncidentLabelProvider;
import org.wcs.smart.shared.labels.IntelligenceLabelProvider;
import org.wcs.smart.shared.labels.IntelligenceQueryLabelProvider;
import org.wcs.smart.shared.labels.ObservationQueryLabelProvider;
import org.wcs.smart.shared.labels.OperatorLabelProvider;
import org.wcs.smart.shared.labels.PatrolLabelProvider;
import org.wcs.smart.shared.labels.PatrolQueryLabelProvider;
import org.wcs.smart.shared.labels.PlanLabelProvider;
import org.wcs.smart.shared.labels.QueryDateLabelProvider;
import org.wcs.smart.shared.labels.SmartLabelProvider;
import org.wcs.smart.shared.labels.SurveyQueryLabelProvider;
import org.wcs.smart.shared.query.columns.EntityQueryColumnProvider;
import org.wcs.smart.shared.query.columns.IntelligenceQueryColumnProvider;
import org.wcs.smart.shared.query.columns.ObservationQueryColumnProvider;
import org.wcs.smart.shared.query.columns.PatrolQueryColumnProvider;
import org.wcs.smart.shared.query.columns.SurveyQueryColumnProvider;

@WebListener
public class SmartContextListener implements ServletContextListener{

	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
	}

	@Override
	public void contextInitialized(ServletContextEvent arg0) {
		SmartContext.INSTANCE.setClass(IEntityLabelProvider.class, new EntityLabelProvider());
		SmartContext.INSTANCE.setClass(IEntityQueryLabelProvider.class, new EntityQueryLabelProvider());
		SmartContext.INSTANCE.setClass(IErLabelProvider.class, new ErLabelProvider());
		SmartContext.INSTANCE.setClass(IGridQueryColumnLabelProvider.class, new GridQueryColumnLabelProvider());
		SmartContext.INSTANCE.setClass(IIntelligenceLabelProvider.class, new IntelligenceLabelProvider());
		SmartContext.INSTANCE.setClass(IIntelligenceQueryLabelProvider.class, new IntelligenceQueryLabelProvider());
		SmartContext.INSTANCE.setClass(IObservationQueryLabelProvider.class, new ObservationQueryLabelProvider());
		SmartContext.INSTANCE.setClass(IOperatorLabelProvider.class, new OperatorLabelProvider());
		SmartContext.INSTANCE.setClass(IPatrolLabelProvider.class, new PatrolLabelProvider());
		SmartContext.INSTANCE.setClass(IQueryPatrolLabelProvider.class, new PatrolQueryLabelProvider());
		SmartContext.INSTANCE.setClass(IPlanLabelProvider.class, new PlanLabelProvider());
		SmartContext.INSTANCE.setClass(IQueryDateLabelProvider.class, new QueryDateLabelProvider());
		SmartContext.INSTANCE.setClass(ICoreLabelProvider.class, new SmartLabelProvider());
		SmartContext.INSTANCE.setClass(ISurveyQueryLabelProvider.class, new SurveyQueryLabelProvider());
		SmartContext.INSTANCE.setClass(ISurveyQueryLabelProvider.class, new SurveyQueryLabelProvider());
		SmartContext.INSTANCE.setClass(IIncidentLabelProvider.class, new IncidentLabelProvider());
		
		SmartContext.INSTANCE.setClass(IEntityQueryColumnProvider.class, new EntityQueryColumnProvider());
		SmartContext.INSTANCE.setClass(IIntelligenceQueryColumnProvider.class, new IntelligenceQueryColumnProvider());
		SmartContext.INSTANCE.setClass(IObservationQueryColumnProvider.class, new ObservationQueryColumnProvider());
		SmartContext.INSTANCE.setClass(IPatrolQueryColumnProvider.class, new PatrolQueryColumnProvider());
		SmartContext.INSTANCE.setClass(ISurveyQueryColumnProvider.class, new SurveyQueryColumnProvider());
		
		SmartContext.INSTANCE.setClass(IPatrolContributionFinder.class, new PatrolContributionFinder());
		
		SmartContext.INSTANCE.setClass(IWaypointSourceEngine.class, WaypointSourceEngine.INSTANCE);
		SmartContext.INSTANCE.setFilestoreLocation(DataStoreManager.INSTANCE.getRootDirectory().getAbsolutePath());
		SmartContext.INSTANCE.setTempFilestoreLocation(new File("C:\\temp\\webfilestore"));
	}


}
