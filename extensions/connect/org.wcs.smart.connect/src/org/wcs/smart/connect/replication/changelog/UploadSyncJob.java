package org.wcs.smart.connect.replication.changelog;

import java.nio.file.FileSystems;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.SmartContext;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.SmartConnect;
import org.wcs.smart.connect.api.model.UploadStatus;
import org.wcs.smart.connect.model.ConnectServerStatus;
import org.wcs.smart.connect.model.ConnectSyncHistoryRecord;
import org.wcs.smart.connect.server.FileUploaderJob;
import org.wcs.smart.hibernate.HibernateManager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class UploadSyncJob extends FileUploaderJob {

	private ConnectSyncHistoryRecord item;
	
	public UploadSyncJob(ConnectSyncHistoryRecord item, 
			SmartConnect connect) {
		super(null, FileSystems.getDefault().getPath(SmartContext.INSTANCE.getFilestoreLocation(), item.getChangeLogZipFile()),
				connect, "Upload changes to SMART Connect");
		this.item = item;
		
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		String url = null;
		try{
			url = connect.getSyncUploadUrl(item.getConservationArea().getUuid(), file);
		}catch (Exception ex){
			ConnectPlugIn.log(ex.getMessage(), ex);
			onError(null);
			return Status.OK_STATUS;
		}
		
		item.setStatusUrl(url);
		saveHistoryRecord();
		
		super.url = url;
		try {
			super.uploadFile(monitor);
		} catch (Exception e) {
			ConnectPlugIn.log(e.getMessage(), e);
			onError(null);
			return Status.OK_STATUS;
		}
		
		return Status.OK_STATUS;
	}

	private void displayDone(final String msg){
		Display.getDefault().syncExec(new Runnable(){

			@Override
			public void run() {
				if (item.getStatus() == ConnectSyncHistoryRecord.Status.DONE){
					MessageDialog.openInformation(Display.getDefault().getActiveShell(), 
						"SMART Connect Sync Upload", 
						"Sync upload to SMART Connect complete."  + (msg == null ? "" : "\n\n" + msg));
				}else if (item.getStatus() == ConnectSyncHistoryRecord.Status.ERROR){
					MessageDialog.openInformation(Display.getDefault().getActiveShell(), 
							"SMART Connect Sync Upload", 
							"An error occurred during syncing changes to SMART Connect."  + (msg == null ? "" : "\n\n" + msg));
				}
			}});
	}
	
	
	
	@Override
	protected void onUploadComplete(UploadStatus upstatus) {
		deleteLocalFile();
	}

	
	@Override
	protected void onProcessingComplete(UploadStatus upstatus) {
		try{
			String message = upstatus.getMessage();
			JsonNode nd = (new ObjectMapper()).readTree(message);
			long serverRevision = nd.get("server_revision").asLong();
			
			Session s = HibernateManager.openSession();
			try{
				s.beginTransaction();
				ConnectServerStatus serverstatus = (ConnectServerStatus)s.get(ConnectServerStatus.class, item.getConservationArea().getUuid());
				serverstatus.setServerRevision(serverRevision);
				s.getTransaction().commit();
			}finally{
				s.close();
			}

		}catch (Exception ex){
			ConnectPlugIn.log("Could not parse server revision for upload sync response.  This will cause user to require download before they can upload again." + ex.getMessage(), ex);
		}
		
		item.setStatus(ConnectSyncHistoryRecord.Status.DONE);
		saveHistoryRecord();
		
		deleteLocalFile();
		displayDone(null);
		
		super.connect.close();
	}

	@Override
	protected void onError(UploadStatus upstatus) {
		item.setStatus(ConnectSyncHistoryRecord.Status.ERROR);
		saveHistoryRecord();
		deleteLocalFile();
		String msg = null;
		if (upstatus != null){
			msg = upstatus.getMessage();
		}
		displayDone(msg);
		
		super.connect.close();
	}
	
	private void saveHistoryRecord(){
		Session s = HibernateManager.openSession();
		try{
			s.beginTransaction();
			s.saveOrUpdate(item);
			s.getTransaction().commit();
		}finally{
			s.close();
		}
	}

}
