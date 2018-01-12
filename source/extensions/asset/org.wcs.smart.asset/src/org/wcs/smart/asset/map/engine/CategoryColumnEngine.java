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
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.asset.map.engine.parser.Parser;
import org.wcs.smart.asset.ui.views.map.CategoryOverviewColumn;
import org.wcs.smart.asset.ui.views.map.IOverviewTableColumn;
import org.wcs.smart.asset.ui.views.map.IOverviewTableColumn.GroupByOption;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
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

	private String waypointFilterTable;
	
	private Date[] dFilter;
	private Session session;
	
	private Map<String, String> attributeToColumn;
	
	private ConservationArea ca = SmartDB.getCurrentConservationArea();
	/**
	 * Creates a temporary query table 
	 * 
	 * @return
	 */
	public synchronized static String createTempTableName(){
		return "query_temp_asset_" + tableCnter.incrementAndGet();//$NON-NLS-1$ 
	}
	
	public CategoryColumnEngine(Session session, Date[] dFilter) {
		this.dFilter = dFilter;
		attributeToColumn = new HashMap<>();
		this.session  = session;
	}
	
	public boolean canProcess(IOverviewTableColumn column) {
		return column instanceof CategoryOverviewColumn;
	}
	
	//TODOD: drop temporary table
	public HashMap<UUID, Object> computeValues(IOverviewTableColumn column, final IOverviewTableColumn.GroupByOption groupBy) {
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
				sb.append(" c_hkey >= ? AND c_hkey < ? ");
				
				Category category = findCategory(toCompute.getCategoryKey());
				if (category == null) throw new SQLException("No category with key " + toCompute.getCategoryKey() + "found.");
				
				
				if (toCompute.getAttributeFilter() != null && !toCompute.getAttributeFilter().trim().isEmpty()) {
					//we need to parse the attribute filter
					IFilter attributeFilter = null;
					try(InputStream is = new ByteArrayInputStream(toCompute.getAttributeFilter().getBytes())){
						Parser parser = new Parser(is);
						attributeFilter = parser.AttributeExpression();
					} catch (Exception e) {
						throw new SQLException("Could not parse attribute filter.", e);
					}
					
					Set<String> attributeKeys = new HashSet<>();
					IFilterVisitor visitor = new IFilterVisitor() {
						@Override
						public void visit(IFilter filter) {
							if (filter instanceof AttributeFilter) {
								attributeKeys.add(((AttributeFilter) filter).getAttributeKey());
							}
						}
					};
					attributeFilter.accept(visitor);
					
					for (String attributeKey : attributeKeys) {
						Attribute attribute = findAttribute(attributeKey);
						if (attribute == null) throw new SQLException("Could not find a data model attribute with the key " + attributeKey);
						addAttributeColumn(attribute, connection);
					}
					
					String where = asSql(attributeFilter);
					sb.append(" AND ( ");
					sb.append(where);
					sb.append(" ) ");
					
				}
				sb.append(" ) foo GROUP BY uuid");
				
				log (sb.toString() );
				log (c1  + ":" + c2);
				PreparedStatement ps = connection.prepareStatement(sb.toString());
				ps.setString(1, c1);
				ps.setString(2, c2);
				try(ResultSet rs = ps.executeQuery()){
					while(rs.next()) {
						UUID uuid = UuidUtils.byteToUUID(rs.getBytes(1));
						Long cnt = rs.getLong(2);
						System.out.println(uuid.toString() + ":" + cnt);
						results.put(uuid, cnt);
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
	
	
	
	private String asSql(BracketFilter filter) {
		String part1 = asSql(filter.getFilter());
		return "( " + part1 + " )";
	}
	
	private String asSql(AttributeExpression filter) {
		String part1 = asSql(filter.getFilter1());
		String part2 = asSql(filter.getFilter2());
		return part1 + " " + filter.getOperator().operator.sql + " " + part2;
	}
	
	private String asSql(AttributeFilter filter) {
		String columnName = attributeToColumn.get( filter.getAttributeKey() );
		StringBuilder sb = new StringBuilder();
		sb.append(" ( ");
		sb.append(columnName);
		sb.append(" ");
		sb.append(filter.getOperator().operator.sql);
		sb.append(" ");
		//TODO: fix this
		//based on attribut etype - trees need like option
		if (filter.getStringValue() != null) {
			sb.append("'" + filter.getStringValue() + "'");
		}else if (filter.getNumberValue() != null) {
			sb.append(filter.getNumberValue());
		}
		sb.append(" ) ");
		return sb.toString();
	}
	
	public String asSql(IFilter filter) {
		if (filter instanceof AttributeFilter) {
			return asSql((AttributeFilter)filter);
		}else if (filter instanceof BracketFilter) {
			return asSql((BracketFilter)filter);
		}else if (filter instanceof AttributeExpression) {
			return asSql((AttributeExpression)filter);
		}
		return "";
	}
}
