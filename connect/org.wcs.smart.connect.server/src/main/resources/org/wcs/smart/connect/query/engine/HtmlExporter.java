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

import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.Session;
import org.wcs.smart.IProjectionProvider;
import org.wcs.smart.ProjectionUtils;
import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.connect.query.engine.i2.ConnectIntelObservationResultItem;
import org.wcs.smart.connect.query.engine.i2.IntelObservationQueryResults;
import org.wcs.smart.i2.query.IPagedQueryResultSet;
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

/**
 * Query HTML Exporter.  Exports table result sets, memory result sets,
 * and summary result sets.
 * 
 * @author Emily/Jeff
 *
 */
@SuppressWarnings("nls")
public class HtmlExporter {
	
	public static final String FORMAT_KEY = "html"; //$NON-NLS-1$
	
	public static final String getName(Locale l){
		return Messages.getString("HtmlExporter.HtmlName", l); //$NON-NLS-1$
	}

	
	private final Logger logger = Logger.getLogger(HtmlExporter.class.getName());
	
	private Locale l;
	private StringBuilder htmlText;
	
	public HtmlExporter(Locale l){
		this.l = l;
		htmlText = new StringBuilder();
		htmlText.append("<html>"
				+ "<head>"
				+ "<style>"
				+ "table { border-collapse: collapse; width: 50%; } th, td { text-align: left; padding: 8px; } tr:nth-child(even){background-color: #f2f2f2}"
				+ "tr:hover {background-color: #e2f4ff;}"
				+ "</style>"
				+ "<link rel = stylesheet type = text/css href = smart.css>"
				+ "<title>Query Results</title>"
				+ "</head>"
				+ "<body>");
	}
	
	/**
	 * Exports simple queries whose results and represented by a database table.
	 * 
	 * @param query
	 * @param results
	 * @param session
	 */
	public void exportResults(SimpleQuery query, AbstractDbFeatureResultSet results, Session session) throws Exception{
		IProjectionProvider prj = ProjectionUtils.INSTANCE.createProjectionProvider(session, query.getConservationArea());
		List<QueryColumn> cols = query.computeQueryColumns(l, session, prj);
		
		try{
			htmlText.append("<table>");
			htmlText.append("<tr>");
			//headers
			for (int i = 0; i < cols.size(); i ++){
				htmlText.append("<th style='border: solid 1px grey;'>" + cols.get(i).getName() + "</th>");
			}
			htmlText.append("</tr>");
			
			//get data and write
			IQueryResultSetIterator<? extends IResultItem> itemiterator = results.iterator(500, session);
			
			for (Iterator<IResultItem> iterator = itemiterator; iterator.hasNext();) {
				IResultItem resultItem = (IResultItem) iterator.next();
				
				htmlText.append("<tr>");
				for (int i = 0; i < cols.size(); i ++){
					htmlText.append("<td style='border: solid 1px grey;'>" + results.getValueAsString(resultItem, cols.get(i), session) + "</td>");
				}
				htmlText.append("</tr>");
			}					

			htmlText.append("</table></body></html>");				
			
		}catch (Exception ex){
			logger.log(Level.SEVERE, ex.getMessage(), ex);
			throw ex;
		}
	}
	
	
	/**
	 * Exports advanced query results 
	 * 
	 */
	public void exportResults(IPagedQueryResultSet results, Session session) throws Exception{
		try{
			htmlText.append("<table>");
			htmlText.append("<tr>");
			//headers
			List<IQueryColumn> cols = results.getQueryColumns();
			for (int i = 0; i < cols.size(); i ++){
				htmlText.append("<th style='border: solid 1px grey;'>" + cols.get(i).getColumnName() + "</th>");
			}
			htmlText.append("</tr>");
			
			//get data and write
			PagedResultSetIterator rs = results.iterator(session);
			while(rs.hasNext()) {
				org.wcs.smart.i2.query.IResultItem resultItem = rs.next();
				
				htmlText.append("<tr>");
				for (int i = 0; i < cols.size(); i ++){
					String text = cols.get(i).getValue(resultItem, l);
					htmlText.append("<td style='border: solid 1px grey;'>" + text + "</td>");
				}
				htmlText.append("</tr>");
			}					

			htmlText.append("</table></body></html>");				
			
		}catch (Exception ex){
			logger.log(Level.SEVERE, ex.getMessage(), ex);
			throw ex;
		}
	}
	
