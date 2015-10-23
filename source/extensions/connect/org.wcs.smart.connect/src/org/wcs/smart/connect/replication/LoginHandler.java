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
package org.wcs.smart.connect.replication;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.ILoginHandler;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.SmartConnect;
import org.wcs.smart.connect.model.ConnectServerStatus;
import org.wcs.smart.connect.model.ConnectServerStatus.Status;
import org.wcs.smart.connect.model.ConnectSyncHistoryRecord;
import org.wcs.smart.connect.model.ConnectSyncHistoryRecord.Type;
import org.wcs.smart.connect.server.UploadCaEngine;
import org.wcs.smart.connect.server.replication.NothingToUpdateException;
import org.wcs.smart.connect.server.replication.SyncHistoryManager;
import org.wcs.smart.connect.server.replication.UploadChangeLogEngine;
import org.wcs.smart.connect.ui.server.ConnectDialog;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;

/**
 * Login handler to clean up SMART connect processes in cases
 * where SMART terminates in the middle of a background job.
 * 
 * @author Emily
 *
 */
public class LoginHandler implements ILoginHandler {

	@Override
	public void onLogin() throws Exception {
		//ensure replication is disabled; we will enable later if required
		Session s = HibernateManager.openSession();
		try{
			s.beginTransaction();
			System.out.println("REPLICATION: " + DerbyReplicationManager.INSTANCE.isReplicationEnabled(s));
			
			DerbyReplicationManager.INSTANCE.disableReplication(s);
			s.getTransaction().commit();
		}finally{
			s.close();
		}
		
		//never replicate multiple conservation areas
		if (SmartDB.isMultipleAnalysis()) return ;
		
		ConnectServerStatus status;
		s = HibernateManager.openSession();
		try{
			s.beginTransaction();
			status = (ConnectServerStatus)s.get(ConnectServerStatus.class, SmartDB.getCurrentConservationArea().getUuid());
			if (status == null){
				return;
			}
			s.getTransaction().commit();
		}finally{
			s.close();
		}
		
		//process any existing ca upload task
		processCaUploadEvents(status);
		
		//sort out replication
		s = HibernateManager.openSession();
		try{
			s.beginTransaction();
			status = (ConnectServerStatus)s.get(ConnectServerStatus.class, SmartDB.getCurrentConservationArea().getUuid());
			if (status == null){
				return;
			}
			if (status.getStatus() == ConnectServerStatus.Status.UPLOAD ||
				status.getStatus() == ConnectServerStatus.Status.DONE ){
				DerbyReplicationManager.INSTANCE.enableReplication(s);
			}
			s.getTransaction().commit();
		}finally{
			s.close();
		}
		
		//process any upload sync tasks
		processUploadSync();
	}

	/*
	 * checks for ca upload task and performs necessary checks to invalidate
	 * or continue process
	 */
	private void processCaUploadEvents(ConnectServerStatus status){
		if (status.getStatus() == ConnectServerStatus.Status.DONE || 
				status.getStatus() == ConnectServerStatus.Status.ERROR){
			return;
		}
		
		
		if (status.getStatus() == ConnectServerStatus.Status.BACKUP){
			MessageDialog.openWarning(Display.getDefault().getActiveShell(), "Export To Connect", "SMART was terminated before the export to SMART Conenct to be initiated.  You will need to re-export to SMART Connect if you want to upload your conservation area.");
			
			Session s = HibernateManager.openSession();
			try{
				s.beginTransaction();
				s.saveOrUpdate(status);
				status.setStatus(Status.ERROR);
				s.getTransaction().commit();
				return;
			}finally{
				s.close();
			}
				
		}else if (status.getStatus() == ConnectServerStatus.Status.UPLOAD){		
			boolean cont = false;
			if (MessageDialog.openQuestion(Display.getDefault().getActiveShell(), "Export To Connect", 
					"SMART was terminated before the export to SMART Conenct was completed.  Do you want resume the upload process?")){
				
				SmartConnect connect = getSmartConnect();
				if (connect != null){
					//need to continue upload
					cont = true;
					(new UploadCaEngine()).continueUpload(connect, status);		
				}
			}
			if (!cont){
				Session s = HibernateManager.openSession();
				try{
					s.beginTransaction();
					status.setStatus(Status.ERROR);
					s.saveOrUpdate(status);
					s.getTransaction().commit();
					return;
				}finally{
					s.close();
				}
			}
		}
	}
	
	
	/*
	 * checks for ca any upload sync events which may not have completed
	 */
	private void processUploadSync(){
		ConnectSyncHistoryRecord record = null;
		try{
			Session s = HibernateManager.openSession();
			try{
				record = SyncHistoryManager.INSTANCE.getLastNonErrorSyncRecord(s, SmartDB.getCurrentConservationArea(),Type.UPLOAD);
			}finally{
				s.close();
			}
			
			if (record == null) return;
				
			if (record.getStatus() == ConnectSyncHistoryRecord.Status.DONE){
				return;
			}
			if (record.getStatus() == ConnectSyncHistoryRecord.Status.ERROR){
				return;
			}
				
			if (record.getStatus() == ConnectSyncHistoryRecord.Status.ACTIVE){
				if (record.getStatusUrl() == null){
					//never got a url from the server
					//lets warn user; set status to error and return
					MessageDialog.openWarning(Display.getDefault().getActiveShell(), "Sync With Connect", "SMART was terminated before upload sync with connect could be initiated.  You must manually resync changes.");
					
					s = HibernateManager.openSession();
					s.beginTransaction();
					try{
						record.setStatus(ConnectSyncHistoryRecord.Status.ERROR);
						s.getTransaction().commit();
					}finally{
						s.close();
					}
					
					return;
				}else{	
					//continue job waiting for
					MessageDialog.openWarning(Display.getDefault().getActiveShell(), "Sync With Connect", "SMART was terminated before upload sync with connect could finish.  The job will resume and you will be notified when complete.");
					SmartConnect connect = getSmartConnect();
					if (connect != null){
						UploadChangeLogEngine e = new UploadChangeLogEngine(connect);
						try{
							e.createUpload(new NullProgressMonitor());
						}catch (NothingToUpdateException ex){
							//consume this exception
						}
					}else{
						s = HibernateManager.openSession();
						try{
							s.beginTransaction();
							record.setStatus(ConnectSyncHistoryRecord.Status.ERROR);
							s.saveOrUpdate(record);
							s.getTransaction().commit();
						}finally{
							s.close();
						}
					}
				}
			}
		}catch (Exception ex){
			ConnectPlugIn.log("Error continuing connect sync upload", ex);
		}
	}
	
	
	
	private SmartConnect getSmartConnect(){
		final String title = "SMART Connect";
		final String message = "Confirm SMART Connect credentials";
				
		ConnectDialog cd = new ConnectDialog(Display.getDefault().getActiveShell(), true){
			@Override
			protected Control createDialogArea(Composite parent) {
				Control c = super.createDialogArea(parent);
				getShell().setText(title);
				setTitle(title);
				setMessage(message);
				return c;
			}
		};
		
		if (cd.open() == Window.OK){
			return cd.getConnection();
		}
		return null;		
	}
}
