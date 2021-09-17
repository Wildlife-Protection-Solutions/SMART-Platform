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
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;

import org.hibernate.Session;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.query.common.model.GriddedQuery;
import org.wcs.smart.query.common.model.QueryGridResultItem;
import org.wcs.smart.query.model.QueryColumn;

/**
 * Query HTML streaming exporter for exporting grid query results.
 * 
 * @author Emily/Jeff
 *
 */
@SuppressWarnings("nls")
public class HtmlStreamingGridQueryExporter implements StreamingOutput{
	
	public static final String FORMAT_KEY = "html"; //$NON-NLS-1$
	
	public static final String getName(Locale l){
		return Messages.getString("HtmlExporter.HtmlName", l); //$NON-NLS-1$
	}

	
	private final Logger logger = Logger.getLogger(HtmlStreamingGridQueryExporter.class.getName());
	
	private Locale l;
	private GriddedQuery query;
	private IMemoryTableResultSet<QueryGridResultItem> results ;
	private ServletContext ctx;
	
	private OutputStream output;
	
	public HtmlStreamingGridQueryExporter(GriddedQuery query, 
			IMemoryTableResultSet<QueryGridResultItem> results,
			Locale l, ServletContext ctx){
		this.l = l;
		this.ctx = ctx;
		this.query = query;
		this.results = results;
	}
	
	private void printHeader() throws IOException {
		
		StringBuilder htmlText = new StringBuilder();
		htmlText.append("<html>");
		htmlText.append("<head>");
		htmlText.append("<style>");
		htmlText.append("table { border-collapse: collapse; width: 50%; } th, td { text-align: left; padding: 8px; } tr:nth-child(even){background-color: #f2f2f2}");
		htmlText.append("tr:hover {background-color: #e2f4ff;}");
		htmlText.append("</style>");
		htmlText.append("<link rel = stylesheet type = text/css href = smart.css>");
		htmlText.append("<title>" + query.getName() + "</title>");
		htmlText.append("</head>");
		htmlText.append("<body>");
		
		writeString(htmlText.toString());
	}

	private void writeString(String string) throws IOException {
		output.write(string.getBytes());
	}

	@Override
	public void write(OutputStream output) throws IOException, WebApplicationException {
		this.output = output;
		try(Session session = HibernateManager.openNewSession(ctx, l)){
			session.beginTransaction();
			try {

				List<QueryColumn> cols = query.computeQueryColumns(l, session);
				
				printHeader();
					
				writeString("<table>");
				writeString("<tr>");
				//headers
				for (int i = 0; i < cols.size(); i ++){
					writeString("<th style='border: solid 1px grey;'>" + cols.get(i).getName() + "</th>");
				}
				writeString("</tr>");
					
				for (Iterator<QueryGridResultItem> iterator = results.getIterator(); iterator.hasNext();) {
					QueryGridResultItem item = (QueryGridResultItem) iterator.next();

					writeString("<tr>");
					for (int i = 0; i < cols.size(); i++) {
						writeString("<td style='border: solid 1px grey;'>" + results.getValueAsString(item, cols.get(i)) + "</td>");
					}
					writeString("</tr>");
				}				
		
				writeString("</table></body></html>");
	
				this.results.dispose(session);
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
