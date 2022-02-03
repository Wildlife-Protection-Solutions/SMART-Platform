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
import java.util.Locale;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.ws.rs.WebApplicationException;

import org.hibernate.Session;
import org.wcs.smart.IProjectionProvider;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.query.common.model.SummaryQueryResult;
import org.wcs.smart.query.model.Query;

/**
 * Query HTML streaming exporter for exporting summary query results.
 * 
 * @author Emily/Jeff
 *
 */
@SuppressWarnings("nls")
public class HtmlStreamingSummaryQueryExporter extends HtmlStreamingExporter {
	
	
	private final Logger logger = Logger.getLogger(HtmlStreamingSummaryQueryExporter.class.getName());
	
	private SummaryQueryResult results ;
	private org.wcs.smart.i2.query.SummaryQueryResult results2;
	
	
	public HtmlStreamingSummaryQueryExporter(Query query, 
			SummaryQueryResult results,
			IProjectionProvider prjProvider,
			Locale l, ServletContext ctx){
		super(prjProvider, l, ctx, query.getName());
		this.results = results;
	}
	
	public HtmlStreamingSummaryQueryExporter(String queryName,
			org.wcs.smart.i2.query.SummaryQueryResult results,
			IProjectionProvider prjProvider,
			Locale l, ServletContext ctx){
		super(prjProvider, l, ctx, queryName);
		this.results2 = results;
	}
	
	@Override
	public void write(OutputStream output) throws IOException, WebApplicationException {
		this.output = output;
		if (results != null) {
			writeResults();
		}else if (results2 != null) {
			writeResults2();
		}
	}
	
	private void writeResults() throws IOException {	
		try {
			printHeader();
			writeString("<table>");
			
			for (int i = 0; i < results.getColumnHeaderValues().length; i ++){
				writeString("<tr>");
				for (int j = 0; j < results.getRowHeaders().size(); j++){
					writeString("<td style='border: solid 1px grey;'></td>");
				}
				for (int k = 0; k < results.getColumnHeaderValues()[i].length; k ++){
					writeString("<th style='border: solid 1px grey;'>" + results.getColumnHeaderValues()[i][k].getName() + "</th>");
				}
				writeString("</tr>");
			}
				
			//row headers & data
			for (int i = 0; i < results.getNumDataRows(); i ++){
				writeString("<tr>");
				for (int j = 0; j < results.getRowHeaders().size(); j++){
					writeString("<td style='border: solid 1px grey;'>" + results.getRowHeaderValues()[i][j].getName() + "</td>");
				}
				for(int k = 0; k < results.getData()[i].length; k ++){
					if (results.getData()[i][k] == null){
						writeString("<td style='border: solid 1px grey;'></td>");
					}else{
						
						int vindex = k % results.getValueHeaders().size();
						Function<Double, String> formatter = results.getValueHeaders().get(vindex).getFormatter();
						if (formatter == null) {
							formatter = results.getValueHeaders().get(vindex).getUiFormatter();
						}
						String value = "";
						if (formatter == null) {
							value = String.valueOf(results.getData()[i][k]);
						}else {
							value = formatter.apply(results.getData()[i][k]);
						}
						writeString("<td style='border: solid 1px grey;'>" + String.valueOf(value) + "</td>");
						
						
					}
				}
				writeString("</tr>");
			}
		
			try(Session session = HibernateManager.openNewSession(context, locale)){
				session.beginTransaction();
				try {
					dispose(this.results, session);
					session.getTransaction().commit();
				}catch (Exception ex) {
					session.getTransaction().rollback();
					throw ex;
				}
			}
		}catch (Exception ex){
			logger.log(Level.SEVERE, ex.getMessage(), ex);
			throw new IOException(ex);
		}
	}
	
	
	private void writeResults2() throws IOException {
	
		printHeader();
		writeString("<table>");
		writeString("<tr>");
		
		try{
			for (int i = 0; i < results2.getColumnHeaderValues().length; i ++){
				for (int j = 0; j < results2.getRowHeaders().size(); j++){
					writeString("<td style='border: solid 1px grey;'></td>");
				}
				for (int k = 0; k < results2.getColumnHeaderValues()[i].length; k ++){
					writeString("<th style='border: solid 1px grey;'>" + results2.getColumnHeaderValues()[i][k].getName() + "</th>");
				}
				writeString("</tr>");
			}
			
			//row headers & data
			for (int i = 0; i < results2.getNumDataRows(); i ++){
				writeString("<tr>");
				for (int j = 0; j < results2.getRowHeaders().size(); j++){
					writeString("<td style='border: solid 1px grey;'>" + results2.getRowHeaderValues()[i][j].getName() + "</td>");
				}
				for(int k = 0; k < results2.getData()[i].length; k ++){
					if (results2.getData()[i][k] == null){
						writeString("<td style='border: solid 1px grey;'></td>");
					}else{
						writeString("<td style='border: solid 1px grey;'>" + String.valueOf(results2.getData()[i][k]) + "</td>");
					}
				}
				writeString("</tr>");
			}
			
			try(Session session = HibernateManager.openNewSession(context, locale)){
				session.beginTransaction();
				try {
					dispose(this.results2, session);
					session.getTransaction().commit();
				}catch (Exception ex) {
					session.getTransaction().rollback();
					throw ex;
				}
			}
		}catch (Exception ex){
			logger.log(Level.SEVERE, ex.getMessage(), ex);
			throw new IOException(ex);
		}
	
	}
}
