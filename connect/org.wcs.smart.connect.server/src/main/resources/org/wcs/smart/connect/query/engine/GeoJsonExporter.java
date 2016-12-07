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

package org.wcs.smart.connect.query.engine;


import java.io.IOException;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.operation.MathTransform;
import org.wcs.smart.ProjectionProvider;
import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.query.common.engine.IQueryResultSetIterator;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.util.GeometryUtils;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Exports query results to geoJson.  Query results must extend
 * AbstractDbFeatureResultSet 
 * 
 * @author Jeff
 *
 */
public class GeoJsonExporter {

	public static final String FORMAT_KEY = "geojson"; //$NON-NLS-1$
	
	private final Logger logger = Logger.getLogger(ShpExporter.class.getName());
	
	private ProjectionProvider prjProvider;
	private String geoJsonOutput;
	private Locale l;
	
	public GeoJsonExporter(Locale l, ProjectionProvider prjProvider){
		this.l = l;
		this.prjProvider = prjProvider;
	}

	
	public static final String getName(Locale l){
		return Messages.getString("GeoJsonExporter.GeoJson", l); //$NON-NLS-1$
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
		geoJsonOutput = new String();

		session.doWork(new Work(){
			@Override
			public void execute(Connection c) throws SQLException {
				try{
					FeatureJSON fjson = new FeatureJSON();
					StringWriter writer = new StringWriter();
					StringWriter crsWriter = new StringWriter();

					List<QueryColumn> columns = query.computeQueryColumns(l, session, prjProvider);
					SimpleFeatureType type = DataUtilities.createType("smartqueryresults", results.getFeatureSchemaDef(columns, false)); //$NON-NLS-1$
					ArrayList<SimpleFeature> features = new ArrayList<SimpleFeature>();
					
					IQueryResultSetIterator<? extends IResultItem> itemiterator = results.iterator(500, session);
					for (Iterator<IResultItem> iterator = itemiterator; iterator.hasNext();) {
						IResultItem resultItem = (IResultItem) iterator.next();	
						SimpleFeature sf = results.toFeature(resultItem, columns, session, type);
						features.add(sf);

					}
					//reproject 
					List<SimpleFeature> reprojected = new ArrayList<SimpleFeature>();
					if (prjProvider == null || CRS.equalsIgnoreMetadata(GeometryUtils.SMART_CRS, prjProvider.getProjection().getParsedCoordinateReferenceSystem())){
						reprojected = features;
					}else{
						MathTransform transform = CRS.findMathTransform(GeometryUtils.SMART_CRS, prjProvider.getProjection().getParsedCoordinateReferenceSystem(), true);
						for (SimpleFeature f : features){
							SimpleFeature copy = SimpleFeatureBuilder.copy(f);
							copy.setDefaultGeometry(JTS.transform((Geometry)f.getDefaultGeometry(), transform));
							reprojected.add(copy);
						}
						
					}
					fjson.writeCRS(prjProvider.getProjection().getParsedCoordinateReferenceSystem(), crsWriter); //write the new projection into JSON
					SimpleFeatureCollection collection = DataUtilities.collection(reprojected);
					if(collection != null && collection.getSchema() != null){
						fjson.writeFeatureCollection(collection, writer);
						geoJsonOutput = writer.toString();
						if(crsWriter != null){//add the project to the JSON, the JSON library kinda sucks and has no way to write it in automatically from what I can tell...
							geoJsonOutput = "{\"crs\":" + crsWriter.toString() + "," + geoJsonOutput.substring(1);  //the substring pulls off the opening bracket of the json. Replaces the { at the start and add the crs value/object into the json   
						}
					}else{
						geoJsonOutput = "{}";
					}
					
				}catch (Exception ex){
					logger.log(Level.SEVERE, ex.getMessage(), ex);
					throw new SQLException(ex);
				}
			}			
		});
		

	}
	
	public String getGeoJsonOutput(){
		return geoJsonOutput;
	}

}


