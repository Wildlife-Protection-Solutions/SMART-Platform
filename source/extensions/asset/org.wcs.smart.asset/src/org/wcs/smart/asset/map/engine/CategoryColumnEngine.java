/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.asset.map.engine;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.NamedPreparedStatement;
import org.wcs.smart.asset.AssetPlugIn;
import org.wcs.smart.asset.map.engine.parser.Parser;
import org.wcs.smart.asset.ui.views.map.CategoryOverviewColumn;
import org.wcs.smart.asset.ui.views.map.IOverviewTableColumn;
import org.wcs.smart.asset.ui.views.map.IOverviewTableColumn.GroupByOption;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.util.UuidUtils;

/**
 * Category column engine for computing category filter columns in the asset overview
 * map table.
 * 
 * @author Emily
 *
 */
public class CategoryColumnEngine implements IColumnEngine {

	private static AtomicLong tableCnter = new AtomicLong();

	/**
	 * Creates a temporary query table 
	 * 
	 * @return
	 */
	public synchronized static String createTempTableName(){
		return "query_temp_asset_" + tableCnter.incrementAndGet();//$NON-NLS-1$ 
	}
	
	private String waypointFilterTable;
	private Date[] dFilter;
	private Session session;
	private Map<String, String> attributeToColumn;
	private Map<String, Attribute> attributeKeyToAttribute;
	
	private ConservationArea ca = SmartDB.getCurrentConservationArea();
	private IOverviewTableColumn.GroupByOption groupBy;
	
	public CategoryColumnEngine(Date[] dFilter, IOverviewTableColumn.GroupByOption groupBy, Session session) {
		attributeToColumn = new HashMap<>();
		attributeKeyToAttribute = new HashMap<>();
		this.dFilter = dFilter;
		this.session = session;
		this.groupBy = groupBy;
	}
	
	@Override
	public boolean canProcess(IOverviewTableColumn column) {
		return column instanceof CategoryOverviewColumn;
	}
	
	/**
	 * Cleans up all resources
	 * @param session
	 */
	@Override
	public void dispose(Session session) {
		if (waypointFilterTable == null) return;
		try {
			session.createNativeQuery("DROP TABLE " + waypointFilterTable).executeUpdate();
		}catch (Exception ex) {
			AssetPlugIn.log(ex.getMessage(), ex);
		}
	}
	
