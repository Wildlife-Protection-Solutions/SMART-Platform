package org.wcs.smart.connect.query.engine.patrol;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.util.SharedUtils;

import au.com.bytecode.opencsv.CSVWriter;

public class CsvExporter {

	private File f;
	private char delimiter;
	private Locale l;
	
	public CsvExporter(File f, char delimiter, Locale l){
		this.f = f;
		this.delimiter = delimiter;
		this.l = l;
	}
	
	public void exportResults(PsqlObservationEngine engine, Session session){
		session.doWork(new Work(){
			@Override
			public void execute(Connection c) throws SQLException {
				
				try (CSVWriter writer = new CSVWriter(
						new OutputStreamWriter(
			              new FileOutputStream(f.getAbsolutePath()), "utf-8")
						,delimiter)) {
					
					List<QueryColumn> cols = engine.getQuery().getQueryColumns(l, session);
					ResultSet rs = c.createStatement().executeQuery("SELECT * FROM " + engine.getQueryDataTable());
					
					String[] data = new String[cols.size()];
					for (int i = 0; i < cols.size(); i ++){
						data[i] = cols.get(i).getName();
					}
					writer.writeNext(data);
					
					while(rs.next()){
						data = new String[cols.size()];
						for (int i = 0; i < cols.size(); i ++){
							data[i] = engine.getValueAsString(rs, cols.get(i), c);
						}
						writer.writeNext(data);
					}
					rs.close();
				}catch (Exception ex){
					ex.printStackTrace();
					throw new SQLException(ex);
				}
			}			
		});
	}
}
