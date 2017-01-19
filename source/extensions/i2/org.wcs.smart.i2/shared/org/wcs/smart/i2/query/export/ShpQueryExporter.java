package org.wcs.smart.i2.query.export;

import java.io.Serializable;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
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
import org.locationtech.udig.catalog.URLUtils;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.operation.MathTransform;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.IIntelligenceLabelProvider;
import org.wcs.smart.i2.query.IGeometryResultItem;
import org.wcs.smart.i2.query.IPagedQueryResultSet;
import org.wcs.smart.i2.query.IResultItem;
import org.wcs.smart.i2.query.PagedResultSetIterator;
import org.wcs.smart.i2.udig.query.FeatureGenerator;
import org.wcs.smart.i2.udig.query.QueryDataSource;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class ShpQueryExporter implements IQueryExporter {

	@SuppressWarnings("unchecked")
	@Override
	public void exportQuery(IPagedQueryResultSet results, Path destination,
			HashMap<ExportOption, Object> exportOptions) throws Exception {
		Locale l = Locale.getDefault();
		if (exportOptions.containsKey(ExportOption.LOCALE) && exportOptions.get(ExportOption.LOCALE) instanceof Locale){
			l = (Locale) exportOptions.get(ExportOption.LOCALE);
		}
		
		Projection pp  = null;
		if (exportOptions.containsKey(ExportOption.PROJECTION) && exportOptions.get(ExportOption.PROJECTION) instanceof Projection){
			pp= (Projection) exportOptions.get(ExportOption.PROJECTION);
		}
		
		List<SimpleFeature> pointFeatures = new ArrayList<SimpleFeature>();
		List<SimpleFeature> polygonFeatures = new ArrayList<SimpleFeature>();
		
		SimpleFeatureType pointType = FeatureGenerator.generateFeatureType("Point", QueryDataSource.POINT_TYPE, results.getQueryColumns(), true);
		SimpleFeatureType polygonType = FeatureGenerator.generateFeatureType("Polygon", QueryDataSource.POLYGON_TYPE, results.getQueryColumns(), true);
		
		
		Session session = HibernateManager.openSession();
		try{
			PagedResultSetIterator iterator = new PagedResultSetIterator(results, session);
			while(iterator.hasNext()){
				IResultItem i = iterator.next();
				if (i instanceof IGeometryResultItem){
					Geometry g = ((IGeometryResultItem)i).getGeometry();
					if (g instanceof Point){
						SimpleFeature f = FeatureGenerator.toFeature(pointType, i, results.getQueryColumns());
						pointFeatures.add(f);
					}else if (g instanceof Polygon){
						SimpleFeature p = FeatureGenerator.toFeature(polygonType, i, results.getQueryColumns());
						polygonFeatures.add(p);
					}
				}
			}
		}finally{
			session.close();
		}
		
		String filename = destination.getFileName().toString();
		int ext = filename.lastIndexOf('.');
		String pointFile = filename;
		String polygonFile = filename;
		
		if (ext < 0 ){
			pointFile = pointFile + "_point.shp";
			polygonFile = polygonFile + "_polygon.shp";
		}else{
			pointFile = pointFile.substring(0, ext) + "_point" + pointFile.substring(ext);
			polygonFile = polygonFile.substring(0, ext) + "_polygon" + polygonFile.substring(ext);
		}
		
		
		URL shpPointFile = URLUtils.fileToURL(destination.getParent().resolve(pointFile).toFile());
		URL shpPolygonFile = URLUtils.fileToURL(destination.getParent().resolve(polygonFile).toFile());
		
		Object[][] items = {{shpPointFile, pointType, pointFeatures}, {shpPolygonFile, polygonType, polygonFeatures}};
		
		for (Object[] item : items){
			FileDataStoreFactorySpi factory = FileDataStoreFinder.getDataStoreFactory("shp"); //$NON-NLS-1$
			Map<String, Serializable> params = new HashMap<String, Serializable>();
			params.put(ShapefileDataStoreFactory.URLP.key, (URL)item[0]);
		
			DataStore shapefile = factory.createNewDataStore(params);
		
			//retype 
			List<SimpleFeature> reprojected = new ArrayList<SimpleFeature>();
			if (pp == null || CRS.equalsIgnoreMetadata(SmartDB.DATABASE_CRS, pp.getParsedCoordinateReferenceSystem())){
				reprojected = (List<SimpleFeature>) item[2];
				shapefile.createSchema((SimpleFeatureType) item[1]);
			}else{
				SimpleFeatureType reprojectedType = SimpleFeatureTypeBuilder.retype((SimpleFeatureType) item[1], pp.getParsedCoordinateReferenceSystem());
				shapefile.createSchema(reprojectedType);
				MathTransform transform = CRS.findMathTransform(SmartDB.DATABASE_CRS, pp.getParsedCoordinateReferenceSystem(), true);
				for (SimpleFeature f : (List<SimpleFeature>) item[2]){
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
			
			shapefile.dispose();
		}
	}

	@Override
	public boolean supportsOption(ExportOption option) {
		if (option == ExportOption.PROJECTION) return true;
		if (option == ExportOption.LOCALE) return true;
		return false;
	}

	@Override
	public String getName(Locale l) {
		return SmartContext.INSTANCE.getClass(IIntelligenceLabelProvider.class).getLabel(this, l);
	}

	@Override
	public String getExtension() {
		return "shp";
	}

}
