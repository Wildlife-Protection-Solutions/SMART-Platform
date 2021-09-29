/*
 * Copyright (C) 2021 Wildlife Conservation Society
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
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.ws.rs.WebApplicationException;

import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.store.ReprojectingFeatureCollection;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.referencing.CRS;
import org.hibernate.Session;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.IProjectionProvider;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.connect.query.engine.AbstractDbFeatureResultSet;
import org.wcs.smart.connect.query.engine.i2.ConnectIntelObservationResultItem;
import org.wcs.smart.connect.query.engine.i2.IntelObservationQueryResults;
import org.wcs.smart.i2.query.IQueryColumn;
import org.wcs.smart.i2.query.PagedResultSetIterator;
import org.wcs.smart.query.common.engine.IQueryResult;
import org.wcs.smart.query.common.engine.IQueryResultSetIterator;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.util.GeometryUtils;

/**
 * GeoJson query exporter that exports using a stream.
 * 
 * @author Emily
 *
 */
public class GeoJsonStreamingExporter extends AbstractQueryExporter{

	public static final String FORMAT_KEY = "geojson"; //$NON-NLS-1$
	
	public static final String getName(Locale l){
		return Messages.getString("GeoJsonExporter.GeoJson", l); //$NON-NLS-1$
	}
	
	private final Logger logger = Logger.getLogger(ShpExporter.class.getName());
	
	private SimpleQuery query;
	
	private AbstractDbFeatureResultSet<IResultItem> results;
	private IntelObservationQueryResults iresults;
	
	public GeoJsonStreamingExporter(SimpleQuery query, AbstractDbFeatureResultSet<IResultItem> results, IProjectionProvider prjProvider, Locale l, 
			ServletContext ctx){
		this(null, prjProvider, l, ctx);
		this.results = results;
		this.query = query;
	}
	
	public GeoJsonStreamingExporter(IntelObservationQueryResults iresults, IProjectionProvider prjProvider, Locale l, ServletContext ctx){
		super(prjProvider, l, ctx);
		this.iresults = iresults;
	}
	
	@Override
	public void write(OutputStream output) throws IOException, WebApplicationException {
		
		try(Session session = HibernateManager.openNewSession(context, locale)){
			session.beginTransaction();
			try {
				if (results != null) {
					exportResults(output, session);
				}else if (iresults != null) {
					writeIntelResults(output, session);
				}
				session.getTransaction().commit();
			}catch (Exception ex) {
				if (session.getTransaction().isActive()) session.getTransaction().rollback();
				logger.log(Level.SEVERE, ex.getMessage(),ex);
				throw new IOException(ex);
			}
		}finally {
			try(Session session = HibernateManager.openNewSession(context, locale)){
				session.beginTransaction();
				try {
					if (results != null) dispose(results, session);
					if (iresults != null) dispose((IQueryResult)iresults, session);
					session.getTransaction().commit();
				}catch (Exception ex) {
					if (session.getTransaction().isActive()) session.getTransaction().rollback();
					logger.log(Level.SEVERE, ex.getMessage(),ex);
					throw new IOException(ex);
				}
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
	private void exportResults(OutputStream output, Session session) throws IOException{
		try {
		
			FeatureJSON fjson = new FeatureJSON();
			fjson.setEncodeFeatureCollectionCRS(true);
				
			List<QueryColumn> columns = results.getQueryColumns(query, locale, session, prjProvider);
			
			SimpleFeatureType type = DataUtilities.createType("smartqueryresults", results.getFeatureSchemaDef(columns, false, false)); //$NON-NLS-1$
			ArrayList<SimpleFeature> features = new ArrayList<SimpleFeature>();
					
			IQueryResultSetIterator<IResultItem> itemiterator = results.iterator(500, session);
			for (Iterator<IResultItem> iterator = itemiterator; iterator.hasNext();) {
				IResultItem resultItem = (IResultItem) iterator.next();	
				SimpleFeature sf = results.toFeature(resultItem, columns, session, type);
				features.add(sf);
	
			}
			SimpleFeatureCollection collection = DataUtilities.collection(features);
	
			if (prjProvider != null && !CRS.equalsIgnoreMetadata(GeometryUtils.SMART_CRS, prjProvider.getProjection().getParsedCoordinateReferenceSystem())){
				collection = new ReprojectingFeatureCollection(collection, prjProvider.getProjection().getParsedCoordinateReferenceSystem());
			}
			if(collection != null && collection.getSchema() != null){
				fjson.writeFeatureCollection(collection, new OutputStreamWriter(output, StandardCharsets.UTF_8));
			}else{
				output.write("{}".getBytes(StandardCharsets.UTF_8)); //$NON-NLS-1$
			}
		}catch (Exception ex) {
			throw new IOException(ex);
		}

	}
	
	
	/**
	 * Exports advanced query results 
	 * 
	 */
	private void writeIntelResults(OutputStream output, Session session) throws IOException{
		try {
			FeatureJSON fjson = new FeatureJSON();
			fjson.setEncodeFeatureCollectionCRS(true);

			List<IQueryColumn> columns = iresults.getQueryColumns();
			SimpleFeatureType type = DataUtilities.createType("smartqueryresults", //$NON-NLS-1$
					iresults.getFeatureSchemaDef(columns, false));
			ArrayList<SimpleFeature> features = new ArrayList<SimpleFeature>();

			PagedResultSetIterator rs = iresults.iterator(session);
			while (rs.hasNext()) {
				ConnectIntelObservationResultItem resultItem = (ConnectIntelObservationResultItem) rs.next();
				SimpleFeature sf = iresults.toFeature(resultItem, columns, session, type);
				features.add(sf);
			}

			SimpleFeatureCollection collection = DataUtilities.collection(features);

			if (prjProvider != null && !CRS.equalsIgnoreMetadata(GeometryUtils.SMART_CRS,
					prjProvider.getProjection().getParsedCoordinateReferenceSystem())) {
				collection = new ReprojectingFeatureCollection(collection,
						prjProvider.getProjection().getParsedCoordinateReferenceSystem());
			}

			if (collection != null && collection.getSchema() != null) {
				fjson.writeFeatureCollection(collection, output);
			} else {
				output.write("{}".getBytes()); //$NON-NLS-1$
			}
		
		}catch (Exception ex) {
			throw new IOException(ex);
		}
	}

}

