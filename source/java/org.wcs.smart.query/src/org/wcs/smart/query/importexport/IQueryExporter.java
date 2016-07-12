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
package org.wcs.smart.query.importexport;

import java.io.File;
import java.util.HashMap;

import org.eclipse.core.runtime.IProgressMonitor;
import org.wcs.smart.query.common.engine.IQueryResult;
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
	 * Any query exported that exports the query definition should have an id that
	 * starts with this prefix.  This allows reports to be able
	 * to inlucde queries is exports
	 */
	public static final String QUERY_DEFINTION_EXPORTER_ID = "org.wcs.smart.query.export.definition"; //$NON-NLS-1$

	public static final String PROJECTION_PARAM_KEY = "projection"; //$NON-NLS-1$
	
	/**
	 * 
	 * @return unique exporter identifier 
	 */
	public String getId();
	
	/**
	 * 
	 * @return true if the exporter supports projections
	 */
	public boolean supportsProjection();
	
	/**
	 * @return the exporter name
	 */
	public String getName();

	/**
	 * @return the default file extension for the exporter; return null if exporting to directory
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
	 * @param results the query results to export (can be null)
	 * @param file the file to write results to
	 * @param options additional export parameters 
	 * @throws Exception an exception if an error occurs
	 * while exporting
	 */
	void export (Query query, IQueryResult results, File file, HashMap<String, Object> parameters, IProgressMonitor monitor) throws Exception;
}
