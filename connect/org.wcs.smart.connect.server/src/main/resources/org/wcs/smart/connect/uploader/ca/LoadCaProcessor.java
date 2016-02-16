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
package org.wcs.smart.connect.uploader.ca;

import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.connect.datastore.DataStoreManager;
import org.wcs.smart.connect.model.ConservationAreaInfo;
import org.wcs.smart.connect.model.WorkItem;
import org.wcs.smart.connect.model.WorkItem.Status;
import org.wcs.smart.connect.model.WorkItem.Type;
import org.wcs.smart.connect.uploader.IUploadItemProcessor;

/**
 * A upload item processor that loads a conservation area export into
 * the database.
 * 
 * @author Emily
 *
 */
public class LoadCaProcessor implements IUploadItemProcessor {
	
	private final Logger logger = Logger.getLogger(LoadCaProcessor.class.getName());
	
	@Override
	public Type getSupportedType() {
		return WorkItem.Type.UP_CA;
	}

	@Override
	public void processItem(WorkItem item, Session session) {
		session.beginTransaction();
		
		try{
			session.update(item);

			ConservationAreaInfo info = (ConservationAreaInfo) session.get(ConservationAreaInfo.class, item.getConservationAreaInfo().getUuid());
			
			if (info.getStatus() != ConservationAreaInfo.Status.UPLOADING){
				if (info.getStatus() == ConservationAreaInfo.Status.NODATA){
					throw new Exception("Conservation Area deleted from server before Conservation Area import completed.  You need to re-export the Conservation Area to SMART Connect.");	
				}
				//this shouldn't happen - somebody else has loaded data
				throw new Exception("Another process has loaded data for this Conservation Area already.  Cannot duplicate data.");				
			}
			
			//load data
			PostgresqlCaLoader ldr = new PostgresqlCaLoader(session);
			ldr.importData(DataStoreManager.INSTANCE.getFile(item.getLocalFilename()), info);
			session.flush();
			
			//update ca item label
			ConservationArea area =CaProcessorUtils.updateCaLabel(session, info);
			if (area != null){
				if (area.getIsCcaa()){
					info.setStatus(ConservationAreaInfo.Status.CCAA);
				}else{
					info.setStatus(ConservationAreaInfo.Status.DATA);
				}
			}else{
				//this should never happen
				info.setStatus(ConservationAreaInfo.Status.NODATA);
				throw new Exception("Conservation Area was loaded but Conservation Area details were not found. Delete the Conservation area and try again.");
			}
			
			//update item status
			item.setStatus(Status.COMPLETE);
			
			session.getTransaction().commit();
		}catch (Exception ex){
			logger.log(Level.SEVERE, ex.getMessage(), ex);
			ex.printStackTrace();
			session.getTransaction().rollback();
			
			//start a new transaction; update status to error
			session.beginTransaction();
			try{
				//ca exists but data doesn't, so lets update the status to reference that
				ConservationAreaInfo info = (ConservationAreaInfo) session.get(ConservationAreaInfo.class, item.getConservationAreaInfo().getUuid());
				info.setStatus(ConservationAreaInfo.Status.NODATA);
				
				session.update(item);
				item.setStatus(Status.ERROR);
				item.setMessage("Error extracting data: " + ex.getMessage());
				session.getTransaction().commit();
			}catch (Exception ex2){
				logger.log(Level.SEVERE, ex2.getMessage(), ex2);
				session.getTransaction().rollback();
			}
		}finally{
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
