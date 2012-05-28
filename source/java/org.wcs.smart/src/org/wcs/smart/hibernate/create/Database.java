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
package org.wcs.smart.hibernate.create;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Properties;

import net.sourceforge.hatbox.tools.CmdLine;

import org.apache.derby.iapi.services.io.FileUtil;
import org.wcs.smart.SmartProperties;

public class Database {


	private static String[] tableFiles = new String[]{
		"org/wcs/smart/hibernate/create/smart-tables.sql",
		"org/wcs/smart/hibernate/create/smart-tables-dm.sql",
		"org/wcs/smart/hibernate/create/smart-tables-patrol.sql"
	};
			
	private static String userFile = "org/wcs/smart/hibernate/create/smart-users.sql";
	private static String permissionFile = "org/wcs/smart/hibernate/create/smart-permissions.sql";

	private static Connection createConnection(String database)
			throws Exception {
		Class.forName("org.apache.derby.jdbc.EmbeddedDriver").newInstance();
		// Get a connection
		return DriverManager.getConnection("jdbc:derby:" + database
				+ ";create=true;user=smart_admin");
	}
	
	private static Connection createConnection(String database, String user, String password)
			throws Exception {
		Class.forName("org.apache.derby.jdbc.EmbeddedDriver").newInstance();
		// Get a connection
		System.out.println("jdbc:derby:" + database
				+ ";create=true;user=" + user + ";password=" + password + ";");
		return DriverManager.getConnection("jdbc:derby:" + database
				+ ";create=true;user=" + user + ";password=" + password + ";");
	}

	private static void processFile(Connection c, String file) throws Exception {
		InputStream in = ClassLoader.getSystemResourceAsStream(file);
		BufferedReader input = new BufferedReader(new InputStreamReader(in));

		StringBuilder sb = new StringBuilder();
		String line = null;

		while ((line = input.readLine()) != null) {
			if (!line.trim().startsWith("--")) {

				if (line.contains(";")) {
					String bits[] = line.split(";");
					for (int i = 0; i < bits.length - 1; i++) {
						sb.append(bits[i]);
						System.out.println("running:" + sb.toString());
						c.createStatement().execute(sb.toString());
						sb = new StringBuilder();
					}
					if (bits.length > 0){
						sb.append(bits[bits.length - 1]);
					}
					if (line.endsWith(";")) {
						System.out.println("running:" + sb.toString());
						c.createStatement().execute(sb.toString());
						sb = new StringBuilder();
					}
				} else {
					sb.append(line);
					sb.append(" ");
				}
			}
		}
		if (sb.toString().trim().length() > 0) {
			System.out.println("running:" + sb.toString());
			c.createStatement().execute(sb.toString());
		}
		input.close();

	}
	
	private static void spatialize(Connection c) throws Exception{
		
		CmdLine.spatializeDb(c, "derby");
		
		HashMap<String, String> args = new HashMap<String, String>();
		args.put("s", "SMART");
		args.put("t", "AREA_GEOMETRIES");
		args.put("geom", "GEOM");
		args.put("srid", "4326");
		CmdLine.spatialize(c, args);
	
		CmdLine.buildIndex(c, args);
	}

	private static void createDatabase(String databaseName) throws Exception {
		Connection c = createConnection(databaseName);

		

		processFile(c, userFile);
		
		//c.close();
		 try {
			 DriverManager.getConnection("jdbc:derby:" + databaseName + ";shutdown=true;user=smart_admin;password=smart_derby;");
         } catch (SQLException se) {
             if ( !se.getSQLState().equals("08006") ) {
                 throw se;
             }
         }

		
		c = createConnection(databaseName, "smart_admin", "smart_derby");
		for (int i = 0; i < tableFiles.length; i ++){
			processFile(c, tableFiles[i]);	
		}
		
		processFile(c, permissionFile);
		
		spatialize(c);
		c.close();
	}

	public static void main(String args[]) throws Exception {
//		if (true){
//			  //find PK column
//			Class.forName("org.apache.derby.jdbc.EmbeddedDriver").newInstance();
//			// Get a connection
//			Connection c =  DriverManager.getConnection("jdbc:derby:C:\\SMART\\Source\\trunk\\java\\org.wcs.smart\\database\\smartdb;user=smart_admin;password=smart_derby");
//			
//	        List<String> pkColList = new ArrayList<String>();
//	        DatabaseMetaData dbMetaData = c.getMetaData();
//	        ResultSet pkRs = dbMetaData.getPrimaryKeys(null, "SMART", "AREA_GEOMETRIES");
//	        while(pkRs.next()) {
//	        	System.out.println("PK: " + pkRs.getString(4));
//	            pkColList.add(pkRs.getString(4));
//	        }
//	        System.out.println("done.");
//	        pkRs.close();
//	        return;
//		}
		
		Properties prop = new Properties();
		InputStream stream = Database.class.getClassLoader()
				.getResourceAsStream("smart.properties");
		prop.load(stream);

		String database = prop.getProperty(SmartProperties.SMART_DB_KEY);

		File f = new File(database);
		if (f.exists()){
			FileUtil.removeDirectory(f);
			
		}
		createDatabase(database);
	}
}
