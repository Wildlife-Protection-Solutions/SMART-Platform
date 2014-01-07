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
package org.wcs.smart.plan.updatesite;

import java.sql.Connection;
import java.sql.SQLException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.plan.internal.Messages;

/**
 * Job removes all plan related tabled from the database
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class RemovePlanJob extends Job {

	public RemovePlanJob() {
		super(Messages.RemovePlanJob_Title);
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		final Session session = HibernateManager.openSession();
		session.beginTransaction();
		try {
			session.doWork(new Work() {
				@Override
				public void execute(Connection c) throws SQLException {
					try {
						//delete labels
						c.createStatement().execute("delete FROM smart.I18N_LABEL where ELEMENT_UUID in (select uuid from smart.plan)"); //$NON-NLS-1$

						//delete actual tables
						c.createStatement().execute("DROP TABLE smart.patrol_plan"); //$NON-NLS-1$
						c.createStatement().execute("DROP TABLE smart.plan_target_point"); //$NON-NLS-1$
						c.createStatement().execute("DROP TABLE smart.plan_target"); //$NON-NLS-1$
						c.createStatement().execute("DROP TABLE smart.plan"); //$NON-NLS-1$
					} catch (final Exception ex) {
						SmartPlugIn.log(Messages.RemovePlanJob_Error, ex);
					}
					
				}
			});
			
		} catch (Exception e) {
			SmartPlugIn.log(Messages.RemovePlanJob_Error, e);
		} finally {
			try {
				session.getTransaction().commit();
			} catch (Exception ex) {
				SmartPlugIn.log(ex.getMessage(), ex);
			}
			try {
				session.close();
			} catch (Exception ex) {
				SmartPlugIn.log(ex.getMessage(), ex);
			}
		}
		
		return Status.OK_STATUS;
	}

}
