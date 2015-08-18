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
package org.wcs.smart.er.query.engine;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.query.common.engine.IResultItem;

/**
 * Paged result set for waypoint queries.
 * 
 * @author Emily
 *
 */
public class DerbyPagedWaypointResult extends AbstractSurveyPagedResult implements ISurveyQueryMissionResult{


	public DerbyPagedWaypointResult(String queryTempTable,DerbyWaypointEngine engine) {
		this.queryTempTable = queryTempTable;
		this.engine = engine;
	}

	public DerbyPagedWaypointResult(String queryTempTable, int itemCount, DerbyWaypointEngine engine) {
		this.queryTempTable = queryTempTable;
		this.itemCount = itemCount;
		this.engine = engine;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof DerbyPagedWaypointResult) {
			if (queryTempTable == null)
				return super.equals(obj);
			DerbyPagedWaypointResult r2 = (DerbyPagedWaypointResult) obj;
			return queryTempTable.equals(r2.queryTempTable);
		}
		return super.equals(obj);
	}
	

	protected List<IResultItem> getData(final Session session, final int offset, final int pageSize) {
		final List<IResultItem> result = new ArrayList<IResultItem>();
		final String dataSql = "SELECT r.* FROM " + queryTempTable + " r "+ buildSortSql();  //$NON-NLS-1$ //$NON-NLS-2$
		
		session.doWork(new Work() {
			@Override
			public void execute(Connection c) throws SQLException {
				if ((lastSortColumn == null && sortColumn != null) 
						|| (lastSortColumn != null && sortColumn != null && !lastSortColumn.equals(sortColumn)) ){
					updateSortColumn(sortColumn, session, c);
				}
				lastResultSet = c.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY).executeQuery(dataSql);
				//this forces garbage collection; without this the program
				//will fail with out of memory error when sorting
				//on columns multiple times.
				System.gc();
				
				result.addAll(getResults(lastResultSet, offset, pageSize, session));
				attachMissionProperties(result, c, session);
				attachSamplingUnitAttributes(result, c, session);
			}
		});
		return result;
	}
	
	@Override
	protected List<IResultItem> getNextData(final Session session, final int offset, final int pageSize) {
		if (lastResultSet == null)
			return null;
		final List<IResultItem> result = new ArrayList<IResultItem>();
		try {
			result.addAll(getResults(lastResultSet, offset, pageSize, session));
		} catch (SQLException e) {
			//most likely someone closed our old session/connection and old ResultSet is not working
			lastResultSet = null;
			return null;
		}
		session.doWork(new Work() {
			@Override
			public void execute(Connection c) throws SQLException {
				attachMissionProperties(result, c, session);
				attachSamplingUnitAttributes(result, c, session);
			}
		});
		return result;
	}
	
	@Override
	protected void dropResultSet() {
		if (lastResultSet != null) {
			try {
				if (!lastResultSet.isClosed()){
					lastResultSet.getStatement().close();
					lastResultSet.close();
				}
			} catch (SQLException e) {
				//nothing
				e.printStackTrace();
			}
			lastResultSet = null;
		}
	}
	
	
	@Override
	public String[] getTemporaryTableNames() {
		return new String[]{queryTempTable, 
				queryTempTable + "_mlist"}; //$NON-NLS-1$
	}



}
