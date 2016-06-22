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
package org.wcs.smart.query.common.ui;

import org.wcs.smart.IProjectionProvider;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.util.ReprojectUtils;

import com.vividsolutions.jts.geom.Point;

/**
 * Label provider for query columns that need to be reprojected before displayed.
 * 
 * @author Emily
 *
 */
public class ReprojectingQueryColumnLabelProvder extends QueryColumnLabelProvider{

	private QueryColumn x;
	private QueryColumn y;
	private IProjectionProvider prj;
	
	public ReprojectingQueryColumnLabelProvder(QueryColumn col, QueryColumn x, QueryColumn y, IProjectionProvider prj){
		super(col);
		this.x = x;
		this.y = y;
		this.prj = prj;
	}

	public Projection getProjection(){
		return this.prj.getProjection();
	}
	
	@Override
	public String getText(Object element){
		if (element instanceof IResultItem){
			IResultItem result = (IResultItem)element;
			Double x = (Double) this.x.getValue(result);
			Double y = (Double) this.y.getValue(result);
			
			Point pnt = ReprojectUtils.transform(x, y, getProjection().getParsedCoordinateReferenceSystem());
			if (col == this.x){
				return String.valueOf(pnt.getX());
			}else if (col == this.y){
				return String.valueOf(pnt.getY());
			}
			return "Error - reprojecting column is not the x or y column"; //$NON-NLS-1$
		}
		return element.toString();
	}
}