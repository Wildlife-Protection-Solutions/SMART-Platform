package org.wcs.smart.asset.ui.views.map;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.asset.model.AssetStation;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.util.UuidUtils;

public class CategoryComputer {

	private static AtomicLong tableCnter = new AtomicLong();

	private String waypointFilterTable;
	
	private Date[] dFilter;
	private Session session;
	
	private Map<Attribute, String> attributeToColumn;
	
	private ConservationArea ca = SmartDB.getCurrentConservationArea();
	/**
	 * Creates a temporary query table 
	 * 
	 * @return
	 */
	public synchronized static String createTempTableName(){
		return "query_temp_asset_" + tableCnter.incrementAndGet();//$NON-NLS-1$ 
	}
	
	public CategoryComputer(Date[] dFilter, Session session) {
		this.dFilter = dFilter;
		attributeToColumn = new HashMap<>();
		this.session  = session;
		
	}
	
	private HashMap<AssetStation, StationData> computeValuesByStation(CategoryOverviewColumn toCompute) {
		
		session.doWork(new Work() {

			@Override
			public void execute(Connection connection) throws SQLException {
				//create a temporary table of waypoints
				createWaypointTable(connection);
				
				String c1 = toCompute.getCategoryKey();
				String c2 = c1.substring(0,  c1.length() -1);
				
				StringBuilder sb = new StringBuilder();
				sb.append("SELECT distinct wp_uuid FROM ");
				sb.append(waypointFilterTable);
				sb.append(" WHERE ");
				sb.append(" c_hkey >= ? AND c_hkey < ? ");
				
				Category category = findCategory(toCompute.getCategoryKey());
				if (category == null) throw new SQLException("No category with key " + toCompute.getCategoryKey() + "found.");
				
				if (toCompute.getAttributeFilter() != null) {
					//we need to parse the attribute filter
					StringBuilder where = new StringBuilder();
					
					String[] bits = toCompute.getAttributeFilter().split(" ");
					boolean inAttribute = false;
					int part = 0;
					Attribute currentAttribute = null;
					for (String bit : bits) {
						if (bit.trim().isEmpty()) continue;
						
						
						if (bit.equals("[") || bit.startsWith("[")) {
							inAttribute = true;
							part = 0;
						}
						
						if (!inAttribute) {
							if (bit.equals("(")) where.append(" ( ");
							else if (bit.equals(")")) where.append(" ) ");
							else if (bit.equalsIgnoreCase("and")) where.append(" and ");
							else if (bit.equalsIgnoreCase("or")) where.append(" or ");
							else throw new SQLException("Invalid token: " + bit);
						}else {
							if (part == 0) {
								String attributeKey = bit;
								if (attributeKey.startsWith("[")) attributeKey = attributeKey.substring(1);
								currentAttribute = findAttribute(attributeKey);
								if (currentAttribute == null) {
									throw new SQLException("No attribute with key " + attributeKey );
								}
								part++;
							}else if (part == 1) {
								part++;
								// here we have the operator - depends on type
								switch(currentAttribute.getType()) {
								case BOOLEAN:
									break;
								case DATE:
									break;
								case LIST:
									break;
								case NUMERIC:
									break;
								case TEXT:
									break;
								case TREE:
									break;
								default:
									break;
								
								}
							}else {
								
							}
						}
						
						
						
						if (bit.equals("]") || bit.endsWith("]")) {
							inAttribute = false;
							part = 0;
							currentAttribute = null;
						}
						
						
					}
				}
				
				
			}
			
		});
		return null;
	}
	
	private Category findCategory(String hkey) {
		Category a = QueryFactory.buildQuery(session,  Category.class, 
				new Object[] {"conservationArea", ca},
				new Object[] {"hkey", hkey}).uniqueResult();
		return a;
	}
	
	private Attribute findAttribute(String key) {
		
		Attribute a = QueryFactory.buildQuery(session,  Attribute.class, 
				new Object[] {"conservationArea", ca},
				new Object[] {"keyId", key}).uniqueResult();
		return a;
	}
	
	private AttributeListItem findAttributeListItem(String key, Attribute attribute) {
		AttributeListItem a = QueryFactory.buildQuery(session,  AttributeListItem.class, 
				new Object[] {"conservationArea", ca},
				new Object[] {"keyId", key},
				new Object[] {"attribute", attribute}).uniqueResult();
		return a;
	}
	private AttributeTreeNode findAttributeTreeNode(String hkey, Attribute attribute) {
		AttributeTreeNode a = QueryFactory.buildQuery(session,  AttributeTreeNode.class, 
				new Object[] {"conservationArea", ca},
				new Object[] {"hKey", hkey},
				new Object[] {"attribute", attribute}).uniqueResult();
		return a;
	}
	
