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
package org.wcs.smart;

import java.io.File;
import java.util.HashMap;

/**
 * Class for storing context about the SMART instance.
 * 
 * @author Emily
 *
 */
public enum SmartContext {

	INSTANCE;
	
	public static final String FILESTORE_KEY = "filestore_location"; //$NON-NLS-1$
	
	public static final String TEMP_FILESTORE_KEY = "temp_filestore_location"; //$NON-NLS-1$
	
	private java.util.HashMap<Class<?>, Object> map = new HashMap<Class<?>, Object>();
	private java.util.HashMap<String, String> pairs = new HashMap<String, String>();
	
	
	public void setClass(Class<?> clazz, Object object){
		map.put(clazz, object);
	}
	
	public <T> T getClass(Class<T> clazz){
		return (T)map.get(clazz);
	}
	
	public String getFilestoreLocation(){
		return pairs.get(FILESTORE_KEY);
	}
	
	public void setFilestoreLocation(String rootLocation){
		pairs.put(FILESTORE_KEY, rootLocation);
	}
	
	public File getTempFilestoreLocation(){
		return new File(pairs.get(TEMP_FILESTORE_KEY));
	}
	
	public void setTempFilestoreLocation(File location){
		pairs.put(TEMP_FILESTORE_KEY, location.getAbsolutePath());
	}

}
