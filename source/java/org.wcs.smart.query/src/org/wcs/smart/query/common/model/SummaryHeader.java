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
package org.wcs.smart.query.common.model;

/**
 * A header for a summary row/column.
 * <p>
 * A summary header represents a particular header cell in the results
 * table . 
 * 
 * </p>
 * <p>
 * A header consists of a:
 *   <li>name -which is the gui name displayed to the user
 *   <li>key - the key which identifies the column collection (station, team etc). 
 *   <li>identifier - a hex encoded uuid or other key values that represents a particular item in the column (for example station x) 
 *   <li>isValue - true if this header represents a value item
 * 
 * @author egouge
 * @since 1.0.0
 */
public class SummaryHeader {

	private String name;	//name as displayed to user
	private String fullName;	//full name
	
	private String key;		 //unique column identifier (station, team etc)
	private String identifier;  //hex encoded uuid, string or other key value
	private boolean isValue = false;
	
	/**
	 * Creates a new summary header with a null identifier 
	 * @param name the gui name
	 * @param name the complete name (includes entire category path)
	 * @param key the item key
	 * @param isValue if value item or not
	 */
	public SummaryHeader(String name, String fullName, String key, boolean isValue){
		this(name, fullName, key, null, isValue);
	}
	/**
	 * Creates a new summary header 
	 * @param name the gui name
	 * @param name the complete name (includes entire category path)
	 * @param key the item key
	 * @param identifier the identifier
	 * @param isValue if value item or not
	 */
	public SummaryHeader(String name, String fullName, String key, String identifier, boolean isValue){
		this.name = name;
		this.fullName = fullName;
		this.key = key;
		this.identifier = identifier;
		this.isValue = isValue;
	}
	
	/**
	 * @return true if header represents a value cell
	 */
	public boolean isValue(){
		return this.isValue;
	}
	
	/**
	 * @return the summary header name to display to the user
	 */
	public String getName(){
		return this.name;
	}
	
	/**
	 * @return the full summary header name
	 */
	public String getFullName(){
		return this.fullName;
	}
	
	/**
	 * the column key
	 * @return the key 
	 */
	public String getKey(){
		return this.key;
	}
	
	/**
	 *  A hex encoded uuid or other key
	 * value depending on what the cell represents.
	 * @return the identifier
	 */
	public String getIdentifier(){
		return this.identifier;
	}
	
}
