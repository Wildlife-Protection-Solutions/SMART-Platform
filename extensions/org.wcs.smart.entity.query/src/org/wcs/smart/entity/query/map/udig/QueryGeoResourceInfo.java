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
package org.wcs.smart.entity.query.map.udig;

import org.eclipse.core.runtime.IProgressMonitor;
import org.geotools.data.FeatureSource;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.locationtech.udig.catalog.IGeoResourceInfo;
import org.locationtech.udig.catalog.IService;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureVisitor;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.geometry.BoundingBox;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.common.engine.IPagedQueryResultSet;
import org.wcs.smart.query.model.IPagedQuery;

import com.vividsolutions.jts.geom.Envelope;

/**
 * Georesource Information for a smart area resource 
 * @author Emily
 * @since 1.0.0
 */
public class QueryGeoResourceInfo extends IGeoResourceInfo {

	
	public QueryGeoResourceInfo( QueryGeoResource resource, IProgressMonitor monitor){
		this.title = resource.getDataType();
		computeBounds(resource, monitor);
	}
	
	/**
	 * Recomputes the bounds for this resource info.
	 * 
	 * @param resource resource source
	 */
	public void computeBounds(QueryGeoResource resource, IProgressMonitor monitor){

		try {
			@SuppressWarnings("unchecked")
			FeatureSource<SimpleFeatureType, SimpleFeature> fs = resource.resolve(FeatureSource.class, monitor);
			final ReferencedEnvelope env = new ReferencedEnvelope(fs.getSchema().getCoordinateReferenceSystem());
			this.bounds = env;
			QueryService service = (QueryService) resource.resolve(IService.class, monitor);
			if (service.getQuery() instanceof IPagedQuery){
				IPagedQueryResultSet rs = (IPagedQueryResultSet) service.getQuery().getCachedResults();
				if (rs != null){
					Envelope local = rs.getEnvelope();
					env.expandToInclude(local.getMinX(), local.getMinY());
					env.expandToInclude(local.getMaxX(), local.getMaxY());
				}
			}else{
				fs.getFeatures().accepts(new FeatureVisitor() {
					@Override
					public void visit(Feature f) {
						BoundingBox bb = f.getBounds();
						env.expandToInclude(new Envelope(bb.getMinX(), bb.getMaxX(), bb.getMinY(), bb.getMaxY()));
					}
				}, null);
			}
		} catch (Exception e) {
			QueryPlugIn.log(e.getMessage(), e);
		}

	}
}
