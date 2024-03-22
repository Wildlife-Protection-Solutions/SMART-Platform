/*
 * Copyright (C) 2015 Wildlife Conservation Society
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
package org.wcs.smart.connect.query.columns;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.Session;
import org.wcs.smart.asset.query.model.AssetObservationQuery;
import org.wcs.smart.asset.query.model.AssetWaypointQuery;
import org.wcs.smart.asset.query.model.IAssetQueryColumnProvider;
import org.wcs.smart.asset.query.model.observation.FixedQueryColumn;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.connect.query.engine.AbstractQueryEngine;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.query.model.WaypointGeometryQueryColumn;

/**
 * Query column provider for asset queries.
 * 
 * @author Emily
 *
 */
public class AssetQueryColumnProvider implements IAssetQueryColumnProvider{

	private static Logger logger = Logger.getLogger(AssetQueryColumnProvider.class.getName());

	@Override
	public QueryColumn[] getQueryColumns(Query query, Locale l, boolean includeIdColumns, Session session) {
		String queryTypeKey = query.getTypeKey();
		try{
			if (queryTypeKey.equals(AssetObservationQuery.KEY)){
				return getObservationQueryColumns(query, l, includeIdColumns, session);
			}
			if (queryTypeKey.equals(AssetWaypointQuery.KEY)){
				return getWaypointQueryColumns(query, l, includeIdColumns, session);
			}
		}catch (Exception ex) {
			logger.log(Level.SEVERE, "Error determining query columns.", ex); //$NON-NLS-1$
			return null;
		}
		return null;
	}

	private QueryColumn[] getObservationQueryColumns(Query query, Locale l, boolean includeIdColumns, Session session) throws SQLException {
			//load from the database 
			ArrayList<QueryColumn> cols = new ArrayList<QueryColumn>();
			
			for (int i = 0; i < FixedQueryColumn.FixedColumns.values().length; i++) {
				FixedQueryColumn.FixedColumns item = FixedQueryColumn.FixedColumns.values()[i];
				if (item == FixedQueryColumn.FixedColumns.OBS_GROUP_ID) continue;
				boolean add = true;
				if (item == FixedQueryColumn.FixedColumns.CA_ID || item == FixedQueryColumn.FixedColumns.CA_NAME){
					add = query.getConservationArea().getUuid().equals(ConservationArea.MULTIPLE_CA);
				}
				if (add){
					QueryColumn toAdd = new FixedQueryColumn(item, l);
					cols.add(toAdd);

				}
			}
			for (QueryColumn qc : QueryColumnUtils.getDataModelColumns(session, l, AbstractQueryEngine.parseConservationAreaFilter(query))){
				cols.add(qc);
			}
			cols.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.OBS_GROUP_ID, l));
			cols.add(new WaypointGeometryQueryColumn(l));

			if (includeIdColumns ) {
				cols.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.WAYPOINT_UUID, Locale.getDefault()));
				cols.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.OBSERVATION_UUID, Locale.getDefault()));
			}
			return cols.toArray(new QueryColumn[cols.size()]);
	}
		
	private QueryColumn[] getWaypointQueryColumns(Query query, Locale l, boolean includeIdColumns, Session session) throws SQLException {
		//load from the database 
		ArrayList<QueryColumn> cols = new ArrayList<QueryColumn>();
		
		for (int i = 0; i < FixedQueryColumn.FixedColumns.values().length; i++) {
			FixedQueryColumn.FixedColumns item = FixedQueryColumn.FixedColumns.values()[i];
			if (item == FixedQueryColumn.FixedColumns.OBS_GROUP_ID) continue;

			boolean add = true;
			if (item == FixedQueryColumn.FixedColumns.CA_ID || item == FixedQueryColumn.FixedColumns.CA_NAME){
				add = query.getConservationArea().getUuid().equals(ConservationArea.MULTIPLE_CA);
			}
			if (add){
				QueryColumn toAdd = new FixedQueryColumn(item, l);
				cols.add(toAdd);
			}
		}
		cols.add(new WaypointGeometryQueryColumn(l));
		
		if (includeIdColumns ) {
			cols.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.WAYPOINT_UUID, Locale.getDefault()));
		}
		return cols.toArray(new QueryColumn[cols.size()]);
}
	
}

