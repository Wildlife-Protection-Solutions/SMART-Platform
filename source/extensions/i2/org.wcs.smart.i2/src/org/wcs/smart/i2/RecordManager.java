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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.wcs.smart.common.attachment.AttachmentInterceptor;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.event.IntelEvents;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityRecord;
import org.wcs.smart.i2.model.IntelRecord;
import org.wcs.smart.i2.model.IntelRecordAttachment;
import org.wcs.smart.i2.model.IntelRecordSource;
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
						MessageDialog.openError(context.get(Shell.class), Messages.RecordManager_ErrorTitle, 
								MessageFormat.format(Messages.RecordManager_InsufficientPrivileges2, record.getTitle()));
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

}
