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
package org.wcs.smart.i2.search;

import java.util.List;

/**
 * Intelligence record search result package. Contains a list of top rated items
 * and a number of total matched. 
 * 
 * @author Emily
 *
 */
public class IntelRecordResult {

	private long totalMatched;
	private List<IntelRecordSearchResultItem> results;
	private long totalTime;
	
	
	/**
	 * 
	 * @param totalCnt
	 * @param results 
	 * @param totalTime how long search took in nano seconds 
	 */
	public IntelRecordResult(long totalCnt, List<IntelRecordSearchResultItem> results, long totalTime){
		this.totalMatched = totalCnt;
		this.results = results;
		this.totalTime = totalTime;
	}
	
	/**
	 * The total number of items matched
	 * @return
	 */
	public long getTotalMatched(){
		return this.totalMatched;
	}
	
	/**
	 * The top matches.  The maximum number of items
	 * return here is controlled by IIntelEntitySearch.MAX_RESULT_CNT
	 * @return
	 */
	public List<IntelRecordSearchResultItem> getResults(){
		return this.results;
	}
	
	/**
	 * How long the search took in nanoseconds
	 * @return
	 */
	public long getTotalTime(){
		return this.totalTime;
	}
	
	
}
