/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
import org.wcs.smart.paws.PawsEvent;
import org.wcs.smart.paws.PawsPlugIn;
import org.wcs.smart.paws.engine.PawsApi.PawsStatus;
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
			}else{
				items.add(run);
			}	
			schedule(5000);
		}
	}

	
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		int counter = 0;
		List<PawsRun> readyToDownload = new ArrayList<>();
		List<PawsRun> cancelled = new ArrayList<>();
		List<PawsRun> cleanUp = new ArrayList<>();
		
		while (counter < items.size()){
			PawsRun run = items.get(counter++);
			//reload in cases status changed
			try(Session session = HibernateManager.openSession()){
				run = session.get(PawsRun.class, run.getUuid());
			}
			if (run == null) continue;
			
			if (run.getStatus() == PawsRun.Status.RUNNING){
				//check status
				try {
					PawsApi.PawsStatus taskStatus = PawsApi.INSTANCE.checkStatus(run);
					if (taskStatus == PawsStatus.DONE){
						readyToDownload.add(run);
//						cleanUp.add(run);
					}else if (taskStatus == PawsStatus.ERROR) {
						//delete from server
						cleanUp.add(run);
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
		
		PawsEvent.fireModified(items);
		synchronized (items) {
			items.removeAll(readyToDownload);
			items.removeAll(cancelled);
			if(!items.isEmpty()) schedule(5000); //TODO: delay length
		}
		
		for (PawsRun r : cleanUp) {
			try {
				StorageApi.INSTANCE.deleteBlobs(r);
			} catch (Exception e) {
				PawsPlugIn.log(e.getMessage(),e);
			}
		}
		return Status.OK_STATUS;
	}

}
