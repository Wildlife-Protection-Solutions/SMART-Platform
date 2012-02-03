package org.wcs.smart.tests.hibernate;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.Properties;

import org.junit.Test;
import org.wcs.smart.SmartProperties;

public class TestLoginUserPermissions {

	private static String loginUser = "login";
	private static String loginPass = "smrt";
	
	@Test
	public void testConnection() {
		
		Connection c = null;
		try{
			c = getConnection();
			assertNotNull(c);
			
			ResultSet rs = c.createStatement().executeQuery("SELECT count(*) from smart.conservation_area");
			while(rs.next()){}
			
			
		}catch (Exception ex){
			ex.printStackTrace();
			fail(ex.getMessage());
		}		
	}
	
	
	private Connection getConnection() throws Exception{
		Class.forName("org.apache.derby.jdbc.EmbeddedDriver").newInstance();
		
		// Get a connection
		return DriverManager.getConnection("jdbc:derby:" + getDatabaseFromProperties()
				+ ";create=true;user=" + loginUser + ";password=" + loginPass + ";");
		
	}
	
	private String getDatabaseFromProperties() throws Exception{
		Properties prop = new Properties();
		InputStream stream = TestLoginUserPermissions.class.getClassLoader().getResourceAsStream("smart.properties");
		prop.load(stream);

		String database = prop.getProperty(SmartProperties.SMART_DB_KEY);
		return database;
	}

}
