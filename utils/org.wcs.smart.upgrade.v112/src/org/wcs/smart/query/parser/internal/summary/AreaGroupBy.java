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
 * Group by for area option
 * @author Emily
 *
 */
public class AreaGroupBy implements IGroupBy {

	/**
	 * Creates a new category group by of the form:
	 *  <  AREA_GROUPBY_ITEM : "area:" < AREA_TYPE_KEY > ":" ( < DM_KEY > )? (":" < DM_KEY > )* >
	 *  < AREA_TYPE_KEY : ( "CA" | "BA" | "ADMIN" | "MNGT" | "PATRL" ) >
	 * 
	 * 
	 * @param key
	 * @return
	 */
	public final static AreaGroupBy createGroupBy(String key){
		return new AreaGroupBy(key);
	}
	
	private String key;
	
	/**
	 * @param key
	 */
	protected AreaGroupBy(String key){
		this.key = key;
		
	}
	
	
	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IGroupBy#getKeyPart()
	 */
	public String getKeyPart(){
		return key;
	}
	
	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IGroupBy#asString()
	 */
	@Override
	public String asString() {
		return key;
	}

	
}
