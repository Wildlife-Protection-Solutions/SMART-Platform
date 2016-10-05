/*
 * Copyright (C) 2016 Wildlife Conservation Society
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

package org.wcs.smart.i2.model;


/**
 * Interface for working set map layers which include a style and visibility
 * property.
 * 
 * @author Emily
 *
 */
public interface IWorkingSetMapLayer {

	/**
	 * 
	 * See StyleManager.INSTANCE for parsing
	 * @return the map style string
	 */
	public String getMapStyle();
	
	/**
	 * Sets the map style string.  See StyleManager.INSTANCE for configuring
	 * @param style
	 */
	public void setMapStyle(String style);
	
	/**
	 * 
	 * @return the layer visibility
	 */
	public boolean getIsVisible();
	
	/**
	 * Sets the layer visibility
	 * 
	 * @param isVisible
	 */
	public void setIsVisible(boolean isVisible);
}
