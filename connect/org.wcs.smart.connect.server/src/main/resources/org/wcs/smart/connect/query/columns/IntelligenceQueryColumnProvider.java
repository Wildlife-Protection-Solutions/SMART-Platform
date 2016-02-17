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
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.intelligence.query.IIntelligenceQueryColumnProvider;
import org.wcs.smart.intelligence.query.model.FixedQueryColumn;
import org.wcs.smart.intelligence.query.model.FixedQueryColumn.FixedColumns;
import org.wcs.smart.intelligence.query.model.IntelligenceRecordQuery;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryColumn;

/**
 * Implementation for column provider for intelligence queries.
 * 
 * @author Emily
 *
 */
public class IntelligenceQueryColumnProvider implements
		IIntelligenceQueryColumnProvider {
	
	private static Logger logger = Logger.getLogger(IntelligenceQueryColumnProvider.class.getName());
	
	@Override
	public QueryColumn[] getQueryColumns(Query query, Locale l, Session session) {
		try{
			String queryTypeKey = query.getTypeKey();
			if (queryTypeKey.equals(IntelligenceRecordQuery.KEY)){
				return getRecordColumns(query, l, session);
			}
		}catch (SQLException ex){
			logger.log(Level.SEVERE, "Error determining query columns.", ex); //$NON-NLS-1$
			return null;
		}
		return null;
	}

	private QueryColumn[] getRecordColumns(Query q, Locale l, Session session) throws SQLException{
		List<QueryColumn> cols = new ArrayList<QueryColumn>();
		for (FixedQueryColumn.FixedColumns col : FixedQueryColumn.FixedColumns.values()){
			if (col == FixedColumns.CA_ID || col == FixedColumns.CA_NAME){
				if (q.getConservationArea().getUuid().equals(ConservationArea.MULTIPLE_CA)){
					cols.add(new FixedQueryColumn(col, l));		
				}
			}else{
				cols.add(new FixedQueryColumn(col, l));
			}
		}
		return cols.toArray(new QueryColumn[cols.size()]);
	}

}
