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
package org.wcs.smart.asset;

import java.text.Collator;
import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.wcs.smart.asset.internal.Messages;
import org.wcs.smart.asset.model.AssetType;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.advisors.DeleteManager;

/**
 * Tools for managing asset types
 * 
 * @author Emily
 *
 */
public enum AssetTypeManager {
	
	INSTANCE;
	
	private AssetTypeManager(){
		
	}
	
	/**
	 * Loads all asset types and sorts by name
	 * @param session
	 * @param ca
	 * @return
	 */
	public List<AssetType> getAssetTypes(Session session, ConservationArea ca){
		CriteriaBuilder cb = session.getCriteriaBuilder();
		CriteriaQuery<AssetType> c = cb.createQuery(AssetType.class);
		Root<AssetType> root = c.from(AssetType.class);
		c.where(cb.equal(root.get("conservationArea"), ca)); //$NON-NLS-1$
		
		List<AssetType> types = session.createQuery(c).getResultList();
		types.sort((AssetType a, AssetType b) -> Collator.getInstance().compare(a.getName(), b.getName()));
		return types;
	}
	
	/**
	 * Validates if the given asset type can be deleted
	 * @param type
	 * @param session
	 * @throws Exception
	 */
	public void canDelete(AssetType type, Session session) throws Exception{
		if (!DeleteManager.canDelete(type, session)){
			throw new Exception(Messages.AssetTypeManager_DeleteError);
		}
	}
	
	/**
	 * Deletes an asset type an all associated data (assets, deployments, etc)
	 * 
	 * @param type
	 * @param session
	 * @throws Exception
	 */
	public void deleteAssetType(AssetType type, Session session) throws Exception{
		
		//delete all asset attribute values
		Query<?> q = session.createQuery("delete from AssetAttributeValue ieav where ieav.id.asset in (FROM Asset WHERE assetType = :type)"); //$NON-NLS-1$
		q.setParameter("type", type); //$NON-NLS-1$
		q.executeUpdate();
		
		//delete all asset waypoints
		q = session.createQuery("delete from AssetWaypoint ieav where id.assetDeployment in (FROM AssetDeployment ad WHERE ad.asset.assetType = :type)"); //$NON-NLS-1$
		q.setParameter("type", type); //$NON-NLS-1$
		q.executeUpdate();
		
		//delete all asset deployment attributes
		q = session.createQuery("delete from AssetDeploymentAttributeValue ieav where id.assetDeployment in (SELECT ad FROM AssetDeployment ad WHERE ad.asset.assetType = :type)"); //$NON-NLS-1$
		q.setParameter("type", type); //$NON-NLS-1$
		q.executeUpdate();
		
		//delete all asset deployments
		q = session.createQuery("delete from AssetDeployment ieav where id.asset in (FROM Asset WHERE assetType = :type)"); //$NON-NLS-1$
		q.setParameter("type", type); //$NON-NLS-1$
		q.executeUpdate();
		
		//delete all asset type attributes
		q = session.createQuery("delete from AssetTypeAttribute WHERE id.assetType = :type)"); //$NON-NLS-1$
		q.setParameter("type", type); //$NON-NLS-1$
		q.executeUpdate();
		
		//delete all asset type deployment attributes
		q = session.createQuery("delete from AssetTypeDeploymentAttribute WHERE id.assetType = :type)"); //$NON-NLS-1$
		q.setParameter("type", type); //$NON-NLS-1$
		q.executeUpdate();
		
		//delete all assets
		q = session.createQuery("delete from Asset WHERE assetType = :type)"); //$NON-NLS-1$
		q.setParameter("type", type); //$NON-NLS-1$
		q.executeUpdate();
		
		//delete asset type
		session.delete(type);

	}


}
