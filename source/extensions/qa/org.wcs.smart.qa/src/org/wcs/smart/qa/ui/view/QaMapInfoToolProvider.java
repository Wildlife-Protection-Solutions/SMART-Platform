/*
 * Copyright (C) 2017 Wildlife Conservation Society
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
package org.wcs.smart.qa.ui.view;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import org.locationtech.udig.project.internal.Map;
import org.locationtech.udig.project.render.IViewportModel;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.map.GeometryFactoryProvider;
import org.wcs.smart.qa.QaPlugIn;
import org.wcs.smart.qa.model.QaError;
import org.wcs.smart.ui.map.tool.IInfoToolProvider;
import org.wcs.smart.util.ReprojectUtils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.operation.distance.DistanceOp;

public class QaMapInfoToolProvider implements IInfoToolProvider {

	private TableMapQaErrorComposite editor;
	
	public QaMapInfoToolProvider(TableMapQaErrorComposite editor){
		this.editor = editor;
	}
	
	public void selectFeatures(Point start, Point end){
		Map map = editor.getMap();
		
		Coordinate c1 = map.getViewportModel().pixelToWorld(start.x, start.y);
		Coordinate c2 = map.getViewportModel().pixelToWorld(end.x, end.y);

		CoordinateReferenceSystem crs = map.getViewportModel().getCRS();
		
		try{
			c1 = ReprojectUtils.reproject(c1.x, c1.y, crs, SmartDB.DATABASE_CRS);
			c2 = ReprojectUtils.reproject(c2.x, c2.y, crs, SmartDB.DATABASE_CRS);
		}catch (Exception ex){
			QaPlugIn.log(ex.getMessage(), ex);
			return;
		}
		
		Envelope env = new Envelope(c1, c2);
		Polygon pEnv = GeometryFactoryProvider.getFactory().createPolygon(new Coordinate[]{c1, new Coordinate(c1.x, c2.y), c2, new Coordinate(c2.x, c1.y), c1});
		List<QaError> toSelect = new ArrayList<>();
		for (QaError result : editor.getResults()){
			if (result.getGeometryObject() != null){
				if (env.intersects(result.getGeometryObject().getEnvelopeInternal()) && result.getGeometryObject().intersects(pEnv)){
					toSelect.add(result);
				}
			}
		}
		editor.setSelection(toSelect);
	}
				
	
	@Override
	public InfoPoint findFeature(int x, int y, IViewportModel vm) {
		if (editor.getResults() == null) return null;
		
		Coordinate world = vm.pixelToWorld(x, y);
		Coordinate db = null;
		try {
			db = ReprojectUtils.reproject(world.x, world.y, vm.getCRS(), SmartDB.DATABASE_CRS);
		} catch (Exception e) {
			QaPlugIn.log(e.getMessage(), e);
			return null;
		}
		QaError nearest = null;
		com.vividsolutions.jts.geom.Point toTest = GeometryFactoryProvider.getFactory().createPoint(db);
		double distance = Double.MAX_VALUE;
		
		for (QaError result : editor.getResults()){
			if (result.getGeometryObject() != null){
				if ( (result.getGeometryObject() instanceof com.vividsolutions.jts.geom.Point) ||
						result.getGeometryObject().getEnvelopeInternal().contains(db)){
					double d = result.getGeometryObject().distance(toTest);
					if (d < distance){
						distance = d;
						nearest = result;
					}
				}
			}
		}
		
		if (nearest == null) return null;
		Geometry g = nearest.getGeometryObject();
		Coordinate[] c = DistanceOp.nearestPoints(g, toTest);
		if (c.length == 0) return null;

		Coordinate px;
		try {
			px = ReprojectUtils.reproject(c[0].x, c[0].y, SmartDB.DATABASE_CRS, vm.getCRS());
		} catch (Exception e) {
			QaPlugIn.log(e.getMessage(), e);
			return null;
		}
		Point pnt = vm.worldToPixel(px);
		if (pnt.distance(x, y) > 5) return null;
		
		StringBuilder sb = new StringBuilder();
		sb.append(nearest.getErrorId());
		
		editor.getMap().getBlackboard().put(QaFixTool.HOVER_ID, nearest);
		return new InfoPoint(pnt, nearest, sb.toString());
	}
	
	
}
