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
package org.wcs.smart.query.ui.queryfilter;


/**
 * A wrapper for a Category, ATtribute or CategoryAttribute
 * object that is used for wrapping items in the summary tree
 * so we can tell if they should be value items or group by items
 * 
 * @author egouge
 * @since 1.0.0
 */
public class SummaryDmObject {

	private Object dmObject;
	private Object object2;
	private boolean isValue;
	
	/**
	 * 
	 * @param object the underlying object to wrap
	 * @param isValue <code>true</code> if associated with summary values; <code>false</code> if associated with group bys
	 */
	public SummaryDmObject(Object object, boolean isValue){
		this.dmObject = object;
		this.isValue = isValue;
	}
	
	/**
	 * 
	 * @param object the underlying object to wrap
	 * @param object2 a second required object
	 * @param isValue <code>true</code> if associated with summary values; <code>false</code> if associated with group bys
	 */
	public SummaryDmObject(Object object1, Object object2, boolean isValue){
		this(object1, isValue);
		this.object2 = object2;
	}
	
	/**
	 * @return <code>true</code> if the item is from the value part of the tree
	 */
	public boolean isValue(){
		return this.isValue;
	}
	
	/**
	 * @return underlying object 
	 */
	public Object getObject(){
		return this.dmObject;
	}
	
	/**
	 * @return the second object provided
	 */
	public Object getObject2(){
		return this.object2;
	}
}
