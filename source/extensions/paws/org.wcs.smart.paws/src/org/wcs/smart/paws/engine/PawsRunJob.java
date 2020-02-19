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
import org.wcs.smart.paws.internal.Messages;
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
		super(Messages.PawsRunJob_JobName + run.getId());
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
			handleError(Messages.PawsRunJob_PackageError, ex);
			return Status.OK_STATUS;
		}
		
		//upload package to azure
		//https://github.com/Azure/autorest-clientruntime-for-java/issues/569
		ContainerURL containerURL;
		try{
			PawsWorkspace ws;
			try(Session session = HibernateManager.openSession()){
				ws = QueryFactory.buildQuery(session, PawsWorkspace.class,  
						new Object[] {"conservationArea", run.getConservationArea()}).uniqueResult(); //$NON-NLS-1$
				
				if (ws == null || !ws.isConfigured()){
					handleError(Messages.PawsRunJob_WorkspaceNotConfigured, new Exception(Messages.PawsRunJob_NoWorkspaceFound));
					return Status.OK_STATUS;
				}	
			}
			containerURL = StorageApi.INSTANCE.getContainerURL();
						
	        //upload files
	        for(Path p : engine.getDataFiles()) {
				if (Files.exists(p)){
					//upload file to folder
					BlockBlobURL blobURL = containerURL.createBlockBlobURL(run.getRunId() + "/" + p.getFileName().toString()); //$NON-NLS-1$
					blobURL.getProperties();
					uploadFile(blobURL, p, MessageFormat.format(Messages.PawsRunJob_BasemapFileUploadError, p.getFileName().toString()));
				}
			
			}
			
		}catch (Throwable ex){
			String msg = MessageFormat.format(Messages.PawsRunJob_UploadError, run.getRunId() );
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
					throw new Exception(Messages.PawsRunJob_runnotfound);
				}
				session.getTransaction().commit();
			}catch (Exception ex2){
				try{
					session.getTransaction().rollback();
				}catch (Exception e3){
					PawsPlugIn.log(e3.getMessage(), e3);	
				}
				PawsPlugIn.displayLog(Messages.PawsRunJob_StatusFailed + "\n\n" + ex2.getMessage(), ex2); //$NON-NLS-1$
				return Status.OK_STATUS;
			}
		}
		fireModified();
		
		//run paws analysis
		try{
			Path configJson = packageDir.resolve(PawsDataEngine.CONFIG_FILE_NAME);
			String json = Files.readString(configJson);
			PawsApi.INSTANCE.run(run, json);
			
		}catch (Exception ex){
			handleError(Messages.PawsRunJob_APIError, ex);
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
								throw new Exception(errorMsg + " " + MessageFormat.format(Messages.PawsRunJob_ServerReturnCode, stat)); //$NON-NLS-1$
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
		PawsPlugIn.displayLog(msg + "\n\n" + ex.getMessage(), ex); //$NON-NLS-1$
		
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
