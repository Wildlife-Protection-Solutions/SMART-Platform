/*
 * Copyright (C) 2012 Wildlife Conservation Society
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
package org.wcs.smart.asset.query.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.hibernate.Session;
import org.wcs.smart.asset.model.Asset;
import org.wcs.smart.asset.model.AssetStation;
import org.wcs.smart.asset.model.AssetStationLocation;
import org.wcs.smart.asset.model.AssetType;
import org.wcs.smart.asset.query.model.AssetFilterOption;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.ui.model.ListItem;
import org.wcs.smart.util.UuidUtils;

/**
 * Asset option data for fixed asset options.
 * 
 * @author Emily
 *
 */
public class AssetOptionData implements IAssetGroupByOptionData{
	
	private AssetFilterOption option;
	
	public AssetOptionData(AssetFilterOption option){
		this.option = option;
	}
	
	/**
	 * Given a set of keys (hex encoded uuids or string keys), returns
	 * a list of listitems that represent the objects
	 * with the given keys.
	 * 
	 * @param session
	 * @param keys
	 * @return
	 */
	public List<ListItem> getValues(Session session, String[] keys){
		List<ListItem> results = getAllValues(session);
		
		Set<UUID> uuids = new HashSet<>();
		for (String key : keys) {
			uuids.add(UuidUtils.stringToUuid(key));
		}
		for (Iterator<ListItem> iterator = results.iterator(); iterator.hasNext();) {
			ListItem listItem = iterator.next();
			if (!uuids.contains(listItem.getUuid())) iterator.remove();
		}
		Collections.sort(results);
		return results;
	}

	/**
	 * @param session
	 * @return a list of listitems that represent all
	 * active values for a given object 
	 */
	public List<ListItem> getAllValues(Session session){
		List<ListItem> results = new ArrayList<ListItem>();
		
		switch(option) {
		case ASSET:
			List<Asset> assets = QueryFactory.buildQuery(session, Asset.class, new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).list(); //$NON-NLS-1$
			assets.sort((a,b)->a.getId().compareTo(b.getId()));
			for (Asset a : assets) {
				results.add(new ListItem(a.getUuid(), a.getId()));
			}
			break;
		case ASSETTYPE:
			List<AssetType> assetTypes = QueryFactory.buildQuery(session, AssetType.class, new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).list(); //$NON-NLS-1$
			assetTypes.sort((a,b)->a.getKeyId().compareTo(b.getKeyId()));
			for (AssetType a : assetTypes) {
				results.add(new ListItem(a.getUuid(), a.getName()));
			}
			break;
		case STATION:
			List<AssetStation> stations = QueryFactory.buildQuery(session, AssetStation.class, new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).list(); //$NON-NLS-1$
			stations.sort((a,b)->a.getId().compareTo(b.getId()));
			for (AssetStation a : stations) {
				results.add(new ListItem(a.getUuid(), a.getId()));
			}
			break;
		case STATIONLOCATION:
			List<AssetStationLocation> locations = QueryFactory.buildQuery(session, AssetStationLocation.class, new Object[] {"station.conservationArea", SmartDB.getCurrentConservationArea()}).list(); //$NON-NLS-1$
			locations.sort((a,b)->a.getId().compareTo(b.getId()));
			for (AssetStationLocation a : locations) {
				results.add(new ListItem(a.getUuid(), a.getId()));
			}
			break;
		case CONSERVATION_AREA:
			throw new IllegalStateException("Conservation Area option not supported."); //$NON-NLS-1$
		default:
			break;
		}
		Collections.sort(results);
		return results;
	}
	
}
