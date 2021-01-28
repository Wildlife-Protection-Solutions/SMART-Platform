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

import java.text.Collator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.common.attachment.AttachmentInterceptor;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.event.IntelEvents;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityRecord;
import org.wcs.smart.i2.model.IntelRecord;
import org.wcs.smart.i2.model.IntelRecordAttachment;
import org.wcs.smart.i2.model.IntelRecordSource;
import org.wcs.smart.i2.model.IntelRecordSourceAttribute;
import org.wcs.smart.i2.security.IntelSecurityManager;
import org.wcs.smart.i2.ui.editors.record.RecordEditorInput;

/**
 * Functions for managing intelligence records
 * 
 * @author Emily
 *
 */
public enum RecordManager {
	
	INSTANCE;
	
	public List<IntelRecordSource> getSources(Session session){
		List<IntelRecordSource> sources = QueryFactory.buildQuery(session,
				IntelRecordSource.class,"conservationArea",  //$NON-NLS-1$
				SmartDB.getCurrentConservationArea()).getResultList();
		sources.sort((a,b)->Collator.getInstance().compare(a.getName(), b.getName()));

		return sources;

	}
	/**
	 * Delete an intelligence record, associated locations, and attachments
	 * 
	 */
	public void deleteRecord(IntelRecord record, Session session) throws Exception{
		
		record = (IntelRecord) session.get(IntelRecord.class, record.getUuid());

		Query<?> q = session.createQuery("DELETE FROM IntelEntityLocation where id.location IN (FROM IntelLocation ll WHERE ll.record = :record)"); //$NON-NLS-1$
		q.setParameter("record", record); //$NON-NLS-1$
		q.executeUpdate();
		
		q = session.createQuery("DELETE FROM IntelWorkingSetRecord where id.record = :record"); //$NON-NLS-1$
		q.setParameter("record", record); //$NON-NLS-1$
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

	/**
	 * Deletes a collection of intelligence records.
	 * @param records list of IntelRecord or RecordEditorInput
	 * @param context
	 * @param monitor the progress monitor to use for reporting progress to the user. It is the caller's responsibility 
	 *  to call done() on the given monitor. 
	 */
	public void deleteRecords(Collection<? extends Object> records, IEclipseContext context, IProgressMonitor monitor){

		SubMonitor progress = SubMonitor.convert(monitor, Messages.RecordManager_Progress1, records.size());
		List<IntelEntity> entities = new ArrayList<IntelEntity>();
		List<IntelRecord> deleted = new ArrayList<IntelRecord>();
		try(Session s = HibernateManager.openSession(new AttachmentInterceptor())){
			s.beginTransaction();
			try{
				for (Object x : records){
					IntelRecord record = null;
					if (x instanceof IntelRecord){
						record = (IntelRecord) s.get(IntelRecord.class, ((IntelRecord) x).getUuid());		
					}else if (x instanceof RecordEditorInput){
						record = (IntelRecord) s.get(IntelRecord.class, ((RecordEditorInput) x).getUuid());
					}else{
						//this should never happen but if we don't have a record then ignore
						continue;
					}
					if (!IntelSecurityManager.INSTANCE.canDeleteRecord(record.getProfile())){
						final String title = record.getTitle();
						Display.getDefault().syncExec(()->{
							MessageDialog.openError(context.get(Shell.class), Messages.RecordManager_ErrorTitle, 
								MessageFormat.format(Messages.RecordManager_InsufficientPrivileges2, title));
						});
						continue;
					}
					progress.subTask(record.getTitle());
					for (IntelEntityRecord r : record.getEntities()){
						entities.add(r.getEntity());
					}
					
					deleteRecord(record, s);
					deleted.add(record);
					progress.worked(1);
					progress.checkCanceled();
				}
				
				s.getTransaction().commit();
			}catch (OperationCanceledException e) {
				s.getTransaction().rollback();
				return;
			}catch (Exception ex){
				s.getTransaction().rollback();
				Intelligence2PlugIn.displayLog(Messages.RecordManager_DeleteError + ex.getMessage(), ex);
				return;
			}
		}
		IEventBroker broker = context.get(IEventBroker.class);
		broker.send(IntelEvents.RECORD_DELETE, deleted);
		if (!entities.isEmpty()) broker.send(IntelEvents.ENTITY_MODIFIED, entities);
	}

	/**
	 * Checks for another record of the same type in the same conservation area with
	 * the same source that has a matching attribute
	 * 
	 * @param newId new value
	 * @param iattribute attribute to check
	 * @param type source to check
	 * @param ca
	 * @param session
	 * @return
	 */
	public boolean isDuplicateId(Object newId, IntelAttribute iattribute, IntelRecordSource type,
			ConservationArea ca, Session session, UUID currentRecord){
		
		if (newId == null) return false;
		
		//search attributes
		for(IntelRecordSourceAttribute eattribute: type.getAttributes()) {
			if (!iattribute.equals(eattribute.getAttribute()))continue;
			if (!eattribute.getDuplicateCheck()) continue;
			
			IntelAttribute attribute = eattribute.getAttribute();
			
			String query = "SELECT count(*) FROM IntelRecord e join e.attributes as v where e.conservationArea = :ca and e.recordSource = :type and v.attribute.attribute = :attribute "; //$NON-NLS-1$
			switch(attribute.getType()){
				case BOOLEAN:
				case POSITION:
					return false;	//don't both checking we will always have duplicates
				case DATE:
					query += " and v.stringValue = :test"; //$NON-NLS-1$
					break;
				case LIST:
					query += " and v.attributeListItem = :test"; //$NON-NLS-1$
					break;
				case EMPLOYEE:
					query += " and v.employee = :test"; //$NON-NLS-1$
					break;
				case NUMERIC:
					query += " and v.numberValue = :test"; //$NON-NLS-1$
					break;
				case TEXT:
					query += " and v.stringValue = :test ";  //$NON-NLS-1$
					break;
			}
			
			if (currentRecord != null){
				query += " AND e.uuid != :entity "; //$NON-NLS-1$
			}
			Query<?> hql = session.createQuery(query);
			hql.setParameter("attribute", attribute); //$NON-NLS-1$
			hql.setParameter("ca", ca); //$NON-NLS-1$
			hql.setParameter("type", type); //$NON-NLS-1$
			switch(attribute.getType()){
				case BOOLEAN: 
				case POSITION:
					return false; // not supported
				case DATE:
					hql.setParameter("test", ((java.sql.Date)newId).toString()); //$NON-NLS-1$
					break;
				case LIST:
				case NUMERIC:
				case TEXT:
				case EMPLOYEE:
					hql.setParameter("test", newId); //$NON-NLS-1$
					break;
					
			}
			if (currentRecord != null){
				hql.setParameter("entity",  currentRecord); //$NON-NLS-1$
			}
	
			long cnt = (Long) hql.uniqueResult();
			if (cnt > 0) return true;
		}
		return false;
		
	}
	
	/**
	 * Checks for another record of the same name 
	 * 
	 * @return
	 */
	public boolean isDuplicateName(String name, ConservationArea ca, Session session, UUID currentRecord){
		
		boolean isnew = currentRecord == null;
		
		CriteriaBuilder cb = session.getCriteriaBuilder();
		CriteriaQuery<Long> c = cb.createQuery(Long.class);
		Root<IntelRecord> from = c.from(IntelRecord.class);
		c.select(cb.count(from));
		Predicate[] p = new Predicate[isnew?2:3];
		p[0] = cb.equal(from.get("conservationArea"), ca); //$NON-NLS-1$
		p[1] = cb.equal(from.get("title"), name); //$NON-NLS-1$
		if (!isnew) {
			p[2] =cb.notEqual(from.get("uuid"), currentRecord); //$NON-NLS-1$
		} 
		c.where(cb.and(p));
		Long dupCnt = session.createQuery(c).uniqueResult();
		
		return dupCnt > 0;
				
	}
}
