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

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;

import org.eclipse.e4.core.services.events.IEventBroker;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.wcs.smart.asset.internal.Messages;
import org.wcs.smart.asset.model.Asset;
import org.wcs.smart.asset.model.AssetDeployment;
import org.wcs.smart.asset.model.AssetWaypointAttachment;
import org.wcs.smart.asset.model.AssetWaypointSource;
import org.wcs.smart.common.attachment.AttachmentInterceptor;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.observation.model.Waypoint;

/**
 * Tools for managing stations and station locations
 * @author Emily
 *
 */
public enum AssetManager {

	INSTANCE;
	
	private AssetManager(){
		
	}
	
	/**
	 * Deletes the asset and all data associated with the asset. This will delete
	 * waypoints so the provided session should be opened with the AttachmentInterceptor
	 * 
	 * @param type
	 * @param session
	 * @throws Exception
	 */
	public void deleteAsset(Asset asset, IEventBroker broker){
		try(Session session = HibernateManager.openSession(new AttachmentInterceptor())){
			session.beginTransaction();
			try {
				deleteAsset(asset, session);
				session.getTransaction().commit();
			}catch (Exception ex) {
				AssetPlugIn.displayLog(Messages.AssetManager_DeleteError + ex.getMessage(), ex);
				session.getTransaction().rollback();
				return;
			}
		}
		broker.post(AssetEvents.ASSET_DELETE, Collections.singletonList(asset));
	}
	
	/**
	 * Deletes the asset and all data associated with the station. This will delete
	 * waypoints so the provided session should be opened with the AttachmentInterceptor
	 * 
	 * @param type
	 * @param session
	 * @throws Exception
	 */
	private void deleteAsset(Asset asset, Session session) throws Exception{
		//delete all data associated with the station
		
		asset = session.get(Asset.class,  asset.getUuid());
		if (asset== null) return;
		
		//remove all deployments, waypoints and associated attachment links
		//remove waypoint attachment
		//due to circular links in database this 
		//has been implemented by deleting one attachment at a time - I can forsee this being really slow
		//TODO: investigate performance
		try(ScrollableResults<AssetWaypointAttachment> scroll = session.createQuery("FROM AssetWaypointAttachment a WHERE a.id.assetWaypoint.assetDeployment.asset = :asset", AssetWaypointAttachment.class).setParameter("asset",  asset).scroll()){ //$NON-NLS-1$ //$NON-NLS-2$
			while(scroll.next()) {
				AssetWaypointAttachment attachment = scroll.get();
				
				session.remove(attachment);
				attachment.getWaypointAttachment().getWaypoint().getAttachments().remove(attachment.getWaypointAttachment());
				session.remove(attachment.getWaypointAttachment());
				session.flush();
			}
		}
		
		
		String hql = "DELETE FROM AssetWaypoint WHERE assetDeployment in (FROM AssetDeployment WHERE asset = :asset ) "; //$NON-NLS-1$
		session.createMutationQuery(hql).setParameter("asset",  asset).executeUpdate(); //$NON-NLS-1$
		session.flush();
		
		//delete any waypoints not associated with asset waypoint
		try (ScrollableResults<Waypoint> scroll = session.createQuery("FROM Waypoint ww WHERE source = :source and ww not in (SELECT waypoint FROM AssetWaypoint)", Waypoint.class).setParameter("source", AssetWaypointSource.KEY).scroll()){ //$NON-NLS-1$ //$NON-NLS-2$
			while(scroll.next()) {
				Waypoint wp = scroll.get();
				session.remove(wp);
			}
		}
		session.flush();
		
		
		hql = "DELETE FROM AssetDeployment WHERE asset = :asset"; //$NON-NLS-1$
		session.createMutationQuery(hql).setParameter("asset",  asset).executeUpdate(); //$NON-NLS-1$
		session.flush();
		
		//delete history records
		hql = "DELETE FROM AssetHistoryRecord WHERE asset = :asset"; //$NON-NLS-1$
		session.createMutationQuery(hql).setParameter("asset", asset).executeUpdate(); //$NON-NLS-1$
		session.flush();
		
		//delete the asset
		session.remove(asset);
	}
	
	public boolean overlaps(AssetDeployment toValidate, Collection<AssetDeployment> allDeployments) {
//		long now = LocalDate.now().(new Date()).getTime();
//		long start = toValidate.getStartDate().getTime();
//		Long endTime = toValidate.getEndDate() == null ? null : toValidate.getEndDate().getTime();
		
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime start = toValidate.getStartDate();
		LocalDateTime endTime = toValidate.getEndDate() == null ? null : toValidate.getEndDate();
		
		for (AssetDeployment deploy : allDeployments) {
			if (deploy.equals(toValidate)) continue;

			LocalDateTime starttest = deploy.getStartDate(); 
			LocalDateTime endtest = now;
			
			if (deploy.getEndDate() != null) endtest = deploy.getEndDate();
			
			
			if (!(endtest.isBefore(start) || (endTime != null && starttest.isAfter(endTime)))) {
				return true;
			}
			if (endTime == null && deploy.getEndDate() == null) {
				return true;
			}
		}
		return false;
	}
	
}
