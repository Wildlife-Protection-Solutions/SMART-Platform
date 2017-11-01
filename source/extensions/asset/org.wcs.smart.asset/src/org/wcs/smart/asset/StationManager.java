package org.wcs.smart.asset;

import java.util.Collections;

import org.eclipse.e4.core.services.events.IEventBroker;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.wcs.smart.asset.model.AssetDeployment;
import org.wcs.smart.asset.model.AssetStation;
import org.wcs.smart.asset.model.AssetStationLocation;
import org.wcs.smart.asset.model.AssetWaypoint;
import org.wcs.smart.common.attachment.AttachmentInterceptor;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;

public enum StationManager {

	INSTANCE;
	
	private StationManager(){
		
	}
	
	/**
	 * Deletes the station and all data associated with the station. This will delete
	 * waypoints so the provided session should be opened with the AttachmentInterceptor
	 * 
	 * @param type
	 * @param session
	 * @throws Exception
	 */
	public void deleteStation(AssetStation station, IEventBroker broker){
		try(Session session = HibernateManager.openSession(new AttachmentInterceptor())){
			session.beginTransaction();
			try {
				deleteStation(station, session);
				session.getTransaction().commit();
			}catch (Exception ex) {
				AssetPlugIn.displayLog("Unable to delete station: " + ex.getMessage(), ex);
				session.getTransaction().rollback();
				return;
			}
		}
		broker.post(AssetEvents.ASSETSTATION_DELETE, Collections.singletonList(station));
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
				try(ScrollableResults scroll = QueryFactory.buildQuery(session, AssetDeployment.class, new Object[] {"location", loc}).scroll()){
					while(scroll.next()) {
						AssetDeployment d  = (AssetDeployment)scroll.get(0);
						for (AssetWaypoint w : d.getAssetWaypoints()) {
							session.delete(w.getWaypoint());
							session.delete(w);
						}
						session.delete(d);
					}
					
				}
				session.flush();
			}
		}
		
		session.delete(station);
	}
	
}
