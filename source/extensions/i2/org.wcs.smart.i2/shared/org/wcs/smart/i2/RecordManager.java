package org.wcs.smart.i2;

import org.hibernate.Query;
import org.hibernate.Session;
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

}
