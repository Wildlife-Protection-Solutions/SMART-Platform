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
import java.util.Set;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.locationtech.jts.geom.Coordinate;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.util.UuidUtils;

public class Smart6Database implements Closeable{

	private Path derbyPath;
	private Path filestore;
	private Path root;
	
	private Connection connection;
	
	public Smart6Database(Path dir) throws SQLException {
		this.root = dir;
		derbyPath = dir.resolve("smartdb");
		filestore = dir.resolve("filestore");
		
		String csrc = "jdbc:derby:" + derbyPath.toAbsolutePath().normalize().toString();
		csrc += ";user=" + SmartDB.DbUser.ADMIN.getUserName() + ";password=" + SmartDB.DbUser.ADMIN.getPassword();
		
		connection = DriverManager.getConnection(csrc);
	}
	
	public boolean validateIntelligenceVersion() throws SQLException {
		String sql = "SELECT version FROM smart.db_version WHERE plugin_id = 'org.wcs.smart.intelligence'";
		try(Statement s = connection.createStatement()){
			try(ResultSet rs = s.executeQuery(sql)){
				if (rs.next()) {
					String version = rs.getString(1);
					return version.equals("4.0");
				}
			}
		}
		return false;
	}
	
	public List<ConservationArea> getConservationAreasWithData()  throws SQLException{
		String sql = "SELECT uuid, id, name FROM smart.conservation_area WHERE uuid in ( SELECT ca_uuid FROM smart.intelligence )";
		
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
		sb.append("SELECT a.received_date, a.patrol_uuid, a.from_date, ");
		sb.append("a.to_date, a.description, a.source_uuid, a.creator_uuid, b.value as name, a.uuid ");
		sb.append("FROM smart.intelligence a ");
		sb.append("LEFT JOIN (smart.i18n_label b JOIN smart.language c on b.language_uuid = c.uuid and c.isdefault) ");
		sb.append(" on a.uuid = b.element_uuid ");
		sb.append(" WHERE a.ca_uuid = ? ");
		
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
						item.setName("Smart 6 - Intelligence Record ");
					}
					item.setUuid(UuidUtils.byteToUUID(rs.getBytes(9)));
					
					sources.put(item.getUuid(), item);
				}
			}
		}
		
		sb = new StringBuilder();
		sb.append("SELECT a.intelligence_uuid, a.x, a.y ");
		sb.append("FROM smart.intelligence_point a JOIN smart.intelligence b ON a.intelligence_uuid = b.uuid ");
		sb.append(" WHERE b.ca_uuid = ? ");
		
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
		sb.append("SELECT a.intelligence_uuid, a.filename ");
		sb.append("FROM smart.intelligence_attachment a JOIN smart.intelligence b ON a.intelligence_uuid = b.uuid ");
		sb.append(" WHERE b.ca_uuid = ? ");
		
		try(PreparedStatement s = connection.prepareStatement(sb.toString())){
			s.setBytes(1, UuidUtils.uuidToByte(ca.getUuid()));
			
			try(ResultSet rs = s.executeQuery()){
				while(rs.next()) {
					
					UUID intelUuid = UuidUtils.byteToUUID(rs.getBytes(1));
					String filename = rs.getString(2);
					
					Path fpath = filestore.resolve( UuidUtils.uuidToString(ca.getUuid()) )
							.resolve("intelligence")
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
		sb.append("SELECT a.uuid, a.keyid, b.value, a.ca_uuid ");
		sb.append("FROM smart.intelligence_source a left join ");
		sb.append(" (smart.I18N_LABEL b join smart.LANGUAGE c on b.language_uuid = c.uuid and c.isdefault) ");
		sb.append(" on a.uuid = b.element_uuid WHERE a.ca_uuid in (");
		for (int i = 0; i < cas.size(); i ++) {
			sb.append("?,");
		}
		sb.deleteCharAt(sb.length() - 1);
		sb.append(")");
		
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

		String sql = "SELECT smartpassword FROM smart.employee WHERE ca_uuid = ? AND smartuserid = ?";
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
			
			String csrc = "jdbc:derby:" + derbyPath.toAbsolutePath().normalize().toString();
			csrc += ";user=" + SmartDB.DbUser.ADMIN.getUserName() + ";password=" + SmartDB.DbUser.ADMIN.getPassword();
			csrc += ";shutdown=true";
			DriverManager.getConnection(csrc);
		 } catch (SQLNonTransientConnectionException e) {
			 if (!"08006".equals(e.getSQLState())) {
				 throw new IOException(e);
			 }
		} catch (SQLException e) {
			throw new IOException(e);
		}
		
		//delete temporary file
		FileUtils.deleteDirectory(root.toFile());
	}
}
