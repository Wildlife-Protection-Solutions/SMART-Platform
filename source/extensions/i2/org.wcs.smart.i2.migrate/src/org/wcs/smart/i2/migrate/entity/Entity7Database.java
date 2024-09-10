package org.wcs.smart.i2.migrate.entity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.hibernate.Session;
import org.hibernate.jdbc.ReturningWork;
import org.hibernate.jdbc.Work;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.util.UuidUtils;

/**
 * 
 * Support conversion from smart7 entities to smart7 profiles.
 * 
 * @author Emily
 * @since 7.5.11
 */
public class Entity7Database implements IEntityDatabase{

	public Entity7Database() {
		
	}
	
	@Override
	public Collection<EntityItem> getEntities(UUID entityType) throws SQLException {
		try(Session session = HibernateManager.openSession()){
			return session.doReturningWork(new ReturningWork<Collection<EntityItem>>() {
				@Override
				public Collection<EntityItem> execute(Connection connection) throws SQLException {
				
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
			});
		}
	}

	
	public Collection<EntityTypeItem> getTypes(ConservationArea ca) throws SQLException{
		List<EntityTypeItem> sources = new ArrayList<>();
		
		try(Session session = HibernateManager.openSession()){
		
			session.doWork(new Work() {

				@Override
				public void execute(Connection connection) throws SQLException {
			
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
				}
			});
		}
		
		return sources;
	}
}
