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
package org.wcs.smart.i2.search.attachment;

import org.wcs.smart.common.attachment.ISmartAttachment;

/**
 * Search result
 * 
 * @author Emily
 *
 */
public class SearchResult {
	
	private ISmartAttachment attachment;
	private String matchString;
	private String fullString;
	
	private int start;
	private int end;
	
	/**
	 * 
	 * @param attachment the attachment that was searched
	 * @param matchString the string that was being searched for
	 * @param fullString a subset of the attachment file that include the match string to display to the user
	 * @param start the location of the matchString in the fullString
	 * @param end the location of the matchString in the fullString
	 */
	public SearchResult(ISmartAttachment attachment, String matchString, String fullString, int start, int end){
		this.attachment = attachment;
		this.matchString = matchString;
		this.fullString = fullString;
		this.start = start;
		this.end = end;
	}
	
	public int getStart(){
		return this.start;
	}
	
	public int getEnd(){
		return this.end;
	}
	
	public ISmartAttachment getAttachment(){
		return this.attachment;
	}
	
	public String getFullString(){
		return this.fullString;
	}
	
	public String getMatchString(){
		return this.matchString;
	}
}
