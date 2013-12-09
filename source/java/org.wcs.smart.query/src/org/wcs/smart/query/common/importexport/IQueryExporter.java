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
package org.wcs.smart.query.common.importexport;

import java.io.File;

import org.eclipse.core.runtime.IProgressMonitor;
import org.wcs.smart.query.model.Query;

/**
 * Query exporter interface. 
 * <p>
 * To add an additional query exporter, create
 * a new query exporter extension point that
 * extends this class.
 * </p>
 * 
 * @author egouge
 * @since 1.0.0
 */
public interface IQueryExporter {

	/**
	 * 
	 * @return unique exporter identifier 
	 */
	public String getId();
	
	/**
	 * @return the exporter name
	 */
	public String getName();

	/**
	 * @return the default file extension for the exporter
	 */
	String getDefaultExtension();
	
	
	/**
	 * @param query the query to export
	 * @return <code>true</code> if this class can export the
	 * given query.
	 */
	boolean canExport(Query query);
	
	/**
	 * Export the given query.
	 * 
	 * @param query the query to export
	 * @param file the file to write results to
	 * @param monitor the progress monitor
	 * @throws Exception an exception if an error occurs
	 * while exporting
	 */
	void export (Query query, File file, IProgressMonitor monitor) throws Exception;
}
