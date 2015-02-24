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
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.hibernate.Interceptor;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.hql.spi.QueryTranslator;
import org.hibernate.hql.spi.QueryTranslatorFactory;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.ServiceRegistryBuilder;

/**
 * Manage hibernate connections and mappings.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class SmartHibernateManager {
	
	protected static SessionFactory sessionFactory = null;

	private static final String MAPPING_ID = "org.wcs.smart.hibernate.mapping"; //$NON-NLS-1$
	
	private static  String userName = "login"; //$NON-NLS-1$
	private static  String passWord = "smrt"; //$NON-NLS-1$
	private static String databaseLocation = ""; //$NON-NLS-1$
	
	public static final ThreadLocal<Session> sessionMapsThreadLocal = new ThreadLocal<Session>();
	//TODO: perhaps this can be replaced with
	//hibernate.current_session_context_class=thread
	//hibernate.transaction.factory_class=JDBCTransactionFactory
	
	/**
	 * Sets the database connection parameters.
	 * @param dbLocation the derby database location
	 */
	public static void setDatabaseParameter(String dbLocation){
		databaseLocation = dbLocation;
	}
	
	/**
	 * Sets the user name and password to connect to database with.
	 * 
	 * @param username
	 * @param password
	 */
	public static void setUserName(String username, String password){
		userName = username;
		passWord = password;
		if (sessionFactory != null){
			try{
				sessionFactory.close();
			}catch (Exception ex){
				ex.printStackTrace();
			}
		}
		sessionFactory = null;
	}
	
	/**
	 * Creates a new session factory.
	 * 
	 */
	public static synchronized final void createSessionFactory(){
		
		if (sessionFactory == null){
			Configuration config = new Configuration().configure(Thread.currentThread().getContextClassLoader().getResource("hibernate.cfg.xml")); //$NON-NLS-1$
			
			config.setProperty("hibernate.connection.username", userName); //$NON-NLS-1$
			config.setProperty("hibernate.connection.password", passWord); //$NON-NLS-1$
			config.setProperty("hibernate.connection.url", "jdbc:derby:" + databaseLocation + ";create=false"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			
			//add mapping classes
			for (Class<?> c: getMappings()){
				config.addAnnotatedClass(c);
			}
			
			ServiceRegistry service = new ServiceRegistryBuilder().applySettings(config.getProperties()).buildServiceRegistry();
			sessionFactory = config.buildSessionFactory(service);
			
			
			if (!((SessionFactoryImplementor)sessionFactory).getDialect().supportsSequences()){
				//fail
				throw new IllegalStateException("You can't use this database - it does not support sequences"); //$NON-NLS-1$
			}
		}
	}
	
	/**
	 * Convert hql to sql
	 * @param hqlQueryText
	 * @return
	 */
	public static String toSql(String hqlQueryText) {
		if (hqlQueryText != null && hqlQueryText.trim().length() > 0) {

			final QueryTranslatorFactory translatorFactory = ((SessionFactoryImpl) sessionFactory)
					.getSettings().getQueryTranslatorFactory();

			final SessionFactoryImplementor factory = (SessionFactoryImplementor) sessionFactory;

			final QueryTranslator translator = translatorFactory
					.createQueryTranslator(hqlQueryText, hqlQueryText,
							Collections.EMPTY_MAP, factory);
			translator.compile(Collections.EMPTY_MAP, false);
			return translator.getSQLString();
		}
		return null;
	}
	
	/**
	 * Users are required to close the session when they are done with it.
	 * @return
	 */
	public synchronized static final Session openSession(){
		
		if (sessionFactory == null){
			createSessionFactory();
		}
		Session session = (Session) sessionMapsThreadLocal.get();
		if (session == null || !session.isOpen() ){
			session = sessionFactory.openSession();
			sessionMapsThreadLocal.set(session);
		}
		return session;
	}
	
	/**
	 * Users are required to close the session when they are done with it.
	 * <p>
	 * Note you ensure that the current thread session is closed; otherwise
	 * this will not close it to re-open it with the correct interceptor.
	 * </p> 
	 * @param interceptor a session interceptor
	 * @return
	 */
	public synchronized static final Session openSession(Interceptor interceptor){
		if (sessionFactory == null){
			createSessionFactory();
		}
		Session session = (Session) sessionMapsThreadLocal.get();
		
		if (session == null || !session.isOpen() ){
			session = sessionFactory.withOptions().interceptor(interceptor).openSession();
			sessionMapsThreadLocal.set(session);
		}
		
		return session;
	}
	
	/**
	 * Closes the current session factory.
	 */
	protected static void endSessionFactory(){
		if (sessionFactory != null){
			sessionFactory.close();
		}
		sessionFactory = null;
	}
	
	
	/**
	 * @return gets all hibernate mappings
	 */
	private static final List<Class<?>>  getMappings(){
		List<Class<?>> items = new ArrayList<Class<?>>();
		if (Platform.getExtensionRegistry() == null) return Collections.emptyList();
		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(MAPPING_ID);
		try {
			for (IConfigurationElement e : config) {
				items.add(Class.forName(e.getAttribute("class"))); //$NON-NLS-1$
			}
		}catch (Exception ex){
			ex.printStackTrace();
		}
		return items;
	}

	/**
	 * For a given hibernate mapped class this returns the 
	 * the ca_property field of the extension point.
	 * 
	 * @param clazz the mapped hibernate query
	 * @return the ca_property 
	 */
	public static final String getHqlExportQuery(Class<?> clazz){
		if (Platform.getExtensionRegistry() == null) return null;
		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(MAPPING_ID);
		try {
			for (IConfigurationElement e : config) {
				Class<?> clzz = Class.forName(e.getAttribute("class")); //$NON-NLS-1$
				if (clzz == clazz){
					return e.getAttribute("ca_property"); //$NON-NLS-1$
				}
			}
		}catch (Exception ex){
			ex.printStackTrace();
		}
		return null;
	}
}
