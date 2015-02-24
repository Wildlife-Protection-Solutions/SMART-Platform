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
package org.wcs.smart.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.eclipse.swt.graphics.Image;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.PolygonSymbolizer;
import org.geotools.styling.RasterSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;
import org.geotools.styling.Symbolizer;
import org.locationtech.udig.project.ILayer;
import org.locationtech.udig.style.advanced.editorpages.SimpleLineEditorPage;
import org.locationtech.udig.style.advanced.editorpages.SimplePointEditorPage;
import org.locationtech.udig.style.advanced.editorpages.SimplePolygonEditorPage;
import org.locationtech.udig.style.sld.SLD;
import org.locationtech.udig.style.sld.editor.EditorNode;
import org.locationtech.udig.style.sld.editor.EditorPageManager;
import org.locationtech.udig.ui.graphics.Glyph;
import org.locationtech.udig.ui.graphics.SLDs;
import org.opengis.coverage.grid.GridCoverage;
import org.wcs.smart.SmartPlugIn;

/**
 * Style utilities for map layers
 * 
 * @author Emily
 *
 */
public class MapStyleUtil {
	
	private final static Collection<String> EXCLUDED_STYLES = Arrays.asList("cache", "filter", "cache-gridcoverage", "org.locationtech.udig.style.advanced.editorpages.CoverageColorMaskStyleEditorPage");
	public static EditorPageManager createEditorPageManager(ILayer selectedLayer){
		EditorPageManager mgr = EditorPageManager.loadManager(SmartPlugIn.getDefault(), selectedLayer);
		
		List<EditorNode> toRemove = new ArrayList<EditorNode>();
		for (EditorNode en : mgr.getRootSubNodes()){
			if (EXCLUDED_STYLES.contains(en.getId())){
				toRemove.add(en);
			}
		}
		
		for (EditorNode en: toRemove){
			mgr.remove(en);
		}
		return mgr;
	}
	
	public static String findInitialStylePageId(ILayer selectedLayer) {
		String pageId = "simple";
		try {
			if (SLD.POINT.supports(selectedLayer)) {
				pageId = SimplePointEditorPage.ID;
			} else if (SLD.LINE.supports(selectedLayer)) {
				pageId = SimpleLineEditorPage.ID;
			} else if (SLD.POLYGON.supports(selectedLayer)) {
				pageId = SimplePolygonEditorPage.ID;
			} else if (selectedLayer.getGeoResource().canResolve(
					GridCoverage.class)) {
				pageId = "org.locationtech.udig.style.raster.SingleBandRasterPage";
			}
		} catch (Exception e) {
		}
		return pageId;
	}
	/**
	 * Converts and sld style to a glyph image
	 */
	public static Image createImage(Style sld) {
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

	private static Rule getRule(Style sld) {
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
