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
package org.wcs.smart.connect.query.engine.patrol;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.connect.query.engine.IDbTableResultSet;
import org.wcs.smart.connect.query.engine.IMemoryTableResultSet;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.query.common.model.SummaryQuery;
import org.wcs.smart.query.common.model.SummaryQueryResult;
import org.wcs.smart.query.model.QueryColumn;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * Query CSV Exporter.  Exports table result sets, memory result sets,
 * and summary result sets.
 * 
 * @author Emily
 *
 */
public class CsvExporter {
	private final Logger logger = Logger.getLogger(CsvExporter.class.getName());
	
	private File f;
	private char delimiter;
	private Locale l;
	
	public CsvExporter(File f, char delimiter, Locale l){
		this.f = f;
		this.delimiter = delimiter;
		this.l = l;
	}
	
	/**
	 * Exports simple queries whose results and represented by a database table.
	 * 
	 * @param query
	 * @param results
	 * @param session
	 */
	public void exportResults(SimpleQuery query, IDbTableResultSet results, Session session){
		session.doWork(new Work(){
			@Override
			public void execute(Connection c) throws SQLException {
				
				try (CSVWriter writer = new CSVWriter(
						new OutputStreamWriter(
			              new FileOutputStream(f.getAbsolutePath()), "utf-8")
						,delimiter)) {
					
					List<QueryColumn> cols = query.getQueryColumns(l, session);
					
					try (ResultSet rs = results.getQueryResultSet(c)){
						String[] data = new String[cols.size()];
						for (int i = 0; i < cols.size(); i ++){
							data[i] = cols.get(i).getName();
						}
						writer.writeNext(data);
					
						while(rs.next()){
							data = new String[cols.size()];
							for (int i = 0; i < cols.size(); i ++){
								data[i] = results.getValueAsString(rs, cols.get(i), c);
							}
							writer.writeNext(data);
						}
					}
				}catch (Exception ex){
					logger.log(Level.SEVERE, ex.getMessage(), ex);
					throw new SQLException(ex);
				}
			}			
		});
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
			IMemoryTableResultSet<IResultItem> results, 
			Session session) throws SQLException{
		
		try (CSVWriter writer = new CSVWriter(new OutputStreamWriter(
				new FileOutputStream(f.getAbsolutePath()), "utf-8"), delimiter)) {

			List<QueryColumn> cols = query.getQueryColumns(l, session);

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
	public void exportResults(SummaryQuery query, 
			SummaryQueryResult results, 
			Session session) throws SQLException{
		
		try(CSVWriter writer = new CSVWriter(
				new OutputStreamWriter(new FileOutputStream(f.getAbsoluteFile()), "UTF-8"), //$NON-NLS-1$ 
				delimiter, '"',delimiter)){ 
		
			for (int i = 0; i < results.getColumnHeaders().size(); i ++){
				String[] data = new String[results.getNumDataColumns() + results.getRowHeaders().size()];
			
				for (int j = 0; j < results.getRowHeaders().size(); j ++){
					data[j] = ""; //$NON-NLS-1$
				}
				for (int k = 0; k < results.getColumnHeaderValues()[i].length; k ++){
					data[k + results.getRowHeaders().size()]= results.getColumnHeaderValues()[i][k].getName();
				}
				
				writer.writeNext(data);
			}
			
			//value headers
			String[] data = new String[results.getNumDataColumns() + results.getRowHeaders().size()];
			for (int j = 0; j < results.getRowHeaders().size(); j ++){
				data[j] = ""; //$NON-NLS-1$
			}
			for (int k = 0; k < results.getValueHeaders().size(); k ++){
				data[k + results.getRowHeaders().size()]= results.getValueHeaders().get(k).getName();
			}	
			writer.writeNext(data);
		
			//row headers & data
			for (int i = 0; i < results.getNumDataRows(); i ++){
				data = new String[results.getNumDataColumns() + results.getRowHeaders().size()];
				for (int j = 0; j < results.getRowHeaders().size(); j++){
					data[j] = results.getRowHeaderValues()[i][j].getName();
				}
				for(int k = 0; k < results.getData()[i].length; k ++){
					if (results.getData()[i][k] == null){
						data[results.getRowHeaders().size() + k] = null;
					}else{
						data[results.getRowHeaders().size() + k] = String.valueOf(results.getData()[i][k]);
					}
				}
				writer.writeNext(data);
			}
		}catch (Exception ex){
			logger.log(Level.SEVERE, ex.getMessage(), ex);
			throw new SQLException(ex);
		}
	
	}
}
