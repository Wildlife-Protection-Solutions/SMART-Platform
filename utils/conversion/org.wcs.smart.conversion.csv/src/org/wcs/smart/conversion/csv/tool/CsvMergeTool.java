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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.wcs.smart.conversion.csv.ui.CsvMergeMatchDialog;

import au.com.bytecode.opencsv.CSVReader;

/**
 * Tool that merges given csv file into the current database.
 * 
 * @author elitvin
 * @since 3.2.0
 */
public class CsvMergeTool {

	private char DELIMETER = ',';
	
	public List<String> merge(File file, Connection c) throws SQLException, UnsupportedEncodingException, FileNotFoundException, IOException {
		List<String> messages = new ArrayList<String>();
		
		
		try(CSVReader reader = new CSVReader(new InputStreamReader(new FileInputStream(file), "UTF-8"), DELIMETER)) { //$NON-NLS-1$
			boolean autoCommit = c.getAutoCommit();
			c.setAutoCommit(false);

			Map<String, Integer> n2Column = new HashMap<>();
			ResultSet rs = c.createStatement().executeQuery("select n, id from csv_to_smart.attributes order by id"); //$NON-NLS-1$
			while (rs.next()) {
				n2Column.put(rs.getString(1), rs.getInt(2));
			}
			rs.close();
			
			String[] colNames = reader.readNext(); //read column names
			
			List<String> matched = new ArrayList<>();
			List<String> unmatched = new ArrayList<>();
			for (String colName : colNames) {
				if (n2Column.containsKey(colName)) {
					matched.add(colName);
				} else {
					unmatched.add(colName);
				}
			}
			CsvMergeMatchDialog dialog = new CsvMergeMatchDialog(Display.getDefault().getActiveShell(), matched, unmatched);
			if (dialog.open() != Window.OK) {
				messages.add("Merge process was aborted by user.");
				return messages;
			}
			
			List<String> uniqueColumns = dialog.getSelection();
			
			PreparedStatement stAttribute = c.prepareStatement("insert into csv_to_smart.attributes (id, n) values (?, ?)"); //$NON-NLS-1$
			int shift = n2Column.size()+1;
			
			for (int i = 0; i < unmatched.size(); i++) {
				stAttribute.setInt(1, i+shift);
				stAttribute.setString(2, unmatched.get(i));
				stAttribute.executeUpdate();
				c.createStatement().execute("alter table csv_to_smart.csv add column a" + (i+shift) + " varchar(1024)");  //$NON-NLS-1$//$NON-NLS-2$
				
				n2Column.put(unmatched.get(i), i+shift);
			}
			c.commit();
			
			rs = c.createStatement().executeQuery("select max(id) from csv_to_smart.csv"); //$NON-NLS-1$
			rs.next();
			int lastCsvId = rs.getInt(1);
			rs.close();

			
			StringBuilder sbId = new StringBuilder();
			for (String col : uniqueColumns) {
				if (sbId.length() > 0) {
					sbId.append(" and "); //$NON-NLS-1$
				}
				sbId.append("a"); //$NON-NLS-1$
				sbId.append(n2Column.get(col));
				sbId.append("=?"); //$NON-NLS-1$
			}
			PreparedStatement stFetchId = c.prepareStatement("select id from csv_to_smart.csv where " + sbId); //$NON-NLS-1$

			StringBuilder sbInsertCol = new StringBuilder();
			StringBuilder sbInsertVal = new StringBuilder();
			StringBuilder sbUpdate = new StringBuilder();
			for (String col : colNames) {
				sbInsertCol.append(", a"); //$NON-NLS-1$
				sbInsertCol.append(n2Column.get(col));

				sbInsertVal.append(", ?"); //$NON-NLS-1$
				
				if (sbUpdate.length() > 0) {
					sbUpdate.append(", "); //$NON-NLS-1$
				}
				sbUpdate.append("a"); //$NON-NLS-1$
				sbUpdate.append(n2Column.get(col));
				sbUpdate.append("=?"); //$NON-NLS-1$
			}
			PreparedStatement stCsvInsert = c.prepareStatement("insert into csv_to_smart.csv (id" + sbInsertCol + ") values (?" + sbInsertVal + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			PreparedStatement stCsvUpdate = c.prepareStatement("update csv_to_smart.csv set " + sbUpdate + " where id=?"); //$NON-NLS-1$ //$NON-NLS-2$
			
			//reading data
			String[] data;
			Map<String, String> colName2Data = new HashMap<>();
			int count = 1;
			while( (data = reader.readNext()) != null ) {
				
				for (int i = 0; i < colNames.length; i++) {
					colName2Data.put(colNames[i], data[i]);
				}

				for (int i = 0; i < uniqueColumns.size(); i++) {
					stFetchId.setString(i+1, colName2Data.get(uniqueColumns.get(i)));
				}
				rs = stFetchId.executeQuery();
				//check if we merge or add new row
				if (rs.next()) {
					//we have id that matched by unique columns, this means we need to merge current data row with given row
					int id = rs.getInt(1);
					for (int i = 0; i < colNames.length; i++) {
						stCsvUpdate.setString(i+1, colName2Data.get(colNames[i]));
					}
					stCsvUpdate.setInt(colNames.length+1, id);
					stCsvUpdate.executeUpdate();
				} else {
					//no id that matched by unique columns, this means we need to add new row for current data
					lastCsvId++;
					stCsvInsert.setInt(1, lastCsvId);
					for (int i = 0; i < colNames.length; i++) {
						stCsvInsert.setString(i+2, colName2Data.get(colNames[i]));
					}
					stCsvInsert.executeUpdate();
				}
				
				rs.close();
				if (count % 256 == 0) {
					c.commit();
				}
				count++;
			}		
			
			c.commit();
			c.setAutoCommit(autoCommit);
		}
		
		return messages;
		
	}

}
