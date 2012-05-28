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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;

import net.sourceforge.hatbox.tools.CmdLine;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequenceFactory;
import com.vividsolutions.jts.io.WKBWriter;

/**
 * TODO Purpose of 
 * <p>
 * <ul>
 * <li></li>
 * </ul>
 * </p>
 * @author Emily
 * @since 1.0.0
 */
public class SimpleDerbyTest {

	public static void main(String[] cargs) throws Exception{
		  //find PK column
		Class.forName("org.apache.derby.jdbc.EmbeddedDriver").newInstance();
		// Get a connection
		Connection c =  DriverManager.getConnection("jdbc:derby:C:\\SMART\\Source\\trunk\\java\\org.wcs.smart\\database\\smartdb;user=smart_admin;password=smart_derby");
		
		String sql = "DROP TABLE SMART_ADMIN.T1";
		c.createStatement().execute(sql);
		sql = "DROP TABLE SMART_ADMIN.T1_HATBOX";
		c.createStatement().execute(sql);
		
		sql = "create table SMART_ADMIN.T1 (ID integer not null generated always as identity,NAME varchar (20),GEOM varchar(5000) for bit data,primary key (ID))";
		c.createStatement().execute(sql);
		
		HashMap<String, String> args = new HashMap<String, String>();
		args.put("s", "SMART_ADMIN");
		args.put("t", "T1");
		args.put("geom", "GEOM");
		args.put("srid", "4326");
		CmdLine.spatialize(c, args);
		CmdLine.buildIndex(c, args);
		
	     GeometryFactory geomFactory = new GeometryFactory();
	        CoordinateArraySequenceFactory factory = CoordinateArraySequenceFactory.instance();
		 Coordinate[] coord = new Coordinate[1];
		 WKBWriter writer = new WKBWriter();
	        PreparedStatement ps = c.prepareStatement("insert into SMART_ADMIN.T1 (NAME, GEOM) values (?,?)");
	        ps.setString(1, "Test Point");
	        coord[0] = new Coordinate(145.0, -37.0);
	        ps.setBytes(2, writer.write(new Point(factory.create(coord), geomFactory)));
	        ps.execute();
	        coord[0] = new Coordinate(145.1, -37.1);
	        ps.setBytes(2, writer.write(new Point(factory.create(coord), geomFactory)));
	        ps.execute();
	        coord[0] = new Coordinate(145.2, -37.2);
	        ps.setBytes(2, writer.write(new Point(factory.create(coord), geomFactory)));
	        ps.execute();
	        coord[0] = new Coordinate(145.3, -37.3);
	        ps.setBytes(2, writer.write(new Point(factory.create(coord), geomFactory)));
	        ps.execute();
	        ps.close();
		
//	       
	     ResultSet rs = c.createStatement().executeQuery("SELECT count(*) FROM SMART_ADMIN.T1");
	     rs.next();
	     System.out.println(rs.getInt(1));
//	     
	       c.createStatement().execute("DELETE from SMART_ADMIN.T1 ");
//	       c.createStatement().execute("DELETE from smart.T1 WHERE id = 2");
//	       c.createStatement().execute("DELETE from smart.T1 WHERE id = 3");
	       
	        rs = c.createStatement().executeQuery("SELECT count(*) FROM SMART_ADMIN.T1");
		     rs.next();
		     System.out.println(rs.getInt(1));
		     
		
	}
}
