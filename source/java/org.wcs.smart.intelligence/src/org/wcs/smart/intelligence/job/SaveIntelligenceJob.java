package org.wcs.smart.intelligence.job;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.wcs.smart.intelligence.IntelligenceHibernateManager;
import org.wcs.smart.intelligence.internal.Messages;
import org.wcs.smart.intelligence.model.Intelligence;

/**
 * Job is used to save intelligence object
 * 
 * @author elitvin
 *
 */
public class SaveIntelligenceJob extends Job {

	private Intelligence intelligence;
	
    public SaveIntelligenceJob(Intelligence intelligence) {
        super(Messages.NewIntelligenceWizard_SaveIntelligenceJob_Title);
        this.intelligence = intelligence;
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
        if (IntelligenceHibernateManager.saveIntelligence(intelligence)) {
        	return Status.OK_STATUS;
        }
        //no need to use other status as hibernate manager will report error in case something is wrong
        return Status.CANCEL_STATUS;
    }
}