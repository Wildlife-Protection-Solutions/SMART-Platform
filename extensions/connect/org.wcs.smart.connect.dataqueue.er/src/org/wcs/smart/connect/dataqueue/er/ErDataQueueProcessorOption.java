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
package org.wcs.smart.connect.dataqueue.er;

import java.util.HashMap;

import org.wcs.smart.connect.dataqueue.model.DataQueueProcessingOption;

/**
 * Patrol processing options.
 * @author Emily
 *
 */
public enum ErDataQueueProcessorOption {

	ER_GENERATE_IDS(Boolean.TRUE);
	
	Object defaultValue;
	
	ErDataQueueProcessorOption(Object defaultValue){
		this.defaultValue = defaultValue;
	}
	
	public Boolean getValueAsBoolean(DataQueueProcessingOption value){
		if (value == null){
			return (Boolean)defaultValue;
		}else{
			try{
				return  Boolean.valueOf(value.getValue());
			}catch (Exception ex){
				ex.printStackTrace();
				return (Boolean)defaultValue;
			}
		}
	}
	public Boolean getValueAsBoolean(HashMap<String, DataQueueProcessingOption> options){
		DataQueueProcessingOption value = options.get(this.name());
		return getValueAsBoolean(value);
	}
	
}
