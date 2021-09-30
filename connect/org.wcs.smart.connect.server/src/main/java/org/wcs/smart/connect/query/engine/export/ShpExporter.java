/*
 * Copyright (C) 2015 Wildlife Conservation Society
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
package org.wcs.smart.connect.query.engine.export;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.servlet.ServletContext;
import javax.ws.rs.WebApplicationException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.geotools.data.DataStore;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureStore;
import org.geotools.data.FileDataStoreFactorySpi;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.hibernate.Session;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.operation.MathTransform;
import org.wcs.smart.IProjectionProvider;
import org.wcs.smart.SmartContext;
import org.wcs.smart.connect.SmartUtils;
import org.wcs.smart.connect.ZipUtil;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.connect.query.engine.AbstractDbFeatureResultSet;
import org.wcs.smart.query.common.engine.IQueryResultSetIterator;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.util.GeometryUtils;

/**
 * Exports query results to shapefiles.  Query results must extend
 * AbstractDbFeatureResultSet 
 * 
 * @author Emily
 *
 */
public class ShpExporter extends AbstractQueryExporter{
	
	public static final String FORMAT_KEY = "shp"; //$NON-NLS-1$
	
	public static final String getName(Locale l){
		return Messages.getString("ShpExporter.Shapefilename", l); //$NON-NLS-1$
	}
	
	private final Logger logger = Logger.getLogger(ShpExporter.class.getName());

	private SimpleQuery query;
	private AbstractDbFeatureResultSet<IResultItem> results;
	
	private String filename = ""; //$NON-NLS-1$
	
	protected ShpExporter(IProjectionProvider prjProvider, Locale l, ServletContext ctx){
		super(prjProvider, l, ctx);
	}
	
	public ShpExporter(SimpleQuery query, AbstractDbFeatureResultSet<IResultItem> results, 
			IProjectionProvider prjProvider, Locale l, ServletContext ctx) {
		this(prjProvider, l, ctx);
		this.query = query;
		this.results = results;
		filename = SmartUtils.cleanFileName(query.getName() + "_"+ query.getId()) + ".zip"; //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	public String getFileName() {
		return filename;
	}
	
	@Override
	public void write(OutputStream output) throws IOException, WebApplicationException {
		Path zipFile = null;
		try(Session session = HibernateManager.openNewSession(context, locale)){
			session.beginTransaction();
			try {
				zipFile = createShapefiles(session);
				session.getTransaction().commit();
			}catch (IOException ex) {
				if (session.getTransaction().isActive()) session.getTransaction().rollback();
				logger.log(Level.SEVERE, ex.getMessage(),ex);
				throw ex;
			}
		}finally {
			try(Session session = HibernateManager.openNewSession(context, locale)){
				session.beginTransaction();
				try {
					dispose(results, session);
					session.getTransaction().commit();
				}catch (Exception ex) {
					if (session.getTransaction().isActive()) session.getTransaction().rollback();
					logger.log(Level.SEVERE, ex.getMessage(),ex);
					throw new IOException(ex);
				}
			}
		}
		try {
			Files.copy(zipFile, output);
		}finally {
			try {
				Files.delete(zipFile);
			}catch (IOException ex) {
				logger.log(Level.WARNING, ex.getMessage(), ex);
			}
		}
	}
	
	/**
	 * Exports simple queries whose results and represented by a database table.
	 * 
	 * @param query
	 * @param results
	 * @param session
	 * @throws IOException 
	 */
	private Path createShapefiles(Session session) throws IOException{
		
		try {
			java.nio.file.Path zipFile  = SmartContext.INSTANCE.getTempFilestoreLocation().resolve(filename); 
			
			//remove extension and add .shp to filename
			final String newName = FilenameUtils.removeExtension(zipFile.getFileName().toString()) ;
			final Path outDirectory = zipFile.getParent().resolve(String.valueOf(System.nanoTime()));
			Files.createDirectory(outDirectory);
			final Path shpfile = outDirectory.resolve(newName+ ".shp"); //$NON-NLS-1$
					
			List<QueryColumn> columns = results.getQueryColumns(query, locale, session, prjProvider);
			SimpleFeatureType type = DataUtilities.createType("smartqueryresults", results.getFeatureSchemaDef(columns, false, true)); //$NON-NLS-1$
			ArrayList<SimpleFeature> features = new ArrayList<SimpleFeature>();
			IQueryResultSetIterator<IResultItem> itemiterator = results.iterator(500, session);
			for (Iterator<IResultItem> iterator = itemiterator; iterator.hasNext();) {
				IResultItem resultItem = (IResultItem) iterator.next();	
				SimpleFeature sf = results.toFeature(resultItem, columns, session, type);
				features.add(sf);
			}
						
			FileDataStoreFactorySpi factory = FileDataStoreFinder.getDataStoreFactory("shp"); //$NON-NLS-1$
			Map<String, Serializable> params = new HashMap<String, Serializable>();
			params.put(ShapefileDataStoreFactory.URLP.key, shpfile.toUri().toURL());
			params.put(ShapefileDataStoreFactory.DBFCHARSET.key, StandardCharsets.UTF_8.name());
			DataStore shapefile = factory.createNewDataStore(params);
						
						
			//retype 
			List<SimpleFeature> reprojected = new ArrayList<SimpleFeature>();
			if (prjProvider == null || CRS.equalsIgnoreMetadata(GeometryUtils.SMART_CRS, prjProvider.getProjection().getParsedCoordinateReferenceSystem())){
				reprojected = features;
				shapefile.createSchema(type);
			}else{
				SimpleFeatureType reprojectedType = SimpleFeatureTypeBuilder.retype(type, prjProvider.getProjection().getParsedCoordinateReferenceSystem());
				shapefile.createSchema(reprojectedType);
				MathTransform transform = CRS.findMathTransform(GeometryUtils.SMART_CRS, prjProvider.getProjection().getParsedCoordinateReferenceSystem(), true);
				for (SimpleFeature f : features){
					SimpleFeature copy = SimpleFeatureBuilder.copy(f);
					copy.setDefaultGeometry(JTS.transform((Geometry)f.getDefaultGeometry(), transform));
					reprojected.add(copy);
				}
			}
					
			
			FeatureStore<SimpleFeatureType, SimpleFeature> fs = 
					(FeatureStore<SimpleFeatureType, SimpleFeature>) shapefile.getFeatureSource(shapefile.getTypeNames()[0]);
			try(Transaction tx = new DefaultTransaction()){
				fs.setTransaction(tx);
				fs.addFeatures( DataUtilities.collection(reprojected) );
				tx.commit();
			}
			
			ZipUtil.createZip(Files.list(outDirectory).collect(Collectors.toList()), zipFile);
			try {
				FileUtils.deleteDirectory(outDirectory.toAbsolutePath().normalize().toFile());
			}catch(IOException ex) {
				logger.log(Level.WARNING, ex.getMessage(), ex);
			}
			
			return zipFile;
		}catch (Exception ex) {
			throw new IOException(ex);
		}
	}
	
}
