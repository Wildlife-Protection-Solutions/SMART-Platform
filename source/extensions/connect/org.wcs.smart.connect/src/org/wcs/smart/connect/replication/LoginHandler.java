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

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.progress.WorkbenchJob;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ILoginHandler;
import org.wcs.smart.SmartContext;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.SmartConnect;
import org.wcs.smart.connect.model.ConnectServerStatus;
import org.wcs.smart.connect.model.ConnectServerStatus.Status;
import org.wcs.smart.connect.model.ConnectSyncHistoryRecord;
import org.wcs.smart.connect.model.ConnectSyncHistoryRecord.Type;
import org.wcs.smart.connect.server.UploadCaEngine;
import org.wcs.smart.connect.server.replication.ChangeLogTableManager;
import org.wcs.smart.connect.server.replication.NothingToUpdateException;
import org.wcs.smart.connect.server.replication.SyncHistoryManager;
import org.wcs.smart.connect.server.replication.UploadChangeLogEngine;
import org.wcs.smart.connect.ui.server.ConnectDialog;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;

/**
 * Login handler to clean up SMART connect processes in cases
 * where SMART terminates in the middle of a background job.
 * Also cleans out filestore.
 * 
 * @author Emily
 *
 */
public class LoginHandler implements ILoginHandler {

	@Override
	public void onLogin() throws Exception {
		//ensure replication is disabled; we will enable later if required
		//this should be done when db started up; but we'll redo it here to 
		//be sure.
		Session s = HibernateManager.openSession();
		try{
			s.beginTransaction();
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
		//this may effect replication state which is why we disable/enable
		//replication state again after completed
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
			}else if (status.getStatus() == ConnectServerStatus.Status.ERROR){
				//delete any replication records or sync history records
				ChangeLogTableManager.INSTANCE.deleteAll(s, SmartDB.getCurrentConservationArea());
				SyncHistoryManager.INSTANCE.deleteAll(s, SmartDB.getCurrentConservationArea());
			}
			s.getTransaction().commit();
		}finally{
			s.close();
		}
		
		//process any upload sync tasks
		processUploadSync();
		//cleanup download sync tasks
		cleanUpDownloadEvents(status);

		//cleanupfilestore
		cleanUpFilestore();
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
					WorkbenchJob wj = new WorkbenchJob("resume upload job") {
						@Override
						public IStatus runInUIThread(IProgressMonitor monitor) {
							(new UploadCaEngine()).continueUpload(connect, status);
							return org.eclipse.core.runtime.Status.OK_STATUS;
						}
					};
					wj.schedule();
							
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
	 * Here we are going to remove any files that are not associated with
	 * 1) an active CA upload from ANY CA
	 * -> localfilename from ConnectServerStatus
	 * 2) and active sync download from ANY CA
	 * 
	 * All other files/directories will be removed.
	 */
	private void cleanUpFilestore(){
		final List<Path> filesToKeep = new ArrayList<Path>();
		
		Session s = HibernateManager.openSession();
		
		try{
			List<ConnectServerStatus> toKeep = s.createCriteria(ConnectServerStatus.class)
					.add(Restrictions.eq("status", ConnectServerStatus.Status.UPLOAD))
					.list();
			for(ConnectServerStatus server : toKeep){
				Path fileToKeep = Paths.get(SmartContext.INSTANCE.getFilestoreLocation(), server.getLocalFile());
				filesToKeep.add(fileToKeep);
			}
			
			List<ConnectSyncHistoryRecord> upToKeep = s.createCriteria(ConnectSyncHistoryRecord.class)
					.add(Restrictions.eq("status", ConnectSyncHistoryRecord.Status.ACTIVE))
					.add(Restrictions.eq("type", ConnectSyncHistoryRecord.Type.UPLOAD))
					.list();
			for (ConnectSyncHistoryRecord syn : upToKeep){
				Path fileToKeep = Paths.get(SmartContext.INSTANCE.getFilestoreLocation(), syn.getChangeLogZipFile());
				filesToKeep.add(fileToKeep);
			}
			
		}finally{
			s.close();
		}
		
		//delete all files that are not in the filesToKeep Array
		Path smartConnect = Paths.get(SmartContext.INSTANCE.getFilestoreLocation(), ConnectSyncHistoryRecord.CONNECT_FILESTORE_DIR);
		
		try(DirectoryStream<Path> stream = Files.newDirectoryStream(smartConnect, new DirectoryStream.Filter<Path>(){
			@Override
			public boolean accept(Path entry) throws IOException {
				return !filesToKeep.contains(entry);
			}})){
			
			for (Path p : stream){
				try{
					if (Files.isDirectory(p)){
						FileUtils.deleteDirectory(p.toFile());
					}else if (Files.exists(p)){
						Files.delete(p);
					}
				}catch (Exception ex){
					ConnectPlugIn.log("Unable to delete file while cleaning up connect filestore directory. " + p.toString(), ex);
				}
			}
			
		}catch (Exception ex){
			ConnectPlugIn.log("Unable to delete files while cleaning up connect filestore directory. ", ex);
		}
		
	}
	
	/*
	 * Any active download sync events are set to error
	 * 
	 * @param status
	 */
	private void cleanUpDownloadEvents(ConnectServerStatus status){
		List<ConnectSyncHistoryRecord> items = SyncHistoryManager.INSTANCE.getActiveSyncRecords(SmartDB.getCurrentConservationArea(), Type.DOWNLOAD);

		Session s = HibernateManager.openSession();
		try{
			s.beginTransaction();
			for (ConnectSyncHistoryRecord r : items){
				r.setStatus(ConnectSyncHistoryRecord.Status.ERROR);
				s.saveOrUpdate(r);
			}
			s.getTransaction().commit();
		}finally{
			s.close();
		}
	}
	/*
	 * checks for ca any upload sync events which may not have completed
	 */
	private void processUploadSync(){
		ConnectSyncHistoryRecord record = null;
		try{
			List<ConnectSyncHistoryRecord> allActive = SyncHistoryManager.INSTANCE.getActiveSyncRecords(SmartDB.getCurrentConservationArea(), Type.UPLOAD);
			
			Session s = HibernateManager.openSession();
			try{
				record = SyncHistoryManager.INSTANCE.getLastNonErrorSyncRecord(s, SmartDB.getCurrentConservationArea(),Type.UPLOAD);
				
				s.beginTransaction();
				for (ConnectSyncHistoryRecord r : allActive){
					if (r != record){
						r.setStatus(ConnectSyncHistoryRecord.Status.ERROR);
						s.saveOrUpdate(r);
					}
				}
				s.getTransaction().commit();
			}finally{
				s.close();
			}
			
			if (record == null ||
				record.getStatus() == ConnectSyncHistoryRecord.Status.DONE || 
				record.getStatus() == ConnectSyncHistoryRecord.Status.NODATA ||
				record.getStatus() == ConnectSyncHistoryRecord.Status.ERROR){
				
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
