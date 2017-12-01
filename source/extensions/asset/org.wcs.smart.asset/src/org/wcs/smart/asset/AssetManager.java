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
import org.wcs.smart.asset.model.Asset;
import org.wcs.smart.asset.model.AssetDeployment;
import org.wcs.smart.asset.model.AssetWaypoint;
import org.wcs.smart.common.attachment.AttachmentInterceptor;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;

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
				AssetPlugIn.displayLog("Unable to delete asset: " + ex.getMessage(), ex);
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
		
		//remove all deployments
		try(ScrollableResults scroll = QueryFactory.buildQuery(session, AssetDeployment.class, new Object[] {"asset", asset}).scroll()){
			while(scroll.next()) {
				AssetDeployment d = (AssetDeployment) scroll.get(0);
				for (AssetWaypoint aw : d.getAssetWaypoints()) {
					session.delete(aw);
					session.delete(aw.getWaypoint());
				}
				session.delete(d);
				session.flush();
			}
		}
		
		//delete history records
		String hql = "DELETE FROM AssetHistoryRecord WHERE asset = :asset";
		session.createQuery(hql).setParameter("asset", asset).executeUpdate();
		
		//delete the asset
		session.delete(asset);
		
	}
	
}
