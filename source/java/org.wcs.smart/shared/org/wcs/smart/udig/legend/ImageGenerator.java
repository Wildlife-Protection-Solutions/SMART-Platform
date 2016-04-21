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
package org.wcs.smart.udig.legend;

import java.io.IOException;

import org.eclipse.jface.resource.ImageDescriptor;
import org.geotools.data.FeatureSource;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;
import org.geotools.styling.Symbolizer;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.catalog.IGeoResourceInfo;
import org.locationtech.udig.project.ILayer;
import org.locationtech.udig.project.internal.Layer;
import org.locationtech.udig.project.internal.StyleBlackboard;
import org.locationtech.udig.ui.graphics.SLDs;
import org.opengis.coverage.grid.GridCoverageReader;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.GeometryDescriptor;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Class for generating image descriptors for legend items.
 * @author Emily
 *
 */
public class ImageGenerator {
	
	private static ImageGenerator instance = null;

    public static ImageGenerator getInstance() {
        return instance;
    }

    public ImageGenerator() {
        instance=this;
    }

    /**
     * A non null answer when layer has a good gylph.
     * <p>
     * Where a good/real means:
     * <ul>
     * <li>label.getGylph() != null
     * <li>label.getProperties().get( GENERATED_GYLPH ) != null
     * </p>
     * <p>
     * This method does not block and used used by the decorateImage and our thread to test/acquire
     * the right image. If <code>null</code> is returned the thread will be started in the hopes
     * of producing something.
     * <p>
     * 
     * @returns Image for layer, or <code>null</code> if unavailable Image icon( Layer layer ) {
     *          ImageDescriptor glyph = layer.getGlyph(); if (glyph != null) return
     *          cache.getImage(glyph); Image genglyph = (Image)
     *          layer.getProperties().get(GENERATED_ICON); if (genglyph != null &&
     *          !genglyph.isDisposed() ) return genglyph; // we have already generated one return
     *          null; }
     */

    /**
     * Genearte label and place in label.getProperties().getSTring( GENERATED_NAME ).
     * <p>
     * Label is genrated from Resource.
     * </p>
     * 
     * @return gernated layer
     */
    public static ImageDescriptor generateIcon( Layer layer ) {
        StyleBlackboard style = layer.getStyleBlackboard();

        if (style != null && !style.getContent().isEmpty()) {
            ImageDescriptor icon = generateStyledIcon(layer);
            if (icon != null)
                return icon;
        }
        ImageDescriptor icon = generateDefaultIcon(layer);
        if (icon != null)
            return icon;
        return null;
    }

    /**
     * Generate icon based on style information.
     * <p>
     * Will return null if an icom based on the current style could not be generated. You may
     * consult generateDefaultIcon( layer ) for a second opionion based on just the layer
     * information.
     * 
     * @param layer
     * @return ImageDecriptor for layer, or null in style could not be indicated
     */
    public static ImageDescriptor generateStyledIcon( Layer layer ) {
        StyleBlackboard blackboard = layer.getStyleBlackboard();
        if (blackboard == null)
            return null;

        Style sld = (Style) blackboard.lookup(Style.class); // or
        // blackboard.get(
        // "org.locationtech.udig.style.sld"
        // );
        if (sld != null) {
            Rule rule = getRule(sld);
            return generateStyledIcon(layer, rule);
        }
//        if (layer.hasResource(WebMapServer.class)) {
//            return null; // do not support styling for wms yet
//        }
        return null;
    }

    private static Rule getRule( Style sld ) {
        Rule rule = null;
        int size = 0;

        for( FeatureTypeStyle style : sld.featureTypeStyles() ) {
            for( Rule potentialRule : style.rules() ) {
                if (potentialRule != null) {
                    Symbolizer[] symbs = potentialRule.getSymbolizers();
                    for( int m = 0; m < symbs.length; m++ ) {
                        if (symbs[m] instanceof PointSymbolizer) {
                            int newSize = SLDs.pointSize((PointSymbolizer) symbs[m]);
                            if (newSize > 16 && size != 0) {
                                // return with previous rule
                                return rule;
                            }
                            size = newSize;
                            rule = potentialRule;
                        } else {
                            return potentialRule;
                        }
                    }
                }
            }
        }
        return rule;
    }

