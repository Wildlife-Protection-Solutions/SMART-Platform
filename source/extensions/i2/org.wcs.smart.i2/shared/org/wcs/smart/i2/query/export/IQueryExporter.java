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
package org.wcs.smart.i2.query.export;

import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;

import org.hibernate.Session;
import org.wcs.smart.i2.query.IQueryResult;

/**
 * Query result exporter.
 * 
 * @author Emily
 *
 */
public interface IQueryExporter {

	public enum ExportOption{
		DELIMITER,
		PROJECTION,
		ENCODING,
		LOCALE;
	}

	/**
	 * 
	 * @param queryType
	 * @return true if the exporter can export the results from the givane query type key
	 */
	public boolean canExport(String queryType);
	
	/**
	 * Exports the results
	 * @param session
	 * @param results
	 * @param destination
	 * @param exportOptions
	 * @return list of files created
	 * @throws Exception
	 */
	public Collection<Path> exportQuery(Session session, IQueryResult results, Path destination, HashMap<ExportOption,Object> exportOptions) throws Exception;
	
	/**
	 * 
	 * @param option
	 * @return true if the given export option is supported for this exporter
	 */
	public boolean supportsOption(ExportOption option);
	
	/**
	 * 
	 * @param l
	 * @return the name of the exporter
	 */
	public String getName(Locale l);
	
	/**
	 * The file extension 
	 * @return
	 */
	public String getExtension();
}
