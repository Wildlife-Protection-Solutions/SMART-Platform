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
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.ws.rs.WebApplicationException;

import org.hibernate.Session;
import org.wcs.smart.IProjectionProvider;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.connect.query.engine.IMemoryTableResultSet;
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
public class HtmlStreamingGridQueryExporter extends HtmlStreamingExporter {
	
	private final Logger logger = Logger.getLogger(HtmlStreamingGridQueryExporter.class.getName());
	
	private GriddedQuery query;
	private IMemoryTableResultSet<QueryGridResultItem> results ;
	
	public HtmlStreamingGridQueryExporter(GriddedQuery query, 
			IMemoryTableResultSet<QueryGridResultItem> results,
			IProjectionProvider prjProvider, 
			Locale l, ServletContext ctx){
		super(prjProvider, l, ctx, query.getName());
		this.query = query;
		this.results = results;
	}
	

	@Override
	public void write(OutputStream output) throws IOException, WebApplicationException {
		this.output = output;
		try(Session session = HibernateManager.openNewSession(context, locale)){
			session.beginTransaction();
			try {

				List<QueryColumn> cols = query.computeQueryColumns(locale, session);
				
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
	
}
