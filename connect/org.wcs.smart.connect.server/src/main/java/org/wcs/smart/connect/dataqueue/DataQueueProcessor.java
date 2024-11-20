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
package org.wcs.smart.connect.dataqueue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.Session;
import org.wcs.smart.connect.cybertracker.json.importer.SmartMobileJsonFileProcessor;
import org.wcs.smart.connect.cybertracker.json.importer.SmartMobileJsonProcessorManager;
import org.wcs.smart.connect.datastore.DataStoreManager;
import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.connect.model.WorkItem;
import org.wcs.smart.connect.model.WorkItem.Status;
import org.wcs.smart.connect.model.WorkItem.Type;
import org.wcs.smart.connect.uploader.IUploadItemProcessor;
import org.wcs.smart.hibernate.QueryFactory;

/**
 * Data queue processor for processing uploaded data queue items.
 * This sets the status to queued.
 * @author Emily
 *
 */
public class DataQueueProcessor implements IUploadItemProcessor {
	
	private final Logger logger = Logger.getLogger(DataQueueProcessor.class.getName());
	
	@Override
	public Type getSupportedType() {
		return Type.UP_DATAQUEUE;
	}

	@Override
	public void processItem(WorkItem item, Session session) {
		//update the status of the data queue item to processing
		ServerDataQueueItem dqItem = null;
		try{
			session.beginTransaction();
			
			dqItem = QueryFactory.buildQuery(session, ServerDataQueueItem.class, "workItem", item.getUuid()).uniqueResult(); //$NON-NLS-1$
			dqItem.setStatus(ServerDataQueueItem.Status.QUEUED);			
			item.setStatus(Status.COMPLETE);
			session.merge(item);
			session.getTransaction().commit();
		}catch (Exception ex){
			logger.log(Level.SEVERE, ex.getMessage(), ex);
			session.getTransaction().rollback();
			
			session.beginTransaction();
			item.setStatus(org.wcs.smart.connect.model.WorkItem.Status.ERROR);
			item.setMessage(MessageFormat.format(Messages.getString("DataQueueProcessor.DataQueueProcessorError", item.getLocale()), ex.getMessage())); //$NON-NLS-1$
			session.merge(item);
			session.getTransaction().commit();
		}
		
		//check for duplicate SMART Mobile file
		if (dqItem.getType().equals( SmartMobileJsonFileProcessor.CT_TYPE ) || dqItem.getType().equals(SmartMobileJsonFileProcessor.CT_ZIP_TYPE)) {
			session.beginTransaction();
			
			try {
				List<String> files = session.createQuery("SELECT file FROM ServerDataQueueItem WHERE conservationArea = :ca AND type = :type AND uuid != :uuid", String.class) //$NON-NLS-1$
				.setParameter("ca", dqItem.getConservationArea()) //$NON-NLS-1$
				.setParameter("type", dqItem.getType()) //$NON-NLS-1$
				.setParameter("uuid", dqItem.getUuid()) //$NON-NLS-1$
				.list();
				
				Path thisFile = DataStoreManager.INSTANCE.getFile(dqItem.getFile());
				
				for (String file : files) {
					Path thatFile = DataStoreManager.INSTANCE.getFile(file);
					if (!Files.exists(thatFile)) continue;
					try {
						if (Files.mismatch(thisFile, thatFile) == -1) {
							//this is a duplicate file
							dqItem.setStatus(ServerDataQueueItem.Status.DUPLICATE);
							dqItem.setStatusMessage(Messages.getString("DataQueueProcessor.DuplicateFileMessage", item.getLocale())); //$NON-NLS-1$
							break;
						}
					}catch (IOException ex) {
						logger.log(Level.WARNING, ex.getMessage(), ex);
					}
				}
				session.getTransaction().commit();
			}catch (Exception ex) {
				session.getTransaction().rollback();
				logger.log(Level.WARNING, ex.getMessage(), ex);
			}			
		}

		//immediately start processing json file
		SmartMobileJsonProcessorManager.INSTANCE.startProcessing(session.getFactory());
	}

}
