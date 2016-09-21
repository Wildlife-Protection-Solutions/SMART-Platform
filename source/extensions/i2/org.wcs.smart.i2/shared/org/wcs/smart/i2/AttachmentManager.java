package org.wcs.smart.i2;

import java.text.Collator;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.advisors.DeleteManager;
import org.wcs.smart.i2.model.IntelAttachment;
import org.wcs.smart.i2.model.IntelEntityAttachment;
import org.wcs.smart.i2.model.IntelEntityRecord;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelLocation;
import org.wcs.smart.i2.model.IntelRecordAttachment;

public enum AttachmentManager {
	INSTANCE;
	
	private AttachmentManager(){
		
	}

	
	/**
	 * Determines if all links to the attachment are removed.
	 * 
	 * @param type
	 * @param session
	 * @throws Exception
	 */
	public boolean canDelete(IntelAttachment attachment, Session session) throws Exception{
		//attachments are linked to:
		//entities; records; observations
		
		Long recordCnt = (Long)session.createCriteria(IntelRecordAttachment.class)
			.add(Restrictions.eq("id.attachment", attachment))
			.setProjection(Projections.rowCount())
			.uniqueResult();
		if (recordCnt != 0) return false;
		
		recordCnt = (Long)session.createCriteria(IntelEntityAttachment.class)
				.add(Restrictions.eq("id.attachment", attachment))
				.setProjection(Projections.rowCount())
				.uniqueResult();
		if (recordCnt != 0) return false;
		
		//TODO: check observations
//		Long recordCnt = (Long)session.createCriteria(IntelL.class)
//				.add(Restrictions.eq("id.attachment", attachment))
//				.setProjection(Projections.rowCount())
//				.uniqueResult();
//			if (recordCnt != 0) return false;
		return true;
			
	}
}
