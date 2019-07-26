package org.wcs.smart.paws.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.paws.model.PawsRun;

/**
 * Checks the status of a paws run, when complete the 
 * results are downloaded from the server to local computer
 *  
 * 
 * @author Emily
 *
 */
public class PawsStatusJob extends Job {

	private static PawsStatusJob job = null;
	
	public static synchronized PawsStatusJob getInstance(){
		if (job == null){
			job = new PawsStatusJob();
		}
		return job;
	}
	
	private List<PawsRun> items = Collections.synchronizedList(new ArrayList<>());
	
	private PawsStatusJob() {
		super("Checking PAWS Status");
	}
	
	public void addItem(PawsRun run){
		synchronized (items) {
			if (items.isEmpty()){
				items.add(run);
				schedule(500);
			}else{
				items.add(run);
			}	
		}
	}

	
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		int counter = 0;
		List<PawsRun> readyToDownload = new ArrayList<>();
		List<PawsRun> cancelled = new ArrayList<>();
		while (counter < items.size()){
			PawsRun run = items.get(counter++);
			//reload in cases status changed
			try(Session session = HibernateManager.openSession()){
				run = session.get(PawsRun.class, run.getUuid());
			}
			if (run == null) return Status.OK_STATUS;
			if (run.getStatus() == PawsRun.Status.RUNNING){
				//check status
				try {
					if (PawsApi.INSTANCE.checkStatus(run)){
						readyToDownload.add(run);
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}else{
				cancelled.add(run);
			}
		}
		
		
		// download job
		List<PawsDownloadResultJob> jobs = new ArrayList<>();
		try (Session session = HibernateManager.openSession()) {
			session.beginTransaction();
			try {
				for (PawsRun r : readyToDownload) {
					PawsRun dbrun = session.get(PawsRun.class, r.getUuid());
					if (dbrun == null){
						//delete from db, ignore this
					}else{
						dbrun.setStatus(PawsRun.Status.DOWNLOADING_RESULTS);
						jobs.add(new PawsDownloadResultJob(dbrun));
					}
				}
				session.getTransaction().commit();
			} catch (Exception ex) {
				ex.printStackTrace();
				// TODO:
			}
		}
		jobs.forEach(j->j.schedule());
		
		synchronized (items) {
			items.removeAll(readyToDownload);
			items.removeAll(cancelled);
			if(!items.isEmpty()) schedule(5000); //TODO: delay length
		}
		return Status.OK_STATUS;
	}

}
