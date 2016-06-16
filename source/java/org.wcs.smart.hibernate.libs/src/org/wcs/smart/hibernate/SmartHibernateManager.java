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
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Semaphore;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.InvalidRegistryObjectException;
import org.eclipse.core.runtime.Platform;
import org.hibernate.BaseSessionEventListener;
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

	/*
	 * Amount of time to wait between checking if session
	 * has completed in millisecons
	 */
	private static final long SESSION_CLOSE_WAIT = 1000;
	/*
	 * Number of tries before forcing close of sessions 
	 */
	private static final long SESSION_WAIT_COUNT = 10;
	
	
	protected static SessionFactory sessionFactory = null;

	private static final String MAPPING_ID = "org.wcs.smart.hibernate.mapping"; //$NON-NLS-1$
	
	private static  String userName = "login"; //$NON-NLS-1$
	private static  String passWord = "smrt"; //$NON-NLS-1$
	private static String databaseLocation = ""; //$NON-NLS-1$
	
	private static ThreadLocal<Session> sessionMapsThreadLocal = new ThreadLocal<Session>();

	/*
	 * collection of all sessions opened 
	 */
	private static final List<Session> allSessions = Collections.synchronizedList(new ArrayList<Session>());
	
	/*
	 * Lock for database locking.  This is necessary
	 * when applying changes or packaging changes.
	 */
	private static Semaphore thisLock = new Semaphore(1);
	
	private static final Object sessionFactoryLock = new Object();
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
	 * @throws Exception 
	 */
	public static void setUserName(String username, String password) throws Exception{
		userName = username;
		passWord = password;
		endSessionFactory(false);
	}
	
	/**
	 * Creates a new session factory.
	 * 
	 */
	private static final void createSessionFactory(){
		synchronized (sessionFactoryLock) {
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
	 * <p>Attempts to lock the database for the given session.  If it returns
	 * then you can assume you have the only active session to the database.  
	 * You need to call unlockDatabase when done. 
	 * Once locked no other threads will be able to open session until unlockDatabase()
	 * is called.</p>
	 * 
	 * <p>
	 * This attempts to acquire a lock, if acquired it waits a reasonable amount of time for
	 * all other session to close.  If they don't close, then it releases the lock, waits briefly 
	 * then attempts to repeat the process.  It tries 10 times then fails and throws an Exception.
	 * </p>
	 *  
	 * 
	 * @param currentSession
	 * @throws Exception
	 */
	public static void lockDatabase(Session currentSession) throws Exception{
		for (int i = 0; i < 10; i ++){
			//acquire lock
			thisLock.acquire();

			//wait for all sessions except the current session 
			int cnt = 0;
			while(allSessions.size() > 1 && cnt < SESSION_WAIT_COUNT){
				//wait for thread to be closed
				try {
					Thread.sleep( 100 );
				} catch (InterruptedException e) {}
				cnt++;
			}
			if (allSessions.size() > 1 || allSessions.size() == 0 || allSessions.get(0) != currentSession){
				//cannot lock the database; lets release the lock giving other threads a chance
				//to get a session and complete;
				thisLock.release();
				Thread.sleep( 100 );
			}else{
				return;
			}
		}
		
		throw new Exception("Cannot acquire database lock."); //$NON-NLS-1$
	}
	
	public static void unlockDatabase(){
		thisLock.release();
	}
	/**
	 * Locks the database by waiting for active sessions
	 * to complete or closing them automatically and preventing
	 * any other sessions from being granted.  MUST call unlock 
	 * database when complete.  Once locked it closes the session
	 * factory and opens a new session using the given username and password.
	 * @return
	 */
	public static Session lockDatabase(String username, String password) throws Exception{
		//acquire lock
		thisLock.acquire();
		
		//finish or close all sessions
		int cnt = 0;
		while(allSessions.size() > 0 && cnt < SESSION_WAIT_COUNT){
			//wait for thread to be closed
			try {
				Thread.sleep( SESSION_CLOSE_WAIT );
			} catch (InterruptedException e) {}
			cnt++;
		}
		if (allSessions.size() > 0){
			//TODO: warn users or log???
			List<Session> toClose = new ArrayList<Session>();
			synchronized (allSessions) {
				toClose.addAll(allSessions);
			}
			for(Session s : toClose){
				//a listener on the session removes it from the all
				//sessions variable
				if (s.getTransaction().isActive()){
					//TODO: log this case
					s.getTransaction().rollback();
				}
				s.close();
			}
		}
		//ensure all sessions are closed
		SmartHibernateManager.endSessionFactoryNoLock();
		if (username != null){
			userName = username;
			passWord = password;
		}
		
		//open new session
		return openSessionOnly(null);
	}
	
	/**
	 * Unlocks the database, reconnecting using the provided username
	 * and password.  Must be called after lockDatabase.
	 * 
	 * @param username the username to connect to the database after unlocking
	 * @param password the password to connect after unlocking 
	 */
	public static void unlockDatabase(String username, String password){
		thisLock.release();
		if (username != null){
			SmartHibernateManager.endSessionFactoryNoLock();
			userName = username;
			passWord = password;
		}
	}
	
	/**
	 * Users are required to close the session when they are done with it.
	 * @return
	 */
	protected static Session openSession(){
		return openSession(null);
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
	protected static Session openSession(Interceptor interceptor) {
		//ensure the database is not locked then acquire session
		try {
			thisLock.acquire();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		thisLock.release();
		return openSessionOnly(interceptor);	
	}
	
	private static Session openSessionOnly(Interceptor interceptor){
		if (sessionFactory == null){
			createSessionFactory();
		}
		Session session = (Session) sessionMapsThreadLocal.get();
		if (session == null || !session.isOpen() ){
			if (interceptor == null){
				session = sessionFactory.openSession();
			}else{
				session = sessionFactory.withOptions().interceptor(interceptor).openSession();
			}
			//add to thread
			sessionMapsThreadLocal.set(session);
			
			//add to session collection with listener to remove when closed
			allSessions.add(session);
			final Session tmp = session;
			session.addEventListeners(new BaseSessionEventListener() {
				private static final long serialVersionUID = 1L;
				@Override
				public void end() {
					allSessions.remove(tmp)	;
				}	
			});
		}
		
		if (interceptor != null && interceptor instanceof SessionInterceptor){
			((SessionInterceptor)interceptor).setSession(session);
		}
		return session;
		
	}
	
	protected static void endSessionFactoryNoLock(){
		synchronized (sessionFactoryLock) {
			if (sessionFactory != null){
				//clear the thread local; this should not be a problem but 
				//this will clear any sessions linked to old session factory
				sessionMapsThreadLocal = new ThreadLocal<Session>();
			}
			sessionFactory = null;	
		}
	}
	
	/**
	 * Closes the current session factory.
	 * @param force true if the session factory should close even if active session exists
	 */
	protected static void endSessionFactory(boolean force) throws Exception{
		thisLock.acquire();
		try{
			int cnt = 0;
			while(allSessions.size() > 0 && cnt < SESSION_WAIT_COUNT){
				//wait for thread to be closed
				try {
					Thread.sleep( SESSION_CLOSE_WAIT );
				} catch (InterruptedException e) {}
				cnt++;
			}
			if (allSessions.size() > 0 && !force){
				throw new Exception("Could not end current database session.  There are still active transactions.");
			}else if (force){
				for (Session s : allSessions){
					//force close
					s.close();
				}
			}
			
			endSessionFactoryNoLock();
		}finally{
			thisLock.release();
		}
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
	private static HashMap<Class<?>, Object[]> hibernateClassMetadata = null;;
	public static final String getHqlExportQuery(Class<?> clazz) throws ClassNotFoundException, InvalidRegistryObjectException{
		readClassMetadata();
		Object[] values = hibernateClassMetadata.get(clazz);
		if (values != null){
			return (String)values[0];
		}
		return null;
	}
	public static final Boolean supportsCcaa(Class<?> clazz) throws ClassNotFoundException, InvalidRegistryObjectException{
		readClassMetadata();
		Object[] values = hibernateClassMetadata.get(clazz);
		if (values != null){
			return (Boolean)values[1];
		}
		return null;
	}
	
	private static void readClassMetadata() throws ClassNotFoundException, InvalidRegistryObjectException{
		if (hibernateClassMetadata != null) return;
		if (Platform.getExtensionRegistry() == null) ;
		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(MAPPING_ID);
		hibernateClassMetadata = new HashMap<Class<?>, Object[]>();
		for (IConfigurationElement e : config) {
			Class<?> clzz = Class.forName(e.getAttribute("class")); //$NON-NLS-1$
			String caProp = e.getAttribute("ca_property"); //$NON-NLS-1$
			Boolean containsCcaa = Boolean.valueOf(e.getAttribute("supportsCcaa")); //$NON-NLS-1$
			hibernateClassMetadata.put(clzz, new Object[]{caProp, containsCcaa});
		}
	}
	
}
