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
package org.wcs.smart.i2.udig.query;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.IProgressMonitor;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.locationtech.udig.catalog.IGeoResourceInfo;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.util.GeometryUtils;

import com.vividsolutions.jts.geom.Envelope;

/**
 * Georesource information for query results
 * 
 * @author Emily
 * @since 1.0.0
 */
public class QueryGeoResourceInfo extends IGeoResourceInfo {

	private Logger logger = Logger.getLogger(QueryGeoResourceInfo.class.getName());
	
	public QueryGeoResourceInfo( QueryGeoResource resource, IProgressMonitor monitor){
		String namePart = ""; //$NON-NLS-1$
		if (resource.getDataType().equals(QueryDataSource.POINT_TYPE.getLocalPart())){
			namePart = Messages.QueryGeoResourceInfo_pointQueryLayerName;
		}else if (resource.getDataType().equals(QueryDataSource.POLYGON_TYPE.getLocalPart())){
			namePart = Messages.QueryGeoResourceInfo_polygonQueryLayerName;
		}
		this.title = resource.getQueryName() + " (" + namePart + ")"; //$NON-NLS-1$ //$NON-NLS-2$
		computeBounds(resource, monitor);
	}
	
	/**
	 * Recomputes the bounds for this resource info.
	 * 
	 * @param resource resource source
	 */
	public void computeBounds(QueryGeoResource resource, IProgressMonitor monitor){
		//get bounds from result set
		try{
			QueryService service = ((QueryService)resource.resolve(QueryService.class, monitor));
			if (service != null && service.getResultSet() != null){
				Envelope env = service.getResultSet().getEnvelope();
				if (env == null){
					env = new Envelope();
				}
				this.bounds =  new ReferencedEnvelope(env, GeometryUtils.SMART_CRS);
			}
		}catch (IOException ex){
			logger.log(Level.INFO, "Unable to read bounds from query results", this.bounds); //$NON-NLS-1$
		}
	}
}
