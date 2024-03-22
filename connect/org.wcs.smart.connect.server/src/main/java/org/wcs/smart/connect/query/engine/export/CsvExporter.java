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
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.ws.rs.WebApplicationException;

import org.hibernate.Session;
import org.wcs.smart.IProjectionProvider;
import org.wcs.smart.ProjectionUtils;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.connect.query.engine.AbstractDbFeatureResultSet;
import org.wcs.smart.connect.query.engine.IMemoryTableResultSet;
import org.wcs.smart.export.config.ICsvDataExporter;
import org.wcs.smart.i2.query.IQueryColumn;
import org.wcs.smart.i2.query.PagedResultSetIterator;
import org.wcs.smart.query.common.engine.IQueryResultSetIterator;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.common.model.GriddedQuery;
import org.wcs.smart.query.common.model.QueryGridResultItem;
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.query.common.model.SummaryQueryResult;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryColumn;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * Query CSV Exporter.  Exports table result sets, memory result sets,
 * and summary result sets.
 * 
 * @author Emily
 *
 */
public class CsvExporter extends AbstractQueryExporter {
	
	public static final String FORMAT_KEY = "csv"; //$NON-NLS-1$
	
	public static final String getName(Locale l){
		return Messages.getString("CsvExporter.CsvName", l); //$NON-NLS-1$
	}

	private final Logger logger = Logger.getLogger(CsvExporter.class.getName());
	
	private char delimiter;
	
	private Query query;
	private AbstractDbFeatureResultSet<IResultItem> results;
	private IMemoryTableResultSet<IResultItem> mresults;
	private SummaryQueryResult sresults;
	
	private org.wcs.smart.i2.query.IPagedQueryResultSet i2results;
	
	private GriddedQuery gquery;
	private IMemoryTableResultSet<QueryGridResultItem> gresults;
	
	private OutputStream output;
	
	protected CsvExporter(IProjectionProvider prjProvider, char delimiter, Locale l, ServletContext context){
		super(prjProvider, l, context);
		this.delimiter = delimiter;
	}
	
	public CsvExporter(Query query, SummaryQueryResult results, IProjectionProvider prjProvider, char delimiter, Locale l, ServletContext context){
		this(prjProvider, delimiter, l, context);
		this.query = query;
		this.sresults = results;
	}
	
	
	public CsvExporter(SimpleQuery query, AbstractDbFeatureResultSet<IResultItem> results, IProjectionProvider prjProvider, char delimiter, Locale l, ServletContext context){
		this(prjProvider, delimiter, l, context);
		this.query = query;
		this.results = results;
	}
	
	public CsvExporter(SimpleQuery query, IMemoryTableResultSet<IResultItem> results, IProjectionProvider prjProvider, char delimiter, Locale l, ServletContext context){
		this(prjProvider, delimiter, l, context);
		this.query = query;
		this.mresults = results;
	}
	
	public CsvExporter(org.wcs.smart.i2.query.IPagedQueryResultSet results, IProjectionProvider prjProvider, char delimiter, Locale l, ServletContext context){
		this(prjProvider, delimiter, l, context);
		this.i2results = results;
	}
	
	public CsvExporter(GriddedQuery query, IMemoryTableResultSet<QueryGridResultItem> results, IProjectionProvider prjProvider, char delimiter, Locale l, ServletContext context){
		this(prjProvider, delimiter, l, context);
		this.gquery = query;
		this.gresults = results;
	}
	
	@Override
	public void write(OutputStream output) throws IOException, WebApplicationException {
		this.output = output;
		try(Session session = HibernateManager.openNewSession(context, locale)){
			session.beginTransaction();
			
			try {
				if (results != null) {
					exportResults((SimpleQuery)query, results, session);
				}else if (i2results != null) {
					exportResults(i2results, session);
				}else if (gresults != null) {
					exportResults(gquery, gresults, session);
				}else if (mresults != null) {
					exportResults((SimpleQuery)query, mresults, session);
				}else if (sresults != null) {
					exportResults(query, sresults, session);
				}
			}finally {
				session.getTransaction().commit();
			}
		}finally {
		
			try(Session session = HibernateManager.openNewSession(context, locale)){
				session.beginTransaction();
				try {
					if (results != null) dispose(results, session);
					if (i2results != null) dispose(i2results, session);
					if (gresults != null) dispose(gresults, session);
					if (mresults != null) dispose(mresults, session);
					if (sresults != null) dispose(sresults, session);
					
					session.getTransaction().commit();
				}catch (Exception ex) {
					session.getTransaction().rollback();
					logger.log(Level.SEVERE, ex.getMessage(), ex);
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
	 */
	public void exportResults(SimpleQuery query, AbstractDbFeatureResultSet<IResultItem> results, Session session) throws IOException{
		
		IProjectionProvider prj = ProjectionUtils.INSTANCE.createProjectionProvider(session,
				query.getConservationArea());
		List<QueryColumn> cols = results.getQueryColumns(query, locale, session, prj);
		
		//remove the default query column from the query results
		for (Iterator<QueryColumn> iterator = cols.iterator(); iterator.hasNext();) {
			QueryColumn queryColumn = (QueryColumn) iterator.next();
			if (queryColumn.isDefaultGeometryColumn()) iterator.remove();
			
		}
		try (CSVWriter writer = new CSVWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8), delimiter)) {

			// headers
			String[] data = new String[cols.size()];
			for (int i = 0; i < cols.size(); i++) {
				data[i] = cols.get(i).getName();
			}
			writer.writeNext(data);

			// get data and write
			IQueryResultSetIterator<IResultItem> itemiterator = results.iterator(500, session);

			for (Iterator<IResultItem> iterator = itemiterator; iterator.hasNext();) {
				IResultItem resultItem = (IResultItem) iterator.next();

				data = new String[cols.size()];
				for (int i = 0; i < cols.size(); i++) {
					data[i] = results.getValueAsString(resultItem, cols.get(i), session, this.locale, false);
				}
				ICsvDataExporter.removeLineFeeds(data);
				writer.writeNext(data);

			}
		}
	}
	
	/**
	 * Exports advanced query results 
	 * 
	 */
	public void exportResults(org.wcs.smart.i2.query.IPagedQueryResultSet results, Session session) throws IOException{
			
		try (CSVWriter writer = new CSVWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8) ,delimiter)) {
				
			List<IQueryColumn> cols = results.getQueryColumns();
				
			//headers
			String[] data = new String[cols.size()];
			for (int i = 0; i < cols.size(); i ++){
				data[i] = cols.get(i).getColumnName();
			}
			writer.writeNext(data);
				
			//get data and write
			PagedResultSetIterator rs = results.iterator(session);
			while(rs.hasNext()) {
				org.wcs.smart.i2.query.IResultItem resultItem = rs.next();
				data = new String[cols.size()];
				for (int i = 0; i < cols.size(); i ++){
					data[i] = cols.get(i).getValue(resultItem, locale);
				}
				writer.writeNext(data);
			}
		}
		
	}
	
