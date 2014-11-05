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
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.entity.map.EntityQueryDataSource;
import org.wcs.smart.entity.map.EntityQueryDataSourceFeatureReader;
import org.wcs.smart.query.common.importexport.ShapeQueryExporter;
import org.wcs.smart.query.model.IPagedQueryResultSet;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.IResultItem;
import org.wcs.smart.query.model.Query;

/**
 * Shapefile exporter for sighting query
 * @author Emily
 *
 */
public class SightingQueryShpExporter extends ShapeQueryExporter {

	@Override
	public boolean canExport(Query query) {
		return query instanceof EntitySightingQuery;
	}

	@Override
	protected SimpleFeature createFeature(IResultItem it, IQueryType queryType)
			throws Exception {
		return EntityQueryDataSourceFeatureReader.createSightingResult((SightingResultItem)it, queryColumns, shapefile.getSchema());
	}

	@Override
	protected SimpleFeatureType createSchema(IQueryType queryType)
			throws Exception {
		return EntityQueryDataSource.createQuerySchema(queryColumns, false);
	}
	
	/* (non-Javadoc)
	 * @see org.wcs.smart.query.export.IQueryExporter#export(org.wcs.smart.query.model.Query, java.io.File, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void export(Query query, File file, HashMap<String, Object> options, IProgressMonitor monitor) throws Exception {
		this.query = ((EntitySightingQuery)query);
		super.setData((IPagedQueryResultSet)query.getCachedResults(monitor), ((EntitySightingQuery)query).getQueryColumns(), file);
		super.export(monitor);
		
	}

}
