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
package org.wcs.smart.er.ui.mision.udig;

import java.io.IOException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.geotools.data.FeatureSource;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.locationtech.udig.catalog.IGeoResourceInfo;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureVisitor;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.geometry.BoundingBox;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.er.internal.Messages;

import com.vividsolutions.jts.geom.Envelope;

/**
 * Mission geo resource info 
 * 
 * @author Emily
 * @author elitvin
 *
 */
public class MissionGeoResourceInfo  extends IGeoResourceInfo {
	
	public MissionGeoResourceInfo( MissionGeoResource resource, IProgressMonitor monitor){
		if (resource.dataType.equals(MissionDataSource.MISSIONWAYPOINT_TYPE)){
			this.title = Messages.MissionGeoResourceInfo_WaypointsTitle;
		}else if (resource.dataType.equals(MissionDataSource.MISSIONTRACK_TYPE)){
			this.title = Messages.MissionGeoResourceInfo_TracksTitle;
		}else{
			this.title = resource.dataType;
		}
		
		computeBounds(resource, monitor);
	}
	
	/**
	 * Recomputes the bounds for this resource info.
	 * 
	 * @param resource resource source
	 */
	public void computeBounds(MissionGeoResource resource, IProgressMonitor monitor){
		try {
			@SuppressWarnings("unchecked")
			FeatureSource<SimpleFeatureType, SimpleFeature> fs = resource.resolve(FeatureSource.class, monitor);
			final ReferencedEnvelope env = new ReferencedEnvelope(fs.getSchema().getCoordinateReferenceSystem());
			fs.getFeatures().accepts(new FeatureVisitor() {
				@Override
				public void visit(Feature f) {
					BoundingBox bb = f.getBounds();
					env.expandToInclude(new Envelope(bb.getMinX(), bb.getMaxX(), bb.getMinY(), bb.getMaxY()));
				}
			}, null);
			this.bounds = env;
		} catch (IOException e) {
			SmartPlugIn.log("Could not determine bounds for smart resource: " + resource.dataType, e); //$NON-NLS-1$
		}
	}
}

