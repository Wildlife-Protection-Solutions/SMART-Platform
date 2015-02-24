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
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.XMLMemento;
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
import org.locationtech.udig.style.sld.SLDContent;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.er.EcologicalRecordsPlugIn;

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
	
	public MissionGeoResource(MissionService service, String dataType){
		this.service = service;
		this.dataType = dataType;
		URL serviceIdentifer = service.getIdentifier();
		
		try{
			this.url = new URL(serviceIdentifer, serviceIdentifer.toExternalForm() + "#" + dataType, CorePlugin.RELAXED_HANDLER); //$NON-NLS-1$
		 } catch (MalformedURLException e) {
             throw new IllegalArgumentException("The service URL must not contain a #", e); //$NON-NLS-1$
         }
		
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
		return new MissionGeoResourceInfo(this, monitor);
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
        	}else if (dataType.equals(MissionDataSource.MISSIONTRACK_TYPE)){
        		s = createLineStyle();
        	}
        	if (s != null){
        		return adaptee.cast(s);
        	}
        }
        return super.resolve(adaptee, monitor);
    }
    
    private Style createPointStyle(){
    	String sld = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> " //$NON-NLS-1$
    			+ "<styleEntry type=\"SLDStyle\" version=\"1.0\">" //$NON-NLS-1$
    			+ "&lt;sld:StyledLayerDescriptor xmlns=\"http://www.opengis.net/sld\" xmlns:sld=\"http://www.opengis.net/sld\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:gml=\"http://www.opengis.net/gml\" version=\"1.0.0\"&gt;" //$NON-NLS-1$
		    	+ "&lt;sld:UserLayer&gt;" //$NON-NLS-1$
		    	+ "&lt;sld:LayerFeatureConstraints&gt;" //$NON-NLS-1$
		    	+ "&lt;sld:FeatureTypeConstraint/&gt;" //$NON-NLS-1$
		    	+ "&lt;/sld:LayerFeatureConstraints&gt;" //$NON-NLS-1$
		    	+ "&lt;sld:UserStyle&gt;" //$NON-NLS-1$
		    	+ "&lt;sld:Name&gt;MissionPoint&lt;/sld:Name&gt;" //$NON-NLS-1$
		    	+ "&lt;sld:Title/&gt;" //$NON-NLS-1$
		    	+ "&lt;sld:FeatureTypeStyle&gt;" //$NON-NLS-1$
		    	+ "&lt;sld:Name&gt;group 0&lt;/sld:Name&gt;" //$NON-NLS-1$
		    	+ "&lt;sld:FeatureTypeName&gt;Feature&lt;/sld:FeatureTypeName&gt;" //$NON-NLS-1$
		    	+ "&lt;sld:SemanticTypeIdentifier&gt;generic:geometry&lt;/sld:SemanticTypeIdentifier&gt;" //$NON-NLS-1$
		    	+ "&lt;sld:SemanticTypeIdentifier&gt;simple&lt;/sld:SemanticTypeIdentifier&gt;" //$NON-NLS-1$
		    	+ "&lt;sld:Rule&gt;" //$NON-NLS-1$
		    	+ "&lt;sld:Name&gt;default rule&lt;/sld:Name&gt;" //$NON-NLS-1$
		    	+ "&lt;sld:PointSymbolizer&gt;" //$NON-NLS-1$
		    	+ "&lt;sld:Graphic&gt;" //$NON-NLS-1$
		    	+ "&lt;sld:Mark&gt;" //$NON-NLS-1$
		    	+ "&lt;sld:WellKnownName&gt;star&lt;/sld:WellKnownName&gt;" //$NON-NLS-1$
		    	+ "&lt;sld:Fill&gt;" //$NON-NLS-1$
		    	+ "&lt;sld:CssParameter name=\"fill\"&gt;#FF0000&lt;/sld:CssParameter&gt;" //$NON-NLS-1$
		    	+ "&lt;/sld:Fill&gt;" //$NON-NLS-1$
		    	+ "&lt;/sld:Mark&gt;" //$NON-NLS-1$
		    	+ "&lt;sld:Size&gt;12&lt;/sld:Size&gt;" //$NON-NLS-1$
		    	+ "&lt;/sld:Graphic&gt;" //$NON-NLS-1$
		    	+ "&lt;/sld:PointSymbolizer&gt;" //$NON-NLS-1$
		    	+ "&lt;/sld:Rule&gt;" //$NON-NLS-1$
		    	+ "&lt;/sld:FeatureTypeStyle&gt;" //$NON-NLS-1$
		    	+ "&lt;/sld:UserStyle&gt;" //$NON-NLS-1$
		    	+ "&lt;/sld:UserLayer&gt;" //$NON-NLS-1$
		    	+ "&lt;/sld:StyledLayerDescriptor&gt;" //$NON-NLS-1$
    			+ "</styleEntry>"; //$NON-NLS-1$

		try {
			XMLMemento memento = XMLMemento.createReadRoot(new StringReader(sld));
			SLDContent c = new SLDContent();
			Style style = (Style) c.load(memento);
			return style;
		} catch (Exception ex) {
			EcologicalRecordsPlugIn.log("Error generating smart style", ex); //$NON-NLS-1$
			return null;
		}
    }

    private Style createLineStyle(){
    	String sld = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> " //$NON-NLS-1$
    			+ "<styleEntry type=\"SLDStyle\" version=\"1.0\">" //$NON-NLS-1$
    			+ "&lt;sld:StyledLayerDescriptor xmlns=\"http://www.opengis.net/sld\" xmlns:sld=\"http://www.opengis.net/sld\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:gml=\"http://www.opengis.net/gml\" version=\"1.0.0\"&gt;" //$NON-NLS-1$
		    	+ "&lt;sld:UserLayer&gt;" //$NON-NLS-1$
		    	+ "&lt;sld:LayerFeatureConstraints&gt;" //$NON-NLS-1$
		    	+ "&lt;sld:FeatureTypeConstraint/&gt;" //$NON-NLS-1$
		    	+ "&lt;/sld:LayerFeatureConstraints&gt;" //$NON-NLS-1$
		    	+ "&lt;sld:UserStyle&gt;" //$NON-NLS-1$
		    	+ "&lt;sld:Name&gt;MissionPoint&lt;/sld:Name&gt;" //$NON-NLS-1$
		    	+ "&lt;sld:Title/&gt;" //$NON-NLS-1$
		    	+ "&lt;sld:FeatureTypeStyle&gt;" //$NON-NLS-1$
		    	+ "&lt;sld:Name&gt;group 0&lt;/sld:Name&gt;" //$NON-NLS-1$
		    	+ "&lt;sld:Rule&gt;" //$NON-NLS-1$
		    	+ "&lt;sld:Name&gt;default rule&lt;/sld:Name&gt;" //$NON-NLS-1$
		    	+ "&lt;sld:LineSymbolizer&gt;" //$NON-NLS-1$
		    	+ "&lt;sld:Stroke&gt;" //$NON-NLS-1$
		    	+ "&lt;sld:CssParameter name=\"stroke\"&gt;#0080FF&lt;/sld:CssParameter&gt;" //$NON-NLS-1$
		    	+ "&lt;sld:CssParameter name=\"stroke-width\"&gt;2.0&lt;/sld:CssParameter&gt;" //$NON-NLS-1$
		    	+ "&lt;/sld:Stroke&gt;" //$NON-NLS-1$
		    	+ "&lt;/sld:LineSymbolizer&gt;" //$NON-NLS-1$
		    	+ "&lt;/sld:Rule&gt;" //$NON-NLS-1$
		    	+ "&lt;/sld:FeatureTypeStyle&gt;" //$NON-NLS-1$
		    	+ "&lt;/sld:UserStyle&gt;" //$NON-NLS-1$
		    	+ "&lt;/sld:UserLayer&gt;" //$NON-NLS-1$
		    	+ "&lt;/sld:StyledLayerDescriptor&gt;" //$NON-NLS-1$
    			+ "</styleEntry>"; //$NON-NLS-1$

		try {
			XMLMemento memento = XMLMemento.createReadRoot(new StringReader(sld));
			SLDContent c = new SLDContent();
			Style style = (Style) c.load(memento);
			return style;
		} catch (Exception ex) {
			EcologicalRecordsPlugIn.log("Error generating smart style", ex); //$NON-NLS-1$
			return null;
		}
    }
}
