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
package org.wcs.smart.asset.ui;

import java.util.Locale;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.wcs.smart.asset.AssetCoreLabelProvider;
import org.wcs.smart.asset.model.Asset;
import org.wcs.smart.asset.model.AssetStation;
import org.wcs.smart.asset.model.AssetStationLocation;
import org.wcs.smart.asset.model.AssetType;

/**
 * Label provider that provides images and labels for 
 * asset and assettype objects
 * 
 * @author Emily
 *
 */
public class AssetLabelProvider extends LabelProvider{

	private AssetTypeLabelProvider typeProvider = new AssetTypeLabelProvider();
	
	@Override
	public String getText(Object element) {
		if (element instanceof AssetType) return typeProvider.getText(element);
		if (element instanceof Asset) return ((Asset)element).getId();
		if (element instanceof AssetStation) return ((AssetStation)element).getId();
		if (element instanceof AssetStationLocation) return ((AssetStationLocation)element).getId();
		if (element instanceof Asset.Status) return ((Asset.Status)element).getGuiName(Locale.getDefault());
		return super.getText(element);
	}
	
	@Override
	public Image getImage(Object element) {
		if (element instanceof AssetType) return typeProvider.getImage(element);
		if (element instanceof Asset) {
			return AssetCoreLabelProvider.getStatusImage((Asset)element);
		}
		if (element instanceof AssetStation) {
			return AssetCoreLabelProvider.getStatusImage(((AssetStation)element).getCachedStatus());
		}
		if (element instanceof AssetStationLocation) {
			return AssetCoreLabelProvider.getStatusImage(((AssetStationLocation)element).getCachedStatus());
		}
		if (element instanceof Asset.Status) {
			return AssetCoreLabelProvider.getStatusImage((Asset.Status) element);
		}
		return super.getImage(element);
	}
	@Override
	public void dispose(){
		typeProvider.dispose();
		super.dispose();
	}
	
}
