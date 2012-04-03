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

import org.hibernate.Session;
import org.junit.Assert;
import org.junit.Test;
import org.wcs.smart.query.model.WaypointQuery;
import org.wcs.smart.query.parser.internal.Filter;
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
public class QueryHqlTest {

	private Filter parseQuery(String query) throws Exception{
		InputStream is = new ByteArrayInputStream(query.getBytes());
		Parser parser = new Parser(is);		
		Filter myQuery = parser.Expression();
		is.close();
		return myQuery;
	}
	
	@Test
	public void testAsHql() throws Exception{
		
		Session session = Hibernate.openSession();
		String query = null;
		Filter test = null;
		org.hibernate.Query  q = null;
		List results = null;
		
		query = "category:pigs.";
		test = parseQuery(query);
		results = test.getHibernateQuery(session).list();
		Assert.assertTrue(results.size() == 0);
		
		query = "category:threat.";
		test = parseQuery(query);
		results = test.getHibernateQuery(session).list();
		Assert.assertTrue(results.size() > 0);
		
		query = "attribute:n:sizeofcanl > 0";
		test = parseQuery(query);
		results = test.getHibernateQuery(session).list();
		Assert.assertTrue(results.size() > 0);
		
		query = "attribute:b:exists";
		test = parseQuery(query);
		results = test.getHibernateQuery(session).list();
		Assert.assertTrue(results.size() == 0);
		
		query = "attribute:s:color equals \"red\"";
		test = parseQuery(query);
		results = test.getHibernateQuery(session).list();
		Assert.assertTrue(results.size() == 0);
		
		query = "( category:threat AND attribute:n:sizeofcanl > 10 )";
		test = parseQuery(query);
		results = test.getHibernateQuery(session).list();
		Assert.assertTrue(results.size() > 0);
		
		query = "attribute:l:species = catfish";
		test = parseQuery(query);
		results = test.getHibernateQuery(session).list();
		Assert.assertTrue(results.size() == 0);
		
		//TODO: add back this test when ready
//		query = "attribute:t:species = green.blue";
//		test = parseQuery(query);
//		q = session.createQuery(test.asHql());
//		results = q.list();
//		Assert.assertTrue(results.size() == 0);
		
		
		session.close();
	}
	
	@Test
	public void testPatrolAsHql() throws Exception{
		
		Session session = Hibernate.openSession();
		String query = null;
		WaypointQuery test = null;
		org.hibernate.Query  q = null;
		List results = null;
		
		//patrol id
		query = "patrol:id equals \"EMILY_000001\"";
		test = parseQuery(query);
		results = test.getHibernateQuery(session).list();
		Assert.assertTrue(results.size() > 1);
		
		query = "patrol:id equals \"XXXXXXXX\"";
		test = parseQuery(query);
		results = test.getHibernateQuery(session).list();
		Assert.assertTrue(results.size() == 0);
		
		query = "patrol:id contains \"EMILY\"";
		test = parseQuery(query);
		results = test.getHibernateQuery(session).list();
		Assert.assertTrue(results.size() > 1);
		
		//armed
		query = "patrol:armed";
		test = parseQuery(query);
		results = test.getHibernateQuery(session).list();
		Assert.assertTrue(results.size() == 0);
		query = "not patrol:armed";
		test = parseQuery(query);
		results = test.getHibernateQuery(session).list();
		Assert.assertTrue(results.size() > 1);

		//patrol type
		query = "patrol:patroltype equals \"AIR\"";
		test = parseQuery(query);
		results = test.getHibernateQuery(session).list();
		Assert.assertTrue(results.size() == 0);
		
		query = "patrol:patroltype equals \"GROUND\"";
		test = parseQuery(query);
		results = test.getHibernateQuery(session).list();
		Assert.assertTrue(results.size() > 1);
		
		//transport type
		query = "patrol:transport equals \"616263\"";
		test = parseQuery(query);
		results = test.getHibernateQuery(session).list();
		Assert.assertTrue(results.size() == 0);
		
		query = "patrol:transport equals \"b0425f9689af493a87a11e8d6b8db7b1\"";
		test = parseQuery(query);
		results = test.getHibernateQuery(session).list();
		Assert.assertTrue(results.size() >= 0);
		
		
		query = "patrol:station equals \"b0425f9689af493a87a11e8d6b8db7b1\"";
		test = parseQuery(query);
		results = test.getHibernateQuery(session).list();
		Assert.assertTrue(results.size() == 0);
		
		query = "patrol:team equals \"b0425f9689af493a87a11e8d6b8db7b1\"";
		test = parseQuery(query);
		results = test.getHibernateQuery(session).list();
		Assert.assertTrue(results.size() == 0);
		
		query = "patrol:mandate equals \"b0425f9689af493a87a11e8d6b8db7b1\"";
		test = parseQuery(query);
		results = test.getHibernateQuery(session).list();
		Assert.assertTrue(results.size() == 0);
		
		query = "patrol:leader equals \"b0425f9689af493a87a11e8d6b8db7b1\"";
		test = parseQuery(query);
		results = test.getHibernateQuery(session).list();
		Assert.assertTrue(results.size() == 0);
		
		query = "patrol:pilot equals \"b0425f9689af493a87a11e8d6b8db7b1\"";
		test = parseQuery(query);
		results = test.getHibernateQuery(session).list();
		Assert.assertTrue(results.size() == 0);
		
		query = "patrol:leader equals \"21bd4111fb484f90b36422bb3370f3da\"";
		test = parseQuery(query);
		results = test.getHibernateQuery(session).list();
		Assert.assertTrue(results.size() > 1);
		
		query = "patrol:member equals \"21bd4111fb484f90b36422bb3370f3da\"";
		test = parseQuery(query);
		results = test.getHibernateQuery(session).list();
		Assert.assertTrue(results.size() > 1);
		
		session.close();
	}
}
