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

import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
import org.hibernate.Session;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.udig.catalog.URLUtils;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.operation.MathTransform;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.i2.IIntelligenceLabelProvider;
import org.wcs.smart.i2.model.IntelRecordObservationQuery;
import org.wcs.smart.i2.query.IPagedQueryResultSet;
import org.wcs.smart.i2.query.IQueryColumn;
import org.wcs.smart.i2.query.IQueryResult;
import org.wcs.smart.i2.query.IResultItem;
import org.wcs.smart.i2.query.PagedResultSetIterator;
import org.wcs.smart.i2.udig.query.FeatureGenerator;
import org.wcs.smart.util.GeometryUtils;

/**
 * Exports query results to shapefile 
 * @author Emily
 *
 */
public class ShpRecordQueryExporter implements IQueryExporter {

	@Override
	public boolean canExport(String queryType) {
		return queryType.equalsIgnoreCase(IntelRecordObservationQuery.KEY);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Collection<Path> exportQuery(Session session, IQueryResult result, Path destination,
			HashMap<ExportOption, Object> exportOptions) throws Exception {
		
		IPagedQueryResultSet results = (IPagedQueryResultSet) result;
		
		Projection pp  = null;
		if (exportOptions.containsKey(ExportOption.PROJECTION) && exportOptions.get(ExportOption.PROJECTION) instanceof Projection){
			pp= (Projection) exportOptions.get(ExportOption.PROJECTION);
		}
		Charset cs = null;
		if (exportOptions.containsKey(ExportOption.ENCODING) && exportOptions.get(ExportOption.ENCODING) instanceof Charset){
			cs = (Charset) exportOptions.get(ExportOption.ENCODING);
		}
		
		if (!exportOptions.containsKey(ExportOption.GEOMETRY_COLUMN) || !((IQueryColumn)exportOptions.get(ExportOption.GEOMETRY_COLUMN)).getDataType().isGeometry()) {
			throw new Exception("Geometry column must be specified for shapefile export."); //$NON-NLS-1$
		}
		
		IQueryColumn geometryColumn = (IQueryColumn)exportOptions.get(ExportOption.GEOMETRY_COLUMN);
		
		SimpleFeatureType type = FeatureGenerator.generateFeatureType(geometryColumn.getKey(), geometryColumn, results.getQueryColumns(), true); 
		List<SimpleFeature> features = new ArrayList<SimpleFeature>();
		
		PagedResultSetIterator iterator = new PagedResultSetIterator(results, session);
		List<IQueryColumn> columns = results.getQueryColumns();
		while(iterator.hasNext()){
			IResultItem i = iterator.next();
			if (geometryColumn.getValue(i) == null) continue;
			features.add(FeatureGenerator.toFeature(type, i, geometryColumn, columns));
		}
	
	
		FileDataStoreFactorySpi factory = FileDataStoreFinder.getDataStoreFactory("shp"); //$NON-NLS-1$
		Map<String, Serializable> params = new HashMap<String, Serializable>();
		params.put(ShapefileDataStoreFactory.URLP.key, URLUtils.fileToURL(destination.toFile()));
		params.put(ShapefileDataStoreFactory.DBFCHARSET.key, cs.name());
		
		DataStore shapefile = factory.createNewDataStore(params);
		
			//retype 
		List<SimpleFeature> reprojected = new ArrayList<SimpleFeature>();
		if (pp == null || CRS.equalsIgnoreMetadata(GeometryUtils.SMART_CRS, pp.getParsedCoordinateReferenceSystem())){
			reprojected = features;
			shapefile.createSchema(type);
		}else{
			SimpleFeatureType reprojectedType = SimpleFeatureTypeBuilder.retype(type, pp.getParsedCoordinateReferenceSystem());
			shapefile.createSchema(reprojectedType);
			MathTransform transform = CRS.findMathTransform(GeometryUtils.SMART_CRS, pp.getParsedCoordinateReferenceSystem(), true);
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
		
		return List.of(destination);
	}

	@Override
	public boolean supportsOption(ExportOption option) {
		if (option == ExportOption.PROJECTION) return true;
		if (option == ExportOption.LOCALE) return true;
		if (option == ExportOption.ENCODING) return true;
		if (option == ExportOption.GEOMETRY_COLUMN) return true;
		return false;
	}

	@Override
	public String getName(Locale l) {
		return SmartContext.INSTANCE.getClass(IIntelligenceLabelProvider.class).getLabel(this, l);
	}

	@Override
	public String getExtension() {
		return "shp"; //$NON-NLS-1$
	}

}
