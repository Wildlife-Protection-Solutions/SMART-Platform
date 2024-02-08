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
package org.wcs.smart.ca.datamodel;

import java.awt.Color;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.factory.CommonFactoryFinder;
import org.geotools.styling.Fill;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.PolygonSymbolizer;
import org.geotools.styling.Stroke;
import org.geotools.styling.Style;
import org.geotools.styling.StyleBuilder;
import org.geotools.styling.StyleFactory;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * Represents the simple style for a geometry data model attribute. 
 * 
 */
public class AttributeGeometryStyle {

	public static AttributeGeometryStyle fromAttribute(Attribute attribute) {
		if (attribute.getType().isGeometry()) {
			return new AttributeGeometryStyle(attribute);
		}else {
			return null;
		}
	}
	
	private static final String LINESIZE = "linesize"; //$NON-NLS-1$
	private static final String FILLCOLOR = "fillcolor"; //$NON-NLS-1$
	private static final String LINECOLOR = "linecolor"; //$NON-NLS-1$
	private static final String FILLALPHA = "fillalpha"; //$NON-NLS-1$
	private static final String LINEALPHA = "linealpha"; //$NON-NLS-1$
	
	private Color fillColor = Color.LIGHT_GRAY;
	private Color lineColor = Color.DARK_GRAY;
	private int fillAlpha = 100;
	private int lineAlpha = 100;
	private int lineSize = 1;
	private Attribute.AttributeType type;
	
	public AttributeGeometryStyle(Attribute.AttributeType type) {
		this.type = type;
	}
	
	public AttributeGeometryStyle(Attribute.AttributeType type, String format) {
		this.type = type;
		if (format != null) init(format);
	}
	
	private AttributeGeometryStyle(Attribute attribute) {
		this(attribute.getType(), attribute.getRegex());
		this.type = attribute.getType();
	}
	
	private void init(String format) {
		try {
			JSONObject json = (JSONObject) (new JSONParser()).parse(format);
			lineSize = Integer.parseInt(json.get(LINESIZE).toString());
			Object color = json.get(LINECOLOR);
			if (color != null) {
				this.lineColor = stringToColor(color.toString());
			}
			color = json.get(LINEALPHA);
			if (color != null) {
				lineAlpha = Integer.parseInt(color.toString());
			}
			color = json.get(FILLCOLOR);
			if (color != null) {
				this.fillColor = stringToColor(color.toString());
			}
			color = json.get(FILLALPHA);
			if (color != null) {
				fillAlpha = Integer.parseInt(color.toString());
			}
		} catch (ParseException e) {
			Logger.getLogger(AttributeGeometryStyle.class.getName()).log(Level.WARNING, e.getMessage(), e);
		}
	}
	
	private String toString(Color color ) {
		int rgb = color.getRGB();
		return String.format("#%06X", rgb); //$NON-NLS-1$
	}
	
	private Color stringToColor(String hex) {
		 long value = Long.decode(hex);
		  return new Color(
		    (int) (value >> 16) & 0xFF,
		    (int) (value >> 8) & 0xFF,
		    (int) (value & 0xFF)
		  );
	}
	
	public String getAttributeValue() {
		HashMap<String,String> values = new HashMap<>();
		values.put(LINECOLOR, toString(lineColor));
		values.put(LINESIZE, String.valueOf(lineSize));
		values.put(LINEALPHA, String.valueOf(lineAlpha));
		if (this.type == Attribute.AttributeType.POLYGON) {
			values.put(FILLCOLOR, toString(fillColor));
			values.put(FILLALPHA, String.valueOf(fillAlpha));
		}
		return JSONObject.toJSONString(values);
		
	}
	
	public int getFillAlpha() {
		return this.fillAlpha;
	}
	
	public Color getFillColor() {
		return this.fillColor;
	}
	
	public Color getLineColor() {
		return this.lineColor;
	}
	
	public int getLineSize() {
		return this.lineSize;
	}
	
	public int getLineAlpha() {
		return this.lineAlpha;
	}
	
	public void setLineSize(int lineSize) {
		this.lineSize = lineSize;
	}
	
	public void setLineColor(Color lineColor) {
		this.lineColor = lineColor;
	}
	
	public void setLineAlpha(int alpha) {
		this.lineAlpha = alpha;
	}
	
	public void setFillColor(Color fillColor) {
		this.fillColor = fillColor;
	}
	
	public void setFillAlpha(int alpha) {
		this.fillAlpha = alpha;
	}
	
	
	public Style toStyle() {
		StyleFactory sf = CommonFactoryFinder.getStyleFactory();
		StyleBuilder sb = new StyleBuilder(sf);
		
		Stroke s = sb.createStroke(getLineColor(), getLineSize(), getLineAlpha() / 100.0);
		if (type == Attribute.AttributeType.LINE) {
			
			LineSymbolizer ls = sb.createLineSymbolizer(s);
			return sb.createStyle(ls);
		}
		
		Fill f = sb.createFill(getFillColor(), getFillAlpha() / 100.0);
		PolygonSymbolizer ps = sb.createPolygonSymbolizer(s, f);
		return sb.createStyle(ps);
	}
}
