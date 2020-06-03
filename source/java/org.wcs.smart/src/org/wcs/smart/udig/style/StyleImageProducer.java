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
package org.wcs.smart.udig.style;

import org.eclipse.swt.graphics.Image;
import org.geotools.data.DataUtilities;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.renderer.style.GraphicStyle2D;
import org.geotools.renderer.style.SLDStyleFactory;
import org.geotools.renderer.style.Style2D;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Graphic;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.PolygonSymbolizer;
import org.geotools.styling.RasterSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;
import org.geotools.styling.Symbolizer;
import org.geotools.styling.visitor.DuplicatingStyleVisitor;
import org.geotools.util.NumberRange;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.udig.ui.graphics.Glyph;
import org.locationtech.udig.ui.graphics.SLDs;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.udig.legend.GlyphAWT;

/**
 * Converts a style to a glyph icon.
 * @author Emily
 *
 */
public enum StyleImageProducer {
	
	INSTANCE;
	
    static SimpleFeatureType pointSchema;
	static int IMAGE_SIZE = 16;
	static {
		try {
			pointSchema = DataUtilities.createType(
					"generated:point", "*point:Point"); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (SchemaException unExpected) {
			System.err.println(unExpected);
		}
	}
	
	//resize images down so entire image shows up in legend icon even if
    //style says size is larger
    private Rule changeSize(Rule rule, int newsize) {
    	  DuplicatingStyleVisitor copyStyle = new DuplicatingStyleVisitor();
          rule.accept(copyStyle);
          rule = (Rule) copyStyle.getCopy();
          
    	for (Symbolizer s : rule.symbolizers()) {
    		
    		if (s instanceof PointSymbolizer) {
    			Graphic g = ((PointSymbolizer)s).getGraphic();
    			
				SLDStyleFactory styleFactory = new SLDStyleFactory();

				Style2D tmp = styleFactory.createStyle(feature(newsize), (PointSymbolizer)s,
						new NumberRange<Double>(Double.class, Double.MIN_VALUE, Double.MAX_VALUE));
				
				//hack to make rectangular shaped icons not get chopped off 
				if (tmp instanceof GraphicStyle2D) {
					GraphicStyle2D style = (GraphicStyle2D) tmp;
					if (style.getImage().getWidth() > newsize || 
						style.getImage().getHeight() > newsize) {
						
						int newsize2 = newsize;
						if (style.getImage().getWidth() > style.getImage().getHeight()) {
    						newsize2 = (int)(newsize * (((float)style.getImage().getHeight() / style.getImage().getWidth() )));
						}
						g.setSize(CommonFactoryFinder.getFilterFactory().literal(newsize2 - 2));
					}
				}	
            }
        }
    	return rule;
    }
	private SimpleFeature feature(int imageSize) {
		return SimpleFeatureBuilder.build(pointSchema, 
				new Object[] {(new GeometryFactory()).createPoint(new Coordinate(imageSize / 2.0, imageSize/2.0))}
		, null);
	}
	/**
	 * Converts and sld style to a glyph image
	 */
	public Image createImage(Style sld) {
		Rule r = getRule(sld);
		if (r == null) {
			return null;
		}
		if (r.symbolizers().size() == 0) {
			return null;
		}

		Symbolizer sym = r.symbolizers().get(0);
		if (PointSymbolizer.class.isAssignableFrom(sym.getClass())) {
        	r = changeSize(r, IMAGE_SIZE);
			return GlyphAWT.point(r, IMAGE_SIZE).createImage();
		} else if (LineSymbolizer.class.isAssignableFrom(sym.getClass())) {
			return GlyphAWT.line(r, IMAGE_SIZE).createImage();
		} else if (PolygonSymbolizer.class.isAssignableFrom(sym.getClass())) {
			return GlyphAWT.polygon(r, IMAGE_SIZE).createImage();
		} else if (RasterSymbolizer.class.isAssignableFrom(sym.getClass())) {
			return GlyphAWT.grid(null, null, null, null, IMAGE_SIZE).createImage();
		}
		return null;
	}
	
	private Rule getRule(Style sld) {
		Rule rule = null;
		int size = 0;

		for (FeatureTypeStyle style : sld.featureTypeStyles()) {
			for (Rule potentialRule : style.rules()) {
				if (potentialRule != null) {
					Symbolizer[] symbs = potentialRule.getSymbolizers();
					for (int m = 0; m < symbs.length; m++) {
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
}
