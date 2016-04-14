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
package org.wcs.smart.connect.apache;

import java.io.File;

import javax.naming.NamingException;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.wcs.smart.ICoreLabelProvider;
import org.wcs.smart.SmartContext;
import org.wcs.smart.connect.datastore.DataStoreManager;
import org.wcs.smart.connect.i18n.labels.EntityLabelProvider;
import org.wcs.smart.connect.i18n.labels.EntityQueryLabelProvider;
import org.wcs.smart.connect.i18n.labels.ErLabelProvider;
import org.wcs.smart.connect.i18n.labels.GridQueryColumnLabelProvider;
import org.wcs.smart.connect.i18n.labels.IncidentLabelProvider;
import org.wcs.smart.connect.i18n.labels.IntelligenceLabelProvider;
import org.wcs.smart.connect.i18n.labels.IntelligenceQueryLabelProvider;
import org.wcs.smart.connect.i18n.labels.ObservationQueryLabelProvider;
import org.wcs.smart.connect.i18n.labels.OperatorLabelProvider;
import org.wcs.smart.connect.i18n.labels.PatrolLabelProvider;
import org.wcs.smart.connect.i18n.labels.PatrolQueryLabelProvider;
import org.wcs.smart.connect.i18n.labels.PlanLabelProvider;
import org.wcs.smart.connect.i18n.labels.QueryDateLabelProvider;
import org.wcs.smart.connect.i18n.labels.SmartLabelProvider;
import org.wcs.smart.connect.i18n.labels.SurveyQueryLabelProvider;
import org.wcs.smart.connect.query.PatrolContributionFinder;
import org.wcs.smart.connect.query.WaypointSourceEngine;
import org.wcs.smart.connect.query.columns.EntityQueryColumnProvider;
import org.wcs.smart.connect.query.columns.IntelligenceQueryColumnProvider;
import org.wcs.smart.connect.query.columns.ObservationQueryColumnProvider;
import org.wcs.smart.connect.query.columns.PatrolQueryColumnProvider;
import org.wcs.smart.connect.query.columns.SurveyQueryColumnProvider;
import org.wcs.smart.connect.report.SmartServiceLabelProvider;
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
import org.wcs.smart.udig.catalog.smart.ISmartMapLabelProvider;

/**
 * Web listener to initialize the SMART Context on startup.  This configures
 * the various label providers and other items required for supporting query
 * functions and other functions that use the smart desktop libraries.
 * 
 * @author Emily
 *
 */
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
		SmartContext.INSTANCE.setClass(ISmartMapLabelProvider.class, new SmartServiceLabelProvider());
		
		/* filestore configurations */
		SmartContext.INSTANCE.setTempFilestoreLocation((File)arg0.getServletContext().getAttribute(ServletContext.TEMPDIR));
		try{
			DataStoreManager.INSTANCE.initDatastore();
		}catch(NamingException ex){
			throw new IllegalStateException("Cannot initialize datastore.", ex); //$NON-NLS-1$
		}
		SmartContext.INSTANCE.setFilestoreLocation(DataStoreManager.INSTANCE.getRootDirectory().getAbsolutePath());
	}


}
