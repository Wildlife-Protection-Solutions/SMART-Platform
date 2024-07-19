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
package org.wcs.smart.ca;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.wcs.smart.internal.Messages;

/**
 * Handler that deletes all conservation area related information
 * that is not linked to the conservation area class.
 * 
 * 
 * @author Emily
 * @since 1.0.0
 */
public class DeleteConservationAreaHandler implements ICaDeleteHandler{

	public static final int EXECUTE_ORDER = 0;
	/**
	 * @see org.wcs.smart.ca.ICaDeleteHandler#beforeDelete(org.wcs.smart.ca.ConservationArea)
	 */
	@Override
	public void beforeDelete(ConservationArea ca, Session session, IProgressMonitor monitor) throws Exception {
		
		monitor.subTask(Messages.DeleteConservationAreaHandler_deleteTeamsTaskName);
		deleteEmployeeTeams(ca, session);
		monitor.subTask(Messages.DeleteConservationAreaHandler_Progress_Employees);
		deleteEmployees(ca, session);
		monitor.subTask(Messages.DeleteConservationAreaHandler_Progress_AgencyRank);
		deleteAgencyRanks(ca, session);
		monitor.subTask(Messages.DeleteConservationAreaHandler_Progress_DataModel);
		deleteDataModel(ca, session);
		monitor.subTask(Messages.DeleteConservationAreaHandler_Progress_Stations);
		deleteStations(ca, session);
		monitor.subTask(Messages.DeleteConservationAreaHandler_DeletePropertiesProgress);
		deleteCaProperties(ca, session);
		deleteSignatures(ca, session);
		deleteTags(ca, session);
		
		deleteIcons(ca, session);

	}
	
	private void deleteTags(ConservationArea ca, Session session) throws Exception{
		session.createMutationQuery("delete from AttachmentTag where conservationArea = :ca") //$NON-NLS-1$
			.setParameter("ca", ca) //$NON-NLS-1$
			.executeUpdate();
	}
	
	private void deleteSignatures(ConservationArea ca, Session session) throws Exception{
		session.createMutationQuery("delete from SignatureType where conservationArea = :ca") //$NON-NLS-1$
			.setParameter("ca", ca) //$NON-NLS-1$
			.executeUpdate();
	}
	
	private void deleteCaProperties(ConservationArea ca, Session session) throws Exception{
		session.createMutationQuery("delete from ConservationAreaProperty where conservationArea = :ca") //$NON-NLS-1$
			.setParameter("ca", ca) //$NON-NLS-1$
			.executeUpdate();
	}
	
	private void deleteAgencyRanks(ConservationArea ca, Session session) throws Exception{
		session.createMutationQuery("delete from Agency where conservationArea = :ca") //$NON-NLS-1$
			.setParameter("ca", ca) //$NON-NLS-1$
			.executeUpdate();
	}
	
	private void deleteStations(ConservationArea ca, Session session) throws Exception{
		session.createMutationQuery("delete from Station where conservationArea = :ca") //$NON-NLS-1$
			.setParameter("ca", ca) //$NON-NLS-1$
			.executeUpdate();
	}

	private void deleteEmployeeTeams(ConservationArea ca, Session session) throws Exception{
		session.createMutationQuery("delete from EmployeeTeamMember where id.team in (FROM EmployeeTeam WHERE conservationArea = :ca)") //$NON-NLS-1$
			.setParameter("ca", ca) //$NON-NLS-1$
			.executeUpdate();
		
		session.createMutationQuery("delete from EmployeeTeam where conservationArea = :ca") //$NON-NLS-1$
			.setParameter("ca", ca) //$NON-NLS-1$
			.executeUpdate();
	}
	
	private void deleteEmployees(ConservationArea ca, Session session) throws Exception{
		session.createMutationQuery("delete from Employee where conservationArea = :ca") //$NON-NLS-1$
			.setParameter("ca", ca) //$NON-NLS-1$
			.executeUpdate();
	}
	
	private void deleteDataModel(ConservationArea ca, Session session) throws Exception{	
		session.createMutationQuery("delete from Category where conservationArea = :ca") //$NON-NLS-1$
			.setParameter("ca", ca) //$NON-NLS-1$
			.executeUpdate();

		session.createMutationQuery("delete from Attribute where conservationArea = :ca") //$NON-NLS-1$
			.setParameter("ca", ca) //$NON-NLS-1$
			.executeUpdate();	
	}	

	
	private void deleteIcons(ConservationArea ca, Session session) throws Exception{
		session.createMutationQuery("delete from IconSet where conservationArea = :ca") //$NON-NLS-1$
		.setParameter("ca", ca) //$NON-NLS-1$
		.executeUpdate();
		
		//TODO: With icons I ran into a delete issue when there were too many 
		//forgein keys - maximum trigger depth reached
		//The only way I can resolve this is to drop some foreign key constraints, do the
		//Maximum depth of nested triggers was exceeded.
		//session.createNativeMutationQuery("alter table smart.station drop constraint STATION_ICON_UUID_FK").executeUpdate(); //$NON-NLS-1$
		session.createMutationQuery("delete from Icon where conservationArea = :ca") //$NON-NLS-1$
			.setParameter("ca", ca) //$NON-NLS-1$
			.executeUpdate();
		
		//session.createNativeMutationQuery("ALTER TABLE smart.station ADD CONSTRAINT STATION_ICON_UUID_FK FOREIGN KEY (icon_uuid) REFERENCES smart.icon(UUID) ON UPDATE RESTRICT ON DELETE SET NULL DEFERRABLE INITIALLY IMMEDIATE").executeUpdate(); //$NON-NLS-1$
	
	}	
}
