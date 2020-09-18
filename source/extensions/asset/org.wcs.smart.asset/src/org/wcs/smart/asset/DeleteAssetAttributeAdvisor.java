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

import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.asset.internal.Messages;
import org.wcs.smart.asset.model.AssetAttribute;
import org.wcs.smart.asset.model.AssetStationAttribute;
import org.wcs.smart.asset.model.AssetStationLocationAttribute;
import org.wcs.smart.asset.model.AssetTypeAttribute;
import org.wcs.smart.asset.model.AssetTypeDeploymentAttribute;
import org.wcs.smart.ca.advisors.IDeleteAdvisor;
import org.wcs.smart.hibernate.QueryFactory;

/**
 * Checks to ensure attribute can be removed
 * 
 * @author Emily
 *
 */

public class DeleteAssetAttributeAdvisor implements IDeleteAdvisor {

	public DeleteAssetAttributeAdvisor() {
	}
	
	@Override
	public String canDelete(Object object, Session session) {
		if (!(object instanceof AssetAttribute)){
			return Messages.DeleteAssetAttributeAdvisor_InvalidObject;
		}
		AssetAttribute attribute = (AssetAttribute) object;
		
		List<AssetTypeAttribute> links = QueryFactory.buildQuery(session, AssetTypeAttribute.class, "id.attribute", attribute).list(); //$NON-NLS-1$
		if (!links.isEmpty()){
			StringBuilder sb = new StringBuilder();
			sb.append(Messages.DeleteAssetAttributeAdvisor_TypeRef);
			for (AssetTypeAttribute a : links){
				sb.append(a.getAssetType().getName());
				sb.append(", "); //$NON-NLS-1$
			}
			sb.deleteCharAt(sb.length() - 1);
			sb.deleteCharAt(sb.length() - 1);
			sb.append("."); //$NON-NLS-1$
			return sb.toString();
		}
		
		List<AssetTypeDeploymentAttribute> deployments = QueryFactory.buildQuery(session, AssetTypeDeploymentAttribute.class, "id.attribute", attribute).list(); //$NON-NLS-1$
		if (!deployments.isEmpty()){
			StringBuilder sb = new StringBuilder();
			sb.append(Messages.DeleteAssetAttributeAdvisor_DeploymentRef);
			for (AssetTypeDeploymentAttribute a : deployments){
				sb.append(a.getAssetType().getName());
				sb.append(", "); //$NON-NLS-1$
			}
			sb.deleteCharAt(sb.length() - 1);
			sb.deleteCharAt(sb.length() - 1);
			sb.append("."); //$NON-NLS-1$
			return sb.toString();
		}
		
		List<AssetStationAttribute> stations = QueryFactory.buildQuery(session, AssetStationAttribute.class, "attribute", attribute).list(); //$NON-NLS-1$
		if (!stations.isEmpty()){
			StringBuilder sb = new StringBuilder();
			sb.append(Messages.DeleteAssetAttributeAdvisor_StationRef);
			return sb.toString();
		}
		
		List<AssetStationLocationAttribute> locations = QueryFactory.buildQuery(session, AssetStationLocationAttribute.class, "attribute", attribute).list(); //$NON-NLS-1$
		if (!locations.isEmpty()){
			StringBuilder sb = new StringBuilder();
			sb.append("This field sensor is referenced by sensor locations and must be removed from the location attribute list before the attribute can be deleted.");
			return sb.toString();
		}
		
		return null;
	}

}