	/**
	 * Exports simple queries whose results and represented by a database table.
	 * 
	 * @param query
	 * @param results
	 * @param session
	 */
	public void exportResults(GriddedQuery query, IMemoryTableResultSet<QueryGridResultItem> results, Session session) throws IOException{
	
		try (CSVWriter writer = new CSVWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8), delimiter)) {
			
			List<QueryColumn> cols = query.computeQueryColumns(locale, session);
			
			String[] data = new String[cols.size()];
			for (int i = 0; i < cols.size(); i++) {
				data[i] = cols.get(i).getName();
			}
			writer.writeNext(data);
	
			for (Iterator<QueryGridResultItem> iterator = results.getIterator(); iterator.hasNext();) {
				
			
				QueryGridResultItem item = (QueryGridResultItem) iterator.next();
			
				data = new String[cols.size()];
				for (int i = 0; i < cols.size(); i++) {
					data[i] = results.getValueAsString(item, cols.get(i));
				}
				writer.writeNext(data);
			}
		}catch(SQLException ex) {
			throw new IOException(ex);
		}
	}
	
	/**
	 * Exports simple queries whose results are represented by a memory collection.
	 * 
	 * @param query
	 * @param results
	 * @param session
	 * @throws SQLException
	 */
	public void exportResults(SimpleQuery query, 
			IMemoryTableResultSet<IResultItem> results, Session session) throws IOException{
		
		try (CSVWriter writer = new CSVWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8), delimiter)) {
	
			IProjectionProvider prj = ProjectionUtils.INSTANCE.createProjectionProvider(session, query.getConservationArea());
			List<QueryColumn> cols = results.getQueryColumns(query, locale, session, prj);
				
			String[] data = new String[cols.size()];
			for (int i = 0; i < cols.size(); i++) {
				data[i] = cols.get(i).getName();
			}
			writer.writeNext(data);
	
			for (Iterator<? extends IResultItem> iterator = results.getIterator(); iterator.hasNext();) {
				IResultItem item = iterator.next();
				data = new String[cols.size()];
				for (int i = 0; i < cols.size(); i++) {
					data[i] = results.getValueAsString(item, cols.get(i));
				}
				writer.writeNext(data);
			}
		}catch (SQLException ex) {
			throw new IOException (ex);
		}
	}
	
	/**
	 * Exports summary query results.
	 * 
	 * @param query
	 * @param results
	 * @param session
	 * @throws SQLException
	 */
	public void exportResults(Query query, 
			SummaryQueryResult results, Session session) throws IOException{
		
		try(CSVWriter writer = new CSVWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8), delimiter, '"',"\n")){   //$NON-NLS-1$
			
			for (int i = 0; i < results.getColumnHeaderValues().length; i ++){
				String[] data = new String[results.getNumDataColumns() + results.getRowHeaders().size()];
				
				for (int j = 0; j < results.getRowHeaders().size(); j ++){
					data[j] = ""; //$NON-NLS-1$	
				}
				for (int k = 0; k < results.getColumnHeaderValues()[i].length; k ++){
					data[k + results.getRowHeaders().size()]= results.getColumnHeaderValues()[i][k].getName();
				}
				writer.writeNext(data);
			}
				
			//row headers & data
			for (int i = 0; i < results.getNumDataRows(); i ++){
				String[] data = new String[results.getNumDataColumns() + results.getRowHeaders().size()];
				for (int j = 0; j < results.getRowHeaders().size(); j++){
					data[j] = results.getRowHeaderValues()[i][j].getName();
				}
				for(int k = 0; k < results.getData()[i].length; k ++){
					if (results.getData()[i][k] == null){
						data[results.getRowHeaders().size() + k] = null;
					}else {
						int vindex = k % results.getValueHeaders().size();
						Function<Double, String> formatter = results.getValueHeaders().get(vindex).getFormatter();
						if (formatter == null) {
							data[results.getRowHeaders().size() + k] = String.valueOf(results.getData()[i][k]);
						}else {
							data[results.getRowHeaders().size() + k] = formatter.apply(results.getData()[i][k]);
						}
					}
				}
				writer.writeNext(data);
			}
		}
	
	}
}
