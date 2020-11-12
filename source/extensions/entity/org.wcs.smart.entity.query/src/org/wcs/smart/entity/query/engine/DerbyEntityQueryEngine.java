/*
 * Copyright (C) 2012 Wildlife Conservation Society
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
package org.wcs.smart.entity.query.engine;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;

import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.entity.model.Entity;
import org.wcs.smart.entity.model.EntityAttribute;
import org.wcs.smart.entity.model.EntityAttributeValue;
import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.entity.query.internal.Messages;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.common.engine.AbstractQueryEngine;
import org.wcs.smart.query.common.engine.IFilterProcessor;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.util.UuidUtils;

/**
 * Query engine for executing 
 * queries using derby.
 * 
 * @author Emily
 * @since 1.0.0
 */
public abstract class DerbyEntityQueryEngine extends AbstractQueryEngine{

	static {
		tablePrefix.put(Entity.class, "e"); //$NON-NLS-1$
		tablePrefix.put(EntityType.class, "et"); //$NON-NLS-1$
		tablePrefix.put(EntityAttribute.class, "ea"); //$NON-NLS-1$
		tablePrefix.put(EntityAttributeValue.class, "eav"); //$NON-NLS-1$
	}

	
	/**
	 * Maps hibernate classes to database table names
	 */
	static {
		tableNames.put(Entity.class, "smart.entity"); //$NON-NLS-1$
		tableNames.put(EntityType.class, "smart.entity_type"); //$NON-NLS-1$
		tableNames.put(EntityAttribute.class, "smart.entity_attribute"); //$NON-NLS-1$
		tableNames.put(EntityAttributeValue.class, "smart.entity_attribute_value"); //$NON-NLS-1$
	}
	
	
	
	/**
	 * Creates the filter processor based on the query filter type
	 * 
	 * @param filterType
	 * @param queryDataTable
	 * @return
	 */
	public IFilterProcessor getFilterProcessor(IFilter.FilterType filterType, String queryDataTable, Query query){
		if (filterType == IFilter.FilterType.OBSERVATION){
			return new FilterProcessor(queryDataTable, this, query);
		}else if (filterType == IFilter.FilterType.GROUP) {
			return new WaypointGroupFilterProcessor(queryDataTable, this, query);
		}else {
			return new WaypointFilterProcessor(queryDataTable, this, query);
		}
	}
	
	public String getEntityDmAttributeKey(String entityKey,
			Connection c){
		String dmEntityTypeAttributeKey;
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT " + tablePrefix(Attribute.class) + ".keyid"); //$NON-NLS-1$ //$NON-NLS-2$
		sql.append(" FROM "); //$NON-NLS-1$
		sql.append(tableNamePrefix(EntityType.class));
		sql.append(" join "); //$NON-NLS-1$
		sql.append(tableNamePrefix(Attribute.class));
		sql.append(" on "); //$NON-NLS-1$
		sql.append(tablePrefix(EntityType.class) + ".dm_attribute_uuid = "); //$NON-NLS-1$
		sql.append(tablePrefix(Attribute.class) + ".uuid "); //$NON-NLS-1$
		sql.append(" WHERE "); //$NON-NLS-1$
		sql.append(tablePrefix(EntityType.class));
		sql.append(".keyid = '" + entityKey + "'"); //$NON-NLS-1$ //$NON-NLS-2$
		sql.append(" AND "); //$NON-NLS-1$
		sql.append(tablePrefix(EntityType.class));
		if (SmartDB.isMultipleAnalysis()){
			sql.append(".ca_uuid = x'" + UuidUtils.uuidToString(SmartDB.getConservationAreaConfiguration().getMainConservationArea().getUuid()) + "'"); //$NON-NLS-1$ //$NON-NLS-2$
		}else{
			sql.append(".ca_uuid = x'" + UuidUtils.uuidToString(SmartDB.getCurrentConservationArea().getUuid()) + "'"); //$NON-NLS-1$ //$NON-NLS-2$
		}

		try{
			QueryPlugIn.logSql(sql.toString());
			ResultSet rs = c.createStatement().executeQuery(sql.toString());
			if (rs.next()){
				dmEntityTypeAttributeKey = rs.getString(1);
			}else{
				throw new RuntimeException(MessageFormat.format(Messages.EntityAttributeFilter_NoAttributeFound, new Object[]{entityKey}));
			}
			rs.close();
		}catch (Exception ex){
			throw new RuntimeException(ex);
		}
		
		return dmEntityTypeAttributeKey;
		
	}
	
	/**
	 * Drops any tables created to support result test
	 * @param c
	 * @throws SQLException
	 */
	public abstract void dropTables(Connection c) throws SQLException;
}
