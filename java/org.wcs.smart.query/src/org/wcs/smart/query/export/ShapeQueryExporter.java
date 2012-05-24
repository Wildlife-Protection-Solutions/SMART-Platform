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
import java.net.URL;
import java.util.ArrayList;

import net.refractions.udig.catalog.URLUtils;

import org.eclipse.core.runtime.IProgressMonitor;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureStore;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.indexed.IndexedShapefileDataStore;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.query.map.geotools.QueryDataSource;
import org.wcs.smart.query.map.geotools.QueryResultItemFeature;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryResultItem;
import org.wcs.smart.query.model.observation.ObservationQuery;

/**
 * Shapefile query exporter.  Exports
 * the results of a query to a shapefile.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class ShapeQueryExporter extends ObservationQueryExporter implements IQueryExporter{

    private ShapefileDataStore shapefile = null;    
    private ArrayList<SimpleFeature> features = null;
   
    /**
     * Creates new shapefile exporter
     */
    public ShapeQueryExporter() {

	}
	/**
	 * Creates a shapefile and initialises the schema.
	 * 
	 * @see org.wcs.smart.query.export.ObservationQueryExporter#init()
	 */
	@Override
	protected void init() throws Exception {
		URL shpFileURL = URLUtils.fileToURL(this.outputFile);
        shapefile = new IndexedShapefileDataStore(shpFileURL);
		SimpleFeatureType type = DataUtilities.createType("smart." + QueryDataSource.WAYPOINT_TYPE,QueryDataSource.getFeatureSchemaDef(this.queryColumns));
		shapefile.createSchema(type);
		features = new ArrayList<SimpleFeature>();
	}

	/**
	 * @see org.wcs.smart.query.export.ObservationQueryExporter#writeRow(org.wcs.smart.query.model.QueryResultItem)
	 */
	@Override
	protected void writeRow(QueryResultItem row) throws Exception {
		features.add(QueryResultItemFeature.createFeature(row,  queryColumns, shapefile.getSchema()));
	}

	/**
	 * @see org.wcs.smart.query.export.ObservationQueryExporter#finish()
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected void finish() throws Exception {
		FeatureStore<SimpleFeatureType, SimpleFeature> fs = (FeatureStore<SimpleFeatureType, SimpleFeature>) shapefile.getFeatureSource();
		fs.addFeatures( DataUtilities.collection(features) );
		shapefile.dispose();
		
	}
	@Override
	public String getName() {
		return "Shapefile";
	}

	@Override
	public String getDefaultExtension() {
		return "shp";
	}
	/* (non-Javadoc)
	 * @see org.wcs.smart.query.export.IQueryExporter#canExport(org.wcs.smart.query.model.Query)
	 */
	@Override
	public boolean canExport(Query query) {
		if (query instanceof ObservationQuery){
			return true;
		}
		return false;
	}
	/* (non-Javadoc)
	 * @see org.wcs.smart.query.export.IQueryExporter#export(org.wcs.smart.query.model.Query, java.io.File, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void export(Query query, File file, IProgressMonitor monitor)
			throws Exception {
		ObservationQuery q = ((ObservationQuery)query);
		super.setData(q.getLastResults(), q.getQueryColumns(), file);
		super.export(monitor);
		
	}
		
}
