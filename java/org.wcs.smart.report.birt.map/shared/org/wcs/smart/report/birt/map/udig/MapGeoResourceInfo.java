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
package org.wcs.smart.report.birt.map.udig;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.data.FeatureSource;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.locationtech.udig.catalog.IGeoResourceInfo;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureVisitor;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.geometry.BoundingBox;

import com.vividsolutions.jts.geom.Envelope;

/**
 *  Georesource info for BIRT SMART Map Layer
 * @author Emily
 *
 */
public class MapGeoResourceInfo extends IGeoResourceInfo {

	
	public MapGeoResourceInfo( MapGeoResource resource ){
		this.title = resource.getTitle();
		computeBounds(resource);
	}
	
	/**
	 * Recomputes the bounds for this resource info.
	 * 
	 * @param resource resource source
	 */
	public void computeBounds(MapGeoResource resource){
		
		try {
			if (resource.canResolve(FeatureSource.class)){
				@SuppressWarnings("unchecked")
				FeatureSource<SimpleFeatureType, SimpleFeature> fs = resource.resolve(FeatureSource.class, new NullProgressMonitor());
				final ReferencedEnvelope env = new ReferencedEnvelope(fs.getSchema().getCoordinateReferenceSystem());
				this.bounds = env;
				fs.getFeatures().accepts(new FeatureVisitor() {
					@Override
					public void visit(Feature f) {
						BoundingBox bb = f.getBounds();
						env.expandToInclude(new Envelope(bb.getMinX(), bb.getMaxX(), bb.getMinY(), bb.getMaxY()));
					}
				}, null);
			}else if (resource.canResolve(AbstractGridCoverage2DReader.class)){
				AbstractGridCoverage2DReader reader = resource.resolve(AbstractGridCoverage2DReader.class, new NullProgressMonitor());
				GeneralEnvelope ge = reader.getOriginalEnvelope();
				if (ge != null && !ge.isNull()){
					this.bounds = new ReferencedEnvelope(ge.getMinimum(0), ge.getMaximum(0), ge.getMinimum(1), ge.getMaximum(1), ge.getCoordinateReferenceSystem());
				}else{
					this.bounds = new ReferencedEnvelope();
					this.bounds.setToNull();
				}
			}
		} catch (Exception e) {
			Logger.getLogger(MapGeoResourceInfo.class.getName()).log(Level.WARNING, e.getMessage(), e);
		}

	}
}
