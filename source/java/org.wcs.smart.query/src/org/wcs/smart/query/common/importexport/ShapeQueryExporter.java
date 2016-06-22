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
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.geotools.data.DataStore;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureStore;
import org.geotools.data.FileDataStoreFactorySpi;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.udig.catalog.URLUtils;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.QueryTypeManager;
import org.wcs.smart.query.common.engine.IPagedQueryResultSet;
import org.wcs.smart.query.common.engine.IQueryResult;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.common.engine.MemoryQueryResult;
import org.wcs.smart.query.common.model.GridQueryResult;
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.query.importexport.IQueryExporter;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.Query;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Shapefile query exporter.  Exports
 * the results of a query to a shapefile.
 * 
 * @author Emily
 * @since 1.0.0
 */
public abstract class ShapeQueryExporter extends SimpleQueryExporter implements IQueryExporter{

    protected DataStore shapefile = null;    
    protected ArrayList<SimpleFeature> features = null;
    protected Query query;
    protected CoordinateReferenceSystem crs;
    
    /**
     * Creates new shapefile exporter
     */
    public ShapeQueryExporter() {

	}
    

	/**
	 * 
	 * @return shapefile exports support reprojection
	 */
	public boolean supportsProjection(){
		return true;
	}
	
	/**
	 * Creates a shapefile and initialises the schema.
	 * 
	 * @see org.wcs.smart.query.export.SimpleQueryExporter#init()
	 */
	@Override
	protected void init() throws Exception {
		URL shpFileURL = URLUtils.fileToURL(this.outputFile);
		
		FileDataStoreFactorySpi factory = FileDataStoreFinder.getDataStoreFactory("shp"); //$NON-NLS-1$
        Map<String, Serializable> params = new HashMap<String, Serializable>();
		params.put(ShapefileDataStoreFactory.URLP.key, shpFileURL);
		
		shapefile = factory.createNewDataStore(params);
		SimpleFeatureType type = createSchema(QueryTypeManager.INSTANCE.findQueryType(this.query.getTypeKey()));
		
		shapefile.createSchema(type);
		features = new ArrayList<SimpleFeature>();
	}

	/**
	 * @see org.wcs.smart.query.export.SimpleQueryExporter#writeRow(org.wcs.smart.query.model.QueryResultItem)
	 */
	@Override
	protected void writeRow(IResultItem row) throws Exception {
		features.add(createFeature(row, QueryTypeManager.INSTANCE.findQueryType(this.query.getTypeKey())));
	}

	/**
	 * @see org.wcs.smart.query.export.SimpleQueryExporter#finish()
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected void finish() throws Exception {
		//TODO: sort out schema crs - that will likely be wrong
        MathTransform transform = CRS.findMathTransform(SmartDB.DATABASE_CRS, crs, true);
        
		List<SimpleFeature> reprojected = new ArrayList<SimpleFeature>();
		for (SimpleFeature f : features){
			if (CRS.equalsIgnoreMetadata(SmartDB.DATABASE_CRS, crs)){
				reprojected.add(f);
			}else{
				SimpleFeature copy = SimpleFeatureBuilder.copy(f);
				copy.setDefaultGeometry(JTS.transform((Geometry)f.getDefaultGeometry(), transform));
				reprojected.add(f);
			}
		}
		FeatureStore<SimpleFeatureType, SimpleFeature> fs = 
				(FeatureStore<SimpleFeatureType, SimpleFeature>) shapefile.getFeatureSource(shapefile.getTypeNames()[0]);
		fs.setTransaction(new DefaultTransaction());
		
		fs.addFeatures( DataUtilities.collection(reprojected) );
		fs.getTransaction().commit();
		
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
	

	@Override
	public void export(Query query, IQueryResult results, File file, HashMap<String, Object> parameters, IProgressMonitor monitor) throws Exception {
		this.query = ((SimpleQuery)query);
		crs = (CoordinateReferenceSystem) parameters.get(IQueryExporter.PROJECTION_PARAM_KEY);
		
		if (results instanceof IPagedQueryResultSet){
			super.setData((IPagedQueryResultSet)results, ((SimpleQuery)query).getQueryColumns(Locale.getDefault(), null), file);
		}else if (results instanceof MemoryQueryResult){
			super.setData(((MemoryQueryResult)results).getData(), ((SimpleQuery)query).getQueryColumns(Locale.getDefault(), null), file);
		}else if (results instanceof GridQueryResult){
			super.setData(((GridQueryResult)results).getData(), ((SimpleQuery)query).getQueryColumns(Locale.getDefault(), null), file);
		}
		super.export(monitor);
		
	}
		
}
