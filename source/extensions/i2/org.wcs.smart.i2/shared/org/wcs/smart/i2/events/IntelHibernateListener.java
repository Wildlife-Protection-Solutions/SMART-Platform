package org.wcs.smart.i2.events;

import java.util.Date;

import org.apache.commons.lang.ArrayUtils;
import org.hibernate.event.spi.PreInsertEvent;
import org.hibernate.event.spi.PreInsertEventListener;
import org.hibernate.event.spi.PreUpdateEvent;
import org.hibernate.event.spi.PreUpdateEventListener;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.model.IIntelAuditItem;

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
			Intelligence2PlugIn.log("Field '" + propertyToSet
					+ "' not found on entity '" + entity.getClass().getName()
					+ "'.", null);
		}
	}
}
