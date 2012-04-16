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
package org.wcs.smart.query.export;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;

import org.wcs.smart.query.model.QueryResultItem;
import org.wcs.smart.query.xml.QueryXmlManager;
import org.wcs.smart.query.xml.model.Query;
import org.wcs.smart.query.xml.model.QueryType;

/**
 * Query exporter that exports the query definition
 * to a file.  Does not export 
 * query results.
 *  
 * 
 * @author Emily
 * @since 1.0.0
 */
public class DefinitionQueryExporter extends QueryExporter {

	/**
	 * Writes the xml definition.
	 * 
	 * @see org.wcs.smart.query.export.QueryExporter#init()
	 */
	@Override
	protected void init() throws Exception {
		Query wpquery = new Query();
		QueryType qt = new QueryType();
		qt.setName(this.query.getName());
		qt.setDefinition(this.query.getQueryFilter());
		wpquery.setQuery(qt);
		
		OutputStream fout = new BufferedOutputStream(new FileOutputStream(this.outputFile));
		try{
			QueryXmlManager.writeDataModel(wpquery, fout);
		}finally{
			fout.close();
		}
		
	}

	/**
	 * Does nothing.
	 * 
	 * @see org.wcs.smart.query.export.QueryExporter#writeRow(org.wcs.smart.query.model.QueryResultItem)
	 */
	@Override
	protected void writeRow(QueryResultItem row) throws Exception {
	}

	/**
	 * Does nothing.
	 * @see org.wcs.smart.query.export.QueryExporter#finish()
	 */
	@Override
	protected void finish() throws Exception {
	}

	/**
	 * @see org.wcs.smart.query.export.QueryExporter#getName()
	 */
	@Override
	public String getName() {
		return "Query Definition";
	}

	/**
	 * @see org.wcs.smart.query.export.QueryExporter#getDefaultExtension()
	 */
	@Override
	public String getDefaultExtension() {
		return "xml";
	}

	/**
	 * @see org.wcs.smart.query.export.QueryExporter#writeResults()
	 */
	@Override
	protected boolean writeResults() {
		return false;
	}

}
