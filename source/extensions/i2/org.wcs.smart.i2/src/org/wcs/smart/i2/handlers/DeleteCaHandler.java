/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2.handlers;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.DeleteConservationAreaHandler;
import org.wcs.smart.ca.ICaDeleteHandler;
import org.wcs.smart.i2.internal.Messages;

/**
 * Deletes the various components of the intelligence plugin
 * 
 * @author Emily
 *
 */
public class DeleteCaHandler implements ICaDeleteHandler{

	public static final int EXECUTE_ORDER = DeleteConservationAreaHandler.EXECUTE_ORDER + 1;
	
	private static final String SUB_TASK_MSG = Messages.DeleteCaHandler_TaskName;
	
	@Override
	public void beforeDelete(ConservationArea ca, Session session,
			IProgressMonitor monitor) throws Exception {
		//labels are dealt with by core Conservation Area delete engine 
		
		monitor.subTask(Messages.DeleteCaHandler_SubTaskName);
		
		monitor.subTask(MessageFormat.format(SUB_TASK_MSG, "IntelObservationAttribute")); //$NON-NLS-1$
		Query q = session.createQuery("delete from IntelObservationAttribute ioa where ioa.id.attribute in (select a from Attribute a where conservationArea = :ca)"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		monitor.subTask(MessageFormat.format(SUB_TASK_MSG, "IntelObservation")); //$NON-NLS-1$
		q = session.createQuery("delete from IntelObservation io where io.location in (from IntelLocation where conservationArea = :ca)"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		monitor.subTask(MessageFormat.format(SUB_TASK_MSG, "IntelEntityLocation")); //$NON-NLS-1$
		q = session.createQuery("delete from IntelEntityLocation iel where iel.id.location in (from IntelLocation where conservationArea = :ca)"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
				
		monitor.subTask(MessageFormat.format(SUB_TASK_MSG, "IntelLocation")); //$NON-NLS-1$
		q = session.createQuery("delete from IntelLocation where conservationArea = :ca"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		monitor.subTask(MessageFormat.format(SUB_TASK_MSG, "IntelEntitySearch")); //$NON-NLS-1$
		q = session.createQuery("delete from IntelEntitySearch where conservationArea = :ca"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		monitor.subTask(MessageFormat.format(SUB_TASK_MSG, "IntelEntityAttributeValue")); //$NON-NLS-1$
		q = session.createQuery("delete from IntelEntityAttributeValue ieav where ieav.id.attribute in (FROM IntelAttribute WHERE conservationArea = :ca)"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		monitor.subTask(MessageFormat.format(SUB_TASK_MSG, "IntelRelationshipTypeAttribute")); //$NON-NLS-1$
		q = session.createQuery("delete from IntelRelationshipTypeAttribute ii where ii.id.attribute in (from IntelAttribute WHERE conservationArea = :ca)"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		monitor.subTask(MessageFormat.format(SUB_TASK_MSG, "IntelEntityTypeAttribute")); //$NON-NLS-1$
		q = session.createQuery("delete from IntelEntityTypeAttribute ii where ii.id.attribute in (from IntelAttribute WHERE conservationArea = :ca)"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();	

		monitor.subTask(MessageFormat.format(SUB_TASK_MSG, "IntelEntityTypeAttributeGroup")); //$NON-NLS-1$
		q = session.createQuery("delete from IntelEntityTypeAttributeGroup g where g.entityType in (FROM IntelEntityType WHERE conservationArea = :ca)"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		monitor.subTask(MessageFormat.format(SUB_TASK_MSG, "IntelEntityRecord")); //$NON-NLS-1$
		q = session.createQuery("delete from IntelEntityRecord ii where ii.id.record in (from IntelRecord where conservationArea = :ca)"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		monitor.subTask(MessageFormat.format(SUB_TASK_MSG, "IntelRecordAttachment")); //$NON-NLS-1$
		q = session.createQuery("delete from IntelRecordAttachment ii where ii.id.record in (from IntelRecord where conservationArea = :ca)"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		monitor.subTask(MessageFormat.format(SUB_TASK_MSG, "IntelWorkingSetRecord")); //$NON-NLS-1$
		q = session.createQuery("delete from IntelWorkingSetRecord ii where ii.id.workingSet in (from IntelWorkingSet where conservationArea = :ca)"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		monitor.subTask(MessageFormat.format(SUB_TASK_MSG, "IntelWorkingSetQuery")); //$NON-NLS-1$
		q = session.createQuery("delete from IntelWorkingSetQuery ii where ii.id.workingSet in (from IntelWorkingSet where conservationArea = :ca)"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		monitor.subTask(MessageFormat.format(SUB_TASK_MSG, "IntelWorkingSetEntity")); //$NON-NLS-1$
		q = session.createQuery("delete from IntelWorkingSetEntity ii where ii.id.workingSet in (from IntelWorkingSet where conservationArea = :ca)"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		monitor.subTask(MessageFormat.format(SUB_TASK_MSG, "IntelWorkingSet")); //$NON-NLS-1$
		q = session.createQuery("delete from IntelWorkingSet where conservationArea = :ca"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();

		monitor.subTask(MessageFormat.format(SUB_TASK_MSG, "IntelEntityRelationshipAttributeValue")); //$NON-NLS-1$
		q = session.createQuery("delete from IntelEntityRelationshipAttributeValue ii where ii.id.attribute in (from IntelAttribute where conservationArea = :ca)"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();

		
		monitor.subTask(MessageFormat.format(SUB_TASK_MSG, "IntelEntityRelationship")); //$NON-NLS-1$
		q = session.createQuery("delete from IntelEntityRelationship ii where ii.relationshipType in (from IntelRelationshipType where conservationArea = :ca)"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		monitor.subTask(MessageFormat.format(SUB_TASK_MSG, "IntelRelationshipType")); //$NON-NLS-1$
		q = session.createQuery("delete from IntelRelationshipType where conservationArea = :ca"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		monitor.subTask(MessageFormat.format(SUB_TASK_MSG, "IntelRelationshipGroup")); //$NON-NLS-1$
		q = session.createQuery("delete from IntelRelationshipGroup where conservationArea = :ca"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		monitor.subTask(MessageFormat.format(SUB_TASK_MSG, "IntelEntityAttachment")); //$NON-NLS-1$
		q = session.createQuery("delete from IntelEntityAttachment ii where ii.id.attachment in (from IntelAttachment where conservationArea = :ca)"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		monitor.subTask(MessageFormat.format(SUB_TASK_MSG, "IntelAttachment")); //$NON-NLS-1$
		q = session.createQuery("delete from IntelAttachment where conservationArea = :ca"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		monitor.subTask(MessageFormat.format(SUB_TASK_MSG, "IntelAttributeListItem")); //$NON-NLS-1$
		q = session.createQuery("delete from IntelAttributeListItem ii where ii.attribute in (from IntelAttribute where conservationArea = :ca)"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		monitor.subTask(MessageFormat.format(SUB_TASK_MSG, "IntelEntity")); //$NON-NLS-1$
		q = session.createQuery("delete from IntelEntity ii where ii.entityType in (from IntelEntityType where conservationArea = :ca)"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		monitor.subTask(MessageFormat.format(SUB_TASK_MSG, "IntelRecordAttributeValue")); //$NON-NLS-1$
		q = session.createQuery("delete from IntelRecordAttributeValue ii where ii.record in (from IntelRecord where conservationArea = :ca)"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		monitor.subTask(MessageFormat.format(SUB_TASK_MSG, "IntelRecord")); //$NON-NLS-1$
		q = session.createQuery("delete from IntelRecord where conservationArea = :ca"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		monitor.subTask(MessageFormat.format(SUB_TASK_MSG, "IntelRecordObservationQuery")); //$NON-NLS-1$
		q = session.createQuery("delete from IntelRecordObservationQuery where conservationArea = :ca"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		monitor.subTask(MessageFormat.format(SUB_TASK_MSG, "IntelRecordSourceAttribute")); //$NON-NLS-1$
		q = session.createQuery("delete from IntelRecordSourceAttribute ii where ii.source in (from IntelRecordSource where conservationArea = :ca)"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		monitor.subTask(MessageFormat.format(SUB_TASK_MSG, "IntelRecordSource")); //$NON-NLS-1$
		q = session.createQuery("delete from IntelRecordSource where conservationArea = :ca"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		monitor.subTask(MessageFormat.format(SUB_TASK_MSG, "IntelEntityType")); //$NON-NLS-1$
		q = session.createQuery("delete from IntelEntityType where conservationArea = :ca"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();

		monitor.subTask(MessageFormat.format(SUB_TASK_MSG, "IntelAttribute")); //$NON-NLS-1$
		q = session.createQuery("delete from IntelAttribute where conservationArea = :ca"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
	
	}
	
	

}
