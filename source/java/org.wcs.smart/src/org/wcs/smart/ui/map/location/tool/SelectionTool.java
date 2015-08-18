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
package org.wcs.smart.ui.map.location.tool;

import java.util.ArrayList;
import java.util.List;

import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.udig.project.internal.render.ViewportModel;
import org.locationtech.udig.project.ui.internal.MapPart;
import org.locationtech.udig.project.ui.render.displayAdapter.MapMouseEvent;
import org.locationtech.udig.project.ui.render.displayAdapter.ViewportPane;
import org.locationtech.udig.project.ui.tool.AbstractModalTool;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ui.map.location.GeometryFactoryProvider;
import org.wcs.smart.util.GeometryUtils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;

/**
 * Tool used to select points on a map
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class SelectionTool extends AbstractModalTool {

	public static final String ID = "org.wcs.smart.ui.map.location.tool.SelectionTool"; //$NON-NLS-1$
	
	private List<IMapPointSelectionListener> listeners = new ArrayList<IMapPointSelectionListener>();

	public SelectionTool() {
		super();
	}

	@Override
	public void mousePressed(MapMouseEvent e) {
		super.mousePressed(e);
		MapPart mapEditor = ((ViewportPane) e.source).getMapEditor();
		ViewportModel viewportModel = mapEditor.getMap().getViewportModelInternal();
		Coordinate c = viewportModel.pixelToWorld(e.x, e.y);
		if (viewportModel.isSetCRS()) {
			try {
				CoordinateReferenceSystem sourceCrs = viewportModel.getCRS();
				Point point = GeometryFactoryProvider.getFactory().createPoint(new Coordinate(c.x, c.y));
				Point p = (Point) JTS.transform(point, CRS.findMathTransform(sourceCrs, GeometryUtils.SMART_CRS));
				fireListeners(p.getX(), p.getY());
			} catch (Exception exception) {
				SmartPlugIn.displayLog("Error while selecting a point. Point conversion failed", exception); //$NON-NLS-1$
			}
		} else {
			//assume that we are in lat long coordinate system
			fireListeners(c.x, c.y);
		}
	}
	
	private void fireListeners(double x, double y) {
		for (IMapPointSelectionListener listener : listeners) {
			listener.pointSelected(x, y);
		}
	}
	
	public void addListener(IMapPointSelectionListener listener) {
		listeners.add(listener);
	}

	public void removeListener(IMapPointSelectionListener listener) {
		listeners.remove(listener);
	}
	
}
