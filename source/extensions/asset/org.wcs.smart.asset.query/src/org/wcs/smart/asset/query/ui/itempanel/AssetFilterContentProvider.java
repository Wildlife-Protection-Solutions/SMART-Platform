/*
 * Copyright (C) 220 Wildlife Conservation Society
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
package org.wcs.smart.asset.query.ui.itempanel;

import java.text.Collator;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.wcs.smart.asset.model.Asset;
import org.wcs.smart.asset.model.AssetAttribute;
import org.wcs.smart.asset.model.AssetStation;
import org.wcs.smart.asset.model.AssetStationLocation;
import org.wcs.smart.asset.model.AssetType;
import org.wcs.smart.asset.query.internal.Messages;
import org.wcs.smart.asset.query.parser.internal.filter.AssetAttributeFilter;

/**
 * Input for this content provider should be an array of two elements
 * where the first element is a list of assets and the second element
 * is a list of stations.  The asset type and station locations need to be loaded
 * into these objects from hibernate.
 * 
 * @author Emily
 *
 */
public class AssetFilterContentProvider implements ITreeContentProvider {

	public static final String ASSET_ROOT = Messages.AssetFilterContentProvider_AssetFilterTreeNodeName;
	public static final String STATION_ROOT = Messages.AssetFilterContentProvider_StationsLocationFilterTreeNodeName;
	
	public static final String ATTRIBUTE_ROOT = Messages.AssetFilterContentProvider_AttributeTreeNodeName;
	public static final String ASSET_ATTRIBUTE_ROOT = Messages.AssetFilterContentProvider_AttributeFieldSensorTreeNodeName;
	public static final String STATION_ATTRIBUTE_ROOT = Messages.AssetFilterContentProvider_StationAttributeTreeNodeName;
	public static final String STATIONLOCATION_ATTRIBUTE_ROOT = Messages.AssetFilterContentProvider_StationLocationAttributeTreeNodeName;
	public static final String DEPLOYMENT_ATTRIBUTE_ROOT = Messages.AssetFilterContentProvider_DeploymentAttributeTreeNodeName;
	
	private List<AssetType> types = null;
	private AssetFilterInput input;

	@Override
	public void dispose() {
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		if (newInput == null) {
			input = null;
			return;
		}
		input = (AssetFilterInput)newInput;
		types = input.assets.stream().map(a->a.getAssetType()).distinct().collect(Collectors.toList());
		
		Collections.sort(types, (a,b)->Collator.getInstance().compare(a.getName(), b.getName()));
	}

	@Override
	public Object[] getElements(Object inputElement) {
		if (input == null) return null;
		return new Object[] {ASSET_ROOT, STATION_ROOT, ATTRIBUTE_ROOT};
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement == ASSET_ROOT) {
			return types.toArray();
		}else if (parentElement == ATTRIBUTE_ROOT) {
			return new Object[] {ASSET_ATTRIBUTE_ROOT, STATION_ATTRIBUTE_ROOT, STATIONLOCATION_ATTRIBUTE_ROOT, DEPLOYMENT_ATTRIBUTE_ROOT};
		}else if (parentElement == ASSET_ATTRIBUTE_ROOT) {
			return input.assetAttributes.stream().map(a-> new AttributeWrapper(a, AssetAttributeFilter.Source.ASSET)).collect(Collectors.toList()).toArray();
		}else if (parentElement == STATION_ATTRIBUTE_ROOT) {
			return input.stationAttributes.stream().map(a-> new AttributeWrapper(a, AssetAttributeFilter.Source.STATION)).collect(Collectors.toList()).toArray();
		}else if (parentElement == STATIONLOCATION_ATTRIBUTE_ROOT) {
			return input.stationLocationAttributes.stream().map(a-> new AttributeWrapper(a, AssetAttributeFilter.Source.STATIONLOCATION)).collect(Collectors.toList()).toArray();
		}else if (parentElement == DEPLOYMENT_ATTRIBUTE_ROOT) {
			return input.deploymentAttributes.stream().map(a-> new AttributeWrapper(a, AssetAttributeFilter.Source.DEPLOYMENT)).collect(Collectors.toList()).toArray();
		}else if (parentElement instanceof AssetType) {
			return input.assets.stream().filter(e->e.getAssetType().equals(parentElement)).sorted((a,b)->Collator.getInstance().compare(a.getId(), b.getId())).toArray();
		}else if (parentElement == STATION_ROOT) {
			return input.stations.toArray();
		}else if (parentElement instanceof AssetStation) {
			return ((AssetStation)parentElement).getLocations().toArray();
		}
		return null;
	}

	@Override
	public Object getParent(Object element) {
		if (element instanceof AssetType) return ASSET_ROOT;
		if (element instanceof AssetStation) return STATION_ROOT;
		if (element instanceof AssetStationLocation) return ((AssetStationLocation)element).getStation();
		if (element instanceof Asset) return ((Asset)element).getAssetType();
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		return (element == ATTRIBUTE_ROOT ||
				element == ASSET_ATTRIBUTE_ROOT ||
				element == STATION_ATTRIBUTE_ROOT ||
				element == STATIONLOCATION_ATTRIBUTE_ROOT ||
				element == DEPLOYMENT_ATTRIBUTE_ROOT ||
				element == ASSET_ROOT || 
				element == STATION_ROOT || 
				element instanceof AssetStation || 
				element instanceof AssetType);
	}
	
	
	/**
	 * Input expected with this contenxt provider
	 * 
	 * @author Emily
	 *
	 */
	public static class AssetFilterInput{
		public List<AssetStation>  stations = null;
		public List<Asset> assets = null;
		public List<AssetType> types = null;
		public List<AssetAttribute> assetAttributes = null;
		public List<AssetAttribute> stationAttributes = null;
		public List<AssetAttribute> stationLocationAttributes = null;
		public List<AssetAttribute> deploymentAttributes = null;
	}

}
