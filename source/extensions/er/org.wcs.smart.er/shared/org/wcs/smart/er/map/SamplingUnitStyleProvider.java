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
package org.wcs.smart.er.map;

import java.awt.Color;

import org.geotools.factory.CommonFactoryFinder;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Graphic;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.Mark;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.Stroke;
import org.geotools.styling.Style;
import org.geotools.styling.StyleFactory;
import org.opengis.filter.FilterFactory;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.model.SamplingUnit.GeometryType;

/**
 * Style provider for sampling unit layer
 * @author Emily
 *
 */
public class SamplingUnitStyleProvider {
	public static Style createDefaultStyle(SamplingUnit.GeometryType type) {
		StyleFactory styleFactory = CommonFactoryFinder.getStyleFactory();
		FilterFactory filterFactory = CommonFactoryFinder.getFilterFactory();
		
		if (type == GeometryType.PLOT){
			Graphic gr = styleFactory.createDefaultGraphic();

	        Mark mark = styleFactory.getCircleMark();

	        mark.setStroke(styleFactory.createStroke(
	                filterFactory.literal(Color.BLACK), 
	                filterFactory.literal(1)));

	        mark.setFill(styleFactory.createFill(
	        		filterFactory.literal(Color.RED)));

	        gr.graphicalSymbols().clear();
	        gr.graphicalSymbols().add(mark);
	        gr.setSize(filterFactory.literal(6));

	        /*
	         * Setting the geometryPropertyName arg to null signals that we want to
	         * draw the default geomettry of features
	         */
	        PointSymbolizer sym = styleFactory.createPointSymbolizer(gr, null);

	        Rule rule = styleFactory.createRule();
	        rule.symbolizers().add(sym);
	        FeatureTypeStyle fts = styleFactory.createFeatureTypeStyle(new Rule[]{rule});
	        Style style = styleFactory.createStyle();
	        style.featureTypeStyles().add(fts);
	        
	        return style;
	        
		}else if (type == GeometryType.TRANSECT ){
	        Stroke stroke = styleFactory.createStroke(
	                filterFactory.literal(Color.RED),
	                filterFactory.literal(1));

	        /*
	         * Setting the geometryPropertyName arg to null signals that we want to
	         * draw the default geomettry of features
	         */
	        LineSymbolizer sym = styleFactory.createLineSymbolizer(stroke, null);

	        Rule rule = styleFactory.createRule();
	        rule.symbolizers().add(sym);
	        FeatureTypeStyle fts = styleFactory.createFeatureTypeStyle(new Rule[]{rule});
	        Style style = styleFactory.createStyle();
	        style.featureTypeStyles().add(fts);

	        return style;
		}
		return null;
	}
}
