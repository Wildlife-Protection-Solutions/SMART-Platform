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
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.hibernate.Interceptor;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.SessionFactoryImplementor;

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
public class SmartHibernateManager {
	
	protected static SessionFactory sessionFactory = null;

	private static  String userName = "login";
	private static  String passWord = "smrt";
	
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
	
	public static void reset(){
		if (sessionFactory != null){
			sessionFactory.close();
		}
		sessionFactory = null;
	}
	
	
	/**
	 * Creates a new session factory.
	 * 
	 */
	public  static synchronized final void createSessionFactory(){
		
		if (sessionFactory == null){
			Configuration config = new Configuration().configure(Thread.currentThread().getContextClassLoader().getResource("hibernate.cfg.xml"));
			
			config.setProperty("hibernate.connection.username", userName);
			config.setProperty("hibernate.connection.password", passWord);
			
			//add mapping classes
			for (Class c: getMappings()){
				config.addAnnotatedClass(c);
			}
			sessionFactory = config.buildSessionFactory();
				
			if (!((SessionFactoryImplementor)sessionFactory).getSettings().getDialect().supportsSequences()){
				//fail
				throw new IllegalStateException("You can't use this database - it does not support sequences");
			}				
		}
	}
	
	/**
	 * Users are required to close the session when they are done with it.
	 * @return
	 */
	public static final Session openSession(){
		if (sessionFactory == null){
			createSessionFactory();
		}
		return sessionFactory.openSession();
	}
	
	/**
	 * Users are required to close the session when they are done with it.
	 * @return
	 */
	public static final Session openSession(Interceptor interceptor){
		if (sessionFactory == null){
			createSessionFactory();
		}
		return sessionFactory.openSession(interceptor);
	}
	
	/**
	 * Closes the current session factory.
	 */
	public static final void endSessionFactory(){
		if (sessionFactory != null){
			sessionFactory.close();
		}
		sessionFactory = null;
	}
	
	private static final String MAPPING_ID = "org.wcs.smart.hibernate.mapping";
	private static final List<Class>  getMappings(){
		List<Class> items = new ArrayList<Class>();
		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(MAPPING_ID);
		try {
			for (IConfigurationElement e : config) {
				items.add(Class.forName(e.getAttribute("class")));
			}
		}catch (Exception ex){
			ex.printStackTrace();
		}
		return items;
	}
		
}
