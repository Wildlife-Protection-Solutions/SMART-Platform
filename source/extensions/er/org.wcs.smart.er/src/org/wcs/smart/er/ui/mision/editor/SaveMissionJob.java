package org.wcs.smart.er.ui.mision.editor;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.hibernate.SmartHibernateManager;

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
		Session session = SmartHibernateManager.openSession();
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