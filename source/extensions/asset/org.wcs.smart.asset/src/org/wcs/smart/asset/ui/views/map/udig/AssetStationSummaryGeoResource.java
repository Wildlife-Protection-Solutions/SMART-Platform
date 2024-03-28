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
package org.wcs.smart.asset.ui.views.map.udig;

import java.awt.Color;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import org.eclipse.core.runtime.IProgressMonitor;
import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureStore;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.data.store.ContentDataStore;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Font;
import org.geotools.styling.Mark;
import org.geotools.styling.PointPlacement;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.Stroke;
import org.geotools.styling.Style;
import org.geotools.styling.StyleBuilder;
import org.geotools.styling.StyleFactory;
import org.geotools.styling.TextSymbolizer;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.catalog.IGeoResourceInfo;
import org.locationtech.udig.catalog.IService;
import org.locationtech.udig.core.internal.CorePlugin;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.FilterFactory2;
import org.wcs.smart.asset.model.Asset;
import org.wcs.smart.asset.ui.views.map.FixedColumn;
import org.wcs.smart.udig.IFilteringResource;

/**
 * Georesource for a entity locations
 * 
 * @author Emily
 * @since 1.0.0
 */
public class AssetStationSummaryGeoResource extends IGeoResource implements IFilteringResource {
	

	private URL url = null;
	private Style cachedStyle = null;
	
	private ContentDataStore ds;
	
	public AssetStationSummaryGeoResource(AssetStationSummaryService service){
		this.service = service;
		URL serviceIdentifer = service.getIdentifier();
		
		try{
			this.url = URL.of(URI.create(serviceIdentifer.toExternalForm() + "#"), CorePlugin.RELAXED_HANDLER); //$NON-NLS-1$
		 } catch (MalformedURLException e) {
             throw new IllegalArgumentException("The service URL must not contain a #", e); //$NON-NLS-1$
         }
		
	}

