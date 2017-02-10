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
package org.wcs.smart.i2.udig;

import java.awt.Color;

import org.geotools.factory.CommonFactoryFinder;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Graphic;
import org.geotools.styling.Mark;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.PolygonSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;
import org.geotools.styling.StyleBuilder;
import org.geotools.styling.StyleFactory;

/**
 * Represents the two types of location layers - points and polygons
 * 
 * @author Emily
 *
 */
public enum LocationLayerType {
	POINT("Point"), 
	POLYGON("Polygon"),
	ATTRIBUTE("Point");
	
	private String geomType;
	
	private LocationLayerType(String geomType){
		this.geomType = geomType;
	}
	
	public String getGeomType(){
		return this.geomType;
	}
	
	public Style getDefaultLayerStyle(){
		Color lineColor = new Color(69,81,140);
    	Color fillColor = new Color(120,153,215);
    	StyleFactory sf = CommonFactoryFinder.getStyleFactory();
    	StyleBuilder builder = new StyleBuilder(sf);
    	
    	if (this == POINT){
	    	FeatureTypeStyle fts = sf.createFeatureTypeStyle();
	    	fts.setName("Style 1");
	    	Mark x = sf.getXMark();
	    	x.setFill(sf.createFill(builder.colorExpression(fillColor), builder.literalExpression(0.5)));
	    	PointSymbolizer point = sf.createPointSymbolizer();
	    	point.getGraphic().setSize(builder.literalExpression(8));
	    	point.getGraphic().graphicalSymbols().add(x);
	    	point.getGraphic().setRotation(builder.literalExpression(0));
	    	Rule r = sf.createRule();
	    	r.setName("Rule 1");
	    	r.symbolizers().add(point);
	    	fts.rules().add(r);
	    	Style style = sf.createStyle();
	    	style.featureTypeStyles().add(fts);
	    	return style;
    	}else if (this == POLYGON){
    		Style style = sf.createStyle();
    		FeatureTypeStyle fts = sf.createFeatureTypeStyle();
    		fts.setName("Style 1");
    		style.featureTypeStyles().add(fts);
    		
    		PolygonSymbolizer sym = sf.createPolygonSymbolizer();
    		sym.setFill(sf.createFill(builder.colorExpression(fillColor), builder.literalExpression(0.5)));
    		sym.setStroke(sf.createStroke(builder.colorExpression(lineColor), builder.literalExpression(1)));
    		Rule r = sf.createRule();
    		r.setName("Rule 1");
    		r.symbolizers().add(sym);
    		fts.rules().add(r);
    		return style;
    	}else if (this == ATTRIBUTE){
    		StyleBuilder sb = new StyleBuilder();
    		Color darkRed = new Color(153, 0, 0);
			Mark mark = sb.createMark("star", sb.createFill(Color.RED),sb.createStroke(darkRed, 1));
			Graphic g = sb.createGraphic(null, mark, null, 1.0, 13.0,0.0);
			Style style = sb.createStyle(sb.createPointSymbolizer(g));
			return style;
    	}
    	return null;
	}
}