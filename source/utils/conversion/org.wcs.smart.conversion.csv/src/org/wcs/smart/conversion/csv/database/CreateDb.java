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
package org.wcs.smart.conversion.csv.database;

import java.sql.Connection;
import java.sql.DriverManager;

/**
 * Create the empyt database used by the conversion tool.
 * 
 * This class is called by the maven package script to package 
 * empty database with product.
 * 
 * @author Emily
 *
 */
public class CreateDb {
	/**
	 * 
	 * 
	 * @param args takes a single argument, the location to create the database
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		String location = args[0];
		
		Class.forName("org.apache.derby.jdbc.EmbeddedDriver").newInstance();
		
		String connectionString = "jdbc:derby:" + location + ";create=true;user=smart_admin;password=smart_derby;";
		Connection c = DriverManager.getConnection(connectionString);
		

		String sql[] = new String[]{
			"create table csv_to_smart.attributes (id integer not null, n varchar(255), primary key (id))",
			"create table csv_to_smart.csv(id integer not null, a1 varchar(1024), primary key (id))"
		};
	    
		for (String s : sql){
			c.createStatement().executeUpdate(s);
		}

	    c.commit();
		c.close();
		
	}
}
