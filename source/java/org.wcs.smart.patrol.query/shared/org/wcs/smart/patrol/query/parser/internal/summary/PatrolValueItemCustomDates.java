/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.patrol.query.parser.internal.summary;

/**
 * ValueItem for patrol value that counts the number of times a patrol overlaps
 * the user specified time range.
 * 
 * @author egouge
 * @since 7.0.0
 */
public class PatrolValueItemCustomDates extends PatrolValueItem {

	protected int startTime;
	protected int endTime;
	
	/**
	 * Creates a patrol value item from a key of the form
	 * patrol:<aggregation>:patrolkey:starttime:endtime
	 * 
	 * The :nodata is optional. If supplied then we exclude 
	 * days with no data from the value computation.  Nodata only
	 * applies to some patrol value options
	 * 
	 * @param part
	 */
	public PatrolValueItemCustomDates(String key){
		super(key);
		
		String[] bits = key.split(":"); //$NON-NLS-1$
		startTime = Integer.parseInt(bits[3]);
		endTime = Integer.parseInt(bits[4]);
	}
	
	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IValueItem#asString()
	 */
	public String asString(){
		String key = super.asString();
		key += ":" + startTime  + ":" + endTime;  //$NON-NLS-1$//$NON-NLS-2$
		return key;
	}
	
	/**
	 * The user specified start time in seconds
	 * @return
	 */
	public int getStartTime() {
		return this.startTime;
	}
	
	/**
	 * The user specified end time in seconds
	 * @return
	 */
	public int getEndTime() {
		return this.endTime;
	}
	
}
