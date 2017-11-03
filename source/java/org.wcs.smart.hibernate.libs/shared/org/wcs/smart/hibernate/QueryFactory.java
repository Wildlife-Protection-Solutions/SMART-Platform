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
package org.wcs.smart.hibernate;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.hibernate.Session;
import org.hibernate.query.Query;

/**
 * Utility functions for using Hibernate 5 criteria queries
 * 
 * @author Emily
 *
 */
public class QueryFactory {

	public static <T> Query<T> buildQuery(Session session, Class<T> clazz, String field, Object value) {
		return buildQuery(session, clazz, new Object[] {field, value});
	}
	
	/**
	 * 
	 * @param session
	 * @param clazz
	 * @param parameters can be null if nothing to put in the where clause.  Otherwise all parameters
	 * are and'ed together.  Each parameters must be an array of key,value pairs. The key
	 * is a string representing the object field, and the value the equals object. For example:
	 * {{"conservationArea", ca}, {"keyId", keyId}}
	 * 
	 * @return
	 */
	public static <T> Query<T> buildQuery(Session session, Class<T> clazz, Object[]... parameters) {
		CriteriaBuilder cb = session.getCriteriaBuilder();
		CriteriaQuery<T> c = cb.createQuery(clazz);
		Root<T> root = c.from(clazz);
		if (parameters != null) {
			if (parameters.length == 1) {
				c.where(cb.equal(getRoot(root, ((String)parameters[0][0])), parameters[0][1]));
			}else {
				Predicate[] p = new Predicate[parameters.length];
				for (int i = 0; i < parameters.length; i ++) {
					if (parameters[i][1] == null) {
						p[i] = cb.isNull(getRoot(root, ((String)parameters[i][0])));
					} else {
						p[i] = cb.equal(getRoot(root, ((String)parameters[i][0])), parameters[i][1]);
					}
				}
				c.where(p);
			}
		}
		return session.createQuery(c);
	}
	
	private static <T> Path<Object> getRoot(Root<T> root, String key) {
		String[] parts = key.split("\\."); //$NON-NLS-1$
		Path<Object> exp = root.get(parts[0]);
		for (int i = 1; i < parts.length; i ++) {
			exp = exp.get(parts[i]);
		}
		return exp;
	}
	/**
	 * Creates a query that counts the result in the database and returns that count.
	 * 
	 * @param session
	 * @param clazz
	 * @param parameters
	 * @return
	 */
	public static <T> Long buildCountQuery(Session session, Class<T> clazz, Object[]... parameters) {
		
		CriteriaBuilder cb = session.getCriteriaBuilder();
		CriteriaQuery<Long> c = cb.createQuery(Long.class);
		Root<T> root = c.from(clazz);
		c.select(cb.count(root));
		if (parameters != null) {
			if (parameters.length == 1) {
				if (parameters[0][1] == null) {
					c.where(cb.isNull(getRoot(root, ((String)parameters[0][0]))));
				} else {
					c.where(cb.equal(getRoot(root, ((String)parameters[0][0])), parameters[0][1]));
				}
				
				
			}else {
				Predicate[] p = new Predicate[parameters.length];
				for (int i = 0; i < parameters.length; i ++) {
					if (parameters[i][1] == null) {
						p[i] = cb.isNull(getRoot(root, ((String)parameters[i][0])));
					} else {
						p[i] = cb.equal(getRoot(root, ((String)parameters[i][0])), parameters[i][1]);
					}
				}
				c.where(p);
			}
		}
		return session.createQuery(c).uniqueResult();
	}
}
