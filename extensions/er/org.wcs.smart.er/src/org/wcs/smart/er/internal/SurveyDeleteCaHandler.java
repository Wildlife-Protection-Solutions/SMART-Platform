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
package org.wcs.smart.er.internal;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.ICaDeleteHandler;
import org.wcs.smart.observation.CaDeleteHandler;

/**
 * Delete conservation area handle for survey elements.
 * 
 * @author Emily
 *
 */
public class SurveyDeleteCaHandler implements ICaDeleteHandler{

	/**
	 * To be executed after before the conservation area is removed
	 */
	public static final int EXECUTE_ORDER = CaDeleteHandler.DELETE_ORDER + 1;
	
	/* (non-Javadoc)
	 * @see org.wcs.smart.ca.ICaDeleteListener#beforeDelete(org.wcs.smart.ca.ConservationArea, org.hibernate.Session)
	 */
	@Override
	public void beforeDelete(ConservationArea ca, Session session, IProgressMonitor monitor)
			throws Exception {
		monitor.subTask(Messages.SurveyDeleteCaHandler_deleteMissions);
		deleteMissions(ca, session);
		
		monitor.subTask(Messages.SurveyDeleteCaHandler_deleteSurveys);
		deleteSurveys(ca, session);
		
		monitor.subTask(Messages.SurveyDeleteCaHandler_deleteSurveyDesigns);
		deleteSurveyDesign(ca, session);
		
		monitor.subTask(Messages.SurveyDeleteCaHandler_deleteMissionProp);
		deleteMissionAttributes(ca, session);
		
		monitor.subTask(Messages.SurveyDeleteCaHandler_deleteSuAttriutes);
		deleteSamplingUnitAttributes(ca, session);
	}

	private void deleteMissions(ConservationArea ca, Session session) throws Exception{
		
		//mission property values
		Query q = session.createQuery(
				"DELETE MissionPropertyValue mp WHERE mp.id.missionAttribute IN " + //$NON-NLS-1$
				"(SELECT ma FROM MissionAttribute ma WHERE ma.conservationArea = :conservationArea)"); //$NON-NLS-1$
		q.setParameter("conservationArea", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		//survey waypoints
		q = session.createQuery(
				"DELETE SurveyWaypoint sw WHERE sw.id.waypoint IN " + //$NON-NLS-1$
				"(SELECT wp FROM Waypoint wp WHERE wp.conservationArea = :conservationArea)"); //$NON-NLS-1$
		q.setParameter("conservationArea", ca); //$NON-NLS-1$
		q.executeUpdate();		

		//mission members 
		q = session.createQuery(
				"DELETE MissionMember mp WHERE mp.id.mission IN " + //$NON-NLS-1$
				"(SELECT ma FROM Mission ma join ma.survey s join s.surveyDesign sd WHERE sd.conservationArea = :conservationArea)"); //$NON-NLS-1$
		q.setParameter("conservationArea", ca); //$NON-NLS-1$
		q.executeUpdate();

		//mission tracks 
		q = session.createQuery(
				"DELETE MissionTrack mp WHERE mp.mission IN " + //$NON-NLS-1$
				"(SELECT ma FROM Mission ma join ma.survey s join s.surveyDesign sd WHERE sd.conservationArea = :conservationArea)"); //$NON-NLS-1$
		q.setParameter("conservationArea", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		//mission
		q = session.createQuery(
				"delete Mission m where m.survey IN " + //$NON-NLS-1$
				"(SELECT s FROM Survey s join s.surveyDesign sd WHERE sd.conservationArea = :conservationArea)"); //$NON-NLS-1$
		q.setParameter("conservationArea", ca); //$NON-NLS-1$
		q.executeUpdate();
	}
	
	private void deleteSurveys(ConservationArea ca, Session session) throws Exception{
		//entity attribute values
		Query q = session.createQuery(
				"delete Survey s where s IN " + //$NON-NLS-1$
				"(SELECT s FROM Survey s WHERE s.surveyDesign.conservationArea = :conservationArea)"); //$NON-NLS-1$
		q.setParameter("conservationArea", ca); //$NON-NLS-1$
		q.executeUpdate();
	}

	private void deleteSurveyDesign(ConservationArea ca, Session session) throws Exception{
		//sampling unit attribue values
		Query q = session.createQuery(
				"DELETE SamplingUnitAttributeValue s where s.id.samplingUnit IN " + //$NON-NLS-1$
				"(SELECT su FROM SamplingUnit su join su.surveyDesign sd WHERE sd.conservationArea = :conservationArea)"); //$NON-NLS-1$
		q.setParameter("conservationArea", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		//sampling units
		q = session.createQuery(
				"delete SamplingUnit s where s.surveyDesign IN " + //$NON-NLS-1$
				"(SELECT sd FROM SurveyDesign sd WHERE sd.conservationArea = :conservationArea)"); //$NON-NLS-1$
		q.setParameter("conservationArea", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		//sampling unit attribute to survey design links
		q = session.createQuery(
				"DELETE SurveyDesignSamplingUnitAttribute s where s.id.surveyDesign IN " + //$NON-NLS-1$
				"(SELECT sd FROM SurveyDesign sd WHERE sd.conservationArea = :conservationArea)"); //$NON-NLS-1$
		q.setParameter("conservationArea", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		// mission properties
		q = session.createQuery("DELETE MissionProperty mp where mp.id.surveyDesign IN " + //$NON-NLS-1$
			"(SELECT sd FROM SurveyDesign sd WHERE sd.conservationArea = :conservationArea)"); //$NON-NLS-1$
		q.setParameter("conservationArea", ca); //$NON-NLS-1$
		q.executeUpdate();
				
		//survey design properties
		q = session.createQuery("DELETE SurveyDesignProperty p where p.surveyDesign IN " + //$NON-NLS-1$
				"(SELECT sd FROM SurveyDesign sd WHERE sd.conservationArea = :conservationArea)"); //$NON-NLS-1$
		q.setParameter("conservationArea", ca); //$NON-NLS-1$
		q.executeUpdate();
				
		//survey design			.
		q = session.createQuery(
				"delete FROM SurveyDesign where conservationArea = :conservationArea"); //$NON-NLS-1$
		q.setParameter("conservationArea", ca); //$NON-NLS-1$
		q.executeUpdate();
	}
	
	private void deleteMissionAttributes(ConservationArea ca, Session session) throws Exception{
		
		Query q = session.createQuery(
				"DELETE MissionAttributeListItem li WHERE li.attribute IN " +//$NON-NLS-1$
				"(SELECT ma FROM MissionAttribute ma WHERE ma.conservationArea = :conservationArea)"); //$NON-NLS-1$
		q.setParameter("conservationArea", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		q = session.createQuery(
				"DELETE FROM MissionAttribute WHERE conservationArea = :conservationArea"); //$NON-NLS-1$
		q.setParameter("conservationArea", ca); //$NON-NLS-1$
		q.executeUpdate();
	}
	
	private void deleteSamplingUnitAttributes(ConservationArea ca, Session session) throws Exception{
		Query q = session.createQuery(
				"DELETE FROM SamplingUnitAttribute WHERE conservationArea = :conservationArea"); //$NON-NLS-1$
		q.setParameter("conservationArea", ca); //$NON-NLS-1$
		q.executeUpdate();
	}
}
