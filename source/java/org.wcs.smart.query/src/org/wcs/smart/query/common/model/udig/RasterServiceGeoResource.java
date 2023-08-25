/*
 * Copyright (C) 2022 Wildlife Conservation Society
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
package org.wcs.smart.query.common.model.udig;

import java.io.IOException;
import java.io.StringReader;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.XMLMemento;
import org.geotools.styling.Style;
import org.locationtech.udig.catalog.rasterings.AbstractRasterGeoResource;
import org.locationtech.udig.catalog.rasterings.AbstractRasterGeoResourceInfo;
import org.locationtech.udig.style.sld.SLDContent;
import org.wcs.smart.map.raster.GridMetadata;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.common.model.GridQueryResult;
import org.wcs.smart.query.common.model.GriddedQuery;
import org.wcs.smart.query.internal.Messages;

/**
 * 
 * @author Emily
 * @since 8.0.0
 */
public class RasterServiceGeoResource extends AbstractRasterGeoResource{

	private AbstractRasterGeoResourceInfo  info = null;
	
	public RasterServiceGeoResource(RasterService service, String fileName) {
		super(service, fileName);
	}
	
	@Override
	protected AbstractRasterGeoResourceInfo createInfo(
			IProgressMonitor monitor) throws IOException {
		if (info == null){
			info = new AbstractRasterGeoResourceInfo(this, Messages.RasterService_keyword1, Messages.RasterService_keywork2, Messages.RasterService_keyword3){
				 @Override
				 public String getTitle() {
					 return ((RasterService)service).getQuery().getName() + " [" + ((RasterService)service).getQuery().getId() + "]"; //$NON-NLS-1$ //$NON-NLS-2$
				 }
			};
		}
		return info;
	}

	 @Override
	 public Style style( IProgressMonitor monitor ) {
		double minValue = 0;
		double maxValue = 10;
		
		GridQueryResult results = (GridQueryResult) ((RasterService)service).getQuery().getCachedResults();
		if (results == null){
			//query has not been run
			return null;
		}
		GridMetadata metadata = results.getMetadata();
		if (metadata != null){
			 minValue = metadata.getMinResultValue();
			 maxValue = metadata.getMaxResultValue();
		}
		
		
		String sld = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" //$NON-NLS-1$
				+ "<styleEntry type=\"SLDStyle\" version=\"1.0\">&lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot;?&gt;" //$NON-NLS-1$
				+ "&lt;sld:StyledLayerDescriptor xmlns=&quot;http://www.opengis.net/sld&quot; xmlns:sld=&quot;http://www.opengis.net/sld&quot; xmlns:ogc=&quot;http://www.opengis.net/ogc&quot; xmlns:gml=&quot;http://www.opengis.net/gml&quot; version=&quot;1.0.0&quot;&gt;" //$NON-NLS-1$
				+ "&lt;sld:UserLayer&gt; " //$NON-NLS-1$
				+ "&lt;sld:LayerFeatureConstraints&gt; " //$NON-NLS-1$
				+ "&lt;sld:FeatureTypeConstraint/&gt; " //$NON-NLS-1$
				+ "&lt;/sld:LayerFeatureConstraints&gt; " //$NON-NLS-1$
				+ "&lt;sld:UserStyle&gt; " //$NON-NLS-1$
				+ "&lt;sld:Name&gt;000051&lt;/sld:Name&gt; " //$NON-NLS-1$
				+ "&lt;sld:Title/&gt; &lt;sld:FeatureTypeStyle&gt; " //$NON-NLS-1$
				+ "&lt;sld:Name&gt;name&lt;/sld:Name&gt; &lt;sld:Rule&gt; " //$NON-NLS-1$
				+ "&lt;sld:RasterSymbolizer&gt; &lt;sld:Geometry&gt; " //$NON-NLS-1$
				+ "&lt;ogc:PropertyName&gt;grid&lt;/ogc:PropertyName&gt; " //$NON-NLS-1$
				+ "&lt;/sld:Geometry&gt; &lt;sld:ColorMap&gt; " //$NON-NLS-1$
				+ "&lt;sld:ColorMapEntry color=&quot;#FFFFFF&quot; opacity=&quot;0.0&quot; quantity=&quot;-9999&quot; label=&quot;-no data-&quot;/&gt; " //$NON-NLS-1$
			    + "&lt;sld:ColorMapEntry color=&quot;#FFECEC&quot; opacity=&quot;0.8&quot; quantity=&quot;" + minValue //$NON-NLS-1$
			    + "&quot;/&gt;"; //$NON-NLS-1$
	    if(minValue != maxValue){
	    	sld += "&lt;sld:ColorMapEntry color=&quot;#FF0000&quot; opacity=&quot;0.8&quot; quantity=&quot;" //$NON-NLS-1$
				+ maxValue
				+ "&quot;/&gt;"; //$NON-NLS-1$
	    }
	    sld += "&lt;/sld:ColorMap&gt; &lt;/sld:RasterSymbolizer&gt; &lt;/sld:Rule&gt; &lt;/sld:FeatureTypeStyle&gt; &lt;/sld:UserStyle&gt; &lt;/sld:UserLayer&gt;&lt;/sld:StyledLayerDescriptor&gt;</styleEntry>"; //$NON-NLS-1$
	    
		try {
			XMLMemento memento = XMLMemento.createReadRoot(new StringReader(sld));
			SLDContent c = new SLDContent();
				Style style = (Style)c.load(memento);
			 return style;
		} catch (Exception ex) {
			QueryPlugIn.displayLog(Messages.RasterService_InvalidRasterStyle, ex);
			return null;
		}
			
	 }

	 @Override
	 public <T> boolean canResolve(Class<T> adaptee) {
		if (adaptee == null) return false;
		if (adaptee.isAssignableFrom(GriddedQuery.class)) return true;
		return adaptee.isAssignableFrom(RasterService.class) || 
					super.canResolve(adaptee);
	}
	 
	 @Override
	 public <T> T resolve(Class<T> adaptee, IProgressMonitor monitor) throws IOException {
	 	if (adaptee == null) return null;
	 	
	 	if (adaptee.isAssignableFrom(RasterService.class)){
	 		return adaptee.cast(RasterService.class);
	 	}
	 	return super.resolve(adaptee, monitor);
	 }
}
