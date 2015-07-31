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
package org.wcs.smart.query.model.filter;

/**
 * A query filter.
 * @author Emily
 * @since 1.0.0
 */
public interface IFilter {

	public enum FilterType{
		OBSERVATION("observation"),  //$NON-NLS-1$
		WAYPOINT("waypoint"); //$NON-NLS-1$
	
		private String key;
		
		FilterType(String key){
			this.key = key;
		}
		public String getKey(){
			return this.key;
		}		
		public static FilterType parse(String type){
			if (type.equals(WAYPOINT.key)){
				return WAYPOINT;
			}
			return OBSERVATION;
		}
	};
	

	
	/**
	 * @return the string representation of the filter
	 */
	public String asString();
	
	
	/**
	 * runs the visitor on any children filters
	 * then on the filter implementation
	 * 
	 * @param visitor
	 */
	public void accept(IFilterVisitor visitor);
	
	
}
