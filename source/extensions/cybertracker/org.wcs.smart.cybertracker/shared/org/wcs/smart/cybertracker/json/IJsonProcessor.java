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
package org.wcs.smart.cybertracker.json;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.json.simple.JSONObject;

/**
 * Interface for plugins that can processes cybertracker JSON data
 * 
 * @author Emily
 *
 */
public interface IJsonProcessor {

	public static final String EXTENSION_ID = "org.wcs.smart.cybertracker.json.processor"; //$NON-NLS-1$
	
	/**
	 * The number of months old a patrol is before all links to
	 * this patrol and removed from the database
	 */
	public static final int CLEANUP_MONTHS = 6;
	
	/**
	 * @param features
	 * @return a list of objects that have been processed
	 */
	public List<JSONObject> processJson(List<JSONObject> features, Session session, Locale locale, IProgressMonitor monitor) throws Exception;
		
	/**
	 * 
	 * @return an optional status message to include in the processing items
	 * status message
	 */
	public String getStatusMessage(Locale l);
	
	/**
	 * 
	 * @return list of warnings generated during processing
	 */
	public List<JsonImportWarning> getWarnings();
	
	/**
	 * To be called after processing is complete to cleanup 
	 * any temporary files or other resources created during processing
	 * 
	 */
	public void cleanUp();
	
	public default void cleanUpFiles(List<Path> files) {
		for(Path p : files) {
			try {
				if (Files.exists(p)) {
					Files.delete(p);
				}
			}catch (Exception ex) {
				Logger.getLogger(IJsonProcessor.class.getName()).log(Level.WARNING, ex.getMessage(), ex);
			}
		}
	}
}
