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
package org.wcs.smart.connect.report.query;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.eclipse.datatools.connectivity.oda.OdaException;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.connect.query.QueryManager;
import org.wcs.smart.data.oda.smart.impl.AbstractSmartBirtQuery;
import org.wcs.smart.data.oda.smart.impl.SmartConnection;
import org.wcs.smart.data.oda.smart.impl.table.SmartBirtTable;
import org.wcs.smart.query.common.engine.IQueryEngine;
import org.wcs.smart.query.common.engine.IQueryResult;
import org.wcs.smart.query.model.Query;

/**
 * Implementation class of IConnection for SMART ODA runtime driver.
 */
public class ServerSmartConnection extends SmartConnection {

	public static final String CONNECTION_KEY = "org.wcs.smart.session"; //$NON-NLS-1$
	private SmartBirtTableUtils tableFinder = null;
	
	@Override
	public void openSession(){
		localSession = (Session) ((Map)appContext).get(CONNECTION_KEY);
		localSession.beginTransaction();
	}
	
	@Override
	public void closeSession(){
		if (localSession != null){
			if (localSession.getTransaction().isActive()){
				localSession.getTransaction().rollback();
			}
		}
	}

	@Override
	public AbstractSmartBirtQuery createQuery(){
		return new SmartQuery(this);
	}

	@Override
	public IQueryResult executeQuery(Query query) throws Exception {
		IQueryEngine engine = QueryManager.INSTANCE.findQueryEngine(query);
		
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		parameters.put(Session.class.getName(), localSession);
		parameters.put(Locale.class.getName(), getCurrentLocale());
		
		IQueryResult results = engine.executeQuery(query, parameters);
		query.setCachedResults(results);
		return results;
	}

	@Override
	public SmartBirtTable findSmartBirtTable(String queryText) throws OdaException {
		try{
			if (tableFinder == null){
				tableFinder = new SmartBirtTableUtils();
			}
			SmartBirtTable table = tableFinder.findTable(queryText, this);
			if (table == null){
				throw new OdaException(
						MessageFormat.format("Could not find SMART data table {0}.", new Object[]{queryText}));
			}
			return table;
		}catch (Exception ex){
			throw new OdaException (ex);
		}
	}

	@Override
	public Collection<ConservationArea> getConservationAreas() {
		Object x = appContext.get("org.wcs.smart.report.ca");
		if (x == null) return null;
		if (x instanceof ConservationArea){
			return Collections.singleton((ConservationArea)x);
		}
		return (Collection<ConservationArea>)x;
	}

}
