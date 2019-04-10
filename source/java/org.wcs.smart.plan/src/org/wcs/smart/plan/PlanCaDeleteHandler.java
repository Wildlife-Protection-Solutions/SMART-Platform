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
package org.wcs.smart.plan;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.ICaDeleteHandler;
import org.wcs.smart.patrol.PatrolCaDeleteHandler;
import org.wcs.smart.plan.internal.Messages;

/**
 * Delete handler for deleting all plan information attached 
 * to the conservation area.
 * <p>This deletes all plans and plan targets.</p>
 * 
 * @author Emily
 * @since 1.0.0
 */
public class PlanCaDeleteHandler implements ICaDeleteHandler{

	/**
	 * To be executed before patrols are deleted
	 */
	public static final int EXECUTE_ORDER = PatrolCaDeleteHandler.EXECUTE_ORDER + 1;
	
	/* (non-Javadoc)
	 * @see org.wcs.smart.ca.ICaDeleteListener#beforeDelete(org.wcs.smart.ca.ConservationArea, org.hibernate.Session)
	 */
	@Override
	public void beforeDelete(ConservationArea ca, Session session, IProgressMonitor monitor)
			throws Exception {
		monitor.subTask(Messages.PlanCaDeleteHandler_DeletePatrolPlan_SubTasl);
		deletePatrolPlan(ca, session);
		monitor.subTask(Messages.PlanCaDeleteHandler_DeleteTargets_SubTask);
		deletePlanTargets(ca, session);
		monitor.subTask(Messages.PlanCaDeleteHandler_DeletePlans_SubTask);
		deletePlans(ca, session);
				
	}

	private void deletePatrolPlan(ConservationArea ca, Session session) throws Exception{
		Query<?> q = session.createQuery("delete PatrolPlan p where p.id.plan in (select pp.id.plan from PatrolPlan pp where pp.id.plan.conservationArea = :ca)"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
	}
	
	private void deletePlans(ConservationArea ca, Session session) throws Exception{
		Query<?> q = session.createQuery("delete from Plan where conservationArea = :ca"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
	}

	private void deletePlanTargets(ConservationArea ca, Session session) throws Exception{
		Query<?> q = session.createQuery("delete from PlanTarget pt WHERE pt in (SELECT pt2.uuid FROM PlanTarget pt2 where pt2.plan.conservationArea = :ca)"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
	}
	
}
