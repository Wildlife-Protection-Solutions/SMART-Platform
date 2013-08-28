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
package org.wcs.smart.query.parser.internal.summary;


/**
 * Interface for group by parts of summary queries.
 * @author egouge
 * @since 1.0.0
 */
public interface IGroupBy {
	
	/**
	 * @return converts the group by to the string
	 * representation
	 */
	public String asString();
	
	/**
	 * 
	 * The key part of a group by is 
	 * the part of the group by that uniquely identifies
	 * what is being grouped by but does not include
	 * which particular items should be included in
	 * the results.  For example for CategoryGoupBys
	 * <  CATEGORY_GROUP_BY : "category:" < LEVEL > ":" ( < DM_KEY > ":")* >
	 * the key is the <  CATEGORY_GROUP_BY : "category:" < LEVEL> >.
	 * 
	 * 
	 * @return only the key part of the 
	 * group by
	 */
	public String getKeyPart();

		
}
