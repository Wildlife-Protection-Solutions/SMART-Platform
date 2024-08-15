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
package org.wcs.smart.patrol;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.ICaDeleteHandler;
import org.wcs.smart.patrol.internal.Messages;

/**
 * Delete handler for deleting all patrol information attached 
 * to the conservation area.
 * <p>This deletes all patrols, the patrol options,
 * the patrol type, teams, and mandates.</p>
 * @author Emily
 * @since 1.0.0
 */
public class PatrolCaDeleteHandler implements ICaDeleteHandler{

	/**
	 * To be executed before the conservation area is deleted
	 */
	public static final int EXECUTE_ORDER = 32;
	
	/* (non-Javadoc)
	 * @see org.wcs.smart.ca.ICaDeleteListener#beforeDelete(org.wcs.smart.ca.ConservationArea, org.hibernate.Session)
	 */
	@Override
	public void beforeDelete(ConservationArea ca, Session session, IProgressMonitor monitor)
			throws Exception {
		monitor.subTask(Messages.PatrolCaDeleteHandler_DeletePatrolAttributes);
		deleteCustomAttributes(ca, session);	
		monitor.subTask(Messages.PatrolCaDeleteHandler_Progress_DeletingPatrols);
		deletePatrols(ca, session);
		monitor.subTask(Messages.PatrolCaDeleteHandler_Progress_DeletingTeams);
		deletePatrolTeams(ca, session);
		monitor.subTask(Messages.PatrolCaDeleteHandler_Progress_DeletingMandates);
		deleteMandates(ca, session);		
		monitor.subTask(Messages.PatrolCaDeleteHandler_Progress_DeletingTypes1);
		deletePatrolTypes(ca, session);		
	}

	private void deleteCustomAttributes(ConservationArea ca, Session session) throws Exception{
		session.createMutationQuery("delete from PatrolAttributeValue where id.patrolAttribute in (FROM PatrolAttribute WHERE conservationArea = :ca)") //$NON-NLS-1$
			.setParameter("ca", ca) //$NON-NLS-1$
			.executeUpdate();
		
		session.createMutationQuery("delete from PatrolAttributeListItem where attribute in (FROM PatrolAttribute WHERE conservationArea = :ca)") //$NON-NLS-1$
			.setParameter("ca", ca) //$NON-NLS-1$
			.executeUpdate();
		
		session.createMutationQuery("delete from PatrolAttribute where conservationArea = :ca") //$NON-NLS-1$
			.setParameter("ca", ca) //$NON-NLS-1$
			.executeUpdate();
	}
	
	private void deletePatrols(ConservationArea ca, Session session) throws Exception{
		session.createMutationQuery("delete from Patrol where conservationArea = :ca") //$NON-NLS-1$
			.setParameter("ca", ca) //$NON-NLS-1$
			.executeUpdate();
	}

	private void deleteMandates(ConservationArea ca, Session session) throws Exception{
		session.createMutationQuery("delete from PatrolMandate where conservationArea = :ca") //$NON-NLS-1$
			.setParameter("ca", ca) //$NON-NLS-1$
			.executeUpdate();
	}
	
	private void deletePatrolTypes(ConservationArea ca, Session session) throws Exception{
		session.createMutationQuery("delete from PatrolType where id.conservationArea = :ca") //$NON-NLS-1$
			.setParameter("ca", ca) //$NON-NLS-1$
			.executeUpdate();
		
		session.createMutationQuery("delete from PatrolTransportType where conservationArea = :ca") //$NON-NLS-1$
			.setParameter("ca", ca) //$NON-NLS-1$
			.executeUpdate();
		
	}
	
	private void deletePatrolTeams(ConservationArea ca, Session session) throws Exception{
		session.createMutationQuery("delete from Team where conservationArea = :ca") //$NON-NLS-1$
			.setParameter("ca", ca) //$NON-NLS-1$
			.executeUpdate();
	}
}
