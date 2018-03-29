/*
 * Copyright (C) 2017 Wildlife Conservation Society
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
package org.wcs.smart.asset;

import java.util.Collections;

import org.eclipse.e4.core.services.events.IEventBroker;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.wcs.smart.asset.internal.Messages;
import org.wcs.smart.asset.model.AssetStation;
import org.wcs.smart.asset.model.AssetStationLocation;
import org.wcs.smart.asset.model.AssetWaypointSource;
import org.wcs.smart.common.attachment.AttachmentInterceptor;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.observation.model.Waypoint;

/**
 * Tools for managing stations and station locations
 * @author Emily
 *
 */
public enum StationManager {

	INSTANCE;
	
	private StationManager(){
		
	}
	
	/**
	 * Deletes the station and all data associated with the station. 
	 * 
	 * @param station the station to delete
	 * @param broker the event broker for firing events after deletion
	 * @throws Exception
	 */
	public void deleteStation(AssetStation station, IEventBroker broker){
		try(Session session = HibernateManager.openSession(new AttachmentInterceptor())){
			session.beginTransaction();
			try {
				deleteStation(station, session);
				session.getTransaction().commit();
			}catch (Exception ex) {
				AssetPlugIn.displayLog(Messages.StationManager_DeleteStationError + ex.getMessage(), ex);
				session.getTransaction().rollback();
				return;
			}
		}
		broker.post(AssetEvents.ASSETSTATION_DELETE, Collections.singletonList(station));
		broker.post(AssetEvents.ASSETDATA, null);
	}
	

	/**
	 * Deletes the station location and all data associated with the station location. 
	 * 
	 * @param location the station location to delete
	 * @param broker event broker for firing events after deleting
	 * @throws Exception
	 */
	public void deleteStationLocation(AssetStationLocation location, IEventBroker broker){
		try(Session session = HibernateManager.openSession(new AttachmentInterceptor())){
			session.beginTransaction();
			try {
				deleteStationLocation(location, session);
				session.getTransaction().commit();
			}catch (Exception ex) {
				AssetPlugIn.displayLog(Messages.StationManager_DeleteLocationError + ex.getMessage(), ex);
				session.getTransaction().rollback();
				return;
			}
		}
		broker.post(AssetEvents.ASSETSTATIONLOCATION_DELETE, Collections.singletonList(location));
		broker.post(AssetEvents.ASSETDATA, null);
	}
	
	/**
	 * Deletes the station and all data associated with the station. This will delete
	 * waypoints so the provided session should be opened with the AttachmentInterceptor
	 * 
	 * @param type
	 * @param session
	 * @throws Exception
	 */
	private void deleteStation(AssetStation station, Session session) throws Exception{
		//delete all data associated with the station
		
		station = session.get(AssetStation.class,  station.getUuid());
		if (station == null) return;
		
		if (station.getLocations() != null) {
			for (AssetStationLocation loc : station.getLocations() ) {
				deleteLocationWaypoints(session, loc);
			}
		}
		
		session.createQuery("DELETE FROM AssetStationLocationHistoryRecord WHERE stationLocation in (FROM AssetStationLocation where station = :station)") //$NON-NLS-1$
		.setParameter("station", station) //$NON-NLS-1$
		.executeUpdate();
		
		session.delete(station);
	}
	
	/**
	 * Deletes the station location and all data associated with the station location. This will delete
	 * waypoints so the provided session should be opened with the AttachmentInterceptor
	 * 
	 * @param type
	 * @param session
	 * @throws Exception
	 */
	private void deleteStationLocation(AssetStationLocation location, Session session) throws Exception{
		//delete all data associated with the station
		
		location = session.get(AssetStationLocation.class,  location.getUuid());
		if (location == null) return;
		
		deleteLocationWaypoints(session, location);
		
		session.createQuery("DELETE FROM AssetStationLocationHistoryRecord WHERE stationLocation = :location") //$NON-NLS-1$
			.setParameter("location", location) //$NON-NLS-1$
			.executeUpdate();
		session.flush();
		session.delete(location);
	}
	
	private void deleteLocationWaypoints(Session session, AssetStationLocation location) {
		
		String hql = "DELETE FROM AssetWaypointAttachment a where a.id.assetWaypoint in (FROM AssetWaypoint WHERE assetDeployment.stationLocation = :location) "; //$NON-NLS-1$
		session.createQuery(hql).setParameter("location",  location).executeUpdate(); //$NON-NLS-1$
		session.flush();
		
		hql = "DELETE FROM AssetWaypoint WHERE assetDeployment in (FROM AssetDeployment WHERE stationLocation = :location ) "; //$NON-NLS-1$
		session.createQuery(hql).setParameter("location",  location).executeUpdate(); //$NON-NLS-1$
		session.flush();
		
		//delete any waypoints not associated with asset waypoint
		try (ScrollableResults scroll = session.createQuery("FROM Waypoint ww WHERE source = :source and ww not in (SELECT waypoint FROM AssetWaypoint)").setParameter("source", AssetWaypointSource.KEY).scroll()){ //$NON-NLS-1$ //$NON-NLS-2$
			while(scroll.next()) {
				Waypoint wp = (Waypoint)scroll.get(0);
				session.delete(wp);
			}
		}
		session.flush();
		
		hql = "DELETE FROM AssetDeploymentAttributeValue WHERE id.assetDeployment IN (FROM AssetDeployment WHERE stationLocation = :location)"; //$NON-NLS-1$
		session.createQuery(hql).setParameter("location",  location).executeUpdate(); //$NON-NLS-1$
		session.flush();	
		
		hql = "DELETE FROM AssetDeployment WHERE stationLocation = :location "; //$NON-NLS-1$
		session.createQuery(hql).setParameter("location",  location).executeUpdate(); //$NON-NLS-1$
		session.flush();

	}
}
