package org.wcs.smart.intelligence.informant;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.wcs.smart.intelligence.IntelligenceHibernateManager;
import org.wcs.smart.intelligence.internal.Messages;
import org.wcs.smart.intelligence.model.Informant;

/**
 * Job is used to save informant object
 * 
 * @author elitvin
 * @since 3.2.0
 */
public class SaveInformantJob extends Job {

	private Informant informant;
	
    public SaveInformantJob(Informant informant) {
        super("Saving informant");
        this.informant = informant;
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
        if (IntelligenceHibernateManager.saveInformant(informant)) {
        	return Status.OK_STATUS;
        }
        //no need to use other status as hibernate manager will report error in case something is wrong
        return Status.CANCEL_STATUS;
    }
}