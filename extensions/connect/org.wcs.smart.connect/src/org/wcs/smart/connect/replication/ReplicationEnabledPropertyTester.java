
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
package org.wcs.smart.connect.replication;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.progress.WorkbenchJob;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;

/**
 * Property tester for replication properties. 
 * Supports a single property: isEnabled which can be true or false depending on if 
 * replication is enabled for the current conservation area or not.
 * @author Emily
 *
 */
public class ReplicationEnabledPropertyTester extends PropertyTester {

	@Override
	public boolean test(Object receiver, String property, Object[] args,
			Object expectedValue) {

		if (property.equals("isEnabled")){ //$NON-NLS-1$
			//querying the database for the state causes session issues in other
			//unless done in a job, and this is not necessary
			final boolean[] isEnabled = new boolean[]{false};
			Job j = new Job("replicationstatus") {
				
				@Override
				public IStatus run(IProgressMonitor monitor) {
					Session s = HibernateManager.openSession();
					isEnabled[0] = DerbyReplicationManager.INSTANCE.isReplicationEnabled(SmartDB.getCurrentConservationArea().getUuid(), s);
					return Status.OK_STATUS;
				}
			};
			j.schedule();
			try {
				j.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			}
			return isEnabled[0];	
		}return false;
	}

}
