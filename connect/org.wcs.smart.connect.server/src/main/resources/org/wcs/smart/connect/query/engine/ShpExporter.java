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
package org.wcs.smart.connect.query.engine;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.geotools.data.DataStore;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureStore;
import org.geotools.data.FileDataStoreFactorySpi;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.connect.ZipUtil;
import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.query.common.engine.IQueryResultSetIterator;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.query.model.QueryColumn;

/**
 * Exports query results to shapefiles.  Query results must extend
 * AbstractDbFeatureResultSet 
 * 
 * @author Emily
 *
 */
public class ShpExporter {
	
	public static final String FORMAT_KEY = "shp"; //$NON-NLS-1$
	
	public static final String getName(Locale l){
		return Messages.getString("ShpExporter.Shapefilename", l); //$NON-NLS-1$
	}
	
	private final Logger logger = Logger.getLogger(ShpExporter.class.getName());
	
	private Path zipFile;
	private Locale l;
	
	public ShpExporter(Path zipFile,  Locale l){
		this.zipFile = zipFile;
		this.l = l;
	}
	
	
	/**
	 * Exports simple queries whose results and represented by a database table.
	 * 
	 * @param query
	 * @param results
	 * @param session
	 * @throws IOException 
	 */
	public void exportResults(SimpleQuery query, AbstractDbFeatureResultSet results, Session session) throws IOException{
		//remove extension and add .shp to filename
		final String newName = FilenameUtils.removeExtension(zipFile.getFileName().toString()) ;
		final Path outDirectory = zipFile.getParent().resolve(String.valueOf(System.nanoTime()));
		Files.createDirectory(outDirectory);
		final Path shpfile = outDirectory.resolve(newName+ ".shp"); //$NON-NLS-1$
		
		session.doWork(new Work(){
			@SuppressWarnings("unchecked")
			@Override
			public void execute(Connection c) throws SQLException {
				try{
					FileDataStoreFactorySpi factory = FileDataStoreFinder.getDataStoreFactory("shp"); //$NON-NLS-1$
			        Map<String, Serializable> params = new HashMap<String, Serializable>();
					params.put(ShapefileDataStoreFactory.URLP.key, shpfile.toUri().toURL());
					
					List<QueryColumn> columns = query.getQueryColumns(l, session);
					DataStore shapefile = factory.createNewDataStore(params);
					SimpleFeatureType type = DataUtilities.createType("smartqueryresults", results.getFeatureSchemaDef(columns, false)); //$NON-NLS-1$
					
					ArrayList<SimpleFeature> features = new ArrayList<SimpleFeature>();
					
					IQueryResultSetIterator<? extends IResultItem> itemiterator = results.iterator(500, session);
					
					for (Iterator<IResultItem> iterator = itemiterator; iterator.hasNext();) {
						IResultItem resultItem = (IResultItem) iterator.next();
						
						SimpleFeature sf = results.toFeature(resultItem, columns, session, type);
						features.add(sf);
					}
					
					shapefile.createSchema(type);
					
					FeatureStore<SimpleFeatureType, SimpleFeature> fs = 
							(FeatureStore<SimpleFeatureType, SimpleFeature>) shapefile.getFeatureSource(shapefile.getTypeNames()[0]);
					fs.setTransaction(new DefaultTransaction());
					fs.addFeatures( DataUtilities.collection(features) );
					fs.getTransaction().commit();
				}catch (Exception ex){
					logger.log(Level.SEVERE, ex.getMessage(), ex);
					throw new SQLException(ex);
				}
			}			
		});
		
		ZipUtil.createZip(Files.newDirectoryStream(outDirectory), zipFile);
		
		FileUtils.deleteDirectory(outDirectory.toFile());
	}
	
}