	@Override
	public HashMap<UUID, Object> computeValues(IOverviewTableColumn column) {
				
		CategoryOverviewColumn toCompute = (CategoryOverviewColumn)column;
		HashMap<UUID, Object> results = new HashMap<>();
		
		session.doWork(new Work() {

			@Override
			public void execute(Connection connection) throws SQLException {
				connection.setAutoCommit(true);
				//create a temporary table of waypoints
				createWaypointTable(connection);
				
				String c1 = toCompute.getCategoryKey();
				String c2 = c1.substring(0,  c1.length() -1) + "/";
				
				StringBuilder sb = new StringBuilder();
				sb.append("SELECT uuid, count(*) FROM ( ");
				sb.append("SELECT distinct a.wp_uuid as wp_uuid,");
				if (groupBy == GroupByOption.LOCATION) {
					sb.append("c.station_location_uuid as uuid");
				}else if (groupBy == GroupByOption.STATION) {
					sb.append("d.station_uuid as uuid");
				}
				sb.append(" FROM ");
				sb.append("smart.asset_waypoint a JOIN ");
				sb.append( waypointFilterTable );
				sb.append(" b on a.wp_uuid = b.wp_uuid JOIN ");
				sb.append(" smart.asset_deployment c on a.asset_deployment_uuid = c.uuid ");
				if (groupBy == GroupByOption.STATION) {
					sb.append(" JOIN smart.asset_station_location d on d.uuid = c.station_location_uuid ");
				}
				sb.append(" WHERE ");
				sb.append(" c_hkey >= :ckey1 AND c_hkey < :ckey2 ");
				
				HashMap<String, Object> namesToValues = new HashMap<>();
				namesToValues.put(":ckey1", c1);
				namesToValues.put(":ckey2", c2);
				
				Category category = findCategory(toCompute.getCategoryKey());
				if (category == null) throw new SQLException("No category with key " + toCompute.getCategoryKey() + "found.");
				
				
				if (toCompute.getAttributeFilter() != null && !toCompute.getAttributeFilter().trim().isEmpty()) {
					//we need to parse the attribute filter
					IExpression attributeFilter = null;
					try(InputStream is = new ByteArrayInputStream(toCompute.getAttributeFilter().getBytes())){
						Parser parser = new Parser(is);
						attributeFilter = parser.AttributeExpression();
					} catch (Exception e) {
						throw new SQLException("Could not parse attribute filter.", e);
					}
					
					Set<String> attributeKeys = new HashSet<>();
					IExpressionVisitor visitor = new IExpressionVisitor() {
						@Override
						public void visit(IExpression filter) {
							if (filter instanceof AttributeExpression) {
								attributeKeys.add(((AttributeExpression) filter).getAttributeKey());
							}
						}
					};
					attributeFilter.accept(visitor);
					
					for (String attributeKey : attributeKeys) {
						Attribute attribute = findAttribute(attributeKey);
						if (attribute == null) throw new SQLException("Could not find a data model attribute with the key " + attributeKey);
						addAttributeColumn(attribute, connection);
						attributeKeyToAttribute.put(attribute.getKeyId(), attribute);
					}
					try {
						String where = asSql(attributeFilter, namesToValues);
						sb.append(" AND ( ");
						sb.append(where);
						sb.append(" ) ");
					}catch (Exception ex) {
						throw new SQLException(ex);
					}
					
				}
				sb.append(" ) foo GROUP BY uuid");
				
				try(NamedPreparedStatement ps = new NamedPreparedStatement(connection, sb.toString())){
					log ( sb.toString() );
					for (Entry<String,Object> parameter : namesToValues.entrySet()) {
						log( parameter.getKey() + ":" + parameter.getValue().toString() );
						ps.setObject(parameter.getKey(), parameter.getValue());
					}
					try(ResultSet rs = ps.executeQuery()){
						while(rs.next()) {
							UUID uuid = UuidUtils.byteToUUID(rs.getBytes(1));
							Long cnt = rs.getLong(2);
							results.put(uuid, cnt);
						}
					}
				}
				connection.setAutoCommit(false);				
			}
			
		});
		return results;
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
	
//	private AttributeListItem findAttributeListItem(String key, Attribute attribute) {
//		AttributeListItem a = QueryFactory.buildQuery(session,  AttributeListItem.class, 
//				new Object[] {"conservationArea", ca},
//				new Object[] {"keyId", key},
//				new Object[] {"attribute", attribute}).uniqueResult();
//		return a;
//	}
//	private AttributeTreeNode findAttributeTreeNode(String hkey, Attribute attribute) {
//		AttributeTreeNode a = QueryFactory.buildQuery(session,  AttributeTreeNode.class, 
//				new Object[] {"conservationArea", ca},
//				new Object[] {"hKey", hkey},
//				new Object[] {"attribute", attribute}).uniqueResult();
//		return a;
//	}
	
	private synchronized void addAttributeColumn(Attribute attribute, Connection connection) throws SQLException {
		if(attributeToColumn.containsKey(attribute.getKeyId())) return; //already exists
		
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
		sb.append(")");
		
		log(sb.toString());
		log(attribute.getUuid().toString());
		
		PreparedStatement ps = connection.prepareStatement(sb.toString());
		ps.setBytes(1, UuidUtils.uuidToByte(attribute.getUuid()));
		ps.executeUpdate();
		
		attributeToColumn.put(attribute.getKeyId(), column);
	}
	
	
	private synchronized void createWaypointTable(Connection connection) throws SQLException{
		if (waypointFilterTable != null) return;
		
		//create a temporary table of wp/ob/category_uuid
		waypointFilterTable = createTempTableName();
		
		StringBuilder sb = new StringBuilder();
		sb.append("CREATE TABLE ");
		sb.append(waypointFilterTable);
		sb.append(" ( wp_uuid char(16) for bit data, ");
		sb.append(" ob_uuid char(16) for bit data, ");
		sb.append(" c_hkey varchar(32672)) ");
		
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
		log((date1 == null ? "" : date1.getTime()) + ":" + (date2== null ? "" : date2.getTime()));
		
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
	
	
	
	private String asSql(BracketExpression filter, HashMap<String, Object> namesToValues) throws Exception{
		String part1 = asSql(filter.getFilter(), namesToValues);
		return "( " + part1 + " )";
	}
	
	private String asSql(BooleanExpression filter, HashMap<String, Object> namesToValues) throws Exception{
		String part1 = asSql(filter.getFilter1(), namesToValues);
		String part2 = asSql(filter.getFilter2(), namesToValues);
		return part1 + " " + filter.getOperator().operator.sql + " " + part2;
	}
	
	private String asSql(AttributeExpression filter, HashMap<String, Object> namesToValues) throws Exception{
		String columnName = attributeToColumn.get( filter.getAttributeKey() );
		Attribute attribute = attributeKeyToAttribute.get( filter.getAttributeKey() );
		
		StringBuilder sb = new StringBuilder();
		sb.append(" ( ");
		
		switch(attribute.getType()) {
		case BOOLEAN:
			Boolean bool = null;
			try{
				bool = Boolean.valueOf(filter.getStringValue().toUpperCase());
			}catch (Exception ex) {}
			if (bool == null) throw new Exception("Could not parse boolean from '" + filter.getStringValue() + "'.");
			
			sb.append(columnName);
			if (bool) {
				sb.append(" > 0.5 ");
			}else {
				sb.append(" < 0.5 ");
			}
			
			break;
		case DATE:
			if (filter.getOperator().operator != Operator.Op.BEFORE && filter.getOperator().operator != Operator.Op.AFTER
					&& filter.getOperator().operator != Operator.Op.STR_EQUAL) {
				throw new Exception("Operator " + filter.getOperator().operator.key + " is not supported for date filters.");
			}
			Date d = null;
			try {
				d = new SimpleDateFormat(AttributeExpression.JAVA_DATE_FORMAT).parse(filter.getStringValue());
			}catch (Exception ex) {}
			if (d == null) throw new Exception("Could not parse date '" + filter.getStringValue() + "' - ensure date is in the format YYYY-MM-DD");
			
			sb.append("cast(" );
			sb.append(columnName);
			sb.append(" as date ) ");
			sb.append(filter.getOperator().operator.sql);
			sb.append(" ");
			
			String key = ":a" + namesToValues.size();
			sb.append(key);
			namesToValues.put(key, filter.getStringValue());
					
			break;
		case LIST:
			key = ":a" + namesToValues.size();
			namesToValues.put(key, filter.getStringValue());
			sb.append(columnName);
			sb.append(" = ");
			sb.append(key);
			break;
		case NUMERIC:
			if (filter.getOperator().operator != Operator.Op.LG 
				&& filter.getOperator().operator != Operator.Op.LGE
				&& filter.getOperator().operator != Operator.Op.GT
				&& filter.getOperator().operator != Operator.Op.GTE
				&& filter.getOperator().operator != Operator.Op.NOTEQ
				&& filter.getOperator().operator != Operator.Op.EQ) {
			
				throw new Exception("Operator " + filter.getOperator().operator.key + " is not supported for number filters.");
			}
			
			key = ":a" + namesToValues.size();
			
			sb.append(columnName);
			sb.append(" ");
			sb.append(filter.getOperator().operator.sql);
			sb.append(" ");
			sb.append(key);
			namesToValues.put(key, filter.getNumberValue());
			break;
		case TEXT:
			key = ":a" + namesToValues.size();
			sb.append(columnName);
			sb.append(" ");
			if (filter.getOperator().operator == Operator.Op.STR_EQUAL) {
				sb.append(" = ");
				sb.append(key);
				namesToValues.put(key, filter.getStringValue());
				break;
			}else if (filter.getOperator().operator == Operator.Op.STR_CONTAINS) {
				sb.append(" like ");
				sb.append(key);
				namesToValues.put(key, "%" + filter.getStringValue() + "%");
				break;
			}
			throw new Exception("Operator " + filter.getOperator().operator.key + " not supported for string attributes. ");
		case TREE:
			String key1 = ":a" + namesToValues.size();
			String key2 = ":a" + (namesToValues.size()+1);
			String hkey = filter.getStringValue();
			if (hkey == null) throw new Exception("No tree item provided for attribute tree filter " + attribute.getKeyId());
			
			String c1 = hkey;
			String c2 = c1.substring(0,  c1.length() -1) + "/";
			sb.append(columnName);
			sb.append(" ");
			sb.append(" >= ");
			sb.append(key1);
			sb.append(" AND ");
			sb.append(columnName);
			sb.append(" <= ");
			sb.append(key2);
			
			namesToValues.put(key1, c1);
			namesToValues.put(key2, c2);
			break;
		
		}
		
		sb.append(" ) ");
		return sb.toString();
	}
	
	public String asSql(IExpression filter, HashMap<String, Object> namesToValues) throws Exception{
		if (filter instanceof AttributeExpression) {
			return asSql((AttributeExpression)filter, namesToValues);
		}else if (filter instanceof BracketExpression) {
			return asSql((BracketExpression)filter, namesToValues);
		}else if (filter instanceof BooleanExpression) {
			return asSql((BooleanExpression)filter, namesToValues);
		}
		return "";
	}
}