	private synchronized void addAttributeColumn(Attribute attribute, Connection connection) throws SQLException {
		if(attributeToColumn.containsKey(attribute)) return; //already exists
		String column = "attribute_" + attributeToColumn.keySet().size();
		
		StringBuilder sb = new StringBuilder();
		sb.append("ALTER TABLE ");
		sb.append(waypointFilterTable);
		sb.append(" ADD COLUMN ");
		sb.append(column);
		switch(attribute.getType()) {
			case BOOLEAN:
			case NUMERIC:
				sb.append(" double ");
				break;
			case DATE:
			case LIST:
			case TEXT:
			case TREE:
				sb.append(" varchar(32672) ");
				break;
		}
		log(sb.toString());
		connection.createStatement().executeUpdate(sb.toString());
		
		
		sb = new StringBuilder();
		sb.append("UPDATE ");
		sb.append(waypointFilterTable);
		sb.append(" SET ");
		sb.append(column);
		sb.append(" = ");
		
		switch(attribute.getType()) {
		case BOOLEAN:
		case NUMERIC:
			sb.append(" (SELECT a.number_value FROM smart.wp_observation_attributes a WHERE a.observation_uuid = ");
			sb.append(waypointFilterTable);
			sb.append(".ob_uuid AND a.attribute_uuid = ?");
			break;
		case DATE:
		case TEXT:
			sb.append(" (SELECT a.string_value FROM smart.wp_observation_attributes a WHERE a.observation_uuid = ");
			sb.append(waypointFilterTable);
			sb.append(".ob_uuid AND a.attribute_uuid = ?");
			break;
			
		case LIST:
			sb.append(" (SELECT c.keyid FROM smart.dm_attribute_list c join smart.wp_observation_attributes a on a.attribute_uuid = ? and a.list_element_uuid = c.uuid WHERE a.observation_uuid = ");
			sb.append(waypointFilterTable);
			sb.append(".ob_uuid");
			break;
		case TREE:
			sb.append(" (SELECT c.hkey FROM smart.dm_attribute_tree c join smart.wp_observation_attributes a on a.attribute_uuid = ? and a.tree_node_uuid = c.uuid WHERE a.observation_uuid = ");
			sb.append(waypointFilterTable);
			sb.append(".ob_uuid");
			break;
		}
		
		
		log(sb.toString());
		log(attribute.getUuid().toString());
		
		PreparedStatement ps = connection.prepareStatement(sb.toString());
		ps.setBytes(1, UuidUtils.uuidToByte(attribute.getUuid()));
		ps.executeUpdate();
		
		attributeToColumn.put(attribute, column);
	}
	
	
	private synchronized void createWaypointTable(Connection connection) throws SQLException{
		if (waypointFilterTable != null) return;
		
		//create a temporary table of wp/ob/category_uuid
		waypointFilterTable = createTempTableName();
		
		StringBuilder sb = new StringBuilder();
		sb.append("CREATE TABLE ");
		sb.append(waypointFilterTable);
		sb.append(" wp_uuid char(16) as bit data ");
		sb.append(" ob_uuid char(16) as bit data ");
		sb.append(" c_hkey varchar(32672) ");
		
		log(sb.toString());
		connection.createStatement().executeUpdate(sb.toString());
		
		sb = new StringBuilder();
		sb.append(" INSERT INTO ");
		sb.append(waypointFilterTable);
		sb.append(" (wp_uuid, ob_uuid, c_hkey) ");
		sb.append(" SELECT a.uuid as wp_uuid, b.uuid as obs_uuid, c.hkey ");
		sb.append(" FROM smart.waypoint a JOIN smart.WP_OBSERVATION b ON a.uuid = b.wp_uuid ");
		sb.append(" JOIN smart.dm_category c ON c.uuid = b.category_uuid ");
		sb.append(" WHERE ");
		sb.append(" a.ca_uuid = ? ");
		Date date1 = null;
		Date date2 = null;
		if (this.dFilter != null) {
			if (this.dFilter[0] != null) {
				date1 = dFilter[0];
				sb.append(" AND a.datetime >= ? ");
			}
			if (this.dFilter[1] != null) {
				date2 = dFilter[1];
				sb.append(" AND a.datetime <= ? ");
			}
		}
		log(sb.toString());
		log(date1.getTime() + ":" + date2.getTime());
		
		PreparedStatement ps = connection.prepareStatement(sb.toString());
		int index = 1;
		ps.setBytes(index++, UuidUtils.uuidToByte(ca.getUuid()));
		if (date1 != null) ps.setTimestamp(index++, new Timestamp(date1.getTime()));
		if (date2 != null) ps.setTimestamp(index, new Timestamp(date2.getTime()));
		ps.executeUpdate();
		
	}
	
	private void log(String message) {
		System.out.println(message);
	}
	
}
