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
package org.wcs.smart.er.ui.mision.editor;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * Job is used to save Mission object
 * 
 * @author elitvin
 *
 */
public class SaveMissionJob extends Job {

	private Mission mission;
	
    public SaveMissionJob(Mission mission) {
        super(Messages.SaveMissionJob_Title);
        this.mission = mission;
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
		Session session = HibernateManager.openSession();
		session.beginTransaction();
		try {
			//save a name
			session.saveOrUpdate(mission);
			session.getTransaction().commit();
        	return Status.OK_STATUS;
		} catch (Exception ex) {
			session.getTransaction().rollback();
			EcologicalRecordsPlugIn.displayLog(Messages.SaveMissionJob_Error + "\n"+ ex.getLocalizedMessage(), ex); //$NON-NLS-1$
	        return Status.CANCEL_STATUS;
		} finally {
			session.close();
		}
    }
}