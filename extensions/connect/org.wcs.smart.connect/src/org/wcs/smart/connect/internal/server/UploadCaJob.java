/*
 * Copyright (C) 2012 Wildlife Conservation Society
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
package org.wcs.smart.connect.internal.server;

import java.nio.file.FileSystems;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.SmartContext;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.SmartConnect;
import org.wcs.smart.connect.api.model.WorkItemStatus;
import org.wcs.smart.connect.internal.Messages;
import org.wcs.smart.connect.internal.server.replication.ChangeLogTableManager;
import org.wcs.smart.connect.model.ConnectServerStatus;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * Job to upload conservation area export to connect server
 * and monitor results until complete.
 * 
 * @author Emily
 *
 */
public class UploadCaJob extends FileUploaderJob {

	private ConnectServerStatus status;
	
	public UploadCaJob(SmartConnect connect, ConnectServerStatus status){
		super(status.getUploadUrl(), FileSystems.getDefault().getPath(SmartContext.INSTANCE.getFilestoreLocation(), status.getLocalFile()), connect, Messages.UploadCaJob_JobName);
	
		this.status = status;
	}
	
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		try{
			super.uploadFile(monitor);
		}catch (Exception ex){
			ConnectPlugIn.log("Error uploading conservation area to connect.", ex); //$NON-NLS-1$
		}
		return org.eclipse.core.runtime.Status.OK_STATUS;

	}
	
	private void displayComplete(final String msg){
		Display.getDefault().syncExec(new Runnable(){

			@Override
			public void run() {
				if (status.getStatus() == org.wcs.smart.connect.model.ConnectServerStatus.Status.DONE){
					MessageDialog.openInformation(Display.getDefault().getActiveShell(), 
						Messages.UploadCaJob_InfoDialogTitle, 
						Messages.UploadCaJob_InfoComplete +
						(msg != null ? "\n" + msg : "")); //$NON-NLS-1$ //$NON-NLS-2$
				}else if (status.getStatus() == org.wcs.smart.connect.model.ConnectServerStatus.Status.ERROR){
					MessageDialog.openError(Display.getDefault().getActiveShell(), 
							Messages.UploadCaJob_InfoDialogTitle, 
							Messages.UploadCaJob_InfoError +
							(msg != null ? "\n" + msg : "")); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}});
	}
	
	
	/*
	 * Save sthe current status of the status object to the database
	 */
	private void saveStatus(){
		Session s = HibernateManager.openSession();
		s.beginTransaction();
		try{
			s.saveOrUpdate(status);
			s.getTransaction().commit();
		}catch (Exception ex){
			ConnectPlugIn.log(ex.getMessage(),ex);
		}finally{
			s.close();
		}
	}


	@Override
	protected void onUploadComplete(WorkItemStatus upstatus) {
		deleteLocalFile();
	}


	@Override
	protected void onProcessingComplete(WorkItemStatus upstatus) {
		try{
			this.status.setStatus(ConnectServerStatus.Status.DONE);
			saveStatus();
			deleteLocalFile();	
			super.connect.close();
			
			
			displayComplete(null);
		}catch (Exception ex){
			ConnectPlugIn.displayLog(Messages.UploadCaJob_ReplicationNotEnabled, ex);
		}
	}


	@Override
	protected void onError(String errorMessage) {
		this.status.setStatus(ConnectServerStatus.Status.ERROR);
		saveStatus();
		displayComplete(errorMessage == null ? Messages.UploadCaJob_UnknownError : errorMessage);	
		
		//error disable and cleanup replication details
		Session s = HibernateManager.openSession();
		try{
			s.beginTransaction();

			//clean up change log and upload table
			ChangeLogTableManager.INSTANCE.deleteAll(s, connect.getServer().getConservationArea());
			
			s.getTransaction().commit();
		}catch (Exception ex){
			ConnectPlugIn.displayLog(Messages.UploadCaJob_ReplicationNotReenabled, ex);
		}finally{
			s.close();
		}
		
		super.connect.close();
	}

}
