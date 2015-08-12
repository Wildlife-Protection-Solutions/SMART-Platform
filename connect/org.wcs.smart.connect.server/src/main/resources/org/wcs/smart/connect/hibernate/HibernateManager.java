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

public class HibernateManager {
	
	public static final String CONTEXT_KEY = "SessionFactory"; //$NON-NLS-1$
	
	public static Session getSession(ServletContext context){
		return ((SessionFactory)context.getAttribute(CONTEXT_KEY)).getCurrentSession();
	}
	
	public static Session getSession(ServletContext context, Locale l){
		I18nUtil.setLocale(l);
		return ((SessionFactory)context.getAttribute(CONTEXT_KEY)).getCurrentSession();
	}
	
	public static SessionFactory getSessionFactory(ServletContext context){
		return ((SessionFactory)context.getAttribute(CONTEXT_KEY));
	}
	
	public static SmartUser getUser(Session session, String username){
		SmartUser su = (SmartUser)session
				.createCriteria(SmartUser.class)
				.add(Restrictions.eq("username", username)) //$NON-NLS-1$
				.uniqueResult();
		return su;
	}
	
	public static List<SmartUser> getUsers(Session session){
		return (List<SmartUser>)session
					.createCriteria(SmartUser.class)
					.addOrder(Order.asc("username"))
					.list();
	}
	
}
