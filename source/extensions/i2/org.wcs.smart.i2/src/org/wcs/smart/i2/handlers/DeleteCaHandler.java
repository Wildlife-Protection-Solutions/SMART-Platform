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

/**
 * Deletes the various components of the intelligence plugin
 * 
 * @author Emily
 *
 */
public class DeleteCaHandler implements ICaDeleteHandler{

	public static final int EXECUTE_ORDER = DeleteConservationAreaHandler.EXECUTE_ORDER + 1;
	
	@Override
	public void beforeDelete(ConservationArea ca, Session session,
			IProgressMonitor monitor) throws Exception {
		//labels are dealt with by core Conservation Area delete engine 
		
		monitor.subTask("Deleting Intelligence Plugin Data...");
		
		monitor.subTask(MessageFormat.format("Deleting Intelligence Plugin Data ({0}) ...", "IntelObservationAttribute"));
		Query q = session.createQuery("delete from IntelObservationAttribute ioa where ioa.id.attribute in (select a from Attribute a where conservationArea = :ca)"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		monitor.subTask(MessageFormat.format("Deleting Intelligence Plugin Data ({0}) ...", "IntelObservation"));
		q = session.createQuery("delete from IntelObservation io where io.location in (from IntelLocation where conservationArea = :ca)"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		monitor.subTask(MessageFormat.format("Deleting Intelligence Plugin Data ({0}) ...", "IntelEntityLocation"));
		q = session.createQuery("delete from IntelEntityLocation iel where iel.id.location in (from IntelLocation where conservationArea = :ca)"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
				
		monitor.subTask(MessageFormat.format("Deleting Intelligence Plugin Data ({0}) ...", "IntelLocation"));
		q = session.createQuery("delete from IntelLocation where conservationArea = :ca"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		monitor.subTask(MessageFormat.format("Deleting Intelligence Plugin Data ({0}) ...", "IntelDatamodelEvent"));
		q = session.createQuery("delete from IntelDatamodelEvent where conservationArea = :ca"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		monitor.subTask(MessageFormat.format("Deleting Intelligence Plugin Data ({0}) ...", "IntelEntitySearch"));
		q = session.createQuery("delete from IntelEntitySearch where conservationArea = :ca"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		monitor.subTask(MessageFormat.format("Deleting Intelligence Plugin Data ({0}) ...", "IntelEntityAttributeValue"));
		q = session.createQuery("delete from IntelEntityAttributeValue ieav where ieav.id.attribute in (FROM IntelAttribute WHERE conservationArea = :ca)"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		monitor.subTask(MessageFormat.format("Deleting Intelligence Plugin Data ({0}) ...", "IntelRelationshipTypeAttribute"));
		q = session.createQuery("delete from IntelRelationshipTypeAttribute ii where ii.id.attribute in (from IntelAttribute WHERE conservationArea = :ca)"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		monitor.subTask(MessageFormat.format("Deleting Intelligence Plugin Data ({0}) ...", "IntelEntityTypeAttribute"));
		q = session.createQuery("delete from IntelEntityTypeAttribute ii where ii.id.attribute in (from IntelAttribute WHERE conservationArea = :ca)"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();	

		monitor.subTask(MessageFormat.format("Deleting Intelligence Plugin Data ({0}) ...", "IntelEntityTypeAttributeGroup"));
		q = session.createQuery("delete from IntelEntityTypeAttributeGroup g where g.entityType in (FROM IntelEntityType WHERE conservationArea = :ca)"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		monitor.subTask(MessageFormat.format("Deleting Intelligence Plugin Data ({0}) ...", "IntelEntityRecord"));
		q = session.createQuery("delete from IntelEntityRecord ii where ii.id.record in (from IntelRecord where conservationArea = :ca)"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		monitor.subTask(MessageFormat.format("Deleting Intelligence Plugin Data ({0}) ...", "IntelRecordAttachment"));
		q = session.createQuery("delete from IntelRecordAttachment ii where ii.id.record in (from IntelRecord where conservationArea = :ca)"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		monitor.subTask(MessageFormat.format("Deleting Intelligence Plugin Data ({0}) ...", "IntelWorkingSetRecord"));
		q = session.createQuery("delete from IntelWorkingSetRecord ii where ii.id.workingSet in (from IntelWorkingSet where conservationArea = :ca)"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		monitor.subTask(MessageFormat.format("Deleting Intelligence Plugin Data ({0}) ...", "IntelWorkingSetQuery"));
		q = session.createQuery("delete from IntelWorkingSetQuery ii where ii.id.workingSet in (from IntelWorkingSet where conservationArea = :ca)"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		monitor.subTask(MessageFormat.format("Deleting Intelligence Plugin Data ({0}) ...", "IntelWorkingSetEntity"));
		q = session.createQuery("delete from IntelWorkingSetEntity ii where ii.id.workingSet in (from IntelWorkingSet where conservationArea = :ca)"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		monitor.subTask(MessageFormat.format("Deleting Intelligence Plugin Data ({0}) ...", "IntelWorkingSet"));
		q = session.createQuery("delete from IntelWorkingSet where conservationArea = :ca"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();

		monitor.subTask(MessageFormat.format("Deleting Intelligence Plugin Data ({0}) ...", "IntelEntityRelationshipAttributeValue"));
		q = session.createQuery("delete from IntelEntityRelationshipAttributeValue ii where ii.id.attribute in (from IntelAttribute where conservationArea = :ca)"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();

		
		monitor.subTask(MessageFormat.format("Deleting Intelligence Plugin Data ({0}) ...", "IntelEntityRelationship"));
		q = session.createQuery("delete from IntelEntityRelationship ii where ii.relationshipType in (from IntelRelationshipType where conservationArea = :ca)"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		monitor.subTask(MessageFormat.format("Deleting Intelligence Plugin Data ({0}) ...", "IntelRelationshipType"));
		q = session.createQuery("delete from IntelRelationshipType where conservationArea = :ca"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		monitor.subTask(MessageFormat.format("Deleting Intelligence Plugin Data ({0}) ...", "IntelRelationshipGroup"));
		q = session.createQuery("delete from IntelRelationshipGroup where conservationArea = :ca"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		monitor.subTask(MessageFormat.format("Deleting Intelligence Plugin Data ({0}) ...", "IntelEntityAttachment"));
		q = session.createQuery("delete from IntelEntityAttachment ii where ii.id.attachment in (from IntelAttachment where conservationArea = :ca)"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		monitor.subTask(MessageFormat.format("Deleting Intelligence Plugin Data ({0}) ...", "IntelAttachment"));
		q = session.createQuery("delete from IntelAttachment where conservationArea = :ca"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		monitor.subTask(MessageFormat.format("Deleting Intelligence Plugin Data ({0}) ...", "IntelAttributeListItem"));
		q = session.createQuery("delete from IntelAttributeListItem ii where ii.attribute in (from IntelAttribute where conservationArea = :ca)"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		monitor.subTask(MessageFormat.format("Deleting Intelligence Plugin Data ({0}) ...", "IntelEntity"));
		q = session.createQuery("delete from IntelEntity ii where ii.entityType in (from IntelEntityType where conservationArea = :ca)"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		monitor.subTask(MessageFormat.format("Deleting Intelligence Plugin Data ({0}) ...", "IntelRecordAttributeValue"));
		q = session.createQuery("delete from IntelRecordAttributeValue ii where ii.record in (from IntelRecord where conservationArea = :ca)"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		monitor.subTask(MessageFormat.format("Deleting Intelligence Plugin Data ({0}) ...", "IntelRecord"));
		q = session.createQuery("delete from IntelRecord where conservationArea = :ca"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		monitor.subTask(MessageFormat.format("Deleting Intelligence Plugin Data ({0}) ...", "IntelRecordObservationQuery"));
		q = session.createQuery("delete from IntelRecordObservationQuery where conservationArea = :ca"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		monitor.subTask(MessageFormat.format("Deleting Intelligence Plugin Data ({0}) ...", "IntelRecordSourceAttribute"));
		q = session.createQuery("delete from IntelRecordSourceAttribute ii where ii.source in (from IntelRecordSource where conservationArea = :ca)"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		monitor.subTask(MessageFormat.format("Deleting Intelligence Plugin Data ({0}) ...", "IntelRecordSource"));
		q = session.createQuery("delete from IntelRecordSource where conservationArea = :ca"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		monitor.subTask(MessageFormat.format("Deleting Intelligence Plugin Data ({0}) ...", "IntelEntityType"));
		q = session.createQuery("delete from IntelEntityType where conservationArea = :ca"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();

		monitor.subTask(MessageFormat.format("Deleting Intelligence Plugin Data ({0}) ...", "IntelAttribute"));
		q = session.createQuery("delete from IntelAttribute where conservationArea = :ca"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
	
	}
	
	

}
