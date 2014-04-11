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
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import net.refractions.udig.catalog.URLUtils;

import org.eclipse.core.runtime.IProgressMonitor;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureStore;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.indexed.IndexedShapefileDataStore;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.query.importexport.IQueryExporter;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.IPagedQuery;
import org.wcs.smart.query.model.IPagedQueryResultSet;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.IResultItem;
import org.wcs.smart.query.model.Query;

/**
 * Shapefile query exporter.  Exports
 * the results of a query to a shapefile.
 * 
 * @author Emily
 * @since 1.0.0
 */
public abstract class ShapeQueryExporter extends SimpleQueryExporter implements IQueryExporter{

    protected ShapefileDataStore shapefile = null;    
    protected ArrayList<SimpleFeature> features = null;
    protected Query query;
   
    /**
     * Creates new shapefile exporter
     */
    public ShapeQueryExporter() {

	}
	/**
	 * Creates a shapefile and initialises the schema.
	 * 
	 * @see org.wcs.smart.query.export.SimpleQueryExporter#init()
	 */
	@Override
	protected void init() throws Exception {
		URL shpFileURL = URLUtils.fileToURL(this.outputFile);
        shapefile = new IndexedShapefileDataStore(shpFileURL);
		SimpleFeatureType type = createSchema(this.query.getType());
		shapefile.createSchema(type);
		features = new ArrayList<SimpleFeature>();
	}

	/**
	 * @see org.wcs.smart.query.export.SimpleQueryExporter#writeRow(org.wcs.smart.query.model.QueryResultItem)
	 */
	@Override
	protected void writeRow(IResultItem row) throws Exception {
		features.add(createFeature(row, this.query.getType()));
	}

	/**
	 * @see org.wcs.smart.query.export.SimpleQueryExporter#finish()
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected void finish() throws Exception {
		FeatureStore<SimpleFeatureType, SimpleFeature> fs = (FeatureStore<SimpleFeatureType, SimpleFeature>) shapefile.getFeatureSource();
		fs.addFeatures( DataUtilities.collection(features) );
		shapefile.dispose();
		
	}
	
	@Override
	public String getId(){
		return "org.wcs.smart.query.export.observation.shp"; //$NON-NLS-1$
	}
	
	@Override
	public String getName() {
		return Messages.ShapeQueryExporter_ExporterName;
	}

	@Override
	public String getDefaultExtension() {
		return "shp"; //$NON-NLS-1$
	}
	/* (non-Javadoc)
	 * @see org.wcs.smart.query.export.IQueryExporter#canExport(org.wcs.smart.query.model.Query)
	 */
	@Override
	public abstract boolean canExport(Query query);
	
	/**
	 * Creates a feature for the given result item row
	 * @param it
	 * @return
	 */
	protected abstract SimpleFeature createFeature(IResultItem it, IQueryType queryType) throws Exception;
	
	/**
	 * Creates the feature type
	 * @return
	 */
	protected abstract SimpleFeatureType createSchema(IQueryType queryType) throws Exception;
	
	/* (non-Javadoc)
	 * @see org.wcs.smart.query.export.IQueryExporter#export(org.wcs.smart.query.model.Query, java.io.File, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void export(Query query, File file, HashMap<String, Object> parameters, IProgressMonitor monitor) throws Exception {
		this.query = ((SimpleQuery)query);
		
		if (query instanceof IPagedQuery) {
			super.setData((IPagedQueryResultSet)query.getCachedResults(monitor), ((SimpleQuery)query).getQueryColumns(), file);
		} else {
			super.setData((Collection<IResultItem>)query.getCachedResults(monitor), ((SimpleQuery)query).getQueryColumns(), file);
		}

		super.export(monitor);
		
	}
		
}
