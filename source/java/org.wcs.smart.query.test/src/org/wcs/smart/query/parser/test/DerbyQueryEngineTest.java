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
package org.wcs.smart.query.parser.test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import org.junit.Test;
import org.wcs.smart.query.model.QueryResultItem;
import org.wcs.smart.query.model.observation.ObservationQuery;
import org.wcs.smart.query.parser.filter.ConservationAreaFilter;
import org.wcs.smart.query.parser.internal.filter.IFilter;
import org.wcs.smart.query.parser.internal.parser.Parser;

/**
 * TODO Purpose of 
 * <p>
 * <ul>
 * <li></li>
 * </ul>
 * </p>
 * @author Emily
 * @since 1.0.0
 */
public class DerbyQueryEngineTest {

	
	private IFilter parseQuery(String query) throws Exception{
		InputStream is = new ByteArrayInputStream(query.getBytes());
		Parser parser = new Parser(is);		
		IFilter myQuery = parser.Expression();
		is.close();
		return myQuery;
	}
	
	
	@Test
	public void simpleTest() throws Exception{
		Session session = Hibernate.openSession();
		
		ConservationArea ca = (ConservationArea) session.createCriteria(ConservationArea.class).list().get(0);
		ConservationAreaFilter ff = new ConservationAreaFilter();
		ff.addConservationArea(ca);
		
		SmartDB.setCurrentUser(null, ca);
		
		System.out.println(session.createCriteria(Patrol.class).list().size());
		
		String query = "category:threat.";
		ObservationQuery q = new ObservationQuery(parseQuery(query));
		q.setConservationAreaFilter(ff);
		
		List<QueryResultItem> results = q.getQueryResults(session, new NullProgressMonitor());
		System.out.println(results.size());
		
		session.close();
	}
}
