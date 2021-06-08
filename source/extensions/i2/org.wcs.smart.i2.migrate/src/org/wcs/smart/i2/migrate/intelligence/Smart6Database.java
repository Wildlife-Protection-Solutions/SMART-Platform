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
package org.wcs.smart.i2.migrate.intelligence;

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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.locationtech.jts.geom.Coordinate;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.util.UuidUtils;

/**
 * Uses java.sql driver to connect to SMART6 backup database and extract require
 * information for migration tools
 * 
 * @author Emily
 *
 */
public class Smart6Database implements Closeable{

	private Path derbyPath;
	private Path filestore;
	private Path root;
	
	private Connection connection;
	
	public Smart6Database(Path dir) throws SQLException {
		this.root = dir;
		//these might not actually be correct if users configured custom ones
		derbyPath = dir.resolve("smartdb"); //$NON-NLS-1$
		filestore = dir.resolve("filestore"); //$NON-NLS-1$
		
		String csrc = "jdbc:derby:" + derbyPath.toAbsolutePath().normalize().toString(); //$NON-NLS-1$
		csrc += ";user=" + SmartDB.DbUser.ADMIN.getUserName() + ";password=" + SmartDB.DbUser.ADMIN.getPassword(); //$NON-NLS-1$ //$NON-NLS-2$
		
		connection = DriverManager.getConnection(csrc);
	}
	
	public boolean validateIntelligenceVersion() throws SQLException {
		String sql = "SELECT version FROM smart.db_version WHERE plugin_id = 'org.wcs.smart.intelligence'"; //$NON-NLS-1$
		try(Statement s = connection.createStatement()){
			try(ResultSet rs = s.executeQuery(sql)){
				if (rs.next()) {
					String version = rs.getString(1);
					return version.equals("4.0"); //$NON-NLS-1$
				}
			}
		}
		return false;
	}
	
