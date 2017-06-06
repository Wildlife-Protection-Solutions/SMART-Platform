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
package org.wcs.smart.udig;

import java.awt.Point;

import org.locationtech.udig.project.render.IViewportModel;

/**
 * Edit tool manager for Edit Point Tool and Undo Tool.
 * 
 * @author Emily
 *
 */
public interface IMapEditManager{
	
	/**
	 * find the point feature to move; return null if no point to remove
	 * in this area
	 * 
	 * @param x the screen x coordinate to search
	 * @param y the screen y coordinate to search
	 * @return
	 */
	public EditPoint findFeature(int x, int y, IViewportModel vm);
	
	/**
	 * Move the feature to the new location.  Location 
	 * is provided in screen coordinates.
	 * 
	 * @param feature
	 * @param x
	 * @param y
	 */
	public void moveFeature(Object feature, int x, int y, IViewportModel vm);
	
	/**
	 * Undo the last command
	 */
	public void undo();
	
	/**
	 * 
	 * @return true if these is something to undo, false otherwise
	 */
	public boolean canUndo();
	
	
	/**
	 * Class used by edit manager to determine details about the feature object
	 * being editted.
	 * 
	 * @author Emily
	 *
	 */
	public class EditPoint{
		
		private Object feature;
		private Point mapPoint;
		private String infoString;
		
		/**
		 * Creates a new edit point.
		 * @param mapPoint the geometry of the point in map coordinates
		 * @param feature the feature to edit
		 */
		public EditPoint(Point mapPoint, Object feature){
			this(mapPoint, feature, null);
		}
		
		/**
		 * 
		 * @param mapPoint the geometry of the point in map coordinates
		 * @param feature the feature to edit
		 * @param infoString an info to string to display on hover (for multi-line use \n seperator)
		 */
		public EditPoint(Point mapPoint, Object feature, String infoString){
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