	/**
	 * Exports simple queries whose results and represented by a database table.
	 * 
	 * @param query
	 * @param results
	 * @param session
	 */
	public void exportResults(GriddedQuery query, IMemoryTableResultSet<QueryGridResultItem> results, Session session) throws Exception{
		htmlText.append("<table>");
		htmlText.append("<tr>");

		List<QueryColumn> cols = query.computeQueryColumns(l, session);
		for (int i = 0; i < cols.size(); i++) {
			htmlText.append("<th style='border: solid 1px grey;'>" + cols.get(i).getName() + "</th>");
		}
		for (Iterator<QueryGridResultItem> iterator = results.getIterator(); iterator.hasNext();) {
			QueryGridResultItem item = (QueryGridResultItem) iterator.next();

			htmlText.append("<tr>");
			for (int i = 0; i < cols.size(); i++) {
				htmlText.append("<td style='border: solid 1px grey;'>" + results.getValueAsString(item, cols.get(i)) + "</td>");
			}
			htmlText.append("</tr>");
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
	public void exportResults(SimpleQuery query,IMemoryTableResultSet<IResultItem> results,Session session) throws SQLException{
		htmlText.append("<table>");
		htmlText.append("<tr>");

		try {

			IProjectionProvider prj = ProjectionUtils.INSTANCE.createProjectionProvider(session, query.getConservationArea());
			List<QueryColumn> cols = query.computeQueryColumns(l, session, prj);

			for (int i = 0; i < cols.size(); i++) {
				htmlText.append("<th style='border: solid 1px grey;'>" + cols.get(i).getName() + "</th>");
			}

			for (Iterator<? extends IResultItem> iterator = results.getIterator(); iterator.hasNext();) {
				htmlText.append("<tr>");
				IResultItem item = iterator.next();
				for (int i = 0; i < cols.size(); i++) {
					htmlText.append("<td style='border: solid 1px grey;'>" + results.getValueAsString(item, cols.get(i)) + "</td>");
				}
				htmlText.append("</tr>");
			}
			
		}catch (Exception ex){
			logger.log(Level.SEVERE, ex.getMessage(), ex);
			throw new SQLException(ex);
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
	public void exportResults(Query query,SummaryQueryResult results,Session session) throws SQLException{
		htmlText.append("<table>");
		htmlText.append("<tr>");
		
		try{
			for (int i = 0; i < results.getColumnHeaderValues().length; i ++){
				for (int j = 0; j < results.getRowHeaders().size(); j++){
					htmlText.append("<td style='border: solid 1px grey;'></td>");
				}
				for (int k = 0; k < results.getColumnHeaderValues()[i].length; k ++){
					htmlText.append("<th style='border: solid 1px grey;'>" + results.getColumnHeaderValues()[i][k].getName() + "</th>");
				}
				htmlText.append("</tr>");
			}
			
			//row headers & data
			for (int i = 0; i < results.getNumDataRows(); i ++){
				htmlText.append("<tr>");
				for (int j = 0; j < results.getRowHeaders().size(); j++){
					htmlText.append("<td style='border: solid 1px grey;'>" + results.getRowHeaderValues()[i][j].getName() + "</td>");
				}
				for(int k = 0; k < results.getData()[i].length; k ++){
					if (results.getData()[i][k] == null){
						htmlText.append("<td style='border: solid 1px grey;'></td>");
					}else{
						htmlText.append("<td style='border: solid 1px grey;'>" + String.valueOf(results.getData()[i][k]) + "</td>");
					}
				}
				htmlText.append("</tr>");
			}
		}catch (Exception ex){
			logger.log(Level.SEVERE, ex.getMessage(), ex);
			throw new SQLException(ex);
		}
	
	}
	
	public void exportResults(org.wcs.smart.i2.query.SummaryQueryResult results,Session session) throws SQLException{
		htmlText.append("<table>");
		htmlText.append("<tr>");
		
		try{
			for (int i = 0; i < results.getColumnHeaderValues().length; i ++){
				for (int j = 0; j < results.getRowHeaders().size(); j++){
					htmlText.append("<td style='border: solid 1px grey;'></td>");
				}
				for (int k = 0; k < results.getColumnHeaderValues()[i].length; k ++){
					htmlText.append("<th style='border: solid 1px grey;'>" + results.getColumnHeaderValues()[i][k].getName() + "</th>");
				}
				htmlText.append("</tr>");
			}
			
			//row headers & data
			for (int i = 0; i < results.getNumDataRows(); i ++){
				htmlText.append("<tr>");
				for (int j = 0; j < results.getRowHeaders().size(); j++){
					htmlText.append("<td style='border: solid 1px grey;'>" + results.getRowHeaderValues()[i][j].getName() + "</td>");
				}
				for(int k = 0; k < results.getData()[i].length; k ++){
					if (results.getData()[i][k] == null){
						htmlText.append("<td style='border: solid 1px grey;'></td>");
					}else{
						htmlText.append("<td style='border: solid 1px grey;'>" + String.valueOf(results.getData()[i][k]) + "</td>");
					}
				}
				htmlText.append("</tr>");
			}
		}catch (Exception ex){
			logger.log(Level.SEVERE, ex.getMessage(), ex);
			throw new SQLException(ex);
		}
	
	}
	
	public String getHtml(){
		return htmlText.toString();
	}
}
