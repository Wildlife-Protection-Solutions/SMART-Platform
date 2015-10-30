package org.wcs.smart.connect.server.replication;

import java.nio.file.Path;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.SmartConnect;
import org.wcs.smart.connect.api.model.WorkItemStatus;
import org.wcs.smart.connect.model.ConnectServerStatus;
import org.wcs.smart.connect.model.ConnectSyncHistoryRecord;
import org.wcs.smart.connect.model.ConnectSyncHistoryRecord.Status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DownloadChangeLogJob extends Job {

	private SmartConnect connect;
	private ConnectSyncHistoryRecord record;
	private ConnectServerStatus serverInfo;
	
	private Path downloadFile = null;
	
	public DownloadChangeLogJob(SmartConnect connect, 
			ConnectServerStatus serverInfo,
			ConnectSyncHistoryRecord record) {
		super("Download Connect Changelog");
		
		this.connect = connect;
		this.record = record;
		this.serverInfo = serverInfo;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		//TODO: progress monitor
		monitor.beginTask("Downloading change log", 1);
		try{
			/* request ca */
			monitor.subTask("Initializing download");
			String statusUrl = connect.startChangeLogDownload(serverInfo.getUuid(), serverInfo.getVersion(), serverInfo.getServerRevision());
			monitor.worked(1);
			if (monitor.isCanceled()) return cancelled();
			
			/* wait for ca export to be created */
			monitor.subTask("Waiting for CONNECT server to create package");
			Long start = System.nanoTime();
			WorkItemStatus status = null ;
			while(status == null || (status.getStatus() == WorkItemStatus.Status.PROCESSING || 
					status.getStatus() == WorkItemStatus.Status.PROCESSING)){
				Long current = System.nanoTime();
				if ( current - start > connect.getServer().getWaitProcessingTime() * 1000000) throw new Exception("Timed out waiting for export to process.");
				Thread.sleep(connect.getServer().getRetryWaitTime());
				try{
					status = connect.getWorkItemStatus(statusUrl);
				}catch (Exception ex){
					ConnectPlugIn.log("Error requesting ca update download status.", ex);
				}
				if (monitor.isCanceled()) return cancelled();
			}
			monitor.worked(1);
			
			/* download file */
			monitor.subTask("Downloading Upload Package");
			String message = status.getMessage();
			JsonNode nd = (new ObjectMapper()).readTree(message);
			String downloadUrl = nd.get("file_url").asText();
			if (monitor.isCanceled()) return cancelled();
			downloadFile = connect.downloadFileFromUrl(downloadUrl);
			monitor.worked(1);
			monitor.done();
		}catch (Exception ex){
			record.setStatus(Status.ERROR);
			ConnectPlugIn.log(ex.getMessage(), ex);
			//TODO: this error is never display to user
		}
		return org.eclipse.core.runtime.Status.OK_STATUS;
	}
	
	private IStatus cancelled(){
		record.setStatus(Status.ERROR);
		return org.eclipse.core.runtime.Status.CANCEL_STATUS;
	}

	public Path getDownloadFile(){
		return this.downloadFile;
	}
}
