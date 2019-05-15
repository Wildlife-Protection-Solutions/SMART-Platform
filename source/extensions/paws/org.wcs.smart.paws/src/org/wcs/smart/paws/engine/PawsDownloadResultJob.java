package org.wcs.smart.paws.engine;

import java.net.URL;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.events.IEventBroker;
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
import com.microsoft.azure.storage.blob.ListBlobsOptions;
import com.microsoft.azure.storage.blob.PipelineOptions;
import com.microsoft.azure.storage.blob.StorageURL;
import com.microsoft.azure.storage.blob.TransferManager;
import com.microsoft.azure.storage.blob.models.BlobItem;
import com.microsoft.azure.storage.blob.models.ContainerListBlobFlatSegmentResponse;

import io.reactivex.Single;

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
				
				if (ws == null || ws.getApiKey() == null || ws.getUrl() == null){
					handleError("PAWS Workspace not configured.  You must first configure the PAWS Workspace before you can run paws analysis.", new Exception("No Paws Workspace Configured."));
					return Status.OK_STATUS;
				}
				url = ws.getUrl() + "?" + ws.getApiKey();
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
		deleteBlobs(containerURL);
		
		fireModified();
		
		return Status.OK_STATUS;
	}
	
	private void deleteBlobs(ContainerURL containerURL){
		try {
			List<String> urlToDelete = new ArrayList<>();
			ListBlobsOptions options = new ListBlobsOptions();
			containerURL.listBlobsFlatSegment(null, options, null)
					.flatMap(containerListBlobFlatSegmentResponse -> listAllBlobs(containerURL,
							containerListBlobFlatSegmentResponse, urlToDelete))
					.subscribe(response -> {
						synchronized (lock) {
							lock.notify();
						}
					},
						error->{
							synchronized (lock) {
								lock.notify();
							}		
			});

			synchronized (lock) {
				lock.wait();
			}

			List<Throwable> fails = new ArrayList<>();
			for (String d : urlToDelete) {
				containerURL.createBlockBlobURL(d).delete().subscribe(rsp -> {},  error -> fails.add(error));
			}
			if (!fails.isEmpty()){
				for (Throwable t : fails) PawsPlugIn.log(t.getMessage(), t);
				throw new Exception("Delete fail.");
			}
		}catch (Exception ex){
			PawsPlugIn.displayLog(MessageFormat.format("Unable to remove data from Azure container ({0}). You should remove these files manually.", run.getId()), ex);
		}
        
	}

	private Single <ContainerListBlobFlatSegmentResponse> listAllBlobs(ContainerURL url, ContainerListBlobFlatSegmentResponse response, List<String> toDelete) {
		
		   // Process the blobs returned in this result segment (if the segment is empty, blobs() will be null.
     if (response.body().segment() != null) {
         for (BlobItem b : response.body().segment().blobItems()) {
         	System.out.println("testing: " + b.name() + ":" + run.getRunId());
         	if (b.name().startsWith(run.getRunId() + "/")){
         		toDelete.add(b.name());
         	}
         }
     }
     
		 // If there is not another segment, return this response as the final response.
     if (response.body().nextMarker() == null) {
         return Single.just(response);
     } else {
         /*
         IMPORTANT: ListBlobsFlatSegment returns the start of the next segment; you MUST use this to get the next
         segment (after processing the current result segment
         */

         String nextMarker = response.body().nextMarker();

         /*
         The presence of the marker indicates that there are more blobs to list, so we make another call to
         listBlobsFlatSegment and pass the result through this helper function.
         */

         return url.listBlobsFlatSegment(nextMarker, new ListBlobsOptions().withMaxResults(10), null)
                 .flatMap(containersListBlobFlatSegmentResponse ->
                         listAllBlobs(url, containersListBlobFlatSegmentResponse, toDelete));
     }
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
		fireModified();
	}
	
	private void fireModified(){
		EclipseContextFactory.getServiceContext(PawsPlugIn.getDefault().getBundle().getBundleContext()).get(IEventBroker.class)
			.post(PawsEvent.PAWS_RUN_MODIFY, Collections.singleton(run));
	}
}
