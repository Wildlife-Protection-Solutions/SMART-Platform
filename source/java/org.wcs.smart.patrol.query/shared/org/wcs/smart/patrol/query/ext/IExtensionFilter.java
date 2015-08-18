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
package org.wcs.smart.patrol.query.ext;

import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.model.filter.Operator;
/**
 * Filter wrapper for extension items.
 * 
 * Must have an empty constructor
 * 
 * @author Emily
 *
 */
public interface IExtensionFilter extends IFilter {
	/**
	 * Creates a patrol filter for a boolean patrol filter option.
	 * 
	 * If the key is not valid or this function is not supported
	 * for this filter then this must
	 * return null.
	 * 
	 * @param key the patrol key 
	 * @return
	 */
	public IFilter createFilter(String key);

	/**
	 * Creates a filter.
	 * 
	 * If the key is not valid or this function is not supported
	 * for this filter then this must
	 * return null.
	 * 
	 * @param key patrol filter key
	 * @param op patrol filter operator 
	 * @param value patrol filter value
	 * @return
	 */
	public IFilter createFilter(String key, Operator op, Object value);
}
