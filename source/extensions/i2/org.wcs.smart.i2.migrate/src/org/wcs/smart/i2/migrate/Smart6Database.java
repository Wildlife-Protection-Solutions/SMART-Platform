/*
 * Copyright (C) 2021 Wildlife Conservation Society
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
package org.wcs.smart.i2.migrate;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLNonTransientConnectionException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.util.SmartUtils;
import org.wcs.smart.util.UuidUtils;

/**
 * Uses java.sql driver to connect to SMART6 backup database and extract require
 * information for migration tools
 * 
 * @author Emily
 *
 */
public class Smart6Database implements Closeable{

	protected Path derbyPath;
	protected Path filestore;
	protected Path root;
	
	protected Connection connection;
	
	public Smart6Database(Path dir) throws SQLException {
		this.root = dir;
		//these might not actually be correct if users configured custom ones
		derbyPath = dir.resolve("smartdb"); //$NON-NLS-1$
		filestore = dir.resolve("filestore"); //$NON-NLS-1$
		
		String csrc = "jdbc:derby:" + derbyPath.toAbsolutePath().normalize().toString(); //$NON-NLS-1$
		csrc += ";user=" + SmartDB.DbUser.ADMIN.getUserName() + ";password=" + SmartDB.DbUser.ADMIN.getPassword(); //$NON-NLS-1$ //$NON-NLS-2$
		
		connection = DriverManager.getConnection(csrc);
	}
	
	protected String getVersion(String pluginId) throws SQLException {
		String sql = "SELECT version FROM smart.db_version WHERE plugin_id = ?"; //$NON-NLS-1$
		
		try(PreparedStatement ps = connection.prepareStatement(sql)){
			ps.setString(1, pluginId);
			try(ResultSet rs = ps.executeQuery()){
				if (rs.next()) {
					String version = rs.getString(1);
					return version;
				}
			}
		}
		return ""; //$NON-NLS-1$
	}
	
	
	protected List<ConservationArea> getConservationAreas(String sql) throws SQLException{
		List<ConservationArea> cas = new ArrayList<>();
		
		try(Statement s = connection.createStatement()){
			try(ResultSet rs = s.executeQuery(sql)){
				while(rs.next()) {
					UUID uuid = UuidUtils.byteToUUID(rs.getBytes(1));
					String id = rs.getString(2);
					String name = rs.getString(3);
					
					ConservationArea ca = new ConservationArea();
					ca.setUuid(uuid);
					ca.setId(id);
					ca.setName(name);
					
					cas.add(ca);
				}
			}
		}
		return cas;
	}
	
	public boolean validateUser(ConservationArea ca, String username, String password) throws SQLException{

		String sql = "SELECT smartpassword FROM smart.employee WHERE ca_uuid = ? AND smartuserid = ?"; //$NON-NLS-1$
		String capass = null;
		try(PreparedStatement s = connection.prepareStatement(sql)){
			s.setBytes(1, UuidUtils.uuidToByte(ca.getUuid()));
			s.setString(2, username);
			
			try(ResultSet rs = s.executeQuery()){
				if (rs.next()) {
					capass = rs.getString(1);
				}
			}
		}
		
		if (capass == null) return false;
		return HibernateManager.validatePassword(password, capass);

	}
	
	
	@Override
	public void close() throws IOException {
		try {
			//connection.close();
			//shut down the connection
			String csrc = "jdbc:derby:" + derbyPath.toAbsolutePath().normalize().toString(); //$NON-NLS-1$
			csrc += ";user=" + SmartDB.DbUser.ADMIN.getUserName() + ";password=" + SmartDB.DbUser.ADMIN.getPassword(); //$NON-NLS-1$ //$NON-NLS-2$
			csrc += ";shutdown=true"; //$NON-NLS-1$
			DriverManager.getConnection(csrc);
		 } catch (SQLNonTransientConnectionException e) {
			 //derby throws this exception when shutdown is ok
			 if (!"08006".equals(e.getSQLState())) { //$NON-NLS-1$
				 throw new IOException(e);
			 }
		} catch (SQLException e) {
			throw new IOException(e);
		}
		
		//delete temporary file
		SmartUtils.deleteDirectory(root);
	}
}
