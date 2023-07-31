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
package org.wcs.smart.i2.migrate.entity;

import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.util.UuidUtils;

/**
 * Uses java.sql driver to connect to SMART6 backup database and extract require
 * information for migration tools
 * 
 * @author Emily
 *
 */
public class Entity20Database extends EntityDatabase{
	
	public Entity20Database(Path dir) throws SQLException {
		super(dir);
	}
	
	public boolean validateEntityVersion() throws SQLException {
		String version = getVersion("org.wcs.smart.entity"); //$NON-NLS-1$
		return version.equals("2.0"); //$NON-NLS-1$
	}
	
	public List<ConservationArea> getConservationAreasWithEntity()  throws SQLException{
		String sql = "SELECT uuid, id, name FROM smart.conservation_area WHERE uuid in ( select ca_uuid from smart.entity_type where uuid in (select entity_type_uuid from smart.entity)  )"; //$NON-NLS-1$
		return getConservationAreas(sql);
	}

	public Collection<EntityTypeItem> getTypes(ConservationArea ca) throws SQLException{
		List<EntityTypeItem> sources = new ArrayList<>();
		
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT a.uuid, a.keyid, a.date_created, a.creator_uuid, a.status, a.dm_attribute_uuid, "); //$NON-NLS-1$
		sb.append("a.entity_type "); //$NON-NLS-1$
		sb.append("FROM smart.entity_type a "); //$NON-NLS-1$
		sb.append(" WHERE a.ca_uuid = ? "); //$NON-NLS-1$
		
		String namesSql = "SELECT language_uuid, value FROM smart.i18n_label WHERE element_uuid = ?"; //$NON-NLS-1$
		String attSql = "SELECT uuid, dm_attribute_uuid, attribute_order, is_primary, keyid FROM smart.entity_attribute WHERE entity_type_uuid = ?"; //$NON-NLS-1$
		try(PreparedStatement s = connection.prepareStatement(sb.toString());
				PreparedStatement names = connection.prepareStatement(namesSql);
				PreparedStatement attributes = connection.prepareStatement(attSql)){
			
			s.setBytes(1, UuidUtils.uuidToByte(ca.getUuid()));
			
			try(ResultSet rs = s.executeQuery()){
				while(rs.next()) {
					byte[] uuid = rs.getBytes(1);
					
					EntityTypeItem item = new EntityTypeItem(ca);
					item.setUuid(UuidUtils.byteToUUID(uuid));
					item.setKeyId(rs.getString(2));
					item.setDateCreated(rs.getDate(3).toLocalDate());
					
					item.setCreator(UuidUtils.byteToUUID(rs.getBytes(4)));
					
					if (rs.getObject(6) != null) {
						item.setDmUuid(UuidUtils.byteToUUID(rs.getBytes(6)));
					}
					
					String type = rs.getString(7);
					EntityTypeItem.Type etype = EntityTypeItem.Type.TRANSIENT;
					if (type != null) {
						try {
							etype = EntityTypeItem.Type.valueOf(type);
						}catch (Exception ex) {
							ex.printStackTrace();
						}
					}
					item.setType(etype);
							
					
					//names
					names.setBytes(1, uuid);
					try(ResultSet rs2 = names.executeQuery()){
						while(rs2.next()) {
							UUID luuid = UuidUtils.byteToUUID(rs2.getBytes(1));
							String value = rs2.getString(2);
							item.addName(luuid, value);
						}
					}
					
					//attributes
					attributes.setBytes(1, uuid);
					try(ResultSet rs2 = attributes.executeQuery()){
						while(rs2.next()) {
							//SELECT uuid, dm_attribute_uuid, attribute_order, is_primary, keyid 
							
							EntityTypeAttributeItem attributeItem = new EntityTypeAttributeItem();
							UUID auuid = UuidUtils.byteToUUID(rs2.getBytes(1));
							attributeItem.setUuid(auuid);
							
							UUID dmuuid = UuidUtils.byteToUUID(rs2.getBytes(2));
							int order = rs2.getInt(3);
							boolean isprimary = rs2.getBoolean(4);
							String keyid = rs2.getString(5);
							
							attributeItem.setDmAttribute(dmuuid);
							attributeItem.setOrder(order);
							attributeItem.setPrimary(isprimary);
							attributeItem.setKeyId(keyid);
							
							names.setBytes(1, UuidUtils.uuidToByte(auuid));
							try(ResultSet rs3 = names.executeQuery()){
								while(rs3.next()) {
									UUID luuid = UuidUtils.byteToUUID(rs3.getBytes(1));
									String value = rs3.getString(2);
									attributeItem.addName(luuid, value);
								}
							}
							
							item.getAttributes().add(attributeItem);
						}
					}
					
					sources.add(item);
				}
			}
		}
		
		return sources;
	}
	