	public List<ConservationArea> getConservationAreasWithData()  throws SQLException{
		String sql = "SELECT uuid, id, name FROM smart.conservation_area WHERE uuid in ( SELECT ca_uuid FROM smart.intelligence )"; //$NON-NLS-1$
		
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

	public Collection<IntelligenceItem> getIntelItems(ConservationArea ca) throws SQLException{
		Map<UUID, IntelligenceItem> sources = new HashMap<>();
		
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT a.received_date, a.patrol_uuid, a.from_date, "); //$NON-NLS-1$
		sb.append("a.to_date, a.description, a.source_uuid, a.creator_uuid, b.value as name, a.uuid "); //$NON-NLS-1$
		sb.append("FROM smart.intelligence a "); //$NON-NLS-1$
		sb.append("LEFT JOIN (smart.i18n_label b JOIN smart.language c on b.language_uuid = c.uuid and c.isdefault) "); //$NON-NLS-1$
		sb.append(" on a.uuid = b.element_uuid "); //$NON-NLS-1$
		sb.append(" WHERE a.ca_uuid = ? "); //$NON-NLS-1$
		
		try(PreparedStatement s = connection.prepareStatement(sb.toString())){
			s.setBytes(1, UuidUtils.uuidToByte(ca.getUuid()));
			
			try(ResultSet rs = s.executeQuery()){
				while(rs.next()) {

					IntelligenceItem item = new IntelligenceItem();
					item.setRecievedDate(rs.getDate(1).toLocalDate());
					if (rs.getObject(2) != null) {
						item.setPatroluuid(UuidUtils.byteToUUID(rs.getBytes(2)));
					}
					
					if (rs.getObject(3) != null) {
						item.setFromDate(rs.getDate(3).toLocalDate());
					}
					if (rs.getObject(4) != null) {
						item.setToDate(rs.getDate(4).toLocalDate());
					}
					item.setDescription(rs.getString(5));
					
					item.setSource(UuidUtils.byteToUUID(rs.getBytes(6)));
					if (rs.getObject(7) != null) {
						item.setCreator(UuidUtils.byteToUUID(rs.getBytes(7)));
					}
					if (rs.getObject(8) != null) {
						item.setName(rs.getString(8));
					}else {
						item.setName("Smart 6 - Intelligence Record "); //$NON-NLS-1$
					}
					item.setUuid(UuidUtils.byteToUUID(rs.getBytes(9)));
					
					sources.put(item.getUuid(), item);
				}
			}
		}
		
		sb = new StringBuilder();
		sb.append("SELECT a.intelligence_uuid, a.x, a.y "); //$NON-NLS-1$
		sb.append("FROM smart.intelligence_point a JOIN smart.intelligence b ON a.intelligence_uuid = b.uuid "); //$NON-NLS-1$
		sb.append(" WHERE b.ca_uuid = ? "); //$NON-NLS-1$
		
		try(PreparedStatement s = connection.prepareStatement(sb.toString())){
			s.setBytes(1, UuidUtils.uuidToByte(ca.getUuid()));
			
			try(ResultSet rs = s.executeQuery()){
				while(rs.next()) {
					
					UUID uuid = UuidUtils.byteToUUID(rs.getBytes(1));
					Double x = rs.getDouble(2);
					Double y = rs.getDouble(3);
					
					IntelligenceItem item = sources.get(uuid);
					if (item != null) item.addPoint(new Coordinate(x,y));
				}
			}
		}
		
		sb = new StringBuilder();
		sb.append("SELECT a.intelligence_uuid, a.filename "); //$NON-NLS-1$
		sb.append("FROM smart.intelligence_attachment a JOIN smart.intelligence b ON a.intelligence_uuid = b.uuid "); //$NON-NLS-1$
		sb.append(" WHERE b.ca_uuid = ? "); //$NON-NLS-1$
		
		try(PreparedStatement s = connection.prepareStatement(sb.toString())){
			s.setBytes(1, UuidUtils.uuidToByte(ca.getUuid()));
			
			try(ResultSet rs = s.executeQuery()){
				while(rs.next()) {
					
					UUID intelUuid = UuidUtils.byteToUUID(rs.getBytes(1));
					String filename = rs.getString(2);
					
					Path fpath = filestore.resolve( UuidUtils.uuidToString(ca.getUuid()) )
							.resolve("intelligence") //$NON-NLS-1$
							.resolve(UuidUtils.uuidToString(intelUuid))
							.resolve(filename);
					
					IntelligenceItem item = sources.get(intelUuid);
					if (item != null) item.addAttachment(fpath);
				}
			}
		}
		
		return sources.values();
	}
	
	public List<IntelligenceSource> getSources(Collection<ConservationArea> cas) throws SQLException{
		
		List<IntelligenceSource> sources = new ArrayList<>();
		
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT a.uuid, a.keyid, b.value, a.ca_uuid "); //$NON-NLS-1$
		sb.append("FROM smart.intelligence_source a left join "); //$NON-NLS-1$
		sb.append(" (smart.I18N_LABEL b join smart.LANGUAGE c on b.language_uuid = c.uuid and c.isdefault) "); //$NON-NLS-1$
		sb.append(" on a.uuid = b.element_uuid WHERE a.ca_uuid in ("); //$NON-NLS-1$
		for (int i = 0; i < cas.size(); i ++) {
			sb.append("?,"); //$NON-NLS-1$
		}
		sb.deleteCharAt(sb.length() - 1);
		sb.append(")"); //$NON-NLS-1$
		
		try(PreparedStatement s = connection.prepareStatement(sb.toString())){
			int i = 1;
			for (ConservationArea ca : cas) {
				s.setBytes(i++, UuidUtils.uuidToByte(ca.getUuid()));
			}
			
			try(ResultSet rs = s.executeQuery()){
				while(rs.next()) {
					UUID uuid = UuidUtils.byteToUUID(rs.getBytes(1));
					String keyid = rs.getString(2);
					String name = rs.getString(3);
					UUID cauuid = UuidUtils.byteToUUID(rs.getBytes(4));

					ConservationArea temp = null;
					for (ConservationArea ca : cas) {
						if (ca.getUuid().equals(cauuid)) {
							temp = ca;
							break;
						}
					}
					if (temp == null) continue;
					
					
					IntelligenceSource src = new IntelligenceSource(temp, uuid, name, keyid);
					sources.add(src);
				}
			}
		}
		return sources;
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
		FileUtils.deleteDirectory(root.toFile());
	}
}
