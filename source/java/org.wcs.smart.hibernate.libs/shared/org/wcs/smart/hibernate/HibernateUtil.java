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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.Session;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.spi.JdbcServices;

/**
 * Collection of utilities shared with Desktop and Connect applications.
 * 
 * @author Emily
 *
 */
public class HibernateUtil {
	
	/**
	 * the current dialog in use 
	 * @return
	 */
	@SuppressWarnings("deprecation")
	public static Dialect getHibernateCurrentDialect(Session session) {
		try {
			if (session != null && session.getSessionFactory() != null) {
				return session.getSessionFactory().getSessionFactoryOptions().getServiceRegistry().getService(JdbcServices.class).getDialect();
			}
		}catch (Exception ex) {
			Logger.getLogger(HibernateUtil.class.getName()).log(Level.WARNING, ex.getMessage(), ex);
		}
		return null;
	}
	
	
	public static <T> void mergeCollection(Collection<T> toUpdate, Collection<T> newList) {
		if (toUpdate == null) return;
		List<T> toDelete = new ArrayList<>();
		for (T x : toUpdate) {
			if (!newList.contains(x)) toDelete.add(x);
		}
		toUpdate.removeAll(toDelete);
		
		for (T item : newList) {
			if (!toUpdate.contains(item)) toUpdate.add(item);
		}
	}
	
	public static <T> T findInList(Collection<T> list, T item){
		for (T i : list) {
			if (i.equals(item)) return i;
		}
		return null;
	}
}
