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
import org.wcs.smart.query.model.QueryColumn;

import au.com.bytecode.opencsv.CSVWriter;

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
}
