/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.er.query.export.distance;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.hibernate.Session;
import org.wcs.smart.er.query.engine.DerbyPagedObservationResult;
import org.wcs.smart.er.query.engine.DerbySurveyQueryEngine;
import org.wcs.smart.er.query.model.SurveyQueryResultItem;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.util.UuidUtils;

/**
 * Tools to support the Distance query exporter.
 *
 * @author Emily
 * @since 7.0.0
 *
 */
public class DistanceExportHelper {
	
	private String originalTable = null;
	private DerbySurveyQueryEngine engine;
	
	public DistanceExportHelper(DerbyPagedObservationResult result) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException, NoSuchMethodException {
		originalTable = result.getResultsTable();
		engine = result.getEngine();
	}
	

	public String getSrcDataTable() {
		return originalTable;
	}

	public String getDataTable() {
		return originalTable + "_edistance"; //$NON-NLS-1$
	}
	
	public DerbyPagedObservationResult createResultSet(boolean orderbystratum) {
		return new DerbyPagedObservationResult(getDataTable(), engine) {
			@Override
			protected void attachObservations(List<IResultItem> result, Connection c, Session session) throws SQLException {
				boolean hasObservations = false;
				StringBuilder attrSql = new StringBuilder();
				attrSql.append("SELECT r.ob_uuid, a.keyid, wpoa.number_value, wpoa.string_value, rl.value as list_value, rt.value as tree_value, r.ca_uuid FROM "); //$NON-NLS-1$
				attrSql.append(getDataTable());
				attrSql.append(" r left join smart.wp_observation_attributes wpoa on r.ob_uuid = wpoa.observation_uuid left join smart.dm_attribute a on a.uuid = wpoa.attribute_uuid left join "); //$NON-NLS-1$
				attrSql.append(getSrcDataTable()).append("_list rl on wpoa.list_element_uuid = rl.uuid left join "); //$NON-NLS-1$
				attrSql.append(getSrcDataTable()).append("_tree rt on wpoa.tree_node_uuid = rt.UUID WHERE r.ob_uuid in ("); //$NON-NLS-1$
				for (IResultItem irt : result) {
					SurveyQueryResultItem it = (SurveyQueryResultItem)irt;
					if (it.getObservationUuid() != null) {
						if (hasObservations) {
							attrSql.append(',');
						}
						hasObservations = true;
						attrSql.append("x'").append(UuidUtils.uuidToString(it.getObservationUuid())).append('\''); //$NON-NLS-1$
					}
				}
				
				
				if (!hasObservations) {
					//no observations in current data fragment, so no need to select attributes as they will be empty
					return;
				}
				attrSql.append(')');

				try(ResultSet rs = c.createStatement().executeQuery(attrSql.toString())) {
					HashMap<UUID, HashMap<String, Object>> attrMap = getResultsAttributes(rs, session);
					for (IResultItem irt : result) {
						SurveyQueryResultItem it = (SurveyQueryResultItem)irt;
						if (it.getObservationUuid() != null) {
							HashMap<String, Object> attributes = attrMap.get(it.getObservationUuid());
							if (attributes != null) {
								it.setAttributes(attributes);
							}
						}
					}
				}	
			}
			protected String buildSortSql() {
				//always sort by stratum, and sampling unit id
				StringBuilder sb = new StringBuilder();
				sb.append(" ORDER BY "); //$NON-NLS-1$
				if (orderbystratum) {
					sb.append(DistanceQueryExporter.STRATUM_SORT_COLUMN);
					sb.append("," ); //$NON-NLS-1$
				}
				sb.append("samplingunit_id"); //$NON-NLS-1$
				return sb.toString();
			}
		};
	}

}
