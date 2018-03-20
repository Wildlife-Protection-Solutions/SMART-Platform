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
package org.wcs.smart.event;

import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Hibernate listener for audit items to update
 * date created, date modified, and employees fields
 * 
 * @author Emily
 *
 */
public class EventHibernateListener implements PostInsertEventListener{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public void onPostInsert(PostInsertEvent event) {	
	}

	@Override
	public boolean requiresPostCommitHanding(EntityPersister persister) {
		return false;
	}

//	@Override
//	public boolean onPreUpdate(PreUpdateEvent event) {
//		if (event.getEntity() instanceof IIntelAuditItem){
//			IIntelAuditItem item = (IIntelAuditItem) event.getEntity();
//			
//			Date now = new Date();
//			
//			if (item.getCreatedBy() == null){
//    			item.setCreatedBy(SmartDB.getCurrentEmployee());
//    			item.setDateCreated(now);
//    			setValue(event.getState(), event.getPersister().getEntityMetamodel().getPropertyNames(), "createdBy", item.getCreatedBy(), item); //$NON-NLS-1$
//                setValue(event.getState(), event.getPersister().getEntityMetamodel().getPropertyNames(), "dateCreated", item.getDateCreated(), item); //$NON-NLS-1$
//            }
//			
//			item.setLastModifiedBy(SmartDB.getCurrentEmployee());
//			item.setDateModified(now);
//			
//			 setValue(event.getState(), event.getPersister().getEntityMetamodel().getPropertyNames(), "lastModifiedBy", item.getLastModifiedBy(), item); //$NON-NLS-1$
//             setValue(event.getState(), event.getPersister().getEntityMetamodel().getPropertyNames(), "dateModified", item.getDateModified(), item); //$NON-NLS-1$
//		}
//		return false;
//	}
//
//	@Override
//	public boolean onPreInsert(PreInsertEvent event) {
//		if (event.getEntity() instanceof IIntelAuditItem){
//			IIntelAuditItem item = (IIntelAuditItem) event.getEntity();
//			
//			Date now = new Date();
//			
//			if (item.getCreatedBy() == null){
//    			item.setCreatedBy(SmartDB.getCurrentEmployee());
//    			item.setDateCreated(now);
//    			setValue(event.getState(), event.getPersister().getEntityMetamodel().getPropertyNames(), "createdBy", item.getCreatedBy(), item); //$NON-NLS-1$
//                setValue(event.getState(), event.getPersister().getEntityMetamodel().getPropertyNames(), "dateCreated", item.getDateCreated(), item); //$NON-NLS-1$
//            }
//			
//			item.setLastModifiedBy(SmartDB.getCurrentEmployee());
//			item.setDateModified(now);
//			setValue(event.getState(), event.getPersister().getEntityMetamodel().getPropertyNames(), "lastModifiedBy", item.getLastModifiedBy(), item); //$NON-NLS-1$
//            setValue(event.getState(), event.getPersister().getEntityMetamodel().getPropertyNames(), "dateModified", item.getDateModified(), item); //$NON-NLS-1$
//
//            
//			 
//		}
//		return false;
//	}
//
//	private void setValue(Object[] currentState, String[] propertyNames, String propertyToSet, Object value, Object entity) {
//		int index = ArrayUtils.indexOf(propertyNames, propertyToSet);
//		if (index >= 0) {
//			currentState[index] = value;
//		} else {
//			Logger.getLogger(EventHibernateListener.class.getName()).log(Level.INFO, "Field '" + propertyToSet+ "' not found on entity '" + entity.getClass().getName() + "'."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
//		}
//	}
}
