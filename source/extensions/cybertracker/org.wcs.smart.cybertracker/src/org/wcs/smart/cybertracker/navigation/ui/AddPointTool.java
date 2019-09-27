/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.cybertracker.navigation.ui;

import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.udig.project.internal.render.ViewportModel;
import org.locationtech.udig.project.ui.internal.MapPart;
import org.locationtech.udig.project.ui.render.displayAdapter.MapMouseEvent;
import org.locationtech.udig.project.ui.render.displayAdapter.ViewportPane;
import org.locationtech.udig.project.ui.tool.AbstractModalTool;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.map.GeometryFactoryProvider;
import org.wcs.smart.util.GeometryUtils;

/**
 * Map tool for adding new point navigation target. Works in conjunction with
 * ITargeEditor on the map blackboard
 * 
 * @author Emily
 *
 */
public class AddPointTool extends AbstractModalTool {
	
	public static final String ID = "org.wcs.smart.ui.map.navigation.addpoint"; //$NON-NLS-1$

	@Override
	public void mousePressed(MapMouseEvent e) {
		if ((e.button & MapMouseEvent.BUTTON1) != MapMouseEvent.BUTTON1) return;
		super.mousePressed(e);
		
		MapPart mapEditor = ((ViewportPane) e.source).getMapEditor();
		ViewportModel viewportModel = mapEditor.getMap().getViewportModelInternal();
		Coordinate c = viewportModel.pixelToWorld(e.x, e.y);
		if (viewportModel.isSetCRS()) {
			try {
				CoordinateReferenceSystem sourceCrs = viewportModel.getCRS();
				org.locationtech.jts.geom.Point point = GeometryFactoryProvider.getFactory().createPoint(new Coordinate(c.x, c.y));
				org.locationtech.jts.geom.Point p = (org.locationtech.jts.geom.Point) JTS.transform(point, CRS.findMathTransform(sourceCrs, GeometryUtils.SMART_CRS));
				c = new Coordinate(p.getX(), p.getY());
			} catch (Exception exception) {
				SmartPlugIn.displayLog("Error while selecting a point. Point conversion failed", exception); //$NON-NLS-1$
			}
		}
		Object x = mapEditor.getMap().getBlackboard().get(ITargetEditor.ID);
		if (x != null) {
			((ITargetEditor)x).addPointTarget(c);
		}
	}
}
