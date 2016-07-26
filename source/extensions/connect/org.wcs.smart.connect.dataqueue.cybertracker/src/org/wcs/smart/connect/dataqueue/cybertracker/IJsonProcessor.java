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
package org.wcs.smart.connect.dataqueue.cybertracker;

import java.util.List;

import org.hibernate.Session;
import org.json.simple.JSONObject;

/**
 * Interface for plugins that can processes cybertracker JSON data
 * 
 * @author Emily
 *
 */
public interface IJsonProcessor {

	public static final String EXTENSION_ID = "org.wcs.smart.connect.dataqueue.cybertracker.json.processor"; //$NON-NLS-1$
	
	/**
	 * @param features
	 * @return a list of objects that have been processed
	 */
	public List<JSONObject> processJson(List<JSONObject> features, Session session) throws Exception;
	
	/**
	 * Called after save is complete.  This can be used to fire system
	 * events etc.
	 */
	public void afterSave();
	
	/**
	 * 
	 * @return an optional status message to include in the processing items
	 * status message
	 */
	public String getStatusMessage();
}
