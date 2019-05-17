package org.wcs.smart.paws.engine;

import java.net.URL;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Collections;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.paws.PawsEvent;
import org.wcs.smart.paws.PawsPlugIn;
import org.wcs.smart.paws.model.PawsParameter;
import org.wcs.smart.paws.model.PawsRun;
import org.wcs.smart.paws.model.PawsWorkspace;

import com.microsoft.azure.storage.blob.BlockBlobURL;
import com.microsoft.azure.storage.blob.ContainerURL;
import com.microsoft.azure.storage.blob.PipelineOptions;
import com.microsoft.azure.storage.blob.StorageURL;
import com.microsoft.azure.storage.blob.TransferManager;

import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * This job packages up all data specified by a paws run, uploads the data
 * to azure storage folder, and starts the paws analysis. 
 * 
 * @author Emily
 *
 */
public class PawsRunJob extends Job{
	
	private PawsRun run;
	private IEventBroker eventBroker;
	
	//object to wait on for azure callbacks 
	private final Object lock = new Object();
	
	public PawsRunJob(PawsRun run, IEventBroker eventBroker) {
		super("packaging and uploading data for PAWS analysis: " + run.getId());
		this.run = run;
		this.eventBroker = eventBroker;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		
		PawsDataEngine engine = new PawsDataEngine(run);
		//package data
		Path packageDir = null;
		try{
			packageDir = engine.createDataPackage();
		}catch (Exception ex){
			handleError("Unable to package SMART data for PAWS analysis.", ex);
			return Status.OK_STATUS;
		}
		
		//upload package to azure
		//TODO: update the required jar files when new build is released
		//https://github.com/Azure/autorest-clientruntime-for-java/issues/569
		ContainerURL containerURL;
		try{
			String url = null;
			try(Session session = HibernateManager.openSession()){
				PawsWorkspace ws = QueryFactory.buildQuery(session, PawsWorkspace.class,  
						new Object[] {"conservationArea", run.getConservationArea()}).uniqueResult();
				
				if (ws == null || ws.getApiKey() == null || ws.getUrl() == null){
					handleError("PAWS Workspace not configured.  You must first configure the PAWS Workspace before you can run paws analysis.", new Exception("No Paws Workspace Configured."));
					return Status.OK_STATUS;
				}
				url = ws.getUrl() + "?" + ws.getApiKey();
			}
			
	        containerURL = new ContainerURL(new URL(url), StorageURL.createPipeline(new PipelineOptions()));
	    
	        //upload map files
	        PawsParameter.FixedParameter[] bm = {
					PawsParameter.FixedParameter.LYR_BOUNDARY,
					PawsParameter.FixedParameter.LYR_CONTOUR,
					PawsParameter.FixedParameter.LYR_ROAD,
					PawsParameter.FixedParameter.LYR_WATER,
			};
			for (PawsParameter.FixedParameter layer : bm){
				Path zip = packageDir.resolve(layer.name() + ".zip");
				if (Files.exists(zip)){
					//upload file to folder
					BlockBlobURL blobURL = containerURL.createBlockBlobURL(run.getRunId() + "/" + zip.getFileName().toString());
					uploadFile(blobURL, zip, MessageFormat.format("Error uploading basemap data for layer {0}.", layer.name()));
				}
			
			}
			
			//upload data
			Path datafile = packageDir.resolve(PawsDataEngine.DATA_FILE_NAME);
			if (Files.exists(datafile)){
				BlockBlobURL blobURL = containerURL.createBlockBlobURL(run.getRunId() + "/" + datafile.getFileName().toString());
				uploadFile(blobURL, datafile, "Error uploading SMART data");
			}
			
		}catch (Throwable ex){
			String msg = "Unable to upload SMART data to Azure instance of PAWS analysis. "
					+ "Some data may remain on Azure blob storage folder (" 
					+ run.getRunId() 
					+ ").  You should remove this data manually ";
			handleError(msg, ex);
			return Status.OK_STATUS;
		}
		
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try{
				PawsRun pw = session.get(PawsRun.class, run.getUuid());
				if (pw != null){
					pw.setStatus(PawsRun.Status.RUNNING);
				}else{
					//TODO: assume this has been deleted; warn user that there might be data on azure storage folder?
					//Maybe we should do this on delete
				}
				session.getTransaction().commit();
			}catch (Exception ex2){
				try{
					session.getTransaction().rollback();
				}catch (Exception e3){
					PawsPlugIn.log(e3.getMessage(), e3);	
				}
				PawsPlugIn.displayLog("Failed to update Paws Run status.  Run will not continue." + "\n\n" + ex2.getMessage(), ex2);
				return Status.OK_STATUS;
			}
		}
		fireModified();
		
		
		//run paws analysis
		try{
			Path configJson = packageDir.resolve(PawsDataEngine.CONFIG_FILE_NAME);
			String json = Files.readString(configJson);
			System.out.println(json);
			//TODO:
//			PawsApi.INSTANCE.run(run.getConservationArea(), json);
		}catch (Exception ex){
			handleError("Error calling PAWS API", ex);
			return Status.OK_STATUS;
		}
		
		//add to queue to check status 
		PawsStatusJob.getInstance().addItem(run);
		

		return Status.OK_STATUS;
	}

	private void uploadFile(BlockBlobURL blobURL, Path file, String errorMsg) throws Throwable{
		final Throwable[] uperror = new Throwable[]{null}; 
		AsynchronousFileChannel fileChannel = AsynchronousFileChannel.open(file);
			TransferManager.uploadFileToBlockBlob(fileChannel, blobURL, 8 * 1024 * 1024, null, null)
					.subscribe(response -> {
						try{
							int stat = response.response().statusCode();
							if (stat != HttpResponseStatus.OK.code() && stat != HttpResponseStatus.CREATED.code()) {
								throw new Exception(errorMsg + " " + MessageFormat.format("(Server return code: {0})", stat));
							}
						}catch (Throwable t){
							uperror[0] = t;
						}finally{
							synchronized (lock) {
								lock.notifyAll();
							}
						}
					},
					error->{
						uperror[0] = error;
						synchronized (lock) {
							lock.notifyAll();
						}
					});
		
		//wait for transfer to finish
		synchronized (lock) {
			lock.wait();
		}
		if (uperror[0] != null) throw uperror[0];
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
		eventBroker.post(PawsEvent.PAWS_RUN_MODIFY, Collections.singleton(run));
	}
	
	
	
}
