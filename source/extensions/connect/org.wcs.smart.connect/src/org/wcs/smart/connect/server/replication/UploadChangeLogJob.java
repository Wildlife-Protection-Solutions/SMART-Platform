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

import java.nio.file.FileSystems;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.hibernate.Session;
import org.wcs.smart.SmartContext;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.SmartConnect;
import org.wcs.smart.connect.api.model.WorkItemStatus;
import org.wcs.smart.connect.model.ConnectServerStatus;
import org.wcs.smart.connect.model.ConnectSyncHistoryRecord;
import org.wcs.smart.connect.server.FileUploaderJob;
import org.wcs.smart.hibernate.HibernateManager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** 
 * This job is managed by the UploadChangeLogEngine and
 * is configured so that only one will be running
 * at a time. 
 * 
 * @author Emily
 *
 */
public class UploadChangeLogJob extends FileUploaderJob {

	private ConnectSyncHistoryRecord item;

	public UploadChangeLogJob(ConnectSyncHistoryRecord item, 
			SmartConnect connect) {
		super(null, FileSystems.getDefault().getPath(SmartContext.INSTANCE.getFilestoreLocation(), item.getChangeLogZipFile()),
				connect, "Upload changes to SMART Connect");
		this.item = item;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		//reload item to ensure status is not done
		//that will happen if another job was processing the item
		//before this job started.
		
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
			return Status.OK_STATUS;
		}		
		return Status.OK_STATUS;
	}

	@Override
	protected void onUploadComplete(WorkItemStatus upstatus) {
		deleteLocalFile();
	}
	
	@Override
	protected void onProcessingComplete(WorkItemStatus upstatus) {
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
		
		super.connect.close();
	}

	@Override
	protected void onError(WorkItemStatus upstatus) {
		item.setStatus(ConnectSyncHistoryRecord.Status.ERROR);
		saveHistoryRecord();
		deleteLocalFile();
		if (upstatus != null){
			item.setErrorString(upstatus.getMessage());
		}
		
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
