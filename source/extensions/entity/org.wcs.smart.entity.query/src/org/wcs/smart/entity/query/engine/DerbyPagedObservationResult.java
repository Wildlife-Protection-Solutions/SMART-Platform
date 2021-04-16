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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.hibernate.Session;
import org.wcs.smart.entity.query.model.EntityObservationResultItem;
import org.wcs.smart.entity.query.model.columns.FixedQueryColumn;
import org.wcs.smart.entity.query.parser.internal.EntityAttributeFilter;
import org.wcs.smart.entity.query.parser.internal.EntityTypeFilter;
import org.wcs.smart.filter.IFilter;
import org.wcs.smart.filter.IFilterVisitor;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.common.engine.IAttachmentResultItem;
import org.wcs.smart.query.common.engine.IObservationQueryResultItem;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.common.engine.ObservationQueryResult;
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.query.model.QueryColumn.ColumnType;
import org.wcs.smart.util.UuidUtils;

/**
 * Wrapper for resulted temporary table which was build for particular query.
 * Provides ability to lazy load items from this table and sorting
 * functionality.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class DerbyPagedObservationResult extends ObservationQueryResult<EntityObservationResultItem> {

	private List<String> entityTypes;

	public DerbyPagedObservationResult(DerbyEntityObservationEngine engine, SimpleQuery query) throws Exception {
		super(engine, -1, -1);
		

		entityTypes = new ArrayList<String>();
		query.getFilter().getFilter().accept(new IFilterVisitor() {
			@Override
			public void visit(IFilter filter) {
				if (filter instanceof EntityAttributeFilter) {
					entityTypes.add(((EntityAttributeFilter) filter)
							.getEntityKey());
				}else if (filter instanceof EntityTypeFilter){
					entityTypes.add( ((EntityTypeFilter)filter).getEntityTypeKey());
				}
			}
		});
	}

	
	protected void attachObservations(List<? extends IObservationQueryResultItem> result,
			String queryTempTable,
			String labelTable,
			Connection c, Session session) throws SQLException {
		super.attachObservations(result, queryTempTable, labelTable, c, session);
		
	
		if (entityTypes.size() > 0) {
			// attach entity attributes
			StringBuilder attrSql = new StringBuilder();
			attrSql.append("SELECT r.ob_uuid, a.keyid as entitykey, ea.keyid as entityattributekey, eav.number_value, "); //$NON-NLS-1$
			attrSql.append("eav.string_value, ll.value as label_value, r.ca_uuid "); //$NON-NLS-1$
			attrSql.append(" FROM "); //$NON-NLS-1$
			attrSql.append(queryTempTable);
			attrSql.append(" r join smart.wp_observation_attributes wpoa on r.ob_uuid = wpoa.observation_uuid join smart.entity_type a on a.dm_attribute_uuid = wpoa.attribute_uuid "); //$NON-NLS-1$
			attrSql.append(" join smart.entity e on e.attribute_list_item_uuid = wpoa.list_element_uuid "); //$NON-NLS-1$
			attrSql.append(" join smart.entity_attribute_value eav on eav.entity_uuid = e.uuid "); //$NON-NLS-1$
			attrSql.append(" join smart.entity_attribute ea on ea.uuid = eav.entity_attribute_uuid "); //$NON-NLS-1$
			attrSql.append(" left join "); //$NON-NLS-1$
			attrSql.append(labelTable);
			attrSql.append(" ll on ll.uuid = "); //$NON-NLS-1$
			attrSql.append(" case when eav.list_element_uuid is not null then eav.list_element_uuid else eav.tree_node_uuid end "); //$NON-NLS-1$
			attrSql.append(" WHERE "); //$NON-NLS-1$
			attrSql.append(" r.ob_uuid in ("); //$NON-NLS-1$
			
			String obs =  result.stream()
					.filter(e->e.getObservationUuid() != null)
					.map(e-> (String)("x'" + UuidUtils.uuidToString(e.getObservationUuid()) + "'")) //$NON-NLS-1$ //$NON-NLS-2$
					.collect(Collectors.joining(",")); //$NON-NLS-1$
			
			attrSql.append(obs);
			attrSql.append(") AND "); //$NON-NLS-1$
			attrSql.append("a.keyid in ("); //$NON-NLS-1$
			for (String et : entityTypes) {
				attrSql.append("'" + et + "',"); //$NON-NLS-1$//$NON-NLS-2$
			}
			attrSql.deleteCharAt(attrSql.length() - 1);
			attrSql.append(")"); //$NON-NLS-1$
			
			
			QueryPlugIn.logSql(attrSql.toString());

			try (ResultSet rs = c.createStatement().executeQuery(attrSql.toString())) {
				while (rs.next()) {
					byte[] obuuid = rs.getBytes(1);
					String entityKey = rs.getString(2);
					String entityAttributeKey = rs.getString(3);
					Object value = null;
					if (rs.getObject(4) != null) {
						value = rs.getDouble(4);
					} else if (rs.getObject(5) != null) {
						value = rs.getString(5);
					} else if (rs.getObject(6) != null) {
						value = rs.getString(6);
					} else if (rs.getObject(7) != null) {
						value = rs.getString(7);
					}

					for (IResultItem rii : result) {
						EntityObservationResultItem it = (EntityObservationResultItem) rii;
						if (it.getObservationUuid() != null
								&& it.getObservationUuid().equals(
										UuidUtils.byteToUUID(obuuid))) {
							it.addEntityAttribute(entityKey,
									entityAttributeKey, value);
						}
					}
				}
			}

		}

	}

	@Override
	public String buildSortSql() {
		if (sortColumn == null || direction == SWT.NONE) {	
			//default to waypoint date/time, location, station
			StringBuilder sb = new StringBuilder();
			sb.append("ORDER BY "); //$NON-NLS-1$
			sb.append(FixedQueryColumn.getDbColumnName(FixedQueryColumn.FixedColumns.WAYPOINT_DATE.getKey()));
			sb.append(" DESC "); //$NON-NLS-1$
			return sb.toString();
		}

		String result = ""; //$NON-NLS-1$
		if (sortColumn instanceof FixedQueryColumn) {
			String key = sortColumn.getKey();
			key = FixedQueryColumn.getDbColumnName(key);
			if (sortColumn.getKey().equals(
					FixedQueryColumn.FixedColumns.WAYPOINT_TIME.getKey())) {
				result = "order by CAST(r." + key + " as TIME)"; //$NON-NLS-1$ //$NON-NLS-2$
			} else if (sortColumn.getType() == ColumnType.STRING) {
				result = "order by UPPER(r." + key + ")"; //$NON-NLS-1$ //$NON-NLS-2$
			} else {
				result = "order by r." + key; //$NON-NLS-1$
			}
		}else {
			result = super.getSortColumnString(sortColumn);
		}
		
		if (result != null && !result.isEmpty()) {
			result += direction == SWT.UP ? " asc" : " desc"; //$NON-NLS-1$ //$NON-NLS-2$
		}
		return result;
	}

	

	@Override
	protected String getDistinctWaypointQuery(String prefix, boolean includeObservation) {
		return null;
	}

	
	@Override
	public void createTooltip(IAttachmentResultItem data, Composite parent) {
	}

}
