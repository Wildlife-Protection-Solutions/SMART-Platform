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

import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.connect.model.WorkItem;
import org.wcs.smart.connect.model.WorkItem.Status;
import org.wcs.smart.connect.model.WorkItem.Type;
import org.wcs.smart.connect.uploader.IUploadItemProcessor;

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
		try{
			session.beginTransaction();
			
			ServerDataQueueItem dqItem = (ServerDataQueueItem)session.createCriteria(ServerDataQueueItem.class)
					.add(Restrictions.eq("workItem", item.getUuid())) //$NON-NLS-1$
					.uniqueResult();
			dqItem.setStatus(ServerDataQueueItem.Status.QUEUED);			
			item.setStatus(Status.COMPLETE);
			
			session.getTransaction().commit();
		}catch (Exception ex){
			logger.log(Level.SEVERE, ex.getMessage(), ex);
			session.getTransaction().rollback();
			
			session.beginTransaction();
			item.setStatus(org.wcs.smart.connect.model.WorkItem.Status.ERROR);
			item.setMessage(MessageFormat.format("Error processing work item: {0}", ex.getMessage()));
			session.getTransaction().commit();
		}
	}

}
