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

import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.DeleteConservationAreaHandler;
import org.wcs.smart.ca.ICaDeleteHandler;

/**
 * Deletes the various components of the asset plugin
 * 
 * @author Emily
 *
 */
public class DeleteCaHandler implements ICaDeleteHandler{

	public static final int EXECUTE_ORDER = DeleteConservationAreaHandler.EXECUTE_ORDER + 1;
	
	private static final String SUB_TASK_MSG = "Deleting Asset Data ({0}) ...";
	
	@Override
	public void beforeDelete(ConservationArea ca, Session session,
			IProgressMonitor monitor) throws Exception {
		//labels are dealt with by core Conservation Area delete engine 
		
		monitor.subTask("Delete asset data");
		
		monitor.subTask(MessageFormat.format(SUB_TASK_MSG, "Asset Attribute Value")); //$NON-NLS-1$
		Query<?>  q = session.createQuery("delete from  AssetAttributeValue sa where sa.id.attribute in (select a from AssetAttribute a where conservationArea = :ca)"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		monitor.subTask(MessageFormat.format(SUB_TASK_MSG, "Asset Station Attribute Value")); //$NON-NLS-1$
		q = session.createQuery("delete from  AssetStationAttributeValue sa where sa.id.attribute in (select a from AssetAttribute a where conservationArea = :ca)"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		monitor.subTask(MessageFormat.format(SUB_TASK_MSG, "Asset Station Location Attribute Value")); //$NON-NLS-1$
		q = session.createQuery("delete from  AssetStationLocationAttributeValue sa where sa.id.attribute in (select a from AssetAttribute a where conservationArea = :ca)"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		monitor.subTask(MessageFormat.format(SUB_TASK_MSG, "Asset Station Attribute")); //$NON-NLS-1$
		q = session.createQuery("delete from  AssetStationAttribute sa where sa.attribute in (select a from AssetAttribute a where conservationArea = :ca)"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		monitor.subTask(MessageFormat.format(SUB_TASK_MSG, "Asset Location Attribute")); //$NON-NLS-1$
		q = session.createQuery("delete from  AssetStationLocationAttribute sa where sa.attribute in (select a from AssetAttribute a where conservationArea = :ca)"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		monitor.subTask(MessageFormat.format(SUB_TASK_MSG, "Asset Deployment Attribute Value")); //$NON-NLS-1$
		q = session.createQuery("delete from  AssetDeploymentAttributeValue sa where sa.id.attribute in (select a from AssetAttribute a where conservationArea = :ca)"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		monitor.subTask(MessageFormat.format(SUB_TASK_MSG, "Asset Deployment ")); //$NON-NLS-1$
		q = session.createQuery("delete from  AssetDeployment sa where sa.asset in (select a from Asset a where conservationArea = :ca)"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		monitor.subTask(MessageFormat.format(SUB_TASK_MSG, "Asset History Record ")); //$NON-NLS-1$
		q = session.createQuery("delete from  AssetHistoryRecord sa where sa.asset in (select a from Asset a where conservationArea = :ca)"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		monitor.subTask(MessageFormat.format(SUB_TASK_MSG, "Asset Location History Record ")); //$NON-NLS-1$
		q = session.createQuery("delete from  AssetStationLocationHistoryRecord sa where sa.stationLocation in (select a from AssetStationLocation a join a.station b where b.conservationArea = :ca)"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		
		monitor.subTask(MessageFormat.format(SUB_TASK_MSG, "Asset Metadata Mapping ")); //$NON-NLS-1$
		q = session.createQuery("delete from  AssetMetadataMapping sa where conservationArea = :ca"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		monitor.subTask(MessageFormat.format(SUB_TASK_MSG, "Asset Station Location")); //$NON-NLS-1$
		q = session.createQuery("delete from AssetStationLocation a where a.station in (select b from AssetStation b where b.conservationArea = :ca)"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		monitor.subTask(MessageFormat.format(SUB_TASK_MSG, "AssetStation")); //$NON-NLS-1$
		q = session.createQuery("delete from AssetStation where conservationArea = :ca"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		monitor.subTask(MessageFormat.format(SUB_TASK_MSG, "AssetType Attribute")); //$NON-NLS-1$
		q = session.createQuery("delete from AssetTypeAttribute where id.attribute in (select b from AssetAttribute b WHERE b.conservationArea = :ca)"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		monitor.subTask(MessageFormat.format(SUB_TASK_MSG, "AssetType Deployment Attribute")); //$NON-NLS-1$
		q = session.createQuery("delete from AssetTypeDeploymentAttribute where id.attribute in (select b from AssetAttribute b WHERE b.conservationArea = :ca)"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		monitor.subTask(MessageFormat.format(SUB_TASK_MSG, "Asset Waypoint Attachment")); //$NON-NLS-1$
		q = session.createQuery("delete from AssetWaypointAttachment where id.assetWaypoint in (SELECT a from AssetWaypoint a where a.waypoint.conservationArea = :ca)"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		monitor.subTask(MessageFormat.format(SUB_TASK_MSG, "Asset Waypoint")); //$NON-NLS-1$
		q = session.createQuery("delete from AssetWaypoint where waypoint in (SELECT a from Waypoint a where a.conservationArea = :ca)"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		monitor.subTask(MessageFormat.format(SUB_TASK_MSG, "AssetType")); //$NON-NLS-1$
		q = session.createQuery("delete from AssetType where conservationArea = :ca"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		monitor.subTask(MessageFormat.format(SUB_TASK_MSG, "AssetAttributeListItem")); //$NON-NLS-1$
		q = session.createQuery("delete from AssetAttributeListItem where attribute in (SELECT a FROM AssetAttribute a WHERE a.conservationArea = :ca)"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		monitor.subTask(MessageFormat.format(SUB_TASK_MSG, "AssetAttribute")); //$NON-NLS-1$
		q = session.createQuery("delete from AssetAttribute where conservationArea = :ca"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();

		monitor.subTask(MessageFormat.format(SUB_TASK_MSG, "Asset Module Settings")); //$NON-NLS-1$
		q = session.createQuery("delete from org.wcs.smart.asset.model.AssetModuleSettings where conservationArea = :ca"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
	}
	
	

}
