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
package org.wcs.smart.query;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.ICaDeleteHandler;

/**
 * Conservation area delete handler for queries and folders
 * @author egouge
 * @since 1.0.0
 */
public class QueryCaDeleteHandler implements ICaDeleteHandler {

	/**
	 * To be executed before the conservation area is deleted
	 */
	public static final int EXECUTE_ORDER = 1;
	
	/* (non-Javadoc)
	 * @see org.wcs.smart.ca.ICaDeleteListener#beforeDelete(org.wcs.smart.ca.ConservationArea, org.hibernate.Session)
	 */
	@Override
	public void beforeDelete(ConservationArea ca, Session session, IProgressMonitor monitor)
			throws Exception {
		monitor.subTask("Deleting Observation Queries");
		deleteObservationQueries(ca, session);
		monitor.subTask("Deleting Summary Queries");
		deleteSummaryQueries(ca, session);
		//TODO: uncomment this after patrol queries added
//		monitor.subTask("Deleting Patrol Queries");
//		deletePatrolQueries(ca, session);		
		monitor.subTask("Deleting Query Folders");
		deleteQueryFolders(ca, session);		
	}

	private void deleteObservationQueries(ConservationArea ca, Session session) throws Exception{
		Query q = session.createQuery("delete from ObservationQuery where conservationArea = :ca");
		q.setParameter("ca", ca);
		q.executeUpdate();
	}

	private void deleteSummaryQueries(ConservationArea ca, Session session) throws Exception{
		Query q = session.createQuery("delete from SummaryQuery where conservationArea = :ca");
		q.setParameter("ca", ca);
		q.executeUpdate();
	}
	//TODO: uncomment htis after patrol queries added
//	private void deletePatrolQueries(ConservationArea ca, Session session) throws Exception{
//		Query q = session.createQuery("delete from PatrolQuery where conservationArea = :ca");
//		q.setParameter("ca", ca);
//		q.executeUpdate();
//	}
	
	private void deleteQueryFolders(ConservationArea ca, Session session) throws Exception{
		Query q = session.createQuery("delete from QueryFolder where conservationArea = :ca");
		q.setParameter("ca", ca);
		q.executeUpdate();
	}
	
	

}
