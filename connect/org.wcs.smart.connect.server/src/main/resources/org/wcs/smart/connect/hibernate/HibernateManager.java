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
package org.wcs.smart.connect.hibernate;

import java.util.List;
import java.util.Locale;

import javax.servlet.ServletContext;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.connect.model.SmartUser;
import org.wcs.smart.util.I18nUtil;

/**
 * Hibernate manager for SMART Connect application.
 * @author Emily
 *
 */
public class HibernateManager {
	
	public static final String CONTEXT_KEY = "SessionFactory"; //$NON-NLS-1$
	
	/**
	 * Creates a new session from the given context.
	 * @param context
	 * @return
	 */
	public static Session getSession(ServletContext context){
		return ((SessionFactory)context.getAttribute(CONTEXT_KEY)).getCurrentSession();
	}
	
	/**
	 * Creates a new session from the given context that uses the given
	 * locale for loading database labels. 
	 *  
	 * @param context
	 * @param l
	 * @return
	 */
	public static Session getSession(ServletContext context, Locale l){
		I18nUtil.setLocale(l);
		return getSession(context);
	}
	
	/**
	 * Returns the session factory.
	 * 
	 * @param context
	 * @return
	 */
	public static SessionFactory getSessionFactory(ServletContext context){
		return ((SessionFactory)context.getAttribute(CONTEXT_KEY));
	}
	
	/**
	 * Finds the smart user from the given username.
	 * 
	 * @param session
	 * @param username
	 * @return
	 */
	public static SmartUser getUser(Session session, String username){
		SmartUser su = (SmartUser)session
				.createCriteria(SmartUser.class)
				.add(Restrictions.eq("username", username)) //$NON-NLS-1$
				.uniqueResult();
		return su;
	}
	
	/**
	 * Finds all users in the database.
	 * 
	 * @param session
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static List<SmartUser> getUsers(Session session){
		return (List<SmartUser>)session
					.createCriteria(SmartUser.class)
					.addOrder(Order.asc("username"))
					.list();
	}
	
}
