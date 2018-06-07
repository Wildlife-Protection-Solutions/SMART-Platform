package org.wcs.smart.i2.ui.views.entity.search;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelAttribute.AttributeType;
import org.wcs.smart.util.UuidUtils;
import org.wcs.smart.i2.model.IntelAttributeListItem;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelEntityTypeAttribute;

public class AllEntityContentProvider {

	private static final String DB_NAME_NAME = "smart.i_entity_view";
	
	public synchronized void generateData() {
	
		ConservationArea ca = SmartDB.getCurrentConservationArea();
		
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
			
				try {
					session.createNativeQuery("DROP TABLE " + DB_NAME_NAME);
				}catch (Exception ex) {
					//ignore; table likely doesn't exist
				}
				
				StringBuilder sb = new StringBuilder();
				sb.append("CREATE TABLE ");
				sb.append(DB_NAME_NAME);
				sb.append(" ( entity_uuid char(16) for bit data, entity_type_uuid char(16) for bit data, i_primary_id varchar(1024), i_entity_type_name varchar(1024), ");
				
				List<IntelEntityTypeAttribute> etattributes = session.createQuery("FROM IntelEntityTypeAttribute WHERE id.attribute.conservationArea = :ca")
				.setParameter("ca", ca)
				.list();
				
				Set<IntelAttribute> eattributes = new HashSet<>();
				etattributes.forEach( a-> eattributes.add(a.getAttribute()));
				
				//need a column for each attribute
				for (IntelAttribute attribute : eattributes) {
					sb.append(attribute.getKeyId());
					switch(attribute.getType()) {
					case BOOLEAN:
					case NUMERIC:
						sb.append("double");
						break;
					case DATE:
					case EMPLOYEE:
					case LIST:
						sb.append("_uuid char(16) for bit data, ");
						sb.append(attribute.getKeyId() + " varchar(1024)");
						
						break;
					case POSITION:
					case TEXT:
						sb.append("varchar(1024)");
						break;
					}
					sb.append(", ");
				}
				sb.deleteCharAt(sb.length() - 1);
				sb.deleteCharAt(sb.length() - 1);
				
				sb.append(")");
			
				session.createNativeQuery(sb.toString()).executeUpdate();
				
				//now we need to populate this table
				
				//entity uuid for this ca
				sb = new StringBuilder();
				sb.append(" INSERT INTO ");
				sb.append( DB_NAME_NAME );
				sb.append(" (entity_uuid, entity_type_uuid)");
				sb.append(" SELECT e.uuid, t.uuid FROM smart.i_entity e join smart.i_entity_type t on e.entity_type_uuid = t.uuid" );
				sb.append(" WHERE e.ca_uuid = :ca");
				session.createNativeQuery(sb.toString())
					.setParameter("ca", ca.getUuid())
					.executeUpdate();
				
				//entity type names
				sb = new StringBuilder();
				sb.append("SELECT distinct entity_type_uuid FROM " + DB_NAME_NAME);
				List<?> entityTypes = session.createNativeQuery(sb.toString()).list();
				for (Object x : entityTypes) {
					UUID entityTypeUuid = UuidUtils.byteToUUID( (byte[]) x );
					IntelEntityType type = session.get(IntelEntityType.class, entityTypeUuid);
				
					sb = new StringBuilder();
					sb.append(" UPDATE ");
					sb.append(DB_NAME_NAME);
					sb.append(" SET i_entity_type_name = :name WHERE entity_type_uuid = :uuid");
					
					session.createNativeQuery(sb.toString())
						.setParameter("name", type.getName())
						.setParameter("uuid", entityTypeUuid)
						.executeUpdate();
				}
				
				
				
				
				for (IntelAttribute attribute : eattributes) {
					
					sb = new StringBuilder();
					sb.append("UPDATE " + DB_NAME_NAME);
					sb.append(" SET ");
					
					
					
					switch(attribute.getType()) {
					case BOOLEAN:
					case NUMERIC:
						sb.append(attribute.getKeyId() + " = ");
						sb.append(" v.double_value ");
						break;
					case DATE:
					case EMPLOYEE:
					case TEXT:
						sb.append(attribute.getKeyId() + " = ");
						sb.append("v.string_value)");
						break;
					case POSITION:
						//TODO:
						sb.append(attribute.getKeyId() + " = ");
						sb.append(" v.double_value ");
						break;
					case LIST:
						sb.append(attribute.getKeyId() + "_uuid = ");
						sb.append("_uuid v.list_item_uuid");				
					}
					sb.append(" FROM smart.i_entity_attribute_value v join smart.i_attribute a on v.attribute_uuid = a.uuid ");
					sb.append(" WHERE a.keyid = :attribute and v.entity_uuid = ");
					sb.append( DB_NAME_NAME );
					sb.append(".entity_uuid");
					
					session.createNativeQuery(sb.toString())
						.setParameter("attribute", attribute.getKeyId())
						.executeUpdate();
				}
				
				
				//entity attributes
				for (IntelAttribute attribute : eattributes) {
					if (attribute.getType() != AttributeType.LIST) continue;
					
					sb = new StringBuilder();
					sb.append("SELECT distinct " + attribute.getKeyId() + "_uuid FROM " + DB_NAME_NAME + ")");
					List<?> listItems = session.createNativeQuery(sb.toString()).list();
					for (Object x : listItems) {
						if (x == null) continue;
						UUID listItemUuid = UuidUtils.byteToUUID( (byte[]) x );
						IntelAttributeListItem item = session.get(IntelAttributeListItem.class, listItemUuid);
					
						sb = new StringBuilder();
						sb.append(" UPDATE ");
						sb.append(DB_NAME_NAME);
						sb.append(" SET " + attribute.getKeyId() + " = :name WHERE " + attribute.getKeyId() + "_uuid = :uuid");
						
						session.createNativeQuery(sb.toString())
							.setParameter("name", item.getName())
							.setParameter("uuid", listItemUuid)
							.executeUpdate();
					}
				}
				
				session.getTransaction().commit();
			}catch(Exception ex){
				ex.printStackTrace();
				session.getTransaction().rollback();
			}
		}
	}
}
