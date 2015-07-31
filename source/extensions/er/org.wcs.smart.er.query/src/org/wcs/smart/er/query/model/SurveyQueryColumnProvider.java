package org.wcs.smart.er.query.model;

import java.util.List;

import javax.persistence.Transient;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.query.ERQueryPlugIn;
import org.wcs.smart.er.query.internal.Messages;
import org.wcs.smart.er.query.ui.columns.SurveyQueryColumnManager;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.model.QueryColumn;

public class SurveyQueryColumnProvider implements ISurveyQueryColumnProvider {

	@Override
	public QueryColumn[] getQueryColumns(String queryTypeKey, String surveyDesignKey) {
		if (queryTypeKey.equals(SurveyObservationQuery.KEY)){
			SurveyQueryColumnManager.getInstance()
				.getObservationQueryColumns(
						getSurveyDesignAsObject(surveyDesignKey));
		}else if (queryTypeKey.equals(SurveyWaypointQuery.KEY)){
			SurveyQueryColumnManager.getInstance()
			.getWaypointQueryColumns(
					getSurveyDesignAsObject(surveyDesignKey));
		}else if (queryTypeKey.equals(SurveyGriddedQuery.KEY)){
			SurveyQueryColumnManager.getInstance()
			.getGridColumns();
		}else if (queryTypeKey.equals(MissionQuery.KEY)){
			SurveyQueryColumnManager.getInstance()
			.getMissionQueryColumns(
					getSurveyDesignAsObject(surveyDesignKey));
		}else if (queryTypeKey.equals(MissionTrackQuery.KEY)){
			SurveyQueryColumnManager.getInstance()
			.getMissionTrackQueryColumns(
					getSurveyDesignAsObject(surveyDesignKey));
		}
		return null;
	}

	
	/**
	 * 
	 * @return the query filter in the filter format.  Will
	 * attempt to parse the query if it has not been parsed
	 */
	@Transient
	public SurveyDesign getSurveyDesignAsObject(final String surveyDesignKey){
		if (surveyDesignKey == null) return null;
		final SurveyDesign[] surveyDesign = new SurveyDesign[1];
		Job j = new Job(Messages.SurveyObservationQuery_loadingDesignJobName){

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				Session s = HibernateManager.openSession();
				List<?> results = s
						.createCriteria(SurveyDesign.class)
						.add(Restrictions.eq("keyId", surveyDesignKey)) //$NON-NLS-1$
						.add(Restrictions
								.eq("conservationArea", SmartDB.getCurrentConservationArea())).list(); //$NON-NLS-1$
				if (results.size() > 0) {
					surveyDesign[0] = (SurveyDesign) results.get(0);
				}
				return Status.OK_STATUS;
			}
		};
		j.setSystem(true);
		j.schedule();
		try {
			j.join();
			} catch (InterruptedException e) {
			ERQueryPlugIn.log(e.getMessage(), e);
		}
		
		return surveyDesign[0];
	}
}
