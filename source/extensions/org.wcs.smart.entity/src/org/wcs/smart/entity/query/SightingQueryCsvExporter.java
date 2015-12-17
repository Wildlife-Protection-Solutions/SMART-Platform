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
package org.wcs.smart.entity.query;

import java.io.File;
import java.util.HashMap;

import org.eclipse.core.runtime.IProgressMonitor;
import org.wcs.smart.query.common.engine.IQueryResult;
import org.wcs.smart.query.common.importexport.CsvSimpleQueryExporter;
import org.wcs.smart.query.model.Query;

/**
 * CSV Exporter for sightings query
 * @author Emily
 *
 */
public class SightingQueryCsvExporter extends CsvSimpleQueryExporter {

	/* (non-Javadoc)
	 * @see org.wcs.smart.query.export.IQueryExporter#canExport(org.wcs.smart.query.model.Query)
	 */
	@Override
	public boolean canExport(Query query) {
		return query instanceof EntitySightingQuery;
	}

	/* (non-Javadoc)
	 * @see org.wcs.smart.query.export.IQueryExporter#export(org.wcs.smart.query.model.Query, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void export(Query query, IQueryResult result, File file,
			HashMap<String, Object> parameters, IProgressMonitor monitor)
			throws Exception {
		
		if (parameters.get(DELIMITER_KEY) != null){
			try{
				this.delimiter = (Character) parameters.get(DELIMITER_KEY);
			}catch(Exception ex){}
		}
		EntitySightingQuery squery = (EntitySightingQuery) query;
		super.setData((SightingPagedResults)result, squery.getQueryColumns(), file);
		super.export(monitor);
	}
	
}
