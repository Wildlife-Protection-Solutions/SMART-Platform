package org.wcs.smart.paws.engine;

import java.nio.file.Path;
import java.time.LocalDate;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.paws.PawsPlugIn;
import org.wcs.smart.paws.model.PawsRun;

public class PawsRunJob extends Job{


	private PawsRun run;
	
	public PawsRunJob(PawsRun run) {
		super("packaging paws " + run.getId());
		this.run = run;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {

		
		PawsDataEngine engine = new PawsDataEngine(run);
		
		Path packageFile = null;
		try{
			packageFile = engine.createDataPackage();
		}catch (Exception ex){
			PawsPlugIn.displayLog("Unable to create PAWS package for analysis." + "\n\n" + ex.getMessage(), ex);
			
			try(Session session = HibernateManager.openSession()){
				session.beginTransaction();
				try{
					session.saveOrUpdate(run);
					run.setStatus(PawsRun.Status.ERROR);
					run.setStatusMessage("Unable to create PAWS package for analysis: " + ex.getMessage());
					session.getTransaction().commit();
				}catch (Exception ex2){
					PawsPlugIn.log(ex.getMessage(), ex2);
				}
			}
			//fire status changed events?
			return Status.OK_STATUS;
		}
		
		//upload package to azure
		
		return Status.OK_STATUS;
	}

}
