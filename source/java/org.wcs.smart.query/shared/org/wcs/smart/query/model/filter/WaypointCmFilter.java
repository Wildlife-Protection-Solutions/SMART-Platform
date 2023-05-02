/*
 * Copyright (C) 2022 Wildlife Conservation Society
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

import org.wcs.smart.filter.IFilter;
import org.wcs.smart.filter.IFilterVisitor;
import org.wcs.smart.filter.Operator;
import org.wcs.smart.util.SharedUtils;

/**
 * A waypoint configurable model filter.
 * 
 * @author Emily
 * @since 7.5.7
 */
public class WaypointCmFilter implements IFilter {


	public static final String KEY = "waypoint:cm"; //$NON-NLS-1$
	
	/**
	 * Parses a waypoint cm filter 
	 * 
	 * @param value cm uuid or null for no cm  
	 * @return
	 */
	public static WaypointCmFilter createFilter( Object value){
		return new WaypointCmFilter(value);
	}
	
	private Object value= null;
	
	/**
	 * Creates a new filter
	 * @param op operator
	 * @param value filter value
	 */
	public WaypointCmFilter ( Object value){
		this.value = value;
	}
	
	/**
	 * @see org.wcs.smart.patrol.query.parser.filter.IFilter#asString()
	 */
	@Override
	public String asString(){
		if (value == null){
			return KEY;
		}else{
			return KEY + " " + Operator.EQUALS.asSmartValue() + " " + value;  //$NON-NLS-1$  //$NON-NLS-2$
		}
	}
	
	/**
	 * <p>Quotes have been removed</p>
	 * @return the filter value as a string
	 */
	public String getValue(){
		return  SharedUtils.stripQuotes((String)value) ;
	}
	
	/**
	 * The value to set the filter to (without quotes).
	 * @param value
	 */
	public void setValue(String value){
		this.value = "\"" + value + "\""; //$NON-NLS-1$ //$NON-NLS-2$
		
	}


	@Override
	public void accept(IFilterVisitor visitor) {
		visitor.visit(this);		
	}
}
