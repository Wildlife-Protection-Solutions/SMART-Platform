/*
 * Copyright (C) 2015 Wildlife Conservation Society
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
package org.wcs.smart.connect.uploader.sync;

import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.Session;
import org.wcs.smart.connect.database.LockManager;
import org.wcs.smart.connect.datastore.DataStoreManager;
import org.wcs.smart.connect.model.ConservationAreaInfo;
import org.wcs.smart.connect.model.ConservationAreaInfo.Status;
import org.wcs.smart.connect.model.WorkItem;
import org.wcs.smart.connect.model.WorkItem.Type;
import org.wcs.smart.connect.uploader.IUploadItemProcessor;

public class SyncUploadCaProcessor implements IUploadItemProcessor {
	
	private final Logger logger = Logger.getLogger(SyncUploadCaProcessor.class.getName());
	
	@Override
	public Type getSupportedType() {
		return WorkItem.Type.UP_SYNC;
	}

	@Override
	public void processItem(WorkItem item, Session session) {
		try{
			LockManager.INSTANCE.lockDatabase(session, item.getConservationAreaInfo());
		}catch (Exception ex){
			logger.log(Level.SEVERE, "Could not lock database to apply upload changelog. " + item.getUuid());
			
			//set error status
			session.beginTransaction();
			item.setStatus(org.wcs.smart.connect.model.WorkItem.Status.ERROR);
			item.setMessage(MessageFormat.format("Error processing item {0}: {1}.", item.getUuid().toString(), ex.getMessage()));
			session.getTransaction().commit();
			
			cleanUp(item);
			
			return;
		}
		
		try{
			session.beginTransaction();
			
			try{
				session.update(item);
	
				ConservationAreaInfo info = (ConservationAreaInfo) session.get(ConservationAreaInfo.class, item.getConservationAreaInfo().getUuid());
				if (info.getStatus() == Status.NODATA){
					throw new Exception("No data loaded for conservation area.  Cannot sync until data has been uploaded.");
				}
				
				//load data
				Path file = DataStoreManager.INSTANCE.getFile(item.getLocalFilename()).toPath();
				PostgresqlSyncProcessor processor = new PostgresqlSyncProcessor(file, info, session);
				processor.processFile();
				
				session.flush();
	
				long revision = ChangeLogManager.INSTANCE.getLastRevision(session, info.getUuid());
				item.setMessage("{\"server_revision\": " + revision + "}");
				
				item.setStatus(org.wcs.smart.connect.model.WorkItem.Status.COMPLETE);
				
				session.getTransaction().commit();
			}catch (Exception ex){
				logger.log(Level.SEVERE, ex.getMessage(), ex);
				session.getTransaction().rollback();
				
				session.beginTransaction();
				item.setStatus(org.wcs.smart.connect.model.WorkItem.Status.ERROR);
				item.setMessage(MessageFormat.format("Error processing work item: {0}", ex.getMessage()));
				session.getTransaction().commit();
			}
		}finally{
			try{
				LockManager.INSTANCE.releaseDatabase(session, item.getConservationAreaInfo());
			}catch (Exception ex){
				logger.log(Level.SEVERE, "Could not release database lock after applying upload changes. " + item.getUuid());
			}
			cleanUp(item);
		}
	}
	
	private void cleanUp(WorkItem item){
		try{
			Files.deleteIfExists(DataStoreManager.INSTANCE.getFile(item.getLocalFilename()).toPath());
		}catch (Exception ex){
			logger.log(Level.WARNING, "Could not delete ca upload file.", ex);
		}
	}

}
