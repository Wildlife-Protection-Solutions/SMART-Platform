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
package org.wcs.smart.connect.server.replication;

import java.nio.file.Path;
import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.SmartConnect;
import org.wcs.smart.connect.api.model.WorkItemStatus;
import org.wcs.smart.connect.model.ConnectServerOption;
import org.wcs.smart.connect.model.ConnectServerStatus;
import org.wcs.smart.connect.model.ConnectSyncHistoryRecord;
import org.wcs.smart.connect.model.ConnectSyncHistoryRecord.Status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This job downloads a change package from SMART
 * Connect.  It does not apply the file.
 * 
 * @author Emily
 *
 */
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
		monitor.beginTask("Downloading change log", 3);
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
				if ( current - start > connect.getServer().getOptionAsInt(ConnectServerOption.Option.MAX_PROCESSING_WAIT_TIME) * 1000000l) throw new Exception("Timed out waiting for export to process.");
				Thread.sleep(connect.getServer().getOptionAsInt(ConnectServerOption.Option.RETY_WAIT_TIME));
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
			Integer promptSize = null;
			if (connect.getServer().getOptionAsBoolean(ConnectServerOption.Option.PACKAGE_PROMPT)){
				promptSize = connect.getServer().getOptionAsInt(ConnectServerOption.Option.PACKAGE_PROMPT_SIZE);
			}
			downloadFile = connect.downloadFileFromUrl(downloadUrl,promptSize);
			monitor.worked(1);
			monitor.done();
		}catch (PackageToLargeException ex){
			record.setStatus(Status.ERROR);
			record.setErrorString(ex.getMessage());
			ConnectPlugIn.log(ex.getMessage(), ex);
		}catch (Exception ex){
			record.setStatus(Status.ERROR);
			record.setErrorString("Error downloading file:" + ex.getMessage());
			ConnectPlugIn.log(ex.getMessage(), ex);
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
