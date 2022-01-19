package org.wcs.smart.nunavut;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NunavutDataSource {

	private static final String HOST = "";
	private static final int PORT = 3306;
	private static final String DB_NAME = "nunavut";
	private static final String USERNAME = "username";
	private static final String PASSWORD = "password";
	
	private Connection connection;
	
	public NunavutDataSource() throws SQLException {
		
		StringBuilder sb = new StringBuilder();
		sb.append("jdbc:mysql://");
		sb.append(HOST);
		sb.append(":");
		sb.append(PORT);
		sb.append("/");
		sb.append(DB_NAME);
		sb.append("?user=" + USERNAME);
		sb.append("&password=" + PASSWORD);

		connection = DriverManager.getConnection(sb.toString());
	}

	public List<String> getPatrolsIds() throws SQLException{
		
		String sql = "SELECT pid FROM patrols ";
		List<String> patrolIds = new ArrayList<>();
		
		try(Statement s = connection.createStatement();
			ResultSet rs = s.executeQuery(sql)){
			
			while(rs.next()) {
				String pid = rs.getString(1);
				patrolIds.add(pid);
			}
		}
		return Collections.emptyList();
	}
	
	
	public void close() throws SQLException{
		connection.close();
	}
	
	
}
