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

import org.hibernate.Session;
import org.wcs.smart.ILoginHandler;
import org.wcs.smart.SmartContext;
import org.wcs.smart.connect.ConnectDatastore;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.internal.Messages;
import org.wcs.smart.connect.model.ConnectServerStatus;
import org.wcs.smart.connect.model.ConnectSyncHistoryRecord;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.util.SmartUtils;

/**
 * Login handler to clean up SMART connect processes in cases
 * where SMART terminates in the middle of a background job.
 * Also cleans out filestore.
 * 
 * @author Emily
 *
 */
public class PostLoginHandler implements ILoginHandler {

	@Override
	public void onLogin() throws Exception {
		ConnectServerStatus status;
		try(Session s = HibernateManager.openSession()){
		
			s.beginTransaction();
			try {
				//enable replication; we always want to enable replication if logging
				//into a database; triggers will ensure only correct ca data is recorded in
				//the log tables
				DerbyReplicationManager.INSTANCE.enableReplication(s);
	
				//get status
				status = (ConnectServerStatus)s.get(ConnectServerStatus.class, SmartDB.getCurrentConservationArea().getUuid());	
				s.getTransaction().commit();
			}catch (Exception ex) {
				s.getTransaction().rollback();
				throw ex;
			}
		}

		if (status == null){
			cleanUpFilestore();
			return;
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
		//delete all files in the download temp directory
		Path smartConnect = Paths.get(SmartContext.INSTANCE.getFilestoreLocation(), ConnectDatastore.CONNECT_FILESTORE_DIR)
				.resolve(ConnectDatastore.DOWNLOAD_FILESTORE_DIR);
		if(Files.exists(smartConnect)){
			try(DirectoryStream<Path> stream = Files.newDirectoryStream(smartConnect)){
				for (Path p : stream){
					try{
						Files.deleteIfExists(p);
					}catch (Exception ex){
						ConnectPlugIn.log(ex.getMessage(), ex);
					}
				}
			}catch (Exception ex){
				ConnectPlugIn.log(ex.getMessage(), ex);
			}
		}
		
		//in the replication directory 
		//we want to delete any files not associated with
		//1. an active CA upload form any CA 
		//2. an active download sync from any ca

		final List<Path> filesToKeep = new ArrayList<Path>();
		try(Session s = HibernateManager.openSession()){
			
			List<ConnectServerStatus> toKeep = QueryFactory.buildQuery(s, ConnectServerStatus.class, "status", ConnectServerStatus.Status.UPLOAD) //$NON-NLS-1$
					.list(); 
			
			for(ConnectServerStatus server : toKeep){
				Path fileToKeep = Paths.get(SmartContext.INSTANCE.getFilestoreLocation(), server.getLocalFile());
				filesToKeep.add(fileToKeep);
			}
			
			List<ConnectSyncHistoryRecord> upToKeep = QueryFactory.buildQuery(s, ConnectSyncHistoryRecord.class, 
					new Object[] {"status", ConnectSyncHistoryRecord.Status.ACTIVE}, //$NON-NLS-1$
					new Object[] {"type", ConnectSyncHistoryRecord.Type.UPLOAD} //$NON-NLS-1$
					).list(); 
			
			for (ConnectSyncHistoryRecord syn : upToKeep){
				Path fileToKeep = Paths.get(SmartContext.INSTANCE.getFilestoreLocation(), syn.getChangeLogZipFile());
				filesToKeep.add(fileToKeep);
			}
			
		}
		
		//delete all files that are not in the filesToKeep Array
		Path replicationDir = Paths.get(SmartContext.INSTANCE.getFilestoreLocation(), ConnectDatastore.CONNECT_FILESTORE_DIR)
				.resolve(ConnectDatastore.REPLICATION_FILESTORE_DIR);
		if (Files.exists(replicationDir)){
			try(DirectoryStream<Path> stream = Files.newDirectoryStream(replicationDir, new DirectoryStream.Filter<Path>(){
				@Override
				public boolean accept(Path entry) throws IOException {
					return !filesToKeep.contains(entry);
				}})){
				
				for (Path p : stream){
					try{
						if (Files.isDirectory(p)){
							SmartUtils.deleteDirectory(p);
						}else if (Files.exists(p)){
							Files.delete(p);
						}
					}catch (Exception ex){
						ConnectPlugIn.log(Messages.LoginHandler_FileDeleteError + p.toString(), ex);
					}
				}
				
			}catch (Exception ex){
				ConnectPlugIn.log(Messages.LoginHandler_FilesDeleteError, ex);
			}
		}	
	}
}
