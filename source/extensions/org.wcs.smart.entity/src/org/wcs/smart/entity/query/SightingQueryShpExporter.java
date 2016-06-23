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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.eclipse.core.runtime.IProgressMonitor;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.IProjectionProvider;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.entity.map.EntityQueryDataSource;
import org.wcs.smart.entity.map.EntityQueryDataSourceFeatureReader;
import org.wcs.smart.query.common.engine.IPagedQueryResultSet;
import org.wcs.smart.query.common.engine.IQueryResult;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.common.importexport.ShapeQueryExporter;
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.query.importexport.IQueryExporter;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryColumn;

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
	protected SimpleFeature createFeature(IResultItem it, IQueryType queryType, SimpleFeatureType type)
			throws Exception {
		return EntityQueryDataSourceFeatureReader.createSightingResult((SightingResultItem)it, queryColumns, type);
	}

	@Override
	protected SimpleFeatureType createSchema(IQueryType queryType)
			throws Exception {
		return EntityQueryDataSource.createQuerySchema(queryColumns, false, true);
	}
	
	/* (non-Javadoc)
	 * @see org.wcs.smart.query.export.IQueryExporter#export(org.wcs.smart.query.model.Query, java.io.File, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void export(Query query, IQueryResult results, File file, 
			HashMap<String, Object> parameters, IProgressMonitor monitor) throws Exception {
	
		this.query = ((EntitySightingQuery)query);
		
		outputPrj = (Projection) parameters.get(IQueryExporter.PROJECTION_PARAM_KEY);
		IProjectionProvider provider = new IProjectionProvider() {
			@Override
			public Projection getProjection() {
				return outputPrj;
			}
		};
		List<QueryColumn> columns = ((EntitySightingQuery)query).getQueryColumns();
		List<QueryColumn> cols2 = new ArrayList<QueryColumn>();
		for (Iterator<QueryColumn> iterator = columns.iterator(); iterator.hasNext();) {
			QueryColumn column = (QueryColumn) iterator.next();
			if (column.isVisible()){
				QueryColumn clone = column.clone();
				clone.setProjectionProvider(provider);
				cols2.add(clone);
				
			}
		}
		
		super.setData((IPagedQueryResultSet)results, cols2, file);
		super.export(monitor);
		
	}

}
