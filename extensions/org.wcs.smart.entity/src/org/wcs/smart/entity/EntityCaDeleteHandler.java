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
package org.wcs.smart.entity;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.ICaDeleteHandler;
import org.wcs.smart.entity.internal.Messages;
import org.wcs.smart.observation.CaDeleteHandler;

/**
 * Delete handler for deleting all entity information and
 * attributes when a conservation area is deleted.
 * 
 * 
 * @author Emily
 * @since 1.0.0
 */
public class EntityCaDeleteHandler implements ICaDeleteHandler{

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
		monitor.subTask(Messages.EntityCaDeleteHandler_ProgressDeleteAttributes);
		deleteEntityAttributes(ca, session);
		monitor.subTask(Messages.EntityCaDeleteHandler_ProgressDeleteEntities);
		deleteEntities(ca, session);
		monitor.subTask(Messages.EntityCaDeleteHandler_ProgressDeleteType);
		deleteTypes(ca, session);
				
	}

	private void deleteEntityAttributes(ConservationArea ca, Session session) throws Exception{
		//entity attribute values
		Query q = session.createQuery(
				"delete EntityAttributeValue e where e.id.entity IN " + //$NON-NLS-1$
				"(SELECT a from Entity a WHERE a.entityType.conservationArea = :conservationArea)"); //$NON-NLS-1$
		q.setParameter("conservationArea", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		//entity attributes
		q = session.createQuery("delete EntityAttribute e where e.entityType IN " +  //$NON-NLS-1$
				"(SELECT a FROM EntityType a WHERE a.conservationArea = :conservationArea) "); //$NON-NLS-1$
		q.setParameter("conservationArea", ca); //$NON-NLS-1$
		q.executeUpdate();
	}
	
	private void deleteEntities(ConservationArea ca, Session session) throws Exception{
		Query q = session.createQuery("delete Entity e WHERE e.entityType IN "+ //$NON-NLS-1$
				"(SELECT a FROM EntityType a WHERE a.conservationArea = :conservationArea )"); //$NON-NLS-1$
		q.setParameter("conservationArea", ca); //$NON-NLS-1$
		q.executeUpdate();
	}

	private void deleteTypes(ConservationArea ca, Session session) throws Exception{
		Query q = session.createQuery("delete EntityType e where e.conservationArea = :conservationArea "); //$NON-NLS-1$
		q.setParameter("conservationArea", ca); //$NON-NLS-1$
		q.executeUpdate();
	}
	
}
