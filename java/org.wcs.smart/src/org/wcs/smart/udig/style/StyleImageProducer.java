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
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.PolygonSymbolizer;
import org.geotools.styling.RasterSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;
import org.geotools.styling.Symbolizer;
import org.locationtech.udig.ui.graphics.Glyph;
import org.locationtech.udig.ui.graphics.SLDs;

/**
 * Converts a style to a glyph icon.
 * @author Emily
 *
 */
public enum StyleImageProducer {
	
	INSTANCE;
	
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
			return Glyph.point(r).createImage();
		} else if (LineSymbolizer.class.isAssignableFrom(sym.getClass())) {
			return Glyph.line(r).createImage();
		} else if (PolygonSymbolizer.class.isAssignableFrom(sym.getClass())) {
			return Glyph.polygon(r).createImage();
		} else if (RasterSymbolizer.class.isAssignableFrom(sym.getClass())) {
			return Glyph.grid(null, null, null, null).createImage();
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
