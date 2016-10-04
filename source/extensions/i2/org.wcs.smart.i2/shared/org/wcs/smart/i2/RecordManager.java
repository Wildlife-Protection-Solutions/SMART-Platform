package org.wcs.smart.i2;

import java.util.UUID;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.i2.event.IntelEvents;
import org.wcs.smart.i2.model.IntelRecord;
import org.wcs.smart.i2.model.IntelRecordAttachment;

public enum RecordManager {
	
	INSTANCE;
	
	/**
	 * Delete an intelligence record, associated locations, and attachments
	 * 
	 */
	public void deleteRecord(IntelRecord record, Session session) throws Exception{
		
		record = (IntelRecord) session.get(IntelRecord.class, record.getUuid());
		if (record.getAttachments() != null){
			for (IntelRecordAttachment attachment : record.getAttachments()){
				if (AttachmentManager.INSTANCE.canDelete(attachment.getAttachment(), session)){
					session.delete(attachment.getAttachment());
				}
				session.delete(attachment);
			}
		}
		if (record.getLocations() != null){
			//TODO: if can delete -- remove entity list
			
		}
		session.delete(record);
	}

	public void deleteRecord(IntelRecord record, IEclipseContext context){
		Session s = HibernateManager.openSession();
		try{
			s.beginTransaction();
			deleteRecord(record, s);
			s.getTransaction().commit();
		}catch (Exception ex){
			Intelligence2PlugIn.displayLog("Error deleting record. " + ex.getMessage(), ex);
			return;
		}
		context.get(IEventBroker.class).send(IntelEvents.RECORD_DELETE, record);
	}
	
	public void deleteRecord(UUID recordUuid, IEclipseContext context){
		IntelRecord record = null;
		Session s = HibernateManager.openSession();
		try{
			s.beginTransaction();
			record = (IntelRecord) s.get(IntelRecord.class, recordUuid);
			deleteRecord(record, context);
			s.getTransaction().commit();
		}catch (Exception ex){
			Intelligence2PlugIn.displayLog("Error deleting record. " + ex.getMessage(), ex);
			return;
		}
		context.get(IEventBroker.class).send(IntelEvents.RECORD_DELETE, record);
	}
}