    public static ImageDescriptor generateStyledIcon( ILayer layer, Rule rule ) {
        if (layer.hasResource(FeatureSource.class) && rule != null) {
            SimpleFeatureType type = layer.getSchema();
            GeometryDescriptor geom = type.getGeometryDescriptor();
            if (geom != null) {
                Class<?> geom_type = geom.getType().getBinding();
                if (geom_type == Point.class || geom_type == MultiPoint.class) {
                    return GlyphAWT.point(rule);
                } else if (geom_type == LineString.class || geom_type == MultiLineString.class) {
                    return GlyphAWT.line(rule);
                } else if (geom_type == Polygon.class || geom_type == MultiPolygon.class) {
                    return GlyphAWT.polygon(rule);
                } else if (geom_type == Geometry.class || geom_type == GeometryCollection.class) {
                    return GlyphAWT.geometry(rule);
                }
            }
        }
        IGeoResource resource = layer.findGeoResource(FeatureSource.class);
        if (resource == null)
            return null;
        IGeoResourceInfo info;
        try {
            info = resource.getInfo(null);
        } catch (IOException e) {
            info = null;
        }
        if (info != null) {
            ImageDescriptor infoIcon = info.getImageDescriptor();
            if (infoIcon != null)
                return infoIcon;
        }
        if (resource.canResolve(GridCoverageReader.class)) {
            ImageDescriptor icon = GlyphAWT.grid(null, null, null, null);
            if (icon != null)
                return icon;
        }
        if (resource.canResolve(FeatureSource.class)) {
            ImageDescriptor icon = GlyphAWT.geometry(rule);
            if (icon != null)
                return icon;
        }
        return null;
    }

    /**
     * Generate icon based on simple layer type information without style.
     * <p>
     * The following information is checked:
     * <ul>
     * <li>All WMS resources known to the layer - they often have default icon
     * <li>FeatureSoruce known to the layer - icon can be based on SimpleFeatureType
     * <li>IGeoResourceInfo type information
     * </ul>
     * </p>
     * 
     * @param layer
     * @return Icon based on layer, null if unavailable
     */
    static ImageDescriptor generateDefaultIcon( Layer layer ) {
        // check for a WMS layer first as it has a pretty icon
    	//
//        if (layer.hasResource(WebMapServer.class) && layer.hasResource(ImageDescriptor.class)) {
        if (layer.hasResource(ImageDescriptor.class)) {
            try {
                ImageDescriptor legendGraphic = layer.getResource(ImageDescriptor.class, null);
                if (legendGraphic != null)
                    return legendGraphic;
            } catch (IOException notAvailable) {
                // should not really have happened
            }
        }
        // lets try for featuretype based glyph
        //
        if (layer.hasResource(FeatureSource.class)) {

            SimpleFeatureType type = layer.getSchema();
            GeometryDescriptor geom = type.getGeometryDescriptor();
            if (geom != null) {
                Class<?> geom_type = geom.getType().getBinding();
                if (geom_type == Point.class || geom_type == MultiPoint.class) {
                    return GlyphAWT.point(null, null);
                } else if (geom_type == LineString.class || geom_type == MultiLineString.class) {
                    return GlyphAWT.line(null, SLDs.NOTFOUND);
                } else if (geom_type == Polygon.class || geom_type == MultiPolygon.class) {
                    return GlyphAWT.polygon(null, null, SLDs.NOTFOUND);
                } else if (geom_type == Geometry.class || geom_type == GeometryCollection.class) {
                    return GlyphAWT.geometry(null, null);
                } else {
                    return GlyphAWT.geometry(null, null);
                }
            }
        }
        
        //
        // Resource based glyph?
        //
        IGeoResourceInfo info = null;
        try {
            if( !layer.getGeoResources().isEmpty() ){
                info = layer.getGeoResources().get(0).getInfo(null);
            }
        } catch (IOException e) {
            //
        }
        if (info != null) {
            ImageDescriptor infoIcon = info.getImageDescriptor();
            if (infoIcon != null)
                return infoIcon;
        }

        if (layer.hasResource(GridCoverageReader.class)) {
            ImageDescriptor icon = GlyphAWT.grid(null, null, null, null);
            if (icon != null)
                return icon;
        }
        if (layer.hasResource(FeatureSource.class)) {
            ImageDescriptor icon = GlyphAWT.geometry(null, null);
            if (icon != null)
                return icon;
        }
        return null;
    }

}
