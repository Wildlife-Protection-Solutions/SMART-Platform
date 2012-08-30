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
package org.wcs.smart.query.ui.gridded;

import java.util.LinkedList;
import java.util.List;

import net.refractions.udig.project.IMap;
import net.refractions.udig.project.internal.Map;
import net.refractions.udig.project.ui.ApplicationGIS;

import org.eclipse.core.runtime.IProgressMonitor;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.wcs.smart.query.model.GridResultItem;
import org.wcs.smart.query.model.QueryResultItem;

/**
 * 
 * 
 * @author Mauricio Pazos
 *
 */
public class MockQuery {

	/**
	 * This example was built from Jeff user story
	 * 
	 * @param mymonitor
	 * @return
	 */
	public static List<GridResultItem> getQueryResultsExample1(
			IProgressMonitor mymonitor) {

		List<GridResultItem> list = new LinkedList<GridResultItem>();
		
		GridResultItem r1 = new GridResultItem();
		r1.setTileX(1);
		r1.setTileY(1);
		r1.setValue(3.0);
		list.add(r1);
		
		GridResultItem r2 = new GridResultItem();
		r2.setTileX(1);
		r2.setTileY(2);
		r2.setValue(3.0);
		list.add(r2);
		
		GridResultItem r3 = new GridResultItem();
		r3.setTileX(1);
		r3.setTileY(3);
		r3.setValue(2.0);
		list.add(r3);
		
		GridResultItem r4 = new GridResultItem();
		r4.setTileX(3);
		r4.setTileY(3);
		r4.setValue(1.0);
		list.add(r4);
		
		return list;
	}
	
	/**
	 * Generates a values for all cells in the envelope WGS84.
	 * 
	 * @param mymonitor
	 * 
	 * @return
	 */
	public static List<GridResultItem> getQueryResultsExample2(
			IProgressMonitor mymonitor) {

		List<GridResultItem> list = new LinkedList<GridResultItem>();
		// minX=-180 maxX= 180 / minY=-90 maxY=83 for WGS84
		for(int x = -180; x <= 179; x++){
			for (int y = -90; y <= 83; y++) {
				
				GridResultItem r = new GridResultItem();
				
				r.setTileX(x);
				r.setTileY(y);
				double value = Math.abs(x) + Math.abs(y);
				r.setValue( value );
				list.add( r);
			}
		}
		return list;
	}
	
	/**
	 * Generate a test data set with its coordinates in the current map's bounds.
	 * 
	 * @return
	 */
	public static List<GridResultItem> getQueryResultsExample3(
			IProgressMonitor mymonitor) {

		List<GridResultItem> list = new LinkedList<GridResultItem>();
		IMap map = ApplicationGIS.getActiveMap();
		ReferencedEnvelope bounds = map.getBounds(null);
		
		for(long x = Math.round(bounds.getMinX()); x <= Math.round(bounds.getMaxX()); x++){
			for (long y = Math.round(bounds.getMinY()); y <= Math.round(bounds.getMaxY()); y++) {
				
				GridResultItem r = new GridResultItem();
				
				r.setTileX((int)x);
				r.setTileY((int)y);
				double value = Math.abs(Math.random());
				r.setValue( value );
				list.add( r);
			}
		}
		return list;
	}
	

}
