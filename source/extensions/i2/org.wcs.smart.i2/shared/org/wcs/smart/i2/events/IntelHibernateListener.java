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
package org.wcs.smart.i2.events;

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.ArrayUtils;
import org.hibernate.event.spi.PreInsertEvent;
import org.hibernate.event.spi.PreInsertEventListener;
import org.hibernate.event.spi.PreUpdateEvent;
import org.hibernate.event.spi.PreUpdateEventListener;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.model.IIntelAuditItem;

/**
 * Hibernate listener for audit items to update
 * date created, date modified, and employees fields
 * 
 * @author Emily
 *
 */
public class IntelHibernateListener implements PreInsertEventListener, PreUpdateEventListener{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public boolean onPreUpdate(PreUpdateEvent event) {
		if (event.getEntity() instanceof IIntelAuditItem){
			IIntelAuditItem item = (IIntelAuditItem) event.getEntity();
			
			Date now = new Date();
			
			if (item.getCreatedBy() == null){
    			item.setCreatedBy(SmartDB.getCurrentEmployee());
    			item.setDateCreated(now);
    			setValue(event.getState(), event.getPersister().getEntityMetamodel().getPropertyNames(), "createdBy", item.getCreatedBy(), item);
                setValue(event.getState(), event.getPersister().getEntityMetamodel().getPropertyNames(), "dateCreated", item.getDateCreated(), item);
            }
			
			item.setLastModifiedBy(SmartDB.getCurrentEmployee());
			item.setDateModified(now);
			
			 setValue(event.getState(), event.getPersister().getEntityMetamodel().getPropertyNames(), "lastModifiedBy", item.getLastModifiedBy(), item);
             setValue(event.getState(), event.getPersister().getEntityMetamodel().getPropertyNames(), "dateModified", item.getDateModified(), item);
		}
		return false;
	}

	@Override
	public boolean onPreInsert(PreInsertEvent event) {
		if (event.getEntity() instanceof IIntelAuditItem){
			IIntelAuditItem item = (IIntelAuditItem) event.getEntity();
			
			Date now = new Date();
			
			if (item.getCreatedBy() == null){
    			item.setCreatedBy(SmartDB.getCurrentEmployee());
    			item.setDateCreated(now);
    			setValue(event.getState(), event.getPersister().getEntityMetamodel().getPropertyNames(), "createdBy", item.getCreatedBy(), item);
                setValue(event.getState(), event.getPersister().getEntityMetamodel().getPropertyNames(), "dateCreated", item.getDateCreated(), item);
            }
			
			item.setLastModifiedBy(SmartDB.getCurrentEmployee());
			item.setDateModified(now);
			setValue(event.getState(), event.getPersister().getEntityMetamodel().getPropertyNames(), "lastModifiedBy", item.getLastModifiedBy(), item);
            setValue(event.getState(), event.getPersister().getEntityMetamodel().getPropertyNames(), "dateModified", item.getDateModified(), item);

            
			 
		}
		return false;
	}

	private void setValue(Object[] currentState, String[] propertyNames, String propertyToSet, Object value, Object entity) {
		int index = ArrayUtils.indexOf(propertyNames, propertyToSet);
		if (index >= 0) {
			currentState[index] = value;
		} else {
			Logger.getLogger(IntelHibernateListener.class.getName()).log(Level.INFO, "Field '" + propertyToSet+ "' not found on entity '" + entity.getClass().getName() + "'.");
		}
	}
}
