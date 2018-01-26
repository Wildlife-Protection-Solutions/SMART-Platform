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
package org.wcs.smart.asset.query.ui.itempanel;

import java.util.HashMap;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.locationtech.udig.ui.graphics.AWTSWTImageUtils;
import org.wcs.smart.asset.AssetPlugIn;
import org.wcs.smart.asset.model.Asset;
import org.wcs.smart.asset.model.AssetStation;
import org.wcs.smart.asset.model.AssetStationLocation;
import org.wcs.smart.asset.model.AssetType;
import org.wcs.smart.asset.query.AssetQueryPlugIn;
import org.wcs.smart.asset.query.internal.Messages;
import org.wcs.smart.query.common.ui.itempanel.IItemTreeNode;

/**
 * Asset filter items.
 * 
 * @author Emily
 *
 */
public class AssetFilterTreeItem implements IItemTreeNode{

	public static final String KEY = "assetfilter"; //$NON-NLS-1$
	
	private ITreeContentProvider provider;
	
	@Override
	public String getName() {
		return Messages.AssetFilterTreeItem_AssetFiltersTreeItem;
	}

	@Override
	public Image getImage() {
		return AssetPlugIn.getDefault().getImageRegistry().get(AssetPlugIn.ICON_ASSET);
	}

	@Override
	public ITreeContentProvider getContentProvider() {
		if (provider == null){
			provider = new AssetFilterContentProvider();
		}
		return provider;
	}

	@Override
	public ILabelProvider getLabelProvider() {
		return lblProvider;
	}

	@Override
	public String getKey() {
		return KEY;
	}
	
	private LabelProvider lblProvider = new LabelProvider(){
		
		private HashMap<AssetType, Image> assetTypeImages = new HashMap<>();
		
		@Override
		public void dispose() {
			assetTypeImages.values().forEach(e->e.dispose());
			super.dispose();
		}
		
		public String getText(Object element){
			if (element instanceof Asset){
				return ((Asset)element).getId();
			}else if (element instanceof AssetType){
					return ((AssetType)element).getName();
			}else if (element instanceof AssetStation) {
				return ((AssetStation)element).getId();
			}else if (element instanceof AssetStationLocation) {
				return ((AssetStationLocation)element).getId();
			}
			return super.getText(element);
		}
		
		public Image getImage(Object element){
			if (element instanceof Asset){
				return getImage(((Asset) element).getAssetType());
			}else if (element instanceof AssetType){
				return getImage((AssetType)element);
			}else if (element instanceof AssetStation) {
				return AssetPlugIn.getDefault().getImageRegistry().get(AssetPlugIn.ICON_STATION);
			}else if (element instanceof AssetStationLocation) {
				return AssetPlugIn.getDefault().getImageRegistry().get(AssetPlugIn.ICON_STATION_LOCATION);
			}else if (element == AssetFilterContentProvider.STATION_ROOT) {
				return AssetPlugIn.getDefault().getImageRegistry().get(AssetPlugIn.ICON_STATION); 
			}else {
				return AssetPlugIn.getDefault().getImageRegistry().get(AssetPlugIn.ICON_ASSET);
			}
		}
		
		private Image getImage(AssetType type) {
			if (type.getIcon() == null) return null;
			Image x = assetTypeImages.get(type);
			if (x != null) return x;
			
			try {
				x = AWTSWTImageUtils.convertToSWTImage(type.getIconAsImage());
				assetTypeImages.put(type,  x);
				return x;
			} catch (Exception e) {
				AssetQueryPlugIn.log(e.getMessage(), e);
			}
			return null;
			
			
		}
	};

}
