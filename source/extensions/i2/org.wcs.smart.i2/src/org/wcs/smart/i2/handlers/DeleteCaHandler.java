package org.wcs.smart.i2.handlers;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.DeleteConservationAreaHandler;
import org.wcs.smart.ca.ICaDeleteHandler;

public class DeleteCaHandler implements ICaDeleteHandler{

	public static final int EXECUTE_ORDER = DeleteConservationAreaHandler.EXECUTE_ORDER + 1;
	
	@Override
	public void beforeDelete(ConservationArea ca, Session session,
			IProgressMonitor monitor) throws Exception {
		//labels are dealt with by core Conservation Area delete engine 
		
		monitor.subTask("Deleting Intelligence Plugin Data...");
		
		Query q = session.createQuery("delete from IntelObservationAttribute ioa where ioa.id.attribute in (select a from IntelAttribute a where conservationArea = :ca)"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		q = session.createQuery("delete from IntelObservation io where io.location in (from IntelLocation where conservationArea = :ca)"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		q = session.createQuery("delete from IntelEntityLocation iel where iel.id.location in (from IntelLocation where conservationArea = :ca)"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
				
		q = session.createQuery("delete from IntelLocation where conservationArea = :ca"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		q = session.createQuery("delete from IntelDatamodelEvent where conservationArea = :ca"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		q = session.createQuery("delete from IntelEntitySearch where conservationArea = :ca"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		q = session.createQuery("delete from IntelEntityAttributeValue ieav where ieav.id.attribute in (FROM IntelAttribute WHERE conservationArea = :ca)"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		q = session.createQuery("delete from IntelRelationshipTypeAttribute ii where ii.id.attribute in (from IntelAttribute WHERE conservationArea = :ca)"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		q = session.createQuery("delete from IntelEntityTypeAttribute ii where ii.id.attribute in (from IntelAttribute WHERE conservationArea = :ca)"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();	
		
		q = session.createQuery("delete from IntelEntityRecord ii where ii.id.record in (from IntelRecord where conservationArea = :ca)"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		q = session.createQuery("delete from IntelRecordAttachment ii where ii.id.record in (from IntelRecord where conservationArea = :ca)"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		q = session.createQuery("delete from IntelWorkingSetRecord ii where ii.id.workingSet in (from IntelWorkingSet where conservationArea = :ca)"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		q = session.createQuery("delete from IntelWorkingSetQuery ii where ii.id.workingSet in (from IntelWorkingSet where conservationArea = :ca)"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		q = session.createQuery("delete from IntelWorkingSetEntity ii where ii.id.workingSet in (from IntelWorkingSet where conservationArea = :ca)"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		q = session.createQuery("delete from IntelWorkingSet where conservationArea = :ca"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		q = session.createQuery("delete from IntelEntityRelationship ii where ii.relationshipType in (from IntelRelationshipType where conservationArea = :ca)"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		q = session.createQuery("delete from IntelEntityRelationshipAttributeValue ii where ii.id.attribute in (from IntelAttribute where conservationArea = :ca)"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		q = session.createQuery("delete from IntelRelationshipType where conservationArea = :ca"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		q = session.createQuery("delete from IntelRelationshipGroup where conservationArea = :ca"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		q = session.createQuery("delete from IntelAttachment where conservationArea = :ca"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		q = session.createQuery("delete from IntelAttributeListItem ii where ii.attribute in (from IntelAttribute where conservationArea = :ca)"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		q = session.createQuery("delete from IntelAttribute where conservationArea = :ca"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
	
		q = session.createQuery("delete from IntelEntityAttachment ii where ii.id.attachment in (from IntelAttachment where conservationArea = :ca)"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		q = session.createQuery("delete from IntelEntity ii where ii.entityType in (from IntelEntityType where conservationArea = :ca)"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		q = session.createQuery("delete from IntelRecord where conservationArea = :ca"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		q = session.createQuery("delete from IntelRecordQuery where conservationArea = :ca"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		q = session.createQuery("delete from IntelEntityType where conservationArea = :ca"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
	}
	
	

}
