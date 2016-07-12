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
package org.wcs.smart.query.common.model;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.UUID;

import org.hibernate.Session;
import org.wcs.smart.query.common.engine.IQueryResult;

/**
 * Results for compound query.  Contains a map of query results
 * for each sub query.
 * 
 * @author Emily
 *
 */
public class CompoundMapQueryResults implements IQueryResult {

	private boolean isDisposed = false;
	
	private HashMap<UUID, IQueryResult> queryResults = new HashMap<UUID, IQueryResult>();
	
	public void clear(Session session) throws SQLException{
		for (IQueryResult r : queryResults.values()){
			r.dispose(session);
		}
		queryResults.clear();
	}
	
	public void addResults(UUID queryUuid, IQueryResult results){
		queryResults.put(queryUuid, results);
	}
	
	public IQueryResult getResults(UUID queryUuid){
		return queryResults.get(queryUuid);
	}
	
	@Override
	public void dispose(Session session) throws SQLException {
		isDisposed = true;
	}

	@Override
	public boolean isDisposed() {
		return isDisposed;
	}

}
