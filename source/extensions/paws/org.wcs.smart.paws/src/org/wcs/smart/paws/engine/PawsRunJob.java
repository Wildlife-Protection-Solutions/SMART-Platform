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

import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.paws.PawsEvent;
import org.wcs.smart.paws.PawsPlugIn;
import org.wcs.smart.paws.model.PawsRun;
import org.wcs.smart.paws.model.PawsWorkspace;

import com.microsoft.azure.storage.blob.BlockBlobURL;
import com.microsoft.azure.storage.blob.ContainerURL;
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
	
	//object to wait on for azure callbacks 
	private final Object lock = new Object();
	
	
	public PawsRunJob(PawsRun run) {
		super("packaging and uploading data for PAWS analysis: " + run.getId());
		this.run = run;
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
			PawsWorkspace ws;
			try(Session session = HibernateManager.openSession()){
				ws = QueryFactory.buildQuery(session, PawsWorkspace.class,  
						new Object[] {"conservationArea", run.getConservationArea()}).uniqueResult();
				
				if (ws == null || !ws.isConfigured()){
					handleError("PAWS Workspace not configured.  You must first configure the PAWS Workspace before you can run paws analysis.", new Exception("No Paws Workspace Configured."));
					return Status.OK_STATUS;
				}
				
			}
			
			containerURL = StorageApi.INSTANCE.getContainerURL();
						
	        //upload files
	        for(Path p : engine.getDataFiles()) {
				if (Files.exists(p)){
					//upload file to folder
					BlockBlobURL blobURL = containerURL.createBlockBlobURL(run.getRunId() + "/" + p.getFileName().toString());
					blobURL.getProperties();
					uploadFile(blobURL, p, MessageFormat.format("Error uploading basemap data for layer {0}.", p.getFileName().toString()));
				}
			
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
		
		//if (true) return Status.OK_STATUS;
		
		//run paws analysis
		try{
			Path configJson = packageDir.resolve(PawsDataEngine.CONFIG_FILE_NAME);
			String json = Files.readString(configJson);
			System.out.println(json);
			//TODO:
			PawsApi.INSTANCE.run(run, json);
			
			//{
			 // "TaskId": "string",
			 // "Timestamp": "2019-06-05T23:05:01.858Z",
			 // "Status": "string",
			 // "Endpoint": "string"
			//}
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
		try(AsynchronousFileChannel fileChannel = AsynchronousFileChannel.open(file)) {
			
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
		PawsEvent.fireModified(run);
	}
	
}
