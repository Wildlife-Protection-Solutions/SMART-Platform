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
import java.util.Locale;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;

import org.hibernate.Session;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.query.common.model.SummaryQueryResult;
import org.wcs.smart.query.model.Query;

/**
 * Query HTML streaming exporter for exporting summary query results.
 * 
 * @author Emily/Jeff
 *
 */
@SuppressWarnings("nls")
public class HtmlStreamingSummaryQueryExporter implements StreamingOutput{
	
	public static final String FORMAT_KEY = "html"; //$NON-NLS-1$
	
	public static final String getName(Locale l){
		return Messages.getString("HtmlExporter.HtmlName", l); //$NON-NLS-1$
	}

	
	private final Logger logger = Logger.getLogger(HtmlStreamingSummaryQueryExporter.class.getName());
	
	private Locale l;
	private SummaryQueryResult results ;
	private org.wcs.smart.i2.query.SummaryQueryResult results2;
	private ServletContext ctx;
	private String queryName;
	
	private OutputStream output;
	
	public HtmlStreamingSummaryQueryExporter(Query query, 
			SummaryQueryResult results,
			Locale l, ServletContext ctx){
		this.l = l;
		this.ctx = ctx;
		this.results = results;
		this.queryName = query.getName();
	}
	
	public HtmlStreamingSummaryQueryExporter(String queryName,
			org.wcs.smart.i2.query.SummaryQueryResult results,
			Locale l, ServletContext ctx){
		this.l = l;
		this.ctx = ctx;
		this.results2 = results;
		this.queryName = queryName;
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
		htmlText.append("<title>" + queryName + "</title>");
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
		
			try(Session session = HibernateManager.openNewSession(ctx, l)){
				session.beginTransaction();
				try {
					this.results.dispose(session);
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
			
			try(Session session = HibernateManager.openNewSession(ctx, l)){
				session.beginTransaction();
				try {
					this.results2.dispose(session);
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
