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
package org.wcs.smart.ui.map.location;

import org.eclipse.jface.viewers.LabelProvider;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.wcs.smart.IProjectionProvider;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ISmartPoint;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.ui.map.SmartMapEditorPart;
import org.wcs.smart.util.GeometryUtils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;

/**
 * LabelProvider for {@link ISmartPoint}
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class SmartPointLabelProvider extends LabelProvider {
	
	private IProjectionProvider crsProvider;
	private MathTransform transform;
	private CoordinateReferenceSystem transformCrs;
	
	public SmartPointLabelProvider(IProjectionProvider provider) {
		super();
		this.crsProvider = provider;
	}

	public SmartPointLabelProvider() {
		this(null);
	}
	
	@Override
	public String getText(Object element) {
		if (element instanceof ISmartPoint) {
			ISmartPoint smartPoint = (ISmartPoint) element;
			Point p = convert(smartPoint.getX(), smartPoint.getY());
			if (p != null) {
				return p.getX() + SmartMapEditorPart.COORDINATE_XYSEPARATOR + p.getY();
			}
			return Messages.SmartPointLabelProvider_ConversionFail_Label+ smartPoint.getX() + SmartMapEditorPart.COORDINATE_XYSEPARATOR + smartPoint.getY();
		}
		return super.getText(element);
	}
	
	/**
	 * Update the transform used to transform
	 * coordinates to map crs.  Should be called
	 * when map CRS is called.
	 * <p>Note: it is done this way to avoid deadlock issues
	 * I found in geotools if the transform was computed
	 * in the display thread.
	 * </p>
	 */
	public void updateTransform() {
		CoordinateReferenceSystem destCrs = crsProvider.getProjection().getParsedCoordinateReferenceSystem();
		if (destCrs == null) {
			transform = null;
			transformCrs = null;
		}
		if (transform == null || transformCrs != destCrs){
			try {
				transformCrs = destCrs;
				transform = CRS.findMathTransform(GeometryUtils.SMART_CRS, transformCrs);
			} catch (FactoryException e) {
				SmartPlugIn.log(e.getMessage(),e);
			}
		}
	}
	private Point convert(double x, double y) {
		Point point = GeometryFactoryProvider.getFactory().createPoint(new Coordinate(x, y));
		if (crsProvider == null) {
			return point; //no conversion required
		}

		try {
			CoordinateReferenceSystem destCrs = crsProvider.getProjection().getParsedCoordinateReferenceSystem();
			if (destCrs == null) {
				return point;
			}
			if (transform == null || transformCrs != destCrs){
				transformCrs = destCrs;
				transform = CRS.findMathTransform(GeometryUtils.SMART_CRS, transformCrs);
			}
			Point p = (Point) JTS.transform(point, transform);
			return p;
		} catch (Exception e) {
			return null;
		}
	}

}