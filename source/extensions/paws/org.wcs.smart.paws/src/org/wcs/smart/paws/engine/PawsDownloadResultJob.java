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

import java.net.URL;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.paws.PawsEvent;
import org.wcs.smart.paws.PawsManager;
import org.wcs.smart.paws.PawsPlugIn;
import org.wcs.smart.paws.model.PawsRun;
import org.wcs.smart.paws.model.PawsWorkspace;

import com.microsoft.azure.storage.blob.BlockBlobURL;
import com.microsoft.azure.storage.blob.ContainerURL;
import com.microsoft.azure.storage.blob.PipelineOptions;
import com.microsoft.azure.storage.blob.StorageURL;
import com.microsoft.azure.storage.blob.TransferManager;

/*
 * could possibly resechduled if smart is shutdown
 * before download complete.  so we must first
 * delete any existing files and try again
 * 
 */
public class PawsDownloadResultJob extends Job {

	private final Object lock = new Object();

	private ISchedulingRule runMutex = new ISchedulingRule(){
		@Override
		public boolean contains(ISchedulingRule rule) { return rule == this; }
		@Override
		public boolean isConflicting(ISchedulingRule rule) { return rule == this; }		
	};
	
	private PawsRun run;
	
	public PawsDownloadResultJob(PawsRun run) {
		super("Donwload results from " + run.getId());
		this.run = run;
		setRule(runMutex);
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		ContainerURL containerURL;
		Path resultsFile = null;
		String runId = null;
		try{
			String url = null;
			try(Session session = HibernateManager.openSession()){
				PawsRun r = session.get(PawsRun.class, run.getUuid());
				if (r == null){
					//delete just return
					return Status.OK_STATUS;
				}
				runId = r.getRunId();
				resultsFile = PawsManager.INSTANCE.getDirectory(r);
				
				PawsWorkspace ws = QueryFactory.buildQuery(session, PawsWorkspace.class,  
						new Object[] {"conservationArea", run.getConservationArea()}).uniqueResult();
				
				if (ws == null || !ws.isConfigured()) {
					handleError("PAWS Workspace not configured.  You must first configure the PAWS Workspace before you can run paws analysis.", new Exception("No Paws Workspace Configured."));
					return Status.OK_STATUS;
				}
				url = ws.getUrl() + "?" + ws.getClientId();
			}
			
	        containerURL = new ContainerURL(new URL(url), StorageURL.createPipeline(new PipelineOptions()));
		}catch (Exception ex){
			handleError("Error loading paws workspace.", ex);
			return Status.OK_STATUS;
		}
		
		//download results
		try{
			resultsFile = resultsFile.resolve("results.data"); 
	        BlockBlobURL blobUrl = containerURL.createBlockBlobURL(runId +"/results.data");
	        AsynchronousFileChannel fileChannel = AsynchronousFileChannel.open(resultsFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
			
	        final Throwable[] uperror = new Throwable[]{null}; 
	
	        TransferManager.downloadBlobToFile(fileChannel, blobUrl, null, null)
	        .subscribe(response-> {
	        		synchronized (lock) {
	        			lock.notifyAll();
	        		}
	        	},
	        	error->{
	        		uperror[0] = error;
	        		synchronized (lock) {
	        			lock.notifyAll();
	        		}
	        	});
	        if (uperror[0] != null) throw uperror[0];
		}catch (Throwable t){
			String msg = "Unable to download results of PAWS analysis from Azure. "
					+ "Some data may remain on Azure blob storage folder (" 
					+ run.getRunId() 
					+ ").  You should remove this data manually ";
			handleError(msg, t);
			return Status.OK_STATUS;
		}
		
		//update status
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try{
				PawsRun r = session.get(PawsRun.class, run.getUuid());
				if (r != null){
					r.setStatus(PawsRun.Status.COMPLETE);
				}
				session.getTransaction().commit();
			}catch (Exception ex){
				try{ session.getTransaction().rollback(); }catch (Exception ex2){ PawsPlugIn.log(ex2.getMessage(),ex2); }
				PawsPlugIn.displayLog(ex.getMessage(), ex);
			}
		}
		
		//delete all files from azure
		try {
			StorageApi.INSTANCE.deleteBlobs(run);
		} catch (Exception ex) {
			PawsPlugIn.displayLog(ex.getMessage(), ex);
		}
		
		PawsEvent.fireModified(run);
		
		return Status.OK_STATUS;
	}
	
	
	private void handleError(String msg, Throwable ex){
		PawsPlugIn.displayLog(msg + "\n\n" + ex.getMessage(), ex);
		
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try{
				session.saveOrUpdate(run);
				run.setStatus(PawsRun.Status.ERROR);
				run.setStatusMessage(msg + ex.getMessage());
				session.getTransaction().commit();
			}catch (Exception ex2){
				PawsPlugIn.log(ex2.getMessage(), ex2);
			}
		}
		PawsEvent.fireModified(run);
	}
	

}
