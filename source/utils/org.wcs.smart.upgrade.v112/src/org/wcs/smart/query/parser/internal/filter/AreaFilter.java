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
package org.wcs.smart.query.parser.internal.filter;

import java.util.List;

import org.wcs.smart.query.parser.filter.IFilter;


/**
 * Area filter for waypoint queries.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class AreaFilter implements IFilter {

	/**
	 * Type of geometry the area filter
	 * should be applied to. 
	 */
	public enum AreaFilterGeometryType{
		WAYPOINT("wp"), TRACK("t");  //$NON-NLS-1$  //$NON-NLS-2$
		
		String key;
		AreaFilterGeometryType(String key){
			this.key = key;
		}
		public String getKey(){
			return this.key;
		}
	}
	
	/**
	 * Creates a new area filter.
	 * 
	 * @param key the filter key in the form area:<geomtype{wp | t}>:<type>:key
	 * where type is from Area.AreaType
	 * @return
	 */
	public static AreaFilter createFilter(String key){
		
		return new AreaFilter(key);
	}
	
	private String key;
	
	/**
	 * Creates a new area filter
	 * @param type the area type
	 * @param key the key id
	 */
	public AreaFilter(String key){
		this.key = key;
	}
	/**
	 * @see org.wcs.smart.query.parser.filter.IFilter#asString()
	 */
	@Override
	public String asString() {
		return key;
	}

	

	/**
	 * @see org.wcs.smart.query.parser.filter.IFilter#getChildren()
	 */
	@Override
	public List<IFilter> getChildren() {
		return null;
	}

}
