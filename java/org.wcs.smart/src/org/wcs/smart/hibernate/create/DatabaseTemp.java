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

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.Properties;

import org.wcs.smart.SmartProperties;

public class DatabaseTemp {


	private static String tableFile = "org/wcs/smart/hibernate/create/smart-tables.sql";
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


	public static void main(String args[]) throws Exception {
		Properties prop = new Properties();
		InputStream stream = Database.class.getClassLoader()
				.getResourceAsStream("smart.properties");
		prop.load(stream);

		String database = prop.getProperty(SmartProperties.SMART_DB_KEY);
		Connection c = createConnection(database, "smart_admin", "smart_derby");
		
		String sql = "SELECT uuid, keyid, att_type from smart.dm_attribute";
		ResultSet rs = c.createStatement().executeQuery(sql);
		while(rs.next()){
			System.out.println((rs.getString(1)) + "  :  " + rs.getString(2) + "  :  " + rs.getString(3));
		}
		rs.close();
		System.out.println("------------------------------------------------------------");
		
		sql = "SELECT category_uuid, attribute_uuid, att_order from smart.dm_cat_att_map";
		rs = c.createStatement().executeQuery(sql);
		while(rs.next()){
			System.out.println((rs.getString(1)) + "  :  " + rs.getString(2) + "  :  " + rs.getString(3));
		}
		
		System.out.println("------------------------------------------------------------");
		rs.close();
		
//		sql ="SELECT uuid FROM smart.station";
//		rs = c.createStatement().executeQuery(sql);
//		while(rs.next()){
//			System.out.println( rs.getString(1) );
//		}

//		sql = "UPDATE smart.i18n_label set element_uuid = (select uuid from smart.station)";
//		c.createStatement().execute(sql);
		
//		sql = " INSERT INTO smart.i18n_label (language_uuid, element_uuid, value)" +
//				"SELECT a.uuid, b.uuid, 'EmilyLabel' from smart.language a, smart.station b";
//		c.createStatement().execute(sql);
		
		
		

	}
}
