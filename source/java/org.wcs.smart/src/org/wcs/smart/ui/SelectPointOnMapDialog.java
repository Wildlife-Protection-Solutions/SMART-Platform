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

package org.wcs.smart.ui;

import java.awt.Color;
import java.util.Collections;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Graphic;
import org.geotools.styling.Mark;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.Style;
import org.geotools.styling.StyleBuilder;
import org.wcs.smart.ca.ISmartPoint;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.ui.map.location.LocationSelectComposite;

/**
 * Map dialog for selecting points on map
 * 
 * @author Emily
 *
 */
public class SelectPointOnMapDialog extends SmartStyledDialog{

	private TmpPoint point;
	private LocationSelectComposite<TmpPoint> locationComp;
	private TmpPoint initPoint = null;
	
	public SelectPointOnMapDialog(Shell parentShell) {
		super(parentShell);
	}
	

	/**
	 * Sets the initial location in lat/long
	 * @param x
	 * @param y
	 */
	public void setInitPoint(double x, double y){
		initPoint = new TmpPoint();
		initPoint.setX(x);
		initPoint.setY(y);
	}
	
	@Override
	protected void okPressed(){
		point = null;
		List<TmpPoint> pnts = locationComp.getPoints();
		if (pnts.size() > 0){
			point = pnts.get(0);
		}
		super.okPressed();
	}
	
	public ISmartPoint getPoint(){
		return point;
	}
	
	@Override
	protected Point getInitialSize() {
		return new Point(450, 400);
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		parent = (Composite) super.createDialogArea(parent);
		
		locationComp = new LocationSelectComposite<TmpPoint>(parent, SWT.SINGLE, getLayerStyle()) {
			@Override
			protected TmpPoint createNewPoint() {
				return new TmpPoint();
			}
		};

		if (initPoint != null){
			locationComp.setPoints(Collections.singletonList(initPoint));
		}
		getShell().setText(Messages.SelectPointOnMapDialog_SelectMapPointdialogTitle);
		return parent;
	}
	
	
	@Override
	public boolean isResizable(){
		return true;
	}
	
	private Style getLayerStyle() {
		
		StyleBuilder sb = new StyleBuilder();
		
		Mark mark = sb.createMark("star",  new Color(255, 0, 0), new Color(255, 0, 0), 1);
		
		Graphic graph2 = sb.createGraphic(null, mark, null, 1, 10, 0);
        PointSymbolizer symb = sb.createPointSymbolizer(graph2);
        
        FeatureTypeStyle fs = sb.createFeatureTypeStyle(symb);
        Style s = sb.createStyle();
        s.featureTypeStyles().add(fs);
        return s;
	}

	class TmpPoint implements ISmartPoint{

		private double x;
		private double y;
		
		@Override
		public double getX() {
			return x;
		}

		@Override
		public void setX(double x) {
			this.x = x;
		}

		@Override
		public double getY() {
			return this.y;
		}

		@Override
		public void setY(double y) {
			this.y = y;
		}
		
	}

}
