package org.wcs.smart.asset.query.ui.itempanel;

import java.text.Collator;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.wcs.smart.asset.model.Asset;
import org.wcs.smart.asset.model.AssetStation;
import org.wcs.smart.asset.model.AssetStationLocation;
import org.wcs.smart.asset.model.AssetType;

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

	public static final String ASSET_ROOT = "Assets";
	public static final String STATION_ROOT = "Stations & Locations";
	
	private List<AssetStation>  stations = null;
	private List<Asset> assets = null;
	private List<AssetType> types = null;

	@Override
	public void dispose() {
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		if (newInput == null) {
			stations = null;
			assets = null;
			types = null;
			return;
		}
		assets = (List<Asset>) ((Object[])newInput)[0];
		stations = (List<AssetStation>) ((Object[])newInput)[1];
		types = assets.stream().map(a->a.getAssetType()).distinct().collect(Collectors.toList());
		
		Collections.sort(types, (a,b)->Collator.getInstance().compare(a.getName(), b.getName()));
	}

	@Override
	public Object[] getElements(Object inputElement) {
		return new Object[] {ASSET_ROOT, STATION_ROOT};
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement == ASSET_ROOT) {
			return types.toArray();
		}else if (parentElement instanceof AssetType) {
			return assets.stream().filter(e->e.getAssetType().equals(parentElement)).sorted((a,b)->Collator.getInstance().compare(a.getId(), b.getId())).toArray();
		}else if (parentElement == STATION_ROOT) {
			return stations.toArray();
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
		return (element == ASSET_ROOT || element == STATION_ROOT || element instanceof AssetStation || element instanceof AssetType);
	}

}
