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
 * A patrol group by option
 * @author egouge
 * @since 1.0.0
 */
public class PatrolGroupBy implements IGroupBy {

	/**
	 * Creates a new patrol group by option
	 * @param key patrol group by key of the form
	 * "patrol:<key>:<uniqueid>,<uniqueid>,<uniqueid>"
	 * 
	 * Where <key> is one of the PatrolQueryOption and uniqueid
	 * is a hex encoded uuid or unique string value.
	 * 
	 * @return
	 */
	public static final PatrolGroupBy createGroupBy(String key){
		return new PatrolGroupBy(key);
	}
	
	private String key;

	/**
	 * Creates a new patrol group by option
	 * @param key patrol group by item key of the format "patrol:key:<uuid>:<uuid>..."
	 */
	public PatrolGroupBy(String key){
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
