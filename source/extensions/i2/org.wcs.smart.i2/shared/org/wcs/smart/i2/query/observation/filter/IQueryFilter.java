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
package org.wcs.smart.i2.query.observation.filter;

/**
 * Interface for query filters.
 * 
 * @author Emily
 *
 */
public interface IQueryFilter {

	public static final String ANY_OPTION_KEY = "list.any"; //$NON-NLS-1$
	
	public static String DATE_FORMAT_STR = "yyyy-MM-dd"; //$NON-NLS-1$
	
	public enum FilterType{
		OBSERVATION("observation"), //$NON-NLS-1$
		WAYPOINT("waypoint"); //$NON-NLS-1$
		
		String key;
		FilterType(String key){
			this.key = key;	
		}
		
		public String getKey(){
			return this.key;
		}
		
		public static FilterType parse(String key){
			for (FilterType t : FilterType.values()){
				if (t.key.equalsIgnoreCase(key)) return t;
			}
			throw new IllegalStateException(key + " invalid filter type"); //$NON-NLS-1$
		}
	}
	
	default public void accept(IFilterVisitor visitor){
		visitor.visitElement(this);
	}
}
