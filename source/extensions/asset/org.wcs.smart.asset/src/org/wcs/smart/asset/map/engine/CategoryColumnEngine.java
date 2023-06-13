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

import java.io.Reader;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.NamedPreparedStatement;
import org.wcs.smart.asset.AssetPlugIn;
import org.wcs.smart.asset.internal.Messages;
import org.wcs.smart.asset.map.engine.parser.Parser;
import org.wcs.smart.asset.ui.views.map.CategoryOverviewColumn;
import org.wcs.smart.asset.ui.views.map.IOverviewTableColumn;
import org.wcs.smart.asset.ui.views.map.IOverviewTableColumn.GroupByOption;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.hibernate.QueryFactory;
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
	private LocalDate[] dFilter;
	private Session session;
	private Map<String, String> attributeToColumn;
	private Map<String, Attribute> attributeKeyToAttribute;
	
	private ConservationArea ca ;
	private IOverviewTableColumn.GroupByOption groupBy;
	
	public CategoryColumnEngine(LocalDate[] dFilter, IOverviewTableColumn.GroupByOption groupBy, ConservationArea ca, Session session) {
		attributeToColumn = new HashMap<>();
		attributeKeyToAttribute = new HashMap<>();
		this.dFilter = dFilter;
		this.session = session;
		this.groupBy = groupBy;
		this.ca = ca;
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
			session.createNativeMutationQuery("DROP TABLE " + waypointFilterTable).executeUpdate(); //$NON-NLS-1$
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
				String c2 = c1.substring(0,  c1.length() -1) + "/"; //$NON-NLS-1$
				
				StringBuilder sb = new StringBuilder();
				sb.append("SELECT uuid, count(*) FROM ( "); //$NON-NLS-1$
				sb.append("SELECT distinct a.wp_uuid as wp_uuid,"); //$NON-NLS-1$
				if (groupBy == GroupByOption.LOCATION) {
					sb.append("c.station_location_uuid as uuid"); //$NON-NLS-1$
				}else if (groupBy == GroupByOption.STATION) {
					sb.append("d.station_uuid as uuid"); //$NON-NLS-1$
				}
				sb.append(" FROM "); //$NON-NLS-1$
				sb.append("smart.asset_waypoint a JOIN "); //$NON-NLS-1$
				sb.append( waypointFilterTable );
				sb.append(" b on a.wp_uuid = b.wp_uuid JOIN "); //$NON-NLS-1$
				sb.append(" smart.asset_deployment c on a.asset_deployment_uuid = c.uuid "); //$NON-NLS-1$
				if (groupBy == GroupByOption.STATION) {
					sb.append(" JOIN smart.asset_station_location d on d.uuid = c.station_location_uuid "); //$NON-NLS-1$
				}
				sb.append(" WHERE "); //$NON-NLS-1$
				sb.append(" c_hkey >= :ckey1 AND c_hkey < :ckey2 "); //$NON-NLS-1$
				
				HashMap<String, Object> namesToValues = new HashMap<>();
				namesToValues.put(":ckey1", c1); //$NON-NLS-1$
				namesToValues.put(":ckey2", c2); //$NON-NLS-1$
				
				Category category = findCategory(toCompute.getCategoryKey());
				if (category == null) throw new SQLException(MessageFormat.format(Messages.CategoryColumnEngine_CategoryNotFound, toCompute.getCategoryKey()));
				
				
				if (toCompute.getAttributeFilter() != null && !toCompute.getAttributeFilter().trim().isEmpty()) {
					//we need to parse the attribute filter
					IExpression attributeFilter = null;
					try(Reader is = new StringReader(toCompute.getAttributeFilter())){
						Parser parser = new Parser(is);
						attributeFilter = parser.AttributeExpression();
					} catch (Exception e) {
						throw new SQLException(Messages.CategoryColumnEngine_AttributeParseError, e);
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
						if (attribute == null) throw new SQLException(MessageFormat.format(Messages.CategoryColumnEngine_AttributeNotFound, attributeKey));
						addAttributeColumn(attribute, connection);
						attributeKeyToAttribute.put(attribute.getKeyId(), attribute);
					}
					try {
						String where = asSql(attributeFilter, namesToValues, connection);
						sb.append(" AND ( "); //$NON-NLS-1$
						sb.append(where);
						sb.append(" ) "); //$NON-NLS-1$
					}catch (Exception ex) {
						throw new SQLException(ex);
					}
					
				}
				sb.append(" ) foo GROUP BY uuid"); //$NON-NLS-1$
				
				try(NamedPreparedStatement ps = new NamedPreparedStatement(connection, sb.toString())){
					log ( sb.toString() );
					for (Entry<String,Object> parameter : namesToValues.entrySet()) {
						log( parameter.getKey() + ":" + (parameter.getValue() == null ? "" : parameter.getValue().toString() )); //$NON-NLS-1$  //$NON-NLS-2$
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
				new Object[] {"conservationArea", ca}, //$NON-NLS-1$
				new Object[] {"hkey", hkey}).uniqueResult(); //$NON-NLS-1$
		return a;
	}
	
	private Attribute findAttribute(String key) {
		
		Attribute a = QueryFactory.buildQuery(session,  Attribute.class, 
				new Object[] {"conservationArea", ca}, //$NON-NLS-1$
				new Object[] {"keyId", key}).uniqueResult(); //$NON-NLS-1$
		return a;
	}
		
	private synchronized void addAttributeColumn(Attribute attribute, Connection connection) throws SQLException {
		if (attribute.getType() == AttributeType.MLIST) return; //must be treated differently
		if(attributeToColumn.containsKey(attribute.getKeyId())) return; //already exists
		
		String column = "attribute_" + attributeToColumn.keySet().size(); //$NON-NLS-1$
		
		StringBuilder sb = new StringBuilder();
		sb.append("ALTER TABLE "); //$NON-NLS-1$
		sb.append(waypointFilterTable);
		sb.append(" ADD COLUMN "); //$NON-NLS-1$
		sb.append(column);
		switch(attribute.getType()) {
			case BOOLEAN:
			case NUMERIC:
				sb.append(" double "); //$NON-NLS-1$
				break;
			case DATE:
			case LIST:
			case TEXT:
			case TREE:
				sb.append(" varchar(32672) "); //$NON-NLS-1$
				break;
			case MLIST:
				return; //not supported here
		}
		log(sb.toString());
		connection.createStatement().executeUpdate(sb.toString());
		
		
		sb = new StringBuilder();
		sb.append("UPDATE "); //$NON-NLS-1$
		sb.append(waypointFilterTable);
		sb.append(" SET "); //$NON-NLS-1$
		sb.append(column);
		sb.append(" = "); //$NON-NLS-1$
		
		switch(attribute.getType()) {
		case BOOLEAN:
		case NUMERIC:
			sb.append(" (SELECT a.number_value FROM smart.wp_observation_attributes a WHERE a.observation_uuid = "); //$NON-NLS-1$
			sb.append(waypointFilterTable);
			sb.append(".ob_uuid AND a.attribute_uuid = ?"); //$NON-NLS-1$
			break;
		case DATE:
		case TEXT:
			sb.append(" (SELECT a.string_value FROM smart.wp_observation_attributes a WHERE a.observation_uuid = "); //$NON-NLS-1$
			sb.append(waypointFilterTable);
			sb.append(".ob_uuid AND a.attribute_uuid = ?"); //$NON-NLS-1$
			break;
			
		case LIST:
			sb.append(" (SELECT c.keyid FROM smart.dm_attribute_list c join smart.wp_observation_attributes a on a.attribute_uuid = ? and a.list_element_uuid = c.uuid WHERE a.observation_uuid = "); //$NON-NLS-1$
			sb.append(waypointFilterTable);
			sb.append(".ob_uuid"); //$NON-NLS-1$
			break;
		case TREE:
			sb.append(" (SELECT c.hkey FROM smart.dm_attribute_tree c join smart.wp_observation_attributes a on a.attribute_uuid = ? and a.tree_node_uuid = c.uuid WHERE a.observation_uuid = "); //$NON-NLS-1$
			sb.append(waypointFilterTable);
			sb.append(".ob_uuid"); //$NON-NLS-1$
			break;
		case MLIST:
			return; //not supported here
		}
		sb.append(")"); //$NON-NLS-1$
		
		log(sb.toString());
		log(attribute.getUuid().toString());
		
		PreparedStatement ps = connection.prepareStatement(sb.toString());
		ps.setBytes(1, UuidUtils.uuidToByte(attribute.getUuid()));
		ps.executeUpdate();
		
		attributeToColumn.put(attribute.getKeyId(), column);
	}
	
	private synchronized String addMultiSelectAttributeListFilter(AttributeExpression filter, Connection connection) throws SQLException {
		Attribute attribute = findAttribute(filter.getAttributeKey());
		if (attribute.getType() != AttributeType.MLIST) return null;
		
		
		String column = "mattribute_" + attributeToColumn.keySet().size(); //$NON-NLS-1$
		
		StringBuilder sb = new StringBuilder();
		sb.append("ALTER TABLE "); //$NON-NLS-1$
		sb.append(waypointFilterTable);
		sb.append(" ADD COLUMN "); //$NON-NLS-1$
		sb.append(column);
		sb.append(" varchar(32672) "); //$NON-NLS-1$
		log(sb.toString());
		connection.createStatement().executeUpdate(sb.toString());
		
		
		sb = new StringBuilder();
		sb.append("UPDATE "); //$NON-NLS-1$
		sb.append(waypointFilterTable);
		sb.append(" SET "); //$NON-NLS-1$
		sb.append(column);
		sb.append(" = "); //$NON-NLS-1$
		
		
		sb.append(" (SELECT c.keyid FROM smart.dm_attribute_list c join "); //$NON-NLS-1$
		sb.append(" smart.wp_observation_attributes_list d on c.uuid = d.list_element_uuid "); //$NON-NLS-1$
		sb.append(" join smart.wp_observation_attributes a on a.attribute_uuid = ? and "); //$NON-NLS-1$
		sb.append(" a.uuid = d.observation_attribute_uuid WHERE a.observation_uuid = "); //$NON-NLS-1$
		sb.append(waypointFilterTable);
		sb.append(".ob_uuid "); //$NON-NLS-1$
		sb.append("AND c.keyid = ? "); //$NON-NLS-1$
		sb.append(")"); //$NON-NLS-1$
		
		log(sb.toString());
		log(attribute.getUuid().toString());
		
		PreparedStatement ps = connection.prepareStatement(sb.toString());
		ps.setBytes(1, UuidUtils.uuidToByte(attribute.getUuid()));
		ps.setString(2, filter.getStringValue());
		ps.executeUpdate();
		
		attributeToColumn.put(filter.toString(), column);
		
		return column;
	}
	
	private synchronized void createWaypointTable(Connection connection) throws SQLException{
		if (waypointFilterTable != null) return;
		
		//create a temporary table of wp/ob/category_uuid
		waypointFilterTable = createTempTableName();
		
		StringBuilder sb = new StringBuilder();
		sb.append("CREATE TABLE "); //$NON-NLS-1$
		sb.append(waypointFilterTable);
		sb.append(" ( wp_uuid char(16) for bit data, "); //$NON-NLS-1$
		sb.append(" ob_uuid char(16) for bit data, "); //$NON-NLS-1$
		sb.append(" c_hkey varchar(32672)) "); //$NON-NLS-1$
		
		log(sb.toString());
		connection.createStatement().executeUpdate(sb.toString());
		
		sb = new StringBuilder();
		sb.append(" INSERT INTO "); //$NON-NLS-1$
		sb.append(waypointFilterTable);
		sb.append(" (wp_uuid, ob_uuid, c_hkey) "); //$NON-NLS-1$
		sb.append(" SELECT a.uuid as wp_uuid, b.uuid as obs_uuid, c.hkey "); //$NON-NLS-1$
		sb.append(" FROM smart.waypoint a JOIN smart.wp_observation_group g on a.uuid = g.wp_uuid "); //$NON-NLS-1$
		sb.append(" join smart.WP_OBSERVATION b ON g.uuid = b.wp_group_uuid "); //$NON-NLS-1$
		sb.append(" JOIN smart.dm_category c ON c.uuid = b.category_uuid "); //$NON-NLS-1$
		sb.append(" WHERE "); //$NON-NLS-1$
		sb.append(" a.ca_uuid = ? "); //$NON-NLS-1$
		LocalDate date1 = null;
		LocalDate date2 = null;
		if (this.dFilter != null) {
			if (this.dFilter[0] != null) {
				date1 = dFilter[0];
				sb.append(" AND a.datetime >= ? "); //$NON-NLS-1$
			}
			if (this.dFilter[1] != null) {
				date2 = dFilter[1];
				sb.append(" AND a.datetime <= ? "); //$NON-NLS-1$
			}
		}
		log(sb.toString());
		log((date1 == null ? "" : date1.toString()) + ":" + (date2== null ? "" : date2.toString())); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		
		PreparedStatement ps = connection.prepareStatement(sb.toString());
		int index = 1;
		ps.setBytes(index++, UuidUtils.uuidToByte(ca.getUuid()));
		if (date1 != null) ps.setObject(index++, Timestamp.valueOf(date1.atStartOfDay()));//new Timestamp(date1.getTime()));
		if (date2 != null) ps.setObject(index, Timestamp.valueOf(date2.atTime(LocalTime.MAX)));//new Timestamp(date2.getTime()));
		ps.executeUpdate();
		
	}
	
	private void log(String message) {
//		System.out.println(message);
	}
	
	private String asSql(BracketExpression filter, HashMap<String, Object> namesToValues, Connection connection) throws Exception{
		String part1 = asSql(filter.getFilter(), namesToValues, connection);
		return "( " + part1 + " )"; //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	private String asSql(BooleanExpression filter, HashMap<String, Object> namesToValues, Connection connection) throws Exception{
		String part1 = asSql(filter.getFilter1(), namesToValues, connection);
		String part2 = asSql(filter.getFilter2(), namesToValues, connection);
		return part1 + " " + filter.getOperator().operator.sql + " " + part2; //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	private String asSql(AttributeExpression filter, HashMap<String, Object> namesToValues, Connection connection) throws Exception{
		String columnName = attributeToColumn.get( filter.getAttributeKey() );
		Attribute attribute = attributeKeyToAttribute.get( filter.getAttributeKey() );
		
		StringBuilder sb = new StringBuilder();
		sb.append(" ( "); //$NON-NLS-1$
		
		switch(attribute.getType()) {
		case BOOLEAN:
			Boolean bool = null;
			try{
				bool = Boolean.valueOf(filter.getStringValue().toUpperCase(Locale.ROOT));
			}catch (Exception ex) {
			}
			if (bool == null) throw new Exception(MessageFormat.format(Messages.CategoryColumnEngine_BooleanParseItem, filter.getStringValue()));
			
			sb.append(columnName);
			if (bool) {
				sb.append(" > 0.5 "); //$NON-NLS-1$
			}else {
				sb.append(" < 0.5 "); //$NON-NLS-1$
			}
			
			break;
		case DATE:
			if (filter.getOperator().operator != Operator.Op.BEFORE && filter.getOperator().operator != Operator.Op.AFTER
					&& filter.getOperator().operator != Operator.Op.STR_EQUAL) {
				throw new Exception(MessageFormat.format(Messages.CategoryColumnEngine_OpNotSupported3, filter.getOperator().operator.key));
			}
//			Date d = null;
//			try {
//				d = new SimpleDateFormat(AttributeExpression.JAVA_DATE_FORMAT).parse(filter.getStringValue());
//			}catch (Exception ex) {}
//			if (d == null) throw new Exception(MessageFormat.format(Messages.CategoryColumnEngine_DateParseError, filter.getStringValue()));
			
			sb.append("cast(" ); //$NON-NLS-1$
			sb.append(columnName);
			sb.append(" as date ) "); //$NON-NLS-1$
			sb.append(filter.getOperator().operator.sql);
			sb.append(" "); //$NON-NLS-1$
			
			String key = ":a" + namesToValues.size(); //$NON-NLS-1$
			sb.append(key);
			namesToValues.put(key, filter.getStringValue());
					
			break;
		case LIST:
			key = ":a" + namesToValues.size(); //$NON-NLS-1$
			namesToValues.put(key, filter.getStringValue());
			sb.append(columnName);
			sb.append(" = "); //$NON-NLS-1$
			sb.append(key);
			break;
		case NUMERIC:
			if (filter.getOperator().operator != Operator.Op.LG 
				&& filter.getOperator().operator != Operator.Op.LGE
				&& filter.getOperator().operator != Operator.Op.GT
				&& filter.getOperator().operator != Operator.Op.GTE
				&& filter.getOperator().operator != Operator.Op.NOTEQ
				&& filter.getOperator().operator != Operator.Op.EQ) {
			
				throw new Exception(MessageFormat.format(Messages.CategoryColumnEngine_OpNotSupported1, filter.getOperator().operator.key));
			}
			
			key = ":a" + namesToValues.size(); //$NON-NLS-1$
			
			sb.append(columnName);
			sb.append(" "); //$NON-NLS-1$
			sb.append(filter.getOperator().operator.sql);
			sb.append(" "); //$NON-NLS-1$
			sb.append(key);
			namesToValues.put(key, filter.getNumberValue());
			break;
		case TEXT:
			key = ":a" + namesToValues.size(); //$NON-NLS-1$
			sb.append(columnName);
			sb.append(" "); //$NON-NLS-1$
			if (filter.getOperator().operator == Operator.Op.STR_EQUAL) {
				sb.append(" = "); //$NON-NLS-1$
				sb.append(key);
				namesToValues.put(key, filter.getStringValue());
				break;
			}else if (filter.getOperator().operator == Operator.Op.STR_CONTAINS) {
				sb.append(" like "); //$NON-NLS-1$
				sb.append(key);
				namesToValues.put(key, "%" + filter.getStringValue() + "%"); //$NON-NLS-1$ //$NON-NLS-2$
				break;
			}
			throw new Exception(MessageFormat.format(Messages.CategoryColumnEngine_OpNotSupported2, filter.getOperator().operator.key));
		case TREE:
			String key1 = ":a" + namesToValues.size(); //$NON-NLS-1$
			String key2 = ":a" + (namesToValues.size()+1); //$NON-NLS-1$
			String hkey = filter.getStringValue();
			if (hkey == null) throw new Exception(MessageFormat.format(Messages.CategoryColumnEngine_TreeNodeNotFound, attribute.getKeyId()));
			
			String c1 = hkey;
			String c2 = c1.substring(0,  c1.length() -1) + "/"; //$NON-NLS-1$
			sb.append(columnName);
			sb.append(" "); //$NON-NLS-1$
			sb.append(" >= "); //$NON-NLS-1$
			sb.append(key1);
			sb.append(" AND "); //$NON-NLS-1$
			sb.append(columnName);
			sb.append(" <= "); //$NON-NLS-1$
			sb.append(key2);
			
			namesToValues.put(key1, c1);
			namesToValues.put(key2, c2);
			break;
		case MLIST:
			String column = addMultiSelectAttributeListFilter(filter, connection);
			sb.append( " "); //$NON-NLS-1$
			sb.append(column);
			sb.append( " is not null "); //$NON-NLS-1$
		}
		
		sb.append(" ) "); //$NON-NLS-1$
		return sb.toString();
	}
	
	public String asSql(IExpression filter, HashMap<String, Object> namesToValues, Connection connection) throws Exception{
		if (filter instanceof AttributeExpression) {
			return asSql((AttributeExpression)filter, namesToValues, connection);
		}else if (filter instanceof BracketExpression) {
			return asSql((BracketExpression)filter, namesToValues, connection);
		}else if (filter instanceof BooleanExpression) {
			return asSql((BooleanExpression)filter, namesToValues, connection);
		}
		return ""; //$NON-NLS-1$
	}
}
