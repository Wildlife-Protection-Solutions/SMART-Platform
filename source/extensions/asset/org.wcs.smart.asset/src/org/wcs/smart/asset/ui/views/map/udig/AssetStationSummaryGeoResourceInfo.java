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
package org.wcs.smart.asset.ui.views.map.udig;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.IProgressMonitor;
import org.geotools.data.FeatureSource;
import org.locationtech.udig.catalog.IGeoResourceInfo;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.asset.internal.Messages;
import org.wcs.smart.util.SharedUtils;

/**
 * Georesource Information for a entity locations 
 *  
 * @author Emily
 */
public class AssetStationSummaryGeoResourceInfo extends IGeoResourceInfo {

	private Logger logger = Logger.getLogger(AssetStationSummaryGeoResourceInfo.class.getName());
	
	public AssetStationSummaryGeoResourceInfo( AssetStationSummaryGeoResource resource, IProgressMonitor monitor){
		this.title = Messages.AssetStationSummaryGeoResourceInfo_LayerTitle ;
		computeBounds(resource, monitor);
	}
	
	public void setTitle(String newTitle){
		this.title = newTitle;
	}
	
	/**
	 * Recomputes the bounds for this resource info.
	 * 
	 * @param resource resource source
	 */
	public void computeBounds(AssetStationSummaryGeoResource resource, IProgressMonitor monitor){
		try {
			@SuppressWarnings("unchecked")
			FeatureSource<SimpleFeatureType, SimpleFeature> fs = resource.resolve(FeatureSource.class, monitor);
			this.bounds = SharedUtils.computeBounds(fs);
		} catch (IOException e) {
			logger.log(Level.WARNING, "Could not determine bounds for smart entity locations resource: ", e); //$NON-NLS-1$
		}
	}
}
