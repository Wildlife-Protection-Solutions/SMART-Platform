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
package org.wcs.smart.observation;

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.ArrayUtils;
import org.hibernate.event.spi.PreInsertEvent;
import org.hibernate.event.spi.PreInsertEventListener;
import org.hibernate.event.spi.PreUpdateEvent;
import org.hibernate.event.spi.PreUpdateEventListener;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.model.ObservationAttachment;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointAttachment;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;

/**
 * Hibernate listener for audit items to update
 * date created, date modified, and employees fields
 * 
 * @author Emily
 *
 */
public class WaypointHibernateListener implements PreInsertEventListener, PreUpdateEventListener{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public boolean onPreUpdate(PreUpdateEvent event) {
		Waypoint wp = null;
		if (event.getEntity() instanceof Waypoint){
			wp = (Waypoint)event.getEntity();
			
			wp.setLastModified(new Date());
			wp.setLastModifiedBy(SmartDB.getCurrentEmployee());
			setValue(event.getState(), event.getPersister().getEntityMetamodel().getPropertyNames(), "lastModified", wp.getLastModified(), wp); //$NON-NLS-1$
			setValue(event.getState(), event.getPersister().getEntityMetamodel().getPropertyNames(), "lastModifiedBy", wp.getLastModifiedBy(), wp); //$NON-NLS-1$
			
			return false;
			
		}else if (event.getEntity() instanceof WaypointAttachment) {
			wp = ((WaypointAttachment)event.getEntity()).getWaypoint();
		}else if (event.getEntity() instanceof WaypointObservation) {
			wp = ((WaypointObservation)event.getEntity()).getWaypoint();
		}else if (event.getEntity() instanceof WaypointObservationAttribute) {
			wp = ((WaypointObservationAttribute)event.getEntity()).getObservation().getWaypoint();
		}else if (event.getEntity() instanceof ObservationAttachment) {
			wp = ((ObservationAttachment)event.getEntity()).getObservation().getWaypoint();
		}
		if (wp != null) {
			//this will fire this event again for the waypoint
			//updating the session objects as required
			wp.setLastModified(new Date());
		}

		return false;
	}

	@Override
	public boolean onPreInsert(PreInsertEvent event) {
		Waypoint wp = null;
		if (event.getEntity() instanceof Waypoint){
			wp = (Waypoint)event.getEntity();
			
			wp.setLastModified(new Date());
			wp.setLastModifiedBy(SmartDB.getCurrentEmployee());
		
			setValue(event.getState(), event.getPersister().getEntityMetamodel().getPropertyNames(), "lastModified", wp.getLastModified(), wp); //$NON-NLS-1$
			setValue(event.getState(), event.getPersister().getEntityMetamodel().getPropertyNames(), "lastModifiedBy", wp.getLastModifiedBy(), wp); //$NON-NLS-1$
			
			return false;
			
		}else if (event.getEntity() instanceof WaypointAttachment) {
			wp = ((WaypointAttachment)event.getEntity()).getWaypoint();
		}else if (event.getEntity() instanceof WaypointObservation) {
			wp = ((WaypointObservation)event.getEntity()).getWaypoint();
		}else if (event.getEntity() instanceof WaypointObservationAttribute) {
			wp = ((WaypointObservationAttribute)event.getEntity()).getObservation().getWaypoint();
		}else if (event.getEntity() instanceof ObservationAttachment) {
			wp = ((ObservationAttachment)event.getEntity()).getObservation().getWaypoint();
		}
		if (wp != null) {
			//this will fire this event again for the waypoint
			//updating the session objects as required
			wp.setLastModified(new Date());
		}

		return false;
	}

	
	private void setValue(Object[] currentState, String[] propertyNames, String propertyToSet, Object value, Object entity) {
		int index = ArrayUtils.indexOf(propertyNames, propertyToSet);
		if (index >= 0) {
			currentState[index] = value;
		} else {
			Logger.getLogger(WaypointHibernateListener.class.getName()).log(Level.INFO, "Field '" + propertyToSet+ "' not found on entity '" + entity.getClass().getName() + "'."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
	}
}
