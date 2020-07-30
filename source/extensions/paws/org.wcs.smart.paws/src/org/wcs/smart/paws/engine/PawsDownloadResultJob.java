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
import java.nio.file.StandardOpenOption;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.paws.PawsEvent;
import org.wcs.smart.paws.PawsFileManager;
import org.wcs.smart.paws.PawsPlugIn;
import org.wcs.smart.paws.internal.Messages;
import org.wcs.smart.paws.model.PawsResultManager;
import org.wcs.smart.paws.model.PawsRun;

import com.microsoft.azure.storage.blob.BlockBlobURL;
import com.microsoft.azure.storage.blob.ContainerURL;
import com.microsoft.azure.storage.blob.StorageException;
import com.microsoft.azure.storage.blob.TransferManager;
import com.microsoft.azure.storage.blob.models.StorageErrorCode;

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
		super(Messages.PawsDownloadResultJob_JobName + run.getId());
		this.run = run;
		setRule(runMutex);
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		
		monitor.beginTask(Messages.PawsDownloadResultJob_DownloadTaskName, 5);

		PawsRun prun = null;
		Path resultsDirectory = null;
		try{
			try(Session session = HibernateManager.openSession()){
				prun = session.get(PawsRun.class, run.getUuid());
				if (prun == null){
					//delete just return
					return Status.OK_STATUS;
				}
				resultsDirectory = PawsFileManager.INSTANCE.getResultsDirectory(prun);
				if (!Files.exists(resultsDirectory)) Files.createDirectories(resultsDirectory);
			}
		}catch (Exception ex){
			handleError(Messages.PawsDownloadResultJob_ErrorLoadingWorkspace, ex);
			return Status.OK_STATUS;
		}
		monitor.worked(1);
		
		//validate token
		Exception[] fex = new Exception[]{null};
		boolean[] iscancelled = new boolean[]{false};
		PawsRun frun = prun;
		Display.getDefault().syncExec(()->{
			try {
				if (!StorageApi.INSTANCE.getAuthorizationCode(Display.getDefault().getActiveShell(), frun)) {
					iscancelled[0] = true;
				}
			} catch (Exception e) {
				PawsPlugIn.displayLog(e.getMessage(), e);
				fex[0] = e;
			}
		});
		if (iscancelled[0]) {
			updateStatus(PawsRun.Status.AUTH_TIMEOUT, Messages.PawsDownloadResultJob_AuthorizatoinFailed);
			return Status.OK_STATUS;
		}
		if (fex[0] != null) {
			handleError(fex[0].getMessage(), fex[0]);
			return Status.OK_STATUS;
		}
		
		//download results
		monitor.subTask(Messages.PawsDownloadResultJob_downloadtaskname);
		try{
			
			ContainerURL containerURL = StorageApi.INSTANCE.getContainerURL(run.getContainerName());

			String endpoint = prun.getRunId() + "/risk_prediction"; //$NON-NLS-1$
			
			List<String> tocopy = StorageApi.INSTANCE.getBlobs(containerURL, endpoint);
			tocopy.add(prun.getRunId() + "/processed_data/" + PawsResultManager.PROJ_FILE); //$NON-NLS-1$
			
			for (String result : tocopy) {
				BlockBlobURL blobUrl = containerURL.createBlockBlobURL(result);
				
				String fname = result.substring(result.lastIndexOf('/')+1);
				Path resultsFile = resultsDirectory.resolve(fname);
				
				AsynchronousFileChannel fileChannel = AsynchronousFileChannel.open(resultsFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
				
				final Throwable[] uperror = new Throwable[]{null}; 
					
			    TransferManager.downloadBlobToFile(fileChannel, blobUrl, null, null)
			    	.subscribe(response-> {
			        	synchronized (lock) {
			        		lock.notifyAll();
			        	}
			        }, error->{
			        	uperror[0] = error;
			        	synchronized (lock) {
			        		lock.notifyAll();
			        	}
			        });
			    synchronized (lock) {
			    	lock.wait();
	        	}
			    if (uperror[0] != null) {
			    	throw uperror[0];
			    }
			}
			
		}catch (Throwable t){
			
			if(t instanceof StorageException) {
				StorageException ex = (StorageException) t;
				if (ex.errorCode() == StorageErrorCode.AUTHENTICATION_FAILED) {
					StorageApi.INSTANCE.resetToken();
					schedule(500);
					return Status.OK_STATUS;
				}
			}
			
			String msg = Messages.PawsDownloadResultJob_DownloadFailedMsg; 
			handleError(msg, t);
			return Status.OK_STATUS;
		}
		monitor.worked(2);
		
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
			monitor.worked(1);
		}
		
		
		//build raster files from result
		monitor.subTask(Messages.PawsDownloadResultJob_buildrasterstaskname);
		try(Session session = HibernateManager.openSession()){
			PawsRun r = session.get(PawsRun.class, run.getUuid());
			if (r != null) {
				PawsResultManager manager = new PawsResultManager(r);
				manager.createImages();
			}
		} catch (Exception e) {
			PawsPlugIn.displayLog(e.getMessage(), e);
		}
		monitor.worked(1);
		
		//delete all files from azure
		monitor.subTask(Messages.PawsDownloadResultJob_cleanuptaskname);
		try {
			StorageApi.INSTANCE.deleteBlobs(run);
		} catch (Exception ex) {
			PawsPlugIn.displayLog(ex.getMessage(), ex);
		}
		
		PawsEvent.fireModified(run);
		
		return Status.OK_STATUS;
	}
	
	
	private void handleError(String msg, Throwable ex){
		PawsRun.Status newStatus = PawsRun.Status.ERROR;
		if (ex instanceof StorageException && ((StorageException)ex).errorCode() == StorageErrorCode.AUTHENTICATION_FAILED) {
			newStatus = PawsRun.Status.AUTH_TIMEOUT;
		}
		
		PawsPlugIn.displayLog(msg + "\n\n" + ex.getMessage(), ex); //$NON-NLS-1$
		updateStatus(newStatus, msg + ": " + ex.getMessage()); //$NON-NLS-1$
	}

	private void updateStatus(PawsRun.Status newStatus, String message) {
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try{
				session.saveOrUpdate(run);
				run.setStatus(newStatus);
				run.setStatusMessage(message);
				session.getTransaction().commit();
			}catch (Exception ex2){
				PawsPlugIn.log(ex2.getMessage(), ex2);
			}
		}
		PawsEvent.fireModified(run);
	}

}
