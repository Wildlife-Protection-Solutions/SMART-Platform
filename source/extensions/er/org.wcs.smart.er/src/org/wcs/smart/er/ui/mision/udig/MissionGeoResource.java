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

import java.awt.Color;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.IProgressMonitor;
import org.geotools.data.DataStore;
import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureStore;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.Stroke;
import org.geotools.styling.Style;
import org.geotools.styling.StyleBuilder;
import org.geotools.styling.StyleFactory;
import org.geotools.styling.Symbolizer;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.catalog.IGeoResourceInfo;
import org.locationtech.udig.catalog.IService;
import org.locationtech.udig.core.internal.CorePlugin;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.util.SmartUtils;

/**
 * Mission observation and track GeoResource
 * 
 * @author Emily
 * @author elitvin
 *
 */
public class MissionGeoResource extends IGeoResource {
	
	private URL url = null;
	protected String dataType;
	protected String name;
	
	public MissionGeoResource(MissionService service, String dataType, String name){
		this.service = service;
		this.dataType = dataType;
		this.name = name;
		URL serviceIdentifer = service.getIdentifier();
		
		try{
			this.url = new URL(serviceIdentifer, serviceIdentifer.toExternalForm() + "#" + dataType, CorePlugin.RELAXED_HANDLER); //$NON-NLS-1$
		 } catch (MalformedURLException e) {
             throw new IllegalArgumentException("The service URL must not contain a #", e); //$NON-NLS-1$
         }
		
	}
	
	@Override
	public String getTitle() {
		return this.name;
	}
	
	public String getType(){
		return dataType;
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
		return new MissionGeoResourceInfo(this, monitor, this.name);
	}

	/**
	 * @see org.locationtech.udig.catalog.IGeoResource#getIdentifier()
	 */
	@Override
	public URL getIdentifier() {
		return this.url;
	}
	
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
	                || adaptee.isAssignableFrom(Style.class)
	                || super.canResolve(adaptee);
	    }
	  
  

    @Override
    public <T> T resolve( Class<T> adaptee, IProgressMonitor monitor ) throws IOException {
    	if (adaptee.isAssignableFrom(IGeoResourceInfo.class)){
    		return adaptee.cast(super.getInfo(monitor));
    	}
        if( adaptee.isAssignableFrom(IService.class) ){
            return adaptee.cast( this.service );
        }
      
        if (adaptee.isAssignableFrom(FeatureSource.class) || adaptee.isAssignableFrom(SimpleFeatureSource.class) ){
        	 DataStore ds = ((MissionService)service).getDataStore(monitor);
             if (ds != null) {
                 FeatureSource<SimpleFeatureType, SimpleFeature> fs = ds.getFeatureSource(dataType);
                 if (fs != null)
                     return adaptee.cast(fs);
             }else{
            	 //throw some sort of error
            	 return null;
             }
        }
        if (adaptee.isAssignableFrom(FeatureStore.class) || adaptee.isAssignableFrom(SimpleFeatureStore.class)){
        	 @SuppressWarnings("unchecked")
			FeatureSource<SimpleFeatureType, SimpleFeature> fs = resolve(FeatureSource.class, monitor);
             if (fs != null && fs instanceof FeatureStore) {
                 return adaptee.cast(fs);
             }
        }
        
        if (adaptee.isAssignableFrom(Style.class)){
        	Style s = null;
        	if (dataType.equals(MissionDataSource.MISSIONWAYPOINT_TYPE)){
        		s = createPointStyle();
        	}if (dataType.equals(MissionDataSource.MISSIONRAWWAYPOINT_TYPE)){
        		s = createPointPrjStyle();
        	}else if (dataType.equals(MissionDataSource.MISSIONTRACK_TYPE)){
        		s = createLineStyle();
        	}else if (MissionDataSource.isGeometryAttribute(dataType)) {
        		s = ((MissionService)service).getDataStore(monitor).getAttribute(dataType).getAttributeGeometryStyle().toStyle();
        	}
        	
        	if (s != null){
        		return adaptee.cast(s);
        	}
        }
        return super.resolve(adaptee, monitor);
    }
    
    private Style createPointStyle(){
    	return SmartUtils.getDefaultWaypointStyle();
    }

    private static Style createPointPrjStyle() {
    	return SmartUtils.getDefaultPrjWaypointStyle();
	}
    
    
    private Style createLineStyle(){
    	StyleFactory sf = CommonFactoryFinder.getStyleFactory();
		StyleBuilder sb = new StyleBuilder(sf);
        
		Stroke linestroke = sb.createStroke(new Color(0, 128, 255), 2, null);
		LineSymbolizer lines = sb.createLineSymbolizer(linestroke);
		
		Rule rr = sb.createRule(new Symbolizer[] {lines});
		
		org.geotools.styling.FeatureTypeStyle fts = sf.createFeatureTypeStyle();
    	fts.setName("Track Style"); //$NON-NLS-1$
    	fts.rules().add(rr);
		
		Style style = sf.createStyle();
    	style.featureTypeStyles().add(fts);
		return style;
    }
}
