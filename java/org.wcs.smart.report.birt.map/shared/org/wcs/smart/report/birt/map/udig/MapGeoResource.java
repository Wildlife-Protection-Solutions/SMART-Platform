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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.birt.data.engine.api.IQueryResults;
import org.eclipse.birt.report.model.api.OdaDataSetHandle;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.geotools.data.DataStore;
import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureStore;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.catalog.IGeoResourceInfo;
import org.locationtech.udig.catalog.IResolve;
import org.locationtech.udig.catalog.IService;
import org.locationtech.udig.catalog.IServiceInfo;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.report.birt.map.MapLayerInfo;

/**
 * Georesource for BIRT SMART Map Layer
 * @author Emily
 *
 */
public class MapGeoResource extends IGeoResource {

	private URL url = null;
	private String dataType;
	
	private BirtDataStore datastore;
	private IQueryResults queryResults;
	private MapLayerInfo mapInfo;
	private OdaDataSetHandle dataSetHandle;
	
	/**
	 * Creates a new query georesource.
	 * 
	 * @param service the query service
	 * @param dataType data type
	 */
	public MapGeoResource(IQueryResults queryResults, 
			MapLayerInfo mapInfo, MapQueryService service){
		
		this.mapInfo = mapInfo;
		this.queryResults = queryResults;
		super.service = service;
		try {
			this.url = new URL(service.getIdentifier(), service.getIdentifier().toExternalForm() + "#" + System.nanoTime(), MapQueryService.RELAXED_HANDLER);			 //$NON-NLS-1$
		} catch (MalformedURLException e) {
			Logger.getLogger(MapGeoResource.class.getName()).log(Level.WARNING, e.getMessage(), e);
		}	
		
		try {
			this.info = createInfo(new NullProgressMonitor());
		} catch (IOException e) {
			Logger.getLogger(MapGeoResource.class.getName()).log(Level.WARNING, e.getMessage(), e);
		}
	}
	
	public MapGeoResource(OdaDataSetHandle dataSetHandle, 
			MapLayerInfo mapInfo, MapQueryService service){
		this.dataSetHandle = dataSetHandle;
		this.mapInfo = mapInfo;
		this.queryResults = null;
		super.service = service;
		try {
			this.url = new URL(service.getIdentifier(), service.getIdentifier().toExternalForm() + "#" + dataType, MapQueryService.RELAXED_HANDLER);			 //$NON-NLS-1$
		} catch (MalformedURLException e) {
			Logger.getLogger(MapGeoResource.class.getName()).log(Level.WARNING, e.getMessage(), e);
		}	
		
		try {
			this.info = createInfo(new NullProgressMonitor());
		} catch (IOException e) {
			Logger.getLogger(MapGeoResource.class.getName()).log(Level.WARNING, e.getMessage(), e);
		}
	}


	/**
	 * @see org.locationtech.udig.catalog.IResolve#getStatus()
	 */
	@Override
	public Status getStatus() {
		return Status.CONNECTED;
	}

	/**
	 * @see org.locationtech.udig.catalog.IResolve#getMessage()
	 */
	@Override
	public Throwable getMessage() {
		return null;
	}

	/**
	 * @see org.locationtech.udig.catalog.IGeoResource#createInfo(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	protected IGeoResourceInfo createInfo(IProgressMonitor monitor)
			throws IOException {
		return new MapGeoResourceInfo(this);
	}

	/**
	 * @see org.locationtech.udig.catalog.IGeoResource#getIdentifier()
	 */
	@Override
	public URL getIdentifier() {
		return this.url;
	}
	
	/**
	 * @see org.locationtech.udig.catalog.IGeoResource#canResolve(java.lang.Class)
	 */
	@Override
	public <T> boolean canResolve( Class<T> adaptee ) {
		if (adaptee == null)
			return false;

		return adaptee.isAssignableFrom(IGeoResourceInfo.class)
				|| adaptee.isAssignableFrom(FeatureSource.class)
				|| adaptee.isAssignableFrom(FeatureStore.class)
				|| adaptee.isAssignableFrom(SimpleFeatureStore.class)
	            || adaptee.isAssignableFrom(SimpleFeatureSource.class)
	            || adaptee.isAssignableFrom(IGeoResource.class) 
	            || adaptee.isAssignableFrom(IService.class)
	            || adaptee.isAssignableFrom(getClass()) 
	            || adaptee.isAssignableFrom(IResolve.class);

	}

	private synchronized DataStore getDataStore(){
		if (datastore != null) return datastore;
		
		if (queryResults == null){
			datastore = new BirtDataStore(dataSetHandle, dataType, mapInfo);
		}else{
			datastore = new BirtDataStore(queryResults, dataType, mapInfo);
		}
		return datastore;
	}
    /**
     * @see org.locationtech.udig.catalog.IGeoResource#resolve(java.lang.Class, org.eclipse.core.runtime.IProgressMonitor)
     */
	@Override
	public <T> T resolve(Class<T> adaptee, IProgressMonitor monitor)
			throws IOException {
		if (adaptee.isAssignableFrom(IGeoResourceInfo.class)) {
			return adaptee.cast(super.getInfo(monitor));
		}
		if (adaptee.isAssignableFrom(FeatureSource.class) || adaptee.isAssignableFrom(SimpleFeatureSource.class) ) {
			DataStore ds = getDataStore();
			if (ds != null) {
				FeatureSource<SimpleFeatureType, SimpleFeature> fs = ds
						.getFeatureSource(dataType);
				if (fs != null)
					return adaptee.cast(fs);
			} 
		}
		if (adaptee.isAssignableFrom(FeatureStore.class) || adaptee.isAssignableFrom(SimpleFeatureStore.class)) {
			@SuppressWarnings("unchecked")
			FeatureSource<SimpleFeatureType, SimpleFeature> fs = resolve(
					FeatureSource.class, monitor);
			if (fs != null && fs instanceof FeatureStore) {
				return adaptee.cast(fs);
			}
		}
		 if (adaptee.isAssignableFrom(IGeoResourceInfo.class)) {
	            return adaptee.cast(getInfo(monitor));
	        }
	        if (adaptee.isAssignableFrom(IService.class)) {
	            return adaptee.cast(service(monitor));
	        }
	        if (adaptee.isAssignableFrom(IServiceInfo.class)) {
	            try {
	                monitor.beginTask("service info", 100); //$NON-NLS-1$
	                IService service = service(new SubProgressMonitor(monitor, 40));
	                if (service != null) {
	                    IServiceInfo info = service.getInfo(new SubProgressMonitor(monitor, 60));
	                    return adaptee.cast(info);
	                }
	            } finally {
	                monitor.done();
	            }
	        }
	        if (adaptee.isAssignableFrom(IGeoResource.class)) {
	            monitor.done();
	            return adaptee.cast(this);
	        }
	        if (adaptee.isAssignableFrom(getClass())) {
	            return adaptee.cast(this);
	        }
	        return null;
	}

}
