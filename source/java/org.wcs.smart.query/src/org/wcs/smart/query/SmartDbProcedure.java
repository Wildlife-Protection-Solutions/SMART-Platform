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
package org.wcs.smart.query;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Class containing Derby DB stored procedure for cleaning up database from temporary tables
 * that might present after hard shutdown.
 *
 * @author elitvin
 * @since 1.0.0
 */

public class SmartDbProcedure {

	/**
	 * Checks if there are some temporary tables (possible after hard application close)
	 * and cleans them up if they present.
	 * 
	 * create procedure smart.cleanUpTempData()
	 *     language java
	 *     parameter style java
	 *     external security definer
	 *     modifies sql data
	 *     external name 'org.wcs.smart.SmartDbProcedure.cleanUpTempData';
	 * 
	 * @throws SQLException
	 */
	public static void cleanUpTempData() throws SQLException {
		Connection connection = DriverManager.getConnection("jdbc:default:connection"); //$NON-NLS-1$
		String sql = "select tbl.TABLENAME, sch.SCHEMANAME from SYS.SYSTABLES tbl inner join SYS.SYSSCHEMAS sch on tbl.SCHEMAID = sch.SCHEMAID WHERE tbl.TABLETYPE = 'T' AND tbl.TABLENAME like 'QUERY_TEMP_%'"; //$NON-NLS-1$
		try(Statement statement = connection.createStatement()){
		List<String> toDrop = new ArrayList<String>();
			try(ResultSet resultSet = statement.executeQuery(sql)) {
				while (resultSet.next()) {
					String tableName = resultSet.getString(1);
					String schemaName = resultSet.getString(2);
					toDrop.add(schemaName+"."+tableName); //$NON-NLS-1$
				}			
			}
			if (!toDrop.isEmpty()) {
				for (String table : toDrop) {
					try (Statement s = connection.createStatement()){
						s.executeUpdate("DROP TABLE "+table); //$NON-NLS-1$
					} catch (Exception e) {
						// nothing
						e.printStackTrace();
					}
				}
			}
		}
	}
	
}
