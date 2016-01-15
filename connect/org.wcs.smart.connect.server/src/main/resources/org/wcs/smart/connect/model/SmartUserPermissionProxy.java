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
package org.wcs.smart.connect.model;

import java.util.UUID;

/**
 * Proxy for a SmartUserAction that tracks name, resource names, and keys.
 * 
 * @author Emily
 *
 */
public class SmartUserPermissionProxy {

	public enum Type {ACTION, ROLE};
	
	private Type type;
	private String name;
	private String key;
	private String resourceName;
	private UUID resource;
	
	protected SmartUserPermissionProxy(){
		
	}
	public SmartUserPermissionProxy(Type type){
		this.type = type;
	}
	
	/**
	 * The type of permission (role or action)
	 * @return
	 */
	public Type getType(){
		return type;
	}
	public void setType(Type type){
		this.type = type;
	}
	
	/**
	 * The action or role key
	 * @return
	 */
	public String getKey(){
		return this.key;
	}
	
	public void setKey(String key){
		this.key = key;
	}
	
	/**
	 * The action or role name
	 * @return
	 */
	public String getName(){
		return this.name;
	}
	public void setName(String name){
		this.name = name;
	}
	
	/**
	 * The resource name.
	 * Specific to actions with resources 
	 * @return
	 */
	public String getResourceName(){
		return this.resourceName;
	}
	
	public void setResourceName(String name){
		this.resourceName = name;
	}
	
	/**
	 * The resource key.  Specific
	 * to actions with resources.
	 * @return
	 */
	public UUID getResource(){
		return this.resource;
	}
	public void setResource(UUID resource){
		this.resource = resource;
	}
}
