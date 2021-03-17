/*
 * Copyright (C) 2015 Wildlife Conservation Society
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
package org.wcs.smart.connect.query.engine;

import java.util.UUID;

/**
 * For tracking query options.  Tracks names, keys and uuids
 * for group by headers.
 * 
 * 
 * @author Emily
 * @since 1.0.0
 */
public class ListItem {
	
	private UUID uuid;
	private String name;
	private String key;
	private String shortName;
	
	/**
	 * Creates a new list item with a name. 
	 * In this case the uuid and key are null; 
	 * 
	 * @param name
	 */
	public ListItem(String name){
		this(null, name, null);
	}
	
	/**
	 * Creates a new list item with a name and uuid. 
	 * In this case the key will be null. 
	 *  
	 * 
	 * @param uuid
	 * @param name
	 */
	public ListItem(UUID uuid, String name){
		this(uuid, name, null);
	}
			
	/**
	 * Creates a new list item
	 * 
	 * @param name
	 * @param key
	 */
	public ListItem(UUID uuid, String name, String key){
		this.uuid = uuid;
		this.name = name;
		this.key = key;
	}
	
	/**
	 * Creates a new list item
	 * 
	 * @param name
	 * @param key
	 */
	public ListItem(UUID uuid, String name, String key, String shortName){
		this.uuid = uuid;
		this.name = name;
		this.key = key;
		this.shortName = shortName;
	}
	
	/**
	 * Short name associated with list item
	 * @return
	 */
	public String getShortName(){
		return this.shortName;
	}
	
	/**
	 * @return the list item name
	 */
	public String getName(){
		return this.name;
	}
	/**
	 * @return the list item uuid
	 */
	public UUID getUuid(){
		return this.uuid;
	}
	/**
	 * @return the list item key
	 */
	public String getKey(){
		return this.key;
	}
	
}