	public Collection<EntityItem> getEntities(UUID entityType) throws SQLException{
		
		HashMap<UUID, EntityItem> items = new HashMap<>();
		
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT uuid, id, status, attribute_list_item_uuid, x, y "); //$NON-NLS-1$
		sb.append(" FROM smart.entity WHERE entity_type_uuid = ?"); //$NON-NLS-1$
		
		try(PreparedStatement ps = connection.prepareStatement(sb.toString())){
			ps.setBytes(1, UuidUtils.uuidToByte(entityType));
			
			try(ResultSet rs = ps.executeQuery()){
				while(rs.next()) {
					
					UUID uuid = UuidUtils.byteToUUID(rs.getBytes(1));
					String id = rs.getString(2);
					String status = rs.getString(3);
					UUID dmUuid = UuidUtils.byteToUUID(rs.getBytes(4));
					Double x = rs.getDouble(5);
					Double y = rs.getDouble(6);
					
					EntityItem item = new EntityItem();
					item.setDmUuid(dmUuid);
					item.seteType(entityType);
					item.setId(id);
					item.setUuid(uuid);
					item.setStatus(EntityItem.Status.valueOf(status));
					if (x != null) item.setX(x);
					if (y != null) item.setY(y);
					
					items.put(item.getUuid(), item);
					
				}
			}
			
		}
		
		//add attributes
		sb = new StringBuilder();
		sb.append("SELECT a.entity_attribute_uuid, a.entity_uuid, a.number_value, a.string_value, a.list_element_uuid, a.tree_node_uuid "); //$NON-NLS-1$
		sb.append(" FROM smart.entity_attribute_value a join smart.entity b on a.entity_uuid = b.uuid WHERE b.entity_type_uuid = ?"); //$NON-NLS-1$
		
		try(PreparedStatement ps = connection.prepareStatement(sb.toString())){
			ps.setBytes(1, UuidUtils.uuidToByte(entityType));
			
			try(ResultSet rs = ps.executeQuery()){
				while(rs.next()) {
					
					UUID attributeuuid = UuidUtils.byteToUUID(rs.getBytes(1));
					UUID entityuuid = UuidUtils.byteToUUID(rs.getBytes(2));
					
					Double numbervalue = null;
					if (rs.getObject(3) != null) {
						numbervalue = rs.getDouble(3);
					}
					String stringvalue = rs.getString(4);
					
					UUID luuid = null;
					if (rs.getObject(5) != null) {
						luuid = UuidUtils.byteToUUID(rs.getBytes(5));
					}
					UUID tuuid = null;
					if (rs.getObject(6) != null) {
						tuuid = UuidUtils.byteToUUID(rs.getBytes(6));
					}
					
					EntityItemAttribute attribute = new EntityItemAttribute();
					attribute.setAttributeUuid(attributeuuid);
					attribute.setDoubleValue(numbervalue);
					attribute.setStringValue(stringvalue);
					attribute.setUuidValue(luuid != null ? luuid : tuuid);
					
					items.get(entityuuid).addAttribute(attribute);
					
				}
			}
			
		}
		return items.values();
	}
	
}
