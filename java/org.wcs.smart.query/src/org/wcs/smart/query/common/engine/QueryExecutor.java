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
package org.wcs.smart.query.common.engine;

import java.text.MessageFormat;
import java.util.HashMap;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.QueryTypeManager;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.Query;

/**
 * Executes a query and returns the query results.
 * 
 * @author Emily
 *
 */
public enum QueryExecutor {
	
	INSTANCE;
	
	public IQueryResult executeQuery(Query query, Session session, IProgressMonitor monitor ) throws Exception{
		IQueryType type = QueryTypeManager.INSTANCE.findQueryType(query.getTypeKey());
		IQueryEngine engine = findQueryEnginge(type);
		if (engine == null){
			throw new Exception(MessageFormat.format(Messages.QueryExecutor_QueryEngineNotFound, type.getGuiName()));
		}
		Session lSession = session;
		if (lSession == null){
			lSession = HibernateManager.openSession();
			lSession.beginTransaction();
		}
		try{
			HashMap<String, Object> parameters = new HashMap<String, Object>();
			parameters.put(Session.class.getName(), lSession);
			parameters.put(IProgressMonitor.class.getName(), monitor);
		
			IQueryResult results = engine.executeQuery(query, parameters);
			query.setCachedResults(results);
			return results;
		}finally{
			if (session == null && lSession.isOpen()){
				lSession.getTransaction().commit();
				lSession.close();
			}
		}
	}
	
	private IQueryEngine findQueryEnginge(IQueryType qType) throws Exception{
		return QueryTypeManager.INSTANCE.getQueryEngine(qType);
	}
}
