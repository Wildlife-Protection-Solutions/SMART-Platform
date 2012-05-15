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

import java.io.File;

import org.eclipse.core.runtime.IProgressMonitor;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.Query.QueryType;

/**
 * CSV Summary Exporter for exporting the
 * summary results as a csv file.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class CsvSummaryExporter implements IQueryExporter {

	public CsvSummaryExporter() {
	}


	@Override
	public boolean canExport(Query query) {
		if (query.getType() == QueryType.SUMMARY){
			return true;
		}
		return false;
	}

	@Override
	public void export(Query query, File f, IProgressMonitor monitor) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.wcs.smart.query.export.IQueryExporter#getName()
	 */
	@Override
	public String getName() {
		return "Comma Separated Values";
	}

	/* (non-Javadoc)
	 * @see org.wcs.smart.query.export.IQueryExporter#getDefaultExtension()
	 */
	@Override
	public String getDefaultExtension() {
		return "*.csv";
	}

}
