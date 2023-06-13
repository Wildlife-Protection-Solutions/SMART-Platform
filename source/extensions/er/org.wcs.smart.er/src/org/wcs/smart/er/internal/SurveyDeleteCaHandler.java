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
		session.createMutationQuery(
				"DELETE MissionPropertyValue mp WHERE mp.id.missionAttribute IN " + //$NON-NLS-1$
				"(SELECT ma FROM MissionAttribute ma WHERE ma.conservationArea = :conservationArea)") //$NON-NLS-1$
		.setParameter("conservationArea", ca) //$NON-NLS-1$
		.executeUpdate();
		
		//survey waypoints
		session.createMutationQuery(
				"DELETE SurveyWaypoint sw WHERE sw.id.waypoint IN " + //$NON-NLS-1$
				"(SELECT wp FROM Waypoint wp WHERE wp.conservationArea = :conservationArea)") //$NON-NLS-1$
		.setParameter("conservationArea", ca) //$NON-NLS-1$
		.executeUpdate();		

		//mission members 
		session.createMutationQuery(
				"DELETE MissionMember mp WHERE mp.id.mission IN " + //$NON-NLS-1$
				"(SELECT ma FROM Mission ma join ma.survey s join s.surveyDesign sd WHERE sd.conservationArea = :conservationArea)") //$NON-NLS-1$
		.setParameter("conservationArea", ca) //$NON-NLS-1$
		.executeUpdate();

		//mission tracks 
		session.createMutationQuery(
				"DELETE MissionTrack mt WHERE mt.missionDay IN (SELECT md FROM MissionDay md WHERE md.mission IN " + //$NON-NLS-1$
				"(SELECT ma FROM Mission ma join ma.survey s join s.surveyDesign sd WHERE sd.conservationArea = :conservationArea))") //$NON-NLS-1$
		.setParameter("conservationArea", ca) //$NON-NLS-1$
		.executeUpdate();

		//mission days
		session.createMutationQuery(
				"DELETE MissionDay md WHERE md.mission IN " + //$NON-NLS-1$
				"(SELECT ma FROM Mission ma join ma.survey s join s.surveyDesign sd WHERE sd.conservationArea = :conservationArea)") //$NON-NLS-1$
		.setParameter("conservationArea", ca) //$NON-NLS-1$
		.executeUpdate();
		
		//mission
		session.createMutationQuery(
				"delete Mission m where m.survey IN " + //$NON-NLS-1$
				"(SELECT s FROM Survey s join s.surveyDesign sd WHERE sd.conservationArea = :conservationArea)") //$NON-NLS-1$
		.setParameter("conservationArea", ca) //$NON-NLS-1$
		.executeUpdate();
	}
	
	private void deleteSurveys(ConservationArea ca, Session session) throws Exception{
		//entity attribute values
		session.createMutationQuery(
				"delete Survey s where s IN " + //$NON-NLS-1$
				"(SELECT s FROM Survey s WHERE s.surveyDesign.conservationArea = :conservationArea)") //$NON-NLS-1$
		.setParameter("conservationArea", ca) //$NON-NLS-1$
		.executeUpdate();
	}

	private void deleteSurveyDesign(ConservationArea ca, Session session) throws Exception{
		//sampling unit attribue values
		session.createMutationQuery(
				"DELETE SamplingUnitAttributeValue s where s.id.samplingUnit IN " + //$NON-NLS-1$
				"(SELECT su FROM SamplingUnit su join su.surveyDesign sd WHERE sd.conservationArea = :conservationArea)") //$NON-NLS-1$
		.setParameter("conservationArea", ca) //$NON-NLS-1$
		.executeUpdate();
		
		//sampling units
		session.createMutationQuery(
				"delete SamplingUnit s where s.surveyDesign IN " + //$NON-NLS-1$
				"(SELECT sd FROM SurveyDesign sd WHERE sd.conservationArea = :conservationArea)") //$NON-NLS-1$
		.setParameter("conservationArea", ca) //$NON-NLS-1$
		.executeUpdate();
		
		//sampling unit attribute to survey design links
		session.createMutationQuery(
				"DELETE SurveyDesignSamplingUnitAttribute s where s.id.surveyDesign IN " + //$NON-NLS-1$
				"(SELECT sd FROM SurveyDesign sd WHERE sd.conservationArea = :conservationArea)") //$NON-NLS-1$
		.setParameter("conservationArea", ca) //$NON-NLS-1$
		.executeUpdate();
		
		// mission properties
		session.createMutationQuery("DELETE MissionProperty mp where mp.id.surveyDesign IN " + //$NON-NLS-1$
			"(SELECT sd FROM SurveyDesign sd WHERE sd.conservationArea = :conservationArea)") //$NON-NLS-1$
		.setParameter("conservationArea", ca) //$NON-NLS-1$
		.executeUpdate();
				
		//survey design properties
		session.createMutationQuery("DELETE SurveyDesignProperty p where p.surveyDesign IN " + //$NON-NLS-1$
				"(SELECT sd FROM SurveyDesign sd WHERE sd.conservationArea = :conservationArea)") //$NON-NLS-1$
		.setParameter("conservationArea", ca) //$NON-NLS-1$
		.executeUpdate();
				
		//survey design			.
		session.createMutationQuery(
				"delete FROM SurveyDesign where conservationArea = :conservationArea")//$NON-NLS-1$
		.setParameter("conservationArea", ca) //$NON-NLS-1$
		.executeUpdate();
	}
	
	private void deleteMissionAttributes(ConservationArea ca, Session session) throws Exception{
		
		session.createMutationQuery(
				"DELETE MissionAttributeListItem li WHERE li.attribute IN " +//$NON-NLS-1$
				"(SELECT ma FROM MissionAttribute ma WHERE ma.conservationArea = :conservationArea)") //$NON-NLS-1$
		.setParameter("conservationArea", ca) //$NON-NLS-1$
		.executeUpdate();
		
		session.createMutationQuery(
				"DELETE FROM MissionAttribute WHERE conservationArea = :conservationArea") //$NON-NLS-1$
		.setParameter("conservationArea", ca) //$NON-NLS-1$
		.executeUpdate();
	}
	
	private void deleteSamplingUnitAttributes(ConservationArea ca, Session session) throws Exception{
		session.createMutationQuery(
				"DELETE SamplingUnitAttributeListItem li WHERE li.attribute IN " +//$NON-NLS-1$
				"(SELECT sa FROM SamplingUnitAttribute sa WHERE sa.conservationArea = :conservationArea)") //$NON-NLS-1$
		.setParameter("conservationArea", ca) //$NON-NLS-1$
		.executeUpdate();
		
		session.createMutationQuery(
				"DELETE FROM SamplingUnitAttribute WHERE conservationArea = :conservationArea") //$NON-NLS-1$
		.setParameter("conservationArea", ca) //$NON-NLS-1$
		.executeUpdate();
	}
}
