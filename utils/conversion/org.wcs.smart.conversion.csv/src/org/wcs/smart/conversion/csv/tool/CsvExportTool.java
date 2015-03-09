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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * Class allows to export current database into csv file.
 * 
 * @author elitvin
 * @since 3.2.0
 */
public class CsvExportTool {
	
	public void export(File file, Connection c) throws SQLException, UnsupportedEncodingException, FileNotFoundException, IOException {
		try (CSVWriter writer = new CSVWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"), ',', '"',System.getProperty("line.separator"))) { //$NON-NLS-1$ //$NON-NLS-2$ 
			// WriteHeaders
			List<String> headerColumns = new ArrayList<>();
			ResultSet rs = c.createStatement().executeQuery("select n, id from csv_to_smart.attributes order by id"); //$NON-NLS-1$
			while (rs.next()) {
				headerColumns.add(rs.getString(1));
			}
			rs.close();
			int size = headerColumns.size();
			writer.writeNext(headerColumns.toArray(new String[size]));
			
			//for each row write one record
			rs = c.createStatement().executeQuery("select * from csv_to_smart.csv"); //$NON-NLS-1$
			while (rs.next()) {
				headerColumns.clear();
				for (int i = 0; i < size; i++) {
					headerColumns.add(rs.getString(i+2)); //skip id
				}
				writer.writeNext(headerColumns.toArray(new String[size]));
			}
			rs.close();
		}
		
	}

}
