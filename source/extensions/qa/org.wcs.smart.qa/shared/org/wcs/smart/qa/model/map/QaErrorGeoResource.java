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
package org.wcs.smart.qa.model.map;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.IProgressMonitor;
import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureStore;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.styling.Style;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.catalog.IGeoResourceInfo;
import org.locationtech.udig.catalog.IService;
import org.locationtech.udig.core.internal.CorePlugin;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.udig.IFilteringResource;

/**
 * Georesource fo QaError layers.  Supports layer filtering.
 * 
 * @author Emily
 *
 */
public class QaErrorGeoResource extends IGeoResource implements IFilteringResource {

	private String typeName;
	private IGeoResourceInfo info = new QaErrorGeoResourceInfo();
	
	public QaErrorGeoResource(IService service, String typeName){
		this.service = service;
		this.typeName = typeName;
	}
	
	@Override
	public Status getStatus() {
		return service.getStatus();
	}

	@Override
	public Throwable getMessage() {
		return service.getMessage();
	}

	@Override
	public boolean canFilter() {
		return true;
	}

	@Override
	protected IGeoResourceInfo createInfo(IProgressMonitor monitor)
			throws IOException {
		return info;
	}

	@Override
	public URL getIdentifier() {
		try {
			return new URL(null, service.getIdentifier().toExternalForm() + "#" + typeName, CorePlugin.RELAXED_HANDLER); 
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public <T> boolean canResolve( Class<T> adaptee ) {
		if (adaptee == null) return false;
		return adaptee.isAssignableFrom(IGeoResourceInfo.class)
				|| adaptee.isAssignableFrom(IService.class)
				|| adaptee.isAssignableFrom(FeatureSource.class)
				|| adaptee.isAssignableFrom(FeatureStore.class)
				|| adaptee.isAssignableFrom(SimpleFeatureStore.class)
				|| adaptee.isAssignableFrom(SimpleFeatureSource.class)
				|| adaptee.isAssignableFrom(Style.class)
				|| super.canResolve(adaptee);
	}
	  


	@Override
	public <T> T resolve(Class<T> adaptee, IProgressMonitor monitor)
			throws IOException {
		
		if (adaptee.isAssignableFrom(FeatureSource.class)
				|| adaptee.isAssignableFrom(SimpleFeatureSource.class)) {
			QaErrorMemoryDatastore ds = ((QaErrorService)service).getDataStore();
			
			if (ds != null) {
				FeatureSource<SimpleFeatureType, SimpleFeature> fs = ds.getFeatureSource(typeName);
				if (fs != null) return adaptee.cast(fs);
			} else {
				// throw some sort of error
				return null;
			}
		}
		if (adaptee.isAssignableFrom(FeatureStore.class)
				|| adaptee.isAssignableFrom(SimpleFeatureStore.class)) {
			@SuppressWarnings("unchecked")
			FeatureSource<SimpleFeatureType, SimpleFeature> fs = resolve(
					FeatureSource.class, monitor);
			if (fs != null && fs instanceof FeatureStore) {
				return adaptee.cast(fs);
			}
		}
		
		return super.resolve(adaptee, monitor);
	}
    
	
	private class QaErrorGeoResourceInfo extends IGeoResourceInfo {
		public QaErrorGeoResourceInfo() {
			this.description = "Resources for QA results";
			this.name = "QA Error GeoResource";
			this.title = this.name;
			this.bounds = new ReferencedEnvelope(SmartDB.DATABASE_CRS);
		}
	}
	
}
