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
					.add(Restrictions.eq("workItem", item.getUuid()))
					.uniqueResult();
			dqItem.setStatus(ServerDataQueueItem.Status.QUEUED);			
			item.setStatus(Status.COMPLETE);
			
			//TODO: test throw an exception here and ensure
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
