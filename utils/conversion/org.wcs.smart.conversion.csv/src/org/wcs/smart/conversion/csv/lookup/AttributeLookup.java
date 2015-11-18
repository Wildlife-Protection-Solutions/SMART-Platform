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
package org.wcs.smart.conversion.csv.lookup;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AttributeLookup {
	
	private static final Logger logger = LogManager.getLogger(AttributeLookup.class); 

	private Map<String, Integer> i2Column;
	
	public AttributeLookup(Connection c)  {
		try {
			i2Column = new HashMap<String, Integer>();
			ResultSet rs = c.createStatement().executeQuery(
					"select n, id from csv_to_smart.attributes"); //$NON-NLS-1$
			while (rs.next()) {
				i2Column.put(rs.getString(1), rs.getInt(2));
			}
			rs.close();
		} catch (SQLException e) {
			i2Column = null;
			logger.error("Failed to create attributes lookup.", e); //$NON-NLS-1$
		}
	}

	public Integer getColumn(String i) {
		return i2Column.get(i);
	}
	
}
