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
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
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
import org.wcs.smart.i2.query.IPagedQueryResultSet;
import org.wcs.smart.i2.query.IQueryColumn;
import org.wcs.smart.i2.query.PagedResultSetIterator;
import org.wcs.smart.query.common.engine.IQueryResultSetIterator;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.query.model.QueryColumn;

/**
 * Query HTML streaming exporter for table result sets.
 * 
 * @author Emily/Jeff
 *
 */
@SuppressWarnings("nls")
public class HtmlStreamingExporter extends AbstractQueryExporter{
	
	public static final String FORMAT_KEY = "html"; //$NON-NLS-1$
	
	public static final String getName(Locale l){
		return Messages.getString("HtmlExporter.HtmlName", l); //$NON-NLS-1$
	}

	
	private final Logger logger = Logger.getLogger(HtmlStreamingExporter.class.getName());
	
	protected String queryName;
	protected OutputStream output;
	
	private SimpleQuery query;
	private AbstractDbFeatureResultSet<IResultItem> results;
	private IPagedQueryResultSet presults;
	
	
	protected HtmlStreamingExporter(IProjectionProvider prjProvider, Locale l, ServletContext ctx, String queryName) {
		super(prjProvider, l, ctx);
		this.queryName = queryName;
	}
	
	public HtmlStreamingExporter(SimpleQuery query, 
			AbstractDbFeatureResultSet<IResultItem> results,
			IProjectionProvider prjProvider, Locale l, ServletContext ctx){
		this(prjProvider, l, ctx, query.getName());
		this.query = query;
		this.results = results;
	}
	
	public HtmlStreamingExporter(String queryName, 
			IPagedQueryResultSet results,
			IProjectionProvider prjProvider,
			Locale l, ServletContext ctx){
		this(prjProvider, l, ctx, queryName);
		this.presults = results;
	}
	
	protected void printHeader() throws IOException {
		
		StringBuilder htmlText = new StringBuilder();
		htmlText.append("<html>");
		htmlText.append("<head>");
		htmlText.append("<style>");
		htmlText.append("table { border-collapse: collapse; width: 50%; } th, td { text-align: left; padding: 8px; } tr:nth-child(even){background-color: #f2f2f2}");
		htmlText.append("tr:hover {background-color: #e2f4ff;}");
		htmlText.append("</style>");
		htmlText.append("<link rel = stylesheet type = text/css href = smart.css>");
		htmlText.append("<title>" + queryName + "</title>");
		htmlText.append("</head>");
		htmlText.append("<body>");
		
		writeString(htmlText.toString());
	}

	protected void writeString(String string) throws IOException {
		output.write(string.getBytes(StandardCharsets.UTF_8));
	}

	@Override
	public void write(OutputStream output) throws IOException, WebApplicationException {
		if (results != null) {
			writeResults(output);
		}else if (presults != null) {
			writePagedResults(output);
		}
	}
	
	private void writeResults(OutputStream output) throws IOException{
		this.output = output;
		try(Session session = HibernateManager.openNewSession(context, locale)){
			session.beginTransaction();
			try {
				IProjectionProvider prj = ProjectionUtils.INSTANCE.createProjectionProvider(session, query.getConservationArea());
				List<QueryColumn> cols = results.getQueryColumns(query, locale, session, prj);
				
				printHeader();
					
				writeString("<table>");
				writeString("<tr>");
				//headers
				for (int i = 0; i < cols.size(); i ++){
					writeString("<th style='border: solid 1px grey;'>" + cols.get(i).getName() + "</th>");
				}
				writeString("</tr>");
					
				//get data and write
				IQueryResultSetIterator<IResultItem> itemiterator = results.iterator(500, session);
					
				for (Iterator<IResultItem> iterator = itemiterator; iterator.hasNext();) {
					IResultItem resultItem = (IResultItem) iterator.next();
					
					writeString("<tr>");
					for (int i = 0; i < cols.size(); i ++){
						writeString("<td style='border: solid 1px grey;'>" + results.getValueAsString(resultItem, cols.get(i), session) + "</td>");
					}
					writeString("</tr>");
				}					
		
				writeString("</table></body></html>");
	
				dispose(this.results, session);
				session.getTransaction().commit();
			}catch (Exception ex) {
				session.getTransaction().rollback();
				throw ex;
			}
		}catch (Exception ex){
			logger.log(Level.SEVERE, ex.getMessage(), ex);
			throw new IOException(ex);
		}
	}
	
	private void writePagedResults(OutputStream output) throws IOException{
		this.output = output;
		try(Session session = HibernateManager.openNewSession(context, locale)){
			session.beginTransaction();
			try {

				printHeader();

				writeString("<table>");
				writeString("<tr>");
				
				List<IQueryColumn> cols = presults.getQueryColumns();
				for (int i = 0; i < cols.size(); i ++){
					writeString("<th style='border: solid 1px grey;'>" + cols.get(i).getColumnName() + "</th>");
				}
				writeString("</tr>");
				
				//get data and write
				PagedResultSetIterator rs = presults.iterator(session);
				while(rs.hasNext()) {
					org.wcs.smart.i2.query.IResultItem resultItem = rs.next();
					
					writeString("<tr>");
					for (int i = 0; i < cols.size(); i ++){
						String text = cols.get(i).getValue(resultItem, locale);
						writeString("<td style='border: solid 1px grey;'>" + text + "</td>");
					}
					writeString("</tr>");
				}					

				writeString("</table></body></html>");	
	
				dispose(this.presults, session);
				session.getTransaction().commit();
			}catch (Exception ex) {
				session.getTransaction().rollback();
				throw ex;
			}
		}catch (Exception ex){
			logger.log(Level.SEVERE, ex.getMessage(), ex);
			throw new IOException(ex);
		}
	}
	
}
