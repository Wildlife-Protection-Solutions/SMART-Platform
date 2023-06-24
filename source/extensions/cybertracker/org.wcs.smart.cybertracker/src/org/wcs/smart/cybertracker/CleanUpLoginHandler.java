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
package org.wcs.smart.cybertracker;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.wcs.smart.ILoginHandler;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * Login handler to clean up ct_incident_link data based on the last
 * modified date of the waypoint.  If the waypoint has not been modified in
 * the last year then any data in the ct_incident_link table is also removed.
 * 
 * @author Emily
 *
 */
public class CleanUpLoginHandler implements ILoginHandler {

	@Override
	public void onLogin() throws Exception {
		Job j = new Job("clean up cybertracker data") { //$NON-NLS-1$
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				cleanLinksTable();
				return Status.OK_STATUS;
			}
		};
		j.setSystem(true);
		j.schedule();
	}
	
	private void cleanLinksTable() {
		//delete anything from the ct_incident_link that links to a waypoint that is has a last modified date that is older than one year
		LocalDateTime oneYearAgo = ChronoUnit.YEARS.addTo(LocalDateTime.now(), -1);
		String hql = "DELETE FROM CtIncidentLink WHERE waypoint IN (FROM Waypoint WHERE lastModified <= :lastModified)"; //$NON-NLS-1$
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				session.createMutationQuery(hql)
					.setParameter("lastModified", oneYearAgo) //$NON-NLS-1$
					.executeUpdate();
				session.getTransaction().commit();
			}catch (Exception ex) {
				CyberTrackerPlugIn.log("Error cleaning up CtIncidentLink table:" + ex.getMessage(),  ex); //$NON-NLS-1$
				session.getTransaction().rollback();
			}
		}
	}
}
