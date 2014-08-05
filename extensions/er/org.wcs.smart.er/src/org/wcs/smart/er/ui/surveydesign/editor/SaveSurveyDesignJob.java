package org.wcs.smart.er.ui.surveydesign.editor;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.hibernate.SmartHibernateManager;

/**
 * Job is used to save survey design object
 * 
 * @author elitvin
 */
public class SaveSurveyDesignJob extends Job {

	private SurveyDesign design;
	
    public SaveSurveyDesignJob(SurveyDesign design) {
        super("Saving survey design");
        this.design = design;
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
		Session session = SmartHibernateManager.openSession();
		session.beginTransaction();
		try {
			session.saveOrUpdate(design);
			session.getTransaction().commit();
			
        	return Status.OK_STATUS;
		} catch (Exception ex){
			try{
				session.getTransaction().rollback();
			}catch (Exception ex2){
				EcologicalRecordsPlugIn.log(ex.getMessage(), ex2);
			}
			EcologicalRecordsPlugIn.displayLog("Error saving new survey design." + "\n\n" + ex.getMessage(), ex);
		} finally {
			session.close();
		}
        //no need to use other status as hibernate manager will report error in case something is wrong
        return Status.CANCEL_STATUS;
    }
}	
