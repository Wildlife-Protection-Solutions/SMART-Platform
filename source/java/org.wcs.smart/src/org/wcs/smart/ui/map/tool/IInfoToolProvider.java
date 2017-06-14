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
package org.wcs.smart.ui.map.tool;

import java.awt.Point;

import org.locationtech.udig.project.render.IViewportModel;

/**
 * Class you can add to a map blackboard to provide hover 
 * details about a feature on the map.  Used in conjunction with
 * the BBoxInfoTool.
 * 
 * @author Emily
 *
 */
public interface IInfoToolProvider {

	/**
	 * Key to use to place provider on blackboard
	 */
	public static final String BLACKBOARD_KEY = IInfoToolProvider.class.getCanonicalName();
	
	/**
	 * Find the feature at the given (x,y) screen coordinate
	 * 
	 * @param x
	 * @param y
	 * @param vm
	 * 
	 * @return
	 */
	public InfoPoint findFeature(int x, int y, IViewportModel vm);
	
	
	/**
	 * Class used by edit manager to determine details about the feature object
	 * being editted.
	 * 
	 * @author Emily
	 *
	 */
	public class InfoPoint{
		
		private Object feature;
		private Point mapPoint;
		private String infoString;
		
		/**
		 * 
		 * @param mapPoint the geometry of the point in map coordinates
		 * @param feature the feature to edit
		 * @param infoString an info to string to display on hover (for multi-line use \n seperator)
		 */
		public InfoPoint(Point mapPoint, Object feature, String infoString){
			this.feature = feature;
			this.mapPoint = mapPoint;
			this.infoString = infoString;
		}
		
		public String getInfoString(){
			return infoString;
		}
		public Object getFeature(){
			return this.feature;
		}
		
		public Point getMapPoint(){
			return this.mapPoint;
		}
	}
}
