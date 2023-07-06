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
package org.wcs.smart.db;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;

/**
 * Runs a collection of sql statements that
 * are terminated by ";".  
 * 
 * <p>
 * Lines that start with '--' are ignored.  '--' anywhere other
 * than the beginning of the line is not treated as a comment.
 * </p>
 * @author Emily
 * @since 1.0.0
 */
public class DatabaseCmdRunner {

	public static final boolean PRINT_STATEMENTS = true;
	
	public static Connection createConnection(String database,
			String user, String password)
			throws Exception {
		Class.forName("org.apache.derby.jdbc.EmbeddedDriver").newInstance();
		
//		System.out.println("jdbc:derby:" + database
//				+ ";create=true;user=" + user + ";password=" + password + ";");
		
		String connectionString = "jdbc:derby:" + database 
				+ ";create=true;user=" + user;
		if (password != null){
			connectionString += ";password=" + password + ";";
		}
		return DriverManager.getConnection(connectionString);
	}
	
	public static void spatialize(Connection c, 
			String schema, 
			String table, 
			String column) throws Exception{
		
//		net.sourceforge.hatbox.tools.CmdLine.spatializeDb(c, "derby");
//		
//		HashMap<String, String> args = new HashMap<String, String>();
//		args.put("s", schema);
//		args.put("t", table);
//		args.put("geom", column);
//		args.put("srid", "4326");
//		net.sourceforge.hatbox.tools.CmdLine.spatialize(c, args);
//		net.sourceforge.hatbox.tools.CmdLine.buildIndex(c, args);
//		c.setAutoCommit(true);
	}
	
	
	public static void processFile(Connection c, File file) throws Exception {
		processFile(c, new FileReader(file));		
	}
	
	public static void processFile(Connection c, Reader data) throws Exception, SQLException{	
		
		BufferedReader input = new BufferedReader(data);

		StringBuilder sb = new StringBuilder();
		String line = null;

		while ((line = input.readLine()) != null) {
			if (!line.trim().startsWith("--")) {
				if (line.contains(";")) {
					String bits[] = line.split(";");
					for (int i = 0; i < bits.length - 1; i++) {
						sb.append(bits[i]);
						printAndRun(c, sb);
						sb = new StringBuilder();
					}
					if (bits.length > 0){
						sb.append(bits[bits.length - 1]);
					}
					if (line.endsWith(";")) {
						printAndRun(c, sb);
						sb = new StringBuilder();
					}
				} else {
					sb.append(line);
					sb.append(" ");
				}
			}
		}
		if (sb.toString().trim().length() > 0) {
			printAndRun(c, sb);
		}
		input.close();

	}
	
	private static void printAndRun(Connection c, StringBuilder sb) throws SQLException{
		if (PRINT_STATEMENTS){
			System.out.println("running:" + sb.toString());
		}
		c.createStatement().execute(sb.toString());
	}
}
