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
package org.wcs.smart.connect.dataqueue.model;

import org.wcs.smart.connect.model.ConnectServer;

/**
 * Data queue server options.
 * @author Emily
 *
 */
public enum DataQueueServerOptions {

	CHECK_ONSTARTUP(Boolean.FALSE),
	STARTUP_AUTOPROCESS(Boolean.FALSE),
	STARTUP_PROMPT(Boolean.TRUE),
	AUTO_CHECK(Boolean.TRUE),
	AUTO_AUTOPROCESS(Boolean.FALSE),
	AUTO_PROMPT(Boolean.TRUE),
	AUTO_MINUTES(30),
	PROMPT_USER(Boolean.TRUE);
		
	Object defaultValue;
		
	private DataQueueServerOptions(Object defaultValue){
		this.defaultValue = defaultValue;
	}
		
	public String getDefaultValueAsString(){
		return this.defaultValue.toString();
	}
		
	public Boolean getDefaultValueAsBoolean(){
		if (defaultValue instanceof Boolean){
			return (Boolean) this.defaultValue;
		}
		return null;
	}
	public Integer getDefaultValueAsInt(){
		if (defaultValue instanceof Integer){
			return (Integer) this.defaultValue;
		}
		return null;
	}
	
	public Boolean getBooleanValue(ConnectServer server){
		Boolean x = server.getOptionAsBoolean(this.name());
		if (x == null){
			return this.getDefaultValueAsBoolean();
		}
		return x;
	}
	
	public Integer getIntegerValue(ConnectServer server){
		Integer x = server.getOptionAsInt(this.name());
		if (x == null){
			return (Integer)this.defaultValue;
		}
		return x;
	}
}