	@Override
	public boolean canFilter(){
		return true;
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
		return new AssetStationSummaryGeoResourceInfo(this, monitor);
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
        	synchronized (this) {
        		if (ds == null) {
            		ds = new AssetStationSummaryDataSource(((AssetStationSummaryService)service));
            	}	
			}
        	FeatureSource<SimpleFeatureType, SimpleFeature> fs = ds.getFeatureSource(AssetStationSummaryDataSource.generateName());
             if (fs != null) return adaptee.cast(fs);
        }
        if (adaptee.isAssignableFrom(FeatureStore.class) || adaptee.isAssignableFrom(SimpleFeatureStore.class)){
        	 @SuppressWarnings("unchecked")
			FeatureSource<SimpleFeatureType, SimpleFeature> fs = resolve(FeatureSource.class, monitor);
             if (fs != null && fs instanceof FeatureStore) {
                 return adaptee.cast(fs);
             }
        }
        if (adaptee.isAssignableFrom(Style.class)){
        	if (cachedStyle == null){
        		cachedStyle = getDefaultLayerStyle();
        	}
        	if (cachedStyle != null) return adaptee.cast(cachedStyle);
        }
        return super.resolve(adaptee, monitor);
    }

    
    public static Style getDefaultLayerStyle(){
		StyleFactory sf = CommonFactoryFinder.getStyleFactory();
    	StyleBuilder builder = new StyleBuilder(sf);
    	FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();

    	FeatureTypeStyle fts = sf.createFeatureTypeStyle();
    	fts.setName("Status Style"); //$NON-NLS-1$
    	
    	// active
    	Color lineColor = new Color(16,104,12);
    	Color fillColor = new Color(90,178,8);
    	Mark active = sf.getCircleMark();
    	active.setFill(sf.createFill(builder.colorExpression(fillColor), builder.literalExpression(1)));
    	PointSymbolizer point = sf.createPointSymbolizer();
    	point.getGraphic().setSize(builder.literalExpression(16));
    	point.getGraphic().graphicalSymbols().add(active);
    	point.getGraphic().setRotation(builder.literalExpression(0));
    	Stroke stroke = sf.createStroke(builder.colorExpression(lineColor), builder.literalExpression(1));
    	stroke.setWidth(builder.literalExpression(1));
    	active.setStroke(stroke);
    	
    	Rule activeRule = sf.createRule();
    	activeRule.setName("Active"); //$NON-NLS-1$
    	activeRule.symbolizers().add(point);
    	activeRule.setFilter(ff.equal(ff.property((new FixedColumn(FixedColumn.Column.STATUS_KEY)).getKey()), ff.literal(Asset.Status.ACTIVE.name()), false));
    	
    	PointPlacement pp = sf.createPointPlacement(sf.createAnchorPoint(builder.literalExpression(0.5), builder.literalExpression(1.0)), sf.createDisplacement(builder.literalExpression(0.0), builder.literalExpression(-10.0)),null);
    	Font fnt = sf.createFont(builder.literalExpression("Tahoma"), builder.literalExpression("normal"), builder.literalExpression("normal"), builder.literalExpression(10.0)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    	
    	TextSymbolizer idlbl = sf.createTextSymbolizer(null, new Font[] {fnt}, null, ff.property(  (new FixedColumn(FixedColumn.Column.ID)).getKey() ), pp, null);
    	activeRule.symbolizers().add(idlbl);
    	
    	// inactive
    	lineColor = new Color(152,10,10);
    	fillColor = new Color(252,0,0);
    	Mark inactive = sf.getCircleMark();
    	inactive.setFill(sf.createFill(builder.colorExpression(fillColor), builder.literalExpression(1)));
    	point = sf.createPointSymbolizer();
    	point.getGraphic().setSize(builder.literalExpression(16));
    	point.getGraphic().graphicalSymbols().add(inactive);
    	point.getGraphic().setRotation(builder.literalExpression(0));
    	stroke = sf.createStroke(builder.colorExpression(lineColor), builder.literalExpression(1));
    	stroke.setWidth(builder.literalExpression(1));
    	inactive.setStroke(stroke);
    	
    	Rule inactiveRule = sf.createRule();
    	inactiveRule.setName("Inactive"); //$NON-NLS-1$
    	inactiveRule.symbolizers().add(point);
    	inactiveRule.setFilter(ff.equal(ff.property((new FixedColumn(FixedColumn.Column.STATUS_KEY)).getKey()), ff.literal(Asset.Status.INACTIVE.name()), false));
    	inactiveRule.symbolizers().add(idlbl);
    	
    	// retired
    	lineColor = new Color(78,78,78);
    	fillColor = new Color(147,147,147);
    	Mark retired = sf.getCircleMark();
    	retired.setFill(sf.createFill(builder.colorExpression(fillColor), builder.literalExpression(1)));
    	point = sf.createPointSymbolizer();
    	point.getGraphic().setSize(builder.literalExpression(16));
    	point.getGraphic().graphicalSymbols().add(retired);
    	point.getGraphic().setRotation(builder.literalExpression(0));
    	stroke = sf.createStroke(builder.colorExpression(lineColor), builder.literalExpression(1));
    	stroke.setWidth(builder.literalExpression(1));
    	retired.setStroke(stroke);
    	
    	Rule retiredRule = sf.createRule();
    	retiredRule.setName("Retired"); //$NON-NLS-1$
    	retiredRule.symbolizers().add(point);
    	retiredRule.setFilter(ff.equal(ff.property((new FixedColumn(FixedColumn.Column.STATUS_KEY)).getKey()), ff.literal(Asset.Status.RETIRED.name()), false));
    	retiredRule.symbolizers().add(idlbl);
    	
    	fts.rules().add(activeRule);
    	fts.rules().add(inactiveRule);
    	fts.rules().add(retiredRule);
    	Style style = sf.createStyle();
    	style.featureTypeStyles().add(fts);
    	return style;

	}
}
