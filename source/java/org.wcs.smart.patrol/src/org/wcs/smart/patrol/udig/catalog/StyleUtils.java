/*
 * Copyright (C) 2021 Wildlife Conservation Society
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
package org.wcs.smart.patrol.udig.catalog;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.ui.WorkbenchException;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Fill;
import org.geotools.styling.Graphic;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.Mark;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.Stroke;
import org.geotools.styling.Style;
import org.geotools.styling.StyleBuilder;
import org.geotools.styling.StyleFactory;
import org.hibernate.Session;
import org.locationtech.udig.project.IStyleBlackboard;
import org.locationtech.udig.project.internal.ProjectFactory;
import org.locationtech.udig.style.sld.SLDContent;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.FilterFactory;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolTransportType;

/**
 * Style utilities for patrol plugin
 * 
 * @author Emily
 *
 */
public enum StyleUtils {

	INSTANCE;
	
	public Style getPointSelectionStyle(SimpleFeatureType type) throws WorkbenchException, IOException {
		StyleFactory sf = CommonFactoryFinder.getStyleFactory();
		StyleBuilder sb = new StyleBuilder(sf);

		Style style = sf.createStyle();
		
		FeatureTypeStyle fts = sf.createFeatureTypeStyle();
		style.featureTypeStyles().add(fts);
		fts.featureTypeNames().add(type.getName());
		
		Stroke starstroke = sb.createStroke(new java.awt.Color(170,170,0), 1);
		Fill starfill = sb.createFill(new java.awt.Color(255,255,170));
		Mark starmark = sb.createMark(sb.literalExpression("circle"), starfill, starstroke); //$NON-NLS-1$
		Graphic starg = sb.createGraphic(null,  starmark,  null);
		starg.setSize(sb.literalExpression(12));

		PointSymbolizer endpoint = sb.createPointSymbolizer(starg);

		Rule r= sf.createRule();
		r.symbolizers().add(endpoint);
		r.setName("Point Selection Style"); //$NON-NLS-1$
		fts.rules().add(r);

		return style;
	}
	
	/**
	 * Styles each track by patrol type. Requires transport_type_key on attribute
	 *  
	 * @param patrol
	 * @return
	 * @throws WorkbenchException
	 * @throws IOException
	 */
	public IStyleBlackboard getPatrolTrackStyle(Patrol patrol) throws WorkbenchException, IOException {
		
		Set<PatrolTransportType> types = new HashSet<>();
		
		try(Session session = HibernateManager.openSession()){

			Patrol p = session.get(Patrol.class, patrol.getUuid());
			for (PatrolLeg pl : p.getLegs()) {
				types.add(pl.getType());
			}
			 
		}
		
		StyleFactory sf = CommonFactoryFinder.getStyleFactory();
		FilterFactory ff = CommonFactoryFinder.getFilterFactory();

		Style style = sf.createStyle();
		FeatureTypeStyle fts = sf.createFeatureTypeStyle();
		style.featureTypeStyles().add(fts);
		
		
		String[] colors = new String[]{"#e31a1c","#1f78b4","#33a02c","#ff7f00","#6a3d9a","#b15928","#a6cee3","#b2df8a","#fb9a99","#fdbf6f","#cab2d6","#ffff99"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$ //$NON-NLS-11$ //$NON-NLS-12$
//		String[] colors = new String[]{"#ff0000","#0000ff","#00ff00","#00ffff","#FF00FF","#FFFF00","#a6cee3","#b2df8a","#fb9a99","#fdbf6f","#cab2d6","#ffff99"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$ //$NON-NLS-11$ //$NON-NLS-12$
		int cnt = 0;
		for (PatrolTransportType type : types) {
			String color = colors[cnt++ % colors.length];
			LineSymbolizer ls = sf.createLineSymbolizer();
			ls.setStroke(sf.createStroke(ff.literal(color), ff.literal(1)));
			
			Rule r= sf.createRule();
			r.setFilter(ff.equal(ff.property("transport_type_key"), ff.literal(type.getKeyId()), false)); //$NON-NLS-1$
			r.setName(type.getName());
			r.symbolizers().add(ls);
			fts.rules().add(r);
		}

		IStyleBlackboard sb = ProjectFactory.eINSTANCE.createStyleBlackboard();
		sb.put(SLDContent.ID, style);
		
		return sb;
	}
}
