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

import java.io.Serializable;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
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
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.udig.catalog.URLUtils;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.operation.MathTransform;
import org.wcs.smart.IProjectionProvider;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.common.engine.IColumnInfoProvider;
import org.wcs.smart.query.common.engine.IGeometryResultItem;
import org.wcs.smart.query.common.engine.IPagedQueryResultSet;
import org.wcs.smart.query.common.engine.IQueryResult;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.common.engine.MemoryQueryResult;
import org.wcs.smart.query.common.model.GridQueryResult;
import org.wcs.smart.query.common.model.IColumnAutoConfigQuery;
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.query.importexport.IQueryExporter;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.map.QueryFeatureSource;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryColumn;

/**
 * Shapefile query exporter.  Exports
 * the results of a query to a shapefile.
 * 
 * @author Emily
 * @since 1.0.0
 */
public abstract class ShapeQueryExporter extends SimpleQueryExporter implements IQueryExporter{

    protected ArrayList<SimpleFeature> features = null;
    protected Query query;
    protected Projection outputPrj;
    
    
    protected SimpleFeatureType featureType;
    protected Charset cs;
    
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
	
	@Override
	public boolean supportsCharEncodings() {
		return true;
	}
	
	/**
	 * Creates a shapefile and initialises the schema.
	 * 
	 * @see org.wcs.smart.query.export.SimpleQueryExporter#init()
	 */
	@Override
	protected void init() throws Exception {
		features = new ArrayList<SimpleFeature>();
		featureType = createSchema();
	}

	/**
	 * @see org.wcs.smart.query.export.SimpleQueryExporter#writeRow(org.wcs.smart.query.model.QueryResultItem)
	 */
	@Override
	protected void writeRow(IResultItem row) throws Exception {
		features.add(createFeature(row));
	}

	/**
	 * @see org.wcs.smart.query.export.SimpleQueryExporter#finish()
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected void finish() throws Exception {
		URL shpFileURL = URLUtils.fileToURL(this.outputFile.toAbsolutePath().toFile());
		
		FileDataStoreFactorySpi factory = FileDataStoreFinder.getDataStoreFactory("shp"); //$NON-NLS-1$
        Map<String, Serializable> params = new HashMap<String, Serializable>();
		params.put(ShapefileDataStoreFactory.URLP.key, shpFileURL);
		params.put(ShapefileDataStoreFactory.DBFCHARSET.key, cs.name());
		
		DataStore shapefile = factory.createNewDataStore(params);
		
		//retype 
		List<SimpleFeature> reprojected = new ArrayList<SimpleFeature>();
		if (outputPrj == null || CRS.equalsIgnoreMetadata(SmartDB.DATABASE_CRS, outputPrj.getParsedCoordinateReferenceSystem())){
			reprojected = features;
			shapefile.createSchema(featureType);
		}else{
			SimpleFeatureType reprojectedType = SimpleFeatureTypeBuilder.retype(featureType, outputPrj.getParsedCoordinateReferenceSystem());
			shapefile.createSchema(reprojectedType);
			MathTransform transform = CRS.findMathTransform(SmartDB.DATABASE_CRS, outputPrj.getParsedCoordinateReferenceSystem(), true);
			for (SimpleFeature f : features){
				SimpleFeature copy = SimpleFeatureBuilder.copy(f);
				copy.setDefaultGeometry(JTS.transform((Geometry)f.getDefaultGeometry(), transform));
				reprojected.add(copy);
			}
		}
		
		FeatureStore<SimpleFeatureType, SimpleFeature> fs = 
				(FeatureStore<SimpleFeatureType, SimpleFeature>) shapefile.getFeatureSource(shapefile.getTypeNames()[0]);
		fs.setTransaction(new DefaultTransaction());
		fs.addFeatures( DataUtilities.collection(reprojected) );
		fs.getTransaction().commit();
		fs.getTransaction().close();
		
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
	protected SimpleFeature createFeature(IResultItem it) throws Exception{
		if (it instanceof IGeometryResultItem git) {
			return git.toSimpleFeature(featureType, geometryColumn, queryColumns);
		}
		return null;
	}

	/**
	 * Creates the feature type
	 * @return
	 */
	protected SimpleFeatureType createSchema() throws Exception{
		return DataUtilities.createType("smart." + geometryColumn.getKey(),  //$NON-NLS-1$
				QueryFeatureSource.getFeatureSchemaDef(this.queryColumns, geometryColumn, false, true));
		
	}
	@Override
	public void export(Query query, IQueryResult results, 
			Path file, 
			Map<String, Object> parameters, 
			IProgressMonitor monitor) throws Exception {
		
		
		this.query = ((SimpleQuery)query);
		outputPrj = (Projection) parameters.get(IQueryExporter.PROJECTION_PARAM_KEY);
		IProjectionProvider provider = new IProjectionProvider() {
			@Override
			public Projection getProjection() {
				return outputPrj;
			}
		};
		
		cs = StandardCharsets.UTF_8;
		if (parameters.containsKey(IQueryExporter.ENCODING_KEY)){
			cs = (Charset)parameters.get(IQueryExporter.ENCODING_KEY);
		}
		
		List<QueryColumn> columns = (List<QueryColumn>) parameters.get(IQueryExporter.QUERY_COLUMN_KEY);
		if (columns == null) {
			columns = ((SimpleQuery)query).computeQueryColumns(Locale.getDefault(), null, provider);
		}
		
		this.geometryColumn = (QueryColumn) parameters.get(IQueryExporter.GEOMETRY_COLUMN_KEY);
		if (this.geometryColumn == null) {
			for (QueryColumn qc : columns) {
				if (qc.isDefaultGeometryColumn()) {
					this.geometryColumn = qc;
					break;
				}
			}
			if (this.geometryColumn == null) {
				throw new Exception("A geometry column to export must be specified.");
			}
		}
		
		boolean isDataFiltering = query instanceof IColumnAutoConfigQuery && results instanceof IColumnInfoProvider && ((IColumnAutoConfigQuery)query).isShowDataColumnsOnly();
		for (Iterator<QueryColumn> iterator = columns.iterator(); iterator.hasNext();) {
			QueryColumn column = iterator.next();
			boolean isVisibleColumn = isDataFiltering ? ((IColumnInfoProvider)results).isDataColumn(column) : column.isVisible();
			if (!isVisibleColumn){
				iterator.remove();		
			}else{
				column.setVisible(true);
			}
		}
		//get all data in default projection and reproject when we write out features
		if (results instanceof IPagedQueryResultSet){
			super.setData((IPagedQueryResultSet<?>)results, geometryColumn, columns, file);
		}else if (results instanceof MemoryQueryResult){
			super.setData(((MemoryQueryResult<?>)results).getData(), geometryColumn, columns, file);
		}else if (results instanceof GridQueryResult){
			super.setData(((GridQueryResult)results).getData(), geometryColumn, columns, file);
		}
		super.export(monitor);
		
	}
		
}
