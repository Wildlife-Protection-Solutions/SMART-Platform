/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.common.attachment.AttachmentInterceptor;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.i2.event.IntelEvents;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityRecord;
import org.wcs.smart.i2.model.IntelRecord;
import org.wcs.smart.i2.model.IntelRecordAttachment;

/**
 * Functions for managing intelligence records
 * 
 * @author Emily
 *
 */
public enum RecordManager {
	
	INSTANCE;
	
	/**
	 * Delete an intelligence record, associated locations, and attachments
	 * 
	 */
	private void deleteRecord(IntelRecord record, Session session) throws Exception{
		
		record = (IntelRecord) session.get(IntelRecord.class, record.getUuid());

		Query q = session.createQuery("DELETE FROM IntelEntityLocation where id.location IN (FROM IntelLocation ll WHERE ll.record = :record)");
		q.setParameter("record", record);
		q.executeUpdate();
		
		q = session.createQuery("DELETE FROM IntelWorkingSetRecord where id.record = :record");
		q.setParameter("record", record);
		q.executeUpdate();
		
		session.delete(record);
		
		if (record.getAttachments() != null){
			for (IntelRecordAttachment attachment : record.getAttachments()){
				if (AttachmentManager.INSTANCE.canDelete(attachment.getAttachment(), session)){
					session.delete(attachment.getAttachment());
				}
			}
		}
	}

	public void deleteRecord(IntelRecord record, IEclipseContext context){
		Session s = HibernateManager.openSession(new AttachmentInterceptor());
		List<IntelEntity> entities = new ArrayList<IntelEntity>();
		try{
			s.beginTransaction();
			record = (IntelRecord) s.get(IntelRecord.class, record.getUuid());
			for (IntelEntityRecord r : record.getEntities()){
				entities.add(r.getEntity());
			}
			deleteRecord(record, s);
			s.getTransaction().commit();
		}catch (Exception ex){
			s.getTransaction().rollback();
			Intelligence2PlugIn.displayLog("Error deleting record. " + ex.getMessage(), ex);
			return;
		}finally{
			s.close();
		}
		context.get(IEventBroker.class).send(IntelEvents.RECORD_DELETE, record);
		entities.forEach(e -> context.get(IEventBroker.class).send(IntelEvents.ENTITY_MODIFIED, e));
	}
	
	public void deleteRecord(UUID recordUuid, IEclipseContext context){
		IntelRecord record = null;
		Session s = HibernateManager.openSession();
		try{
			record = (IntelRecord) s.get(IntelRecord.class, recordUuid);
		}catch (Exception ex){
			Intelligence2PlugIn.displayLog("Error deleting record. " + ex.getMessage(), ex);
			return;
		}
		if (record != null){
			deleteRecord(record, context);
		}
		
	}
}
