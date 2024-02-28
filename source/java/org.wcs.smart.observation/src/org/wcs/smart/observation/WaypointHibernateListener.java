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

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.ArrayUtils;
import org.hibernate.Session;
import org.hibernate.event.spi.PreDeleteEvent;
import org.hibernate.event.spi.PreDeleteEventListener;
import org.hibernate.event.spi.PreInsertEvent;
import org.hibernate.event.spi.PreInsertEventListener;
import org.hibernate.event.spi.PreUpdateEvent;
import org.hibernate.event.spi.PreUpdateEventListener;
import org.hibernate.persister.entity.EntityPersister;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.model.ObservationAttachment;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointAttachment;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;

import jakarta.transaction.Synchronization;

/**
 * Hibernate listener for audit items to update
 * date created, date modified, and employees fields
 * 
 * @author Emily
 *
 */
public class WaypointHibernateListener implements PreInsertEventListener, PreUpdateEventListener, PreDeleteEventListener{


	@Override
	public boolean onPreUpdate(PreUpdateEvent event) {
		return processObject(event.getEntity(), event.getState(), event.getPersister(), event.getSession());
	}
	
	@Override
	public boolean onPreInsert(PreInsertEvent event) {
		return processObject(event.getEntity(), event.getState(), event.getPersister(), event.getSession());
	}

	@Override
	public boolean onPreDelete(PreDeleteEvent event) {
		//don't process waypoints
		if (event.getEntity() instanceof Waypoint) return false;
		//want to process other objects incase it's an attachment being deleted
		return processObject(event.getEntity(), null, null, event.getSession());
	}

	private boolean processObject(Object entity, Object[] state, EntityPersister persister, Session session) {
		Waypoint wp = null;
		
		if (entity instanceof Waypoint){
			wp = (Waypoint)entity;
					
			LocalDateTime nowAsUtc = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);
			wp.setLastModified(nowAsUtc);
			
			if (!wp.getConservationArea().equals(SmartDB.getCurrentEmployee().getConservationArea())) {
				//should never be inserting waypoints in ccaa
				//ticket: #3520
				ObservationPlugIn.log("waypoint being modified by employee not associated with CA", null); //$NON-NLS-1$
			}else {
				wp.setLastModifiedBy(SmartDB.getCurrentEmployee());
			}
			
			setValue(state, persister.getEntityPersister().getPropertyNames(), "lastModified", wp.getLastModified(), wp); //$NON-NLS-1$
			setValue(state, persister.getEntityPersister().getPropertyNames(), "lastModifiedBy", wp.getLastModifiedBy(), wp); //$NON-NLS-1$
			
			return false;
			
		}
		
		if (entity instanceof WaypointAttachment) {
			wp = ((WaypointAttachment)entity).getWaypoint();
		}else if (entity instanceof WaypointObservation) {
			wp = ((WaypointObservation)entity).getWaypoint();
		}else if (entity instanceof WaypointObservationAttribute) {
			wp = ((WaypointObservationAttribute)entity).getObservation().getWaypoint();
		}else if (entity instanceof ObservationAttachment) {
			wp = ((ObservationAttachment)entity).getObservation().getWaypoint();
		}
		if (wp != null) {
			//add for update later
			addWaypoint(wp, session);
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

	
	/*
	 * maps session to transaction so can update waypoint last modified 
	 * when session is committed. This is the only way I could find
	 * to successfully and reliable update waypoint audit information when related objects
	 * (attachments, observations) etc were updated.
	 */
	private HashMap<Session, WpSynchronization> listeners = new HashMap<>();
	
	public synchronized void addWaypoint(Waypoint wp, Session session) {
		if (!listeners.containsKey(session)) {
			WpSynchronization sync = new WpSynchronization(session);
			listeners.put(session, sync);
			session.getTransaction().registerSynchronization(sync);
		}
		listeners.get(session).addWaypoint(wp);
	}
	
	private class WpSynchronization implements Synchronization{

		private Set<Waypoint> points = new HashSet<>();
		private Session session;
		
		public WpSynchronization(Session session) {
			this.session = session;
		}
		
		public void addWaypoint(Waypoint wp) {
			this.points.add(wp);
		}
		@Override
		public void afterCompletion(int arg0) {
		}

		@Override
		public void beforeCompletion() {
			listeners.remove(this.session);
			
			LocalDateTime utc = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);
			Employee by = SmartDB.getCurrentEmployee();
			
			for (Waypoint wp : points) {
				
				session.createMutationQuery("UPDATE Waypoint set lastModified=:lm, lastModifiedBy=:by WHERE uuid=:uuid and conservationArea = :ca") //$NON-NLS-1$
				.setParameter("lm", utc) //$NON-NLS-1$
				.setParameter("by", by) //$NON-NLS-1$
				.setParameter("ca", wp.getConservationArea()) //$NON-NLS-1$
				.setParameter("uuid", wp.getUuid()) //$NON-NLS-1$
				.executeUpdate();

				wp.setLastModified(utc);
				wp.setLastModifiedBy(by);
			}
		}
		
	}
}
