/*
 * Copyright (C) 2012 Wildlife Conservation Society
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
package org.wcs.smart.conversion.csv.tool;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Set;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import au.com.bytecode.opencsv.CSVReader;

/**
 * Provides functionality that loads data frm CSV file to database.
 * 
 * @author elitvin
 * @since 3.2.0
 */
public class Csv2DbLoader {

	private static final Logger logger = LogManager.getLogger(Csv2DbLoader.class); 
	
	private char DELIMETER = ',';

	public void load(File file, Connection c) throws SQLException, IOException {
		cleanDb(c);
		parseFile(file, c);
	}

	public void cleanDb(Connection c) throws SQLException {
		c.createStatement().executeUpdate("TRUNCATE TABLE CSV_TO_SMART.ATTRIBUTES"); //$NON-NLS-1$
		try {
			c.createStatement().executeUpdate("DROP TABLE CSV_TO_SMART.CSV"); //$NON-NLS-1$
		} catch (SQLException e) {
			logger.info("Drop CSV table is not required"); //$NON-NLS-1$
			//ignore, table doesn't exist
		}
		c.createStatement().executeUpdate("create table csv_to_smart.csv (id integer not null, primary key (id))"); //$NON-NLS-1$
	}

	private void parseFile(File file, Connection c) throws SQLException, IOException {
		try(CSVReader reader = new CSVReader(new FileReader(file), DELIMETER)) { //$NON-NLS-1$
			boolean autoCommit = c.getAutoCommit();
			c.setAutoCommit(false);

			PreparedStatement stAttribute = c.prepareStatement("insert into csv_to_smart.attributes (id, n) values (?, ?)"); //$NON-NLS-1$
			
			String[] data = reader.readNext(); //read column names
			StringBuilder sbColumns = new StringBuilder();
			StringBuilder sbValues = new StringBuilder();
			Set<Integer> rowsToIgnore = new TreeSet<Integer>();
			int i, attrId;
			for (i = 0; i < data.length; i++) {
				attrId = i+1;
				sbColumns.append(", a"); //$NON-NLS-1$
				sbColumns.append(attrId);

				sbValues.append(", ?"); //$NON-NLS-1$
				
				stAttribute.setInt(1, attrId);
				stAttribute.setString(2, data[i]);
				stAttribute.executeUpdate();
				c.createStatement().execute("alter table csv_to_smart.csv add column a" + attrId + " varchar(1024)");  //$NON-NLS-1$//$NON-NLS-2$
				
				if (data[i] == null || data[i].trim().isEmpty()) {
					rowsToIgnore.add(attrId);
				}
			}
			c.commit();
			
			PreparedStatement stCsv = c.prepareStatement("insert into csv_to_smart.csv (id" + sbColumns + ") values (?" + sbValues + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			i = 1;
			//reading data
			while( (data = reader.readNext()) != null ) {
				stCsv.setInt(1, i);
				for (int j = 0; j < data.length; j++) {
					stCsv.setString(j+2, data[j]);
				}
				stCsv.executeUpdate();
				if (i % 256 == 0) {
					c.commit();
				}
				i++;
			}		
			
			c.commit();
			
			//removing columns that need to be ignored (empty columns - see #1644)
			if (!rowsToIgnore.isEmpty()) {
				logger.debug("removing columns that need to be ignored (empty columns)"); //$NON-NLS-1$
				StringBuilder sbIds = new StringBuilder();
				for (Integer x : rowsToIgnore) {
					if (sbIds.length() > 0) {
						sbIds.append(',');
					}
					sbIds.append(x);
					c.createStatement().execute("alter table csv_to_smart.csv drop column a" + x);  //$NON-NLS-1$
				}
				c.createStatement().execute("delete from csv_to_smart.attributes where id in (" + sbIds + ")");  //$NON-NLS-1$//$NON-NLS-2$
				c.commit();
			}
			
			c.setAutoCommit(autoCommit);
		}
	}
	
}
