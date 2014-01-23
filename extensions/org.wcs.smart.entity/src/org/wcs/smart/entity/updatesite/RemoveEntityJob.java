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
package org.wcs.smart.entity.updatesite;

import java.sql.Connection;
import java.sql.SQLException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.entity.EntityPlugIn;
import org.wcs.smart.entity.internal.Messages;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * Job removes all entity plug-in related tabled from the database
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class RemoveEntityJob extends Job {

	@SuppressWarnings("nls")
	private static String[] TABLES = new String[]{
		"DROP TABLE SMART.ENTITY",
		"DROP TABLE SMART.ENTITY_ATTRIBUTE",
		"DROP TABLE SMART.ENTITY_ATTRIBUTE_VALUE",
		"DROP TABLE SMART.ENTITY_TYPE",
	};
	
	public RemoveEntityJob() {
		super(Messages.RemoveEntityJob_JobName);
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		return dropTables();
	}

	
	private IStatus dropTables(){
		Session session = HibernateManager.openSession();
		//check is required table exists
		try {
			session.beginTransaction();
			session.doWork(new Work() {
				@Override
				public void execute(Connection connection) throws SQLException {
					for (int i = 0; i < TABLES.length; i ++){
						connection.createStatement().execute(TABLES[i]);
					}	
				}
			});			
			session.getTransaction().commit();
		} catch (Exception e) {
			EntityPlugIn.displayLog("Error un-installing entity module.  Could not remove database tables.", e);
			return new Status(IStatus.ERROR, EntityPlugIn.PLUGIN_ID, 1, "", null); //$NON-NLS-1$
		} finally {
			if (session.getTransaction().isActive()) {
				session.getTransaction().rollback();
			}
			session.close();
		}
		return Status.OK_STATUS;
	}
}
