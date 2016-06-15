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
package org.wcs.smart.er.map.samplingunit;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.IProgressMonitor;
import org.geotools.data.DataStore;
import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureStore;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.styling.Style;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.catalog.IGeoResourceInfo;
import org.locationtech.udig.catalog.IService;
import org.locationtech.udig.core.internal.CorePlugin;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.map.SamplingUnitStyleProvider;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.model.SurveyDesign;

/**
 * Georesource for a sampling unit.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class SamplingUnitGeoResource extends IGeoResource {
	
	private URL url = null;
	private String dataType;
	
	/**
	 * Creates a new sampling unit georesource.
	 * 
	 * @param service the query service
	 * @param dataType data type
	 */
	public SamplingUnitGeoResource(SamplingUnitService service, String dataType){
		this.service = service;
		this.dataType = dataType;
		URL serviceIdentifer = service.getIdentifier();
		
		try{
			if (serviceIdentifer != null){
				this.url = new URL(serviceIdentifer, serviceIdentifer.toExternalForm() + "#" + dataType, CorePlugin.RELAXED_HANDLER); //$NON-NLS-1$
			}
		 } catch (MalformedURLException e) {
             throw new IllegalArgumentException("The service URL must not contain a #", e); //$NON-NLS-1$
         }	
	}

	/**
	 * @return the query data type
	 */
	public String getDataType(){
		return this.dataType;
	}
	
	/**
	 * @see org.locationtech.udig.catalog.IResolve#getStatus()
	 */
	@Override
	public Status getStatus() {
		return service.getStatus();
	}

	/**
	 * @see org.locationtech.udig.catalog.IResolve#getMessage()
	 */
	@Override
	public Throwable getMessage() {
		return service.getMessage();
	}

	/**
	 * @see org.locationtech.udig.catalog.IGeoResource#createInfo(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	protected IGeoResourceInfo createInfo(IProgressMonitor monitor)
			throws IOException {
		return new SamplingUnitGeoResourceInfo(this, monitor);
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
				|| adaptee.isAssignableFrom(IService.class)
				|| adaptee.isAssignableFrom(FeatureSource.class)
				|| adaptee.isAssignableFrom(FeatureStore.class)
				|| adaptee.isAssignableFrom(SimpleFeatureStore.class)
	            || adaptee.isAssignableFrom(SimpleFeatureSource.class)
	            || adaptee.isAssignableFrom(SurveyDesign.class)
	            || adaptee.isAssignableFrom(Style.class)
				|| super.canResolve(adaptee);
	}

    /**
     * @see org.locationtech.udig.catalog.IGeoResource#resolve(java.lang.Class, org.eclipse.core.runtime.IProgressMonitor)
     */
	@Override
	public <T> T resolve(Class<T> adaptee, IProgressMonitor monitor)
			throws IOException {
		if (adaptee.isAssignableFrom(SurveyDesign.class)){
			return adaptee.cast(((SamplingUnitService)service).getSurveyDesign());
		}
		if (adaptee.isAssignableFrom(IGeoResourceInfo.class)) {
			return adaptee.cast(super.getInfo(monitor));
		}
		if (adaptee.isAssignableFrom(IService.class)) {
			return adaptee.cast(this.service);
		}

		if (adaptee.isAssignableFrom(FeatureSource.class) || adaptee.isAssignableFrom(SimpleFeatureSource.class) ) {
			DataStore ds = ((SamplingUnitService) service).getDataStore(monitor);
			if (ds != null) {
				FeatureSource<SimpleFeatureType, SimpleFeature> fs = ds
						.getFeatureSource(dataType);
				if (fs != null)
					return adaptee.cast(fs);
			} else {
				EcologicalRecordsPlugIn.log("Sampling unit datasource not created.", null); //$NON-NLS-1$
				return null;
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
		if (adaptee.isAssignableFrom(Style.class)){
			return adaptee.cast(SamplingUnitStyleProvider.createDefaultStyle(SamplingUnit.GeometryType.valueOf(dataType)));
		}
		return super.resolve(adaptee, monitor);
	}

	
}
