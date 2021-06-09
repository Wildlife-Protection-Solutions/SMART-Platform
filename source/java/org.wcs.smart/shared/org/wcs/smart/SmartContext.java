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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

import org.hibernate.type.UUIDBinaryType;

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
	
	private SmartContext() {
		map.put(UUIDBinaryType.class, UUIDBinaryType.INSTANCE);
	}
	
	public void setClass(Class<?> clazz, Object object){
		map.put(clazz, object);
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getClass(Class<T> clazz){
		return (T)map.get(clazz);
	}
	
	public String getFilestoreLocation(){
		return pairs.get(FILESTORE_KEY);
	}
	
	public void setFilestoreLocation(String rootLocation){
		pairs.put(FILESTORE_KEY, rootLocation);
	}
	
	/**
	 * A temporary working directory.  Files in this directory are not
	 * included in system backups or CA exports (nor at they sync'd to Connect).
	 * 
	 * This function creates the directory if it is not already created
	 * @return 
	 */
	public Path getTempFilestoreLocation(){
		Path path = Paths.get(pairs.get(TEMP_FILESTORE_KEY)).normalize();
		if (!Files.exists(path)) {
			try {
				Files.createDirectories(path);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return path;
	}
	
	public void setTempFilestoreLocation(Path location){
		pairs.put(TEMP_FILESTORE_KEY, location.toAbsolutePath().toString());
	}

}
