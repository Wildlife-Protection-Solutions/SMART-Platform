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
package org.wcs.smart.query.model;

/**
 * A class for tracking grid results.
 * 
 * @author egouge
 *
 */
public class GridResultItem implements IResultItem{


	private double value;
	private int tileX;
	private int tileY;
	
	/**
	 * the gird value
	 * @param value
	 */
	public void setValue(double value) {
		this.value = value;
	}

	/**
	 * 
	 * @param x x tile id starting at 1 in the lower left
	 */
	public void setTileX(int x) {
		this.tileX = x;
		
	}
	/**
	 * 
	 * @param y y tile id starting at 1 in the lower left
	 */
	public void setTileY(int y) {
		this.tileY = y;
		
	}
	
	/**
	 * 
	 * @return get cell value
	 */
	public double getValue() {
		return value;
	}


	/**
	 * 
	 * @return x tile id
	 */
	public int getTileX() {
		return tileX;
	}
	
	/**
	 * 
	 * @return y tile id
	 */
	public int getTileY() {
		return tileY;
	}
		
}
