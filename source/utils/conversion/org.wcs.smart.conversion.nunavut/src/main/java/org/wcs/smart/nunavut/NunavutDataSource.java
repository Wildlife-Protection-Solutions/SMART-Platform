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

	private static final String HOST = "54.196.200.248";
	private static final int PORT = 3306;
	private static final String DB_NAME = "kitikmeot";
	private static final String USERNAME = "devel";
	private static final String PASSWORD = "asdfoiueSS!99kjlkqwe";
	
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
		
		String sql = "SELECT MOBILE_DATA_RECORD_TRIP_ID FROM mobile_data_record_trip";
		List<String> patrolIds = new ArrayList<>();
		
		try(Statement s = connection.createStatement();
			ResultSet rs = s.executeQuery(sql)){
			
			while(rs.next()) {
				String pid = rs.getString(1);
				patrolIds.add(pid);
			}
		}
		return patrolIds;
		//return Collections.emptyList();
	}
	
	
	public void close() throws SQLException{
		connection.close();
	}
	
	
}
