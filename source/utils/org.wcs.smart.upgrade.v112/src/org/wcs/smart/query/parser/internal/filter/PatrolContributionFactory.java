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
import org.wcs.smart.query.parser.filter.Operator;

/**
 * Factory for providing all patrol contribution related information added via extension point.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class PatrolContributionFactory {
		
	/**
	 * Creates a patrol filter
	 * 
	 * @param key patrol filter key
	 * @param op patrol filter operator 
	 * @param value patrol filter value
	 * @return
	 */
	public static IFilter createStringFilter(final String key, 
			final Operator op, final Object value) {
		return new IFilter() {
			
			@Override
			public List<IFilter> getChildren() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public String asString() {
				return key + op.asSmartValue() + (String)value;
			}
		};
	}
	
	/**
	 * Creates a patrol filter for a boolean patrol filter option
	 * 
	 * @param key the patrol key 
	 * @return
	 */
	public static IFilter createBooleanFilter(final String key){
		return new IFilter() {
			
			@Override
			public List<IFilter> getChildren() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public String asString() {
				return key;
			}
		};
	}
	

}
