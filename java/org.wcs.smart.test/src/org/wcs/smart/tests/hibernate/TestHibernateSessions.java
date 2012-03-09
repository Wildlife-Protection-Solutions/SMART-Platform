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
package org.wcs.smart.tests.hibernate;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.SessionFactoryImplementor;
import org.junit.Test;
import org.wcs.smart.ca.Agency;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.Label;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.Rank;
import org.wcs.smart.hibernate.HibernateManager;

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
public class TestHibernateSessions {

	
	@Test
	public void testHibernateSession() throws Exception{
		
		Runnable r1 = new Runnable() {
			
			@Override
			public void run() {
				Session session1 = openSession();
				session1.beginTransaction();
				
				try {
					Thread.sleep(20 * 1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}  //sleep for a minute
				session1.getTransaction().rollback();
				session1.close();
				System.out.println("Thread 1 done");
			}
		};
		
		Runnable r2 = new Runnable(){
			@Override
			public void run() {
				Session session1 = openSession();
				session1.beginTransaction();
				
				try {
					//10 seconds
					Thread.sleep(5 * 1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				session1.getTransaction().rollback();
				session1.close();
				System.out.println("Thread 2 done");
			}
		};
		
		
//		System.out.println("starting threads");
//		Thread t = new Thread(r1);
//		Thread t2 = new Thread(r2);
//		
//		t.start();
//		t2.start();
		
//		Thread.sleep(2*1000);
		Session session1 = openSession();
		session1.beginTransaction();
		
		Query q = session1.createQuery("FROM Employee");
		Employee e1 = (Employee) q.list().get(0);
		e1.getConservationArea().getName();
		
		
		for (int i = 0; i < 100; i ++){
		Session session2 = openSession();
		session2.beginTransaction();
		q = session2.createQuery("FROM ConservationArea");
		System.out.println(q.list().size());
		ConservationArea ca1 = (ConservationArea) q.list().get(0);
		System.out.println(ca1.getName());
		//session1.getTransaction().commit();
		session2.getTransaction().rollback();
		session2.close();
		
		}
		
		
		
		q = session1.createQuery("FROM ConservationArea");
		ConservationArea a1 = (ConservationArea) q.list().get(0);
		e1.getConservationArea().getName();
		
		session1.getTransaction().rollback();
		session1.close();
		
	}
	
	
	SessionFactory sessionFactory = null;
	public  synchronized final void createSessionFactory(){
		
		if (sessionFactory == null){
			Configuration config = new Configuration().configure(Thread.currentThread().getContextClassLoader().getResource("hibernate.cfg.xml"));
			
			config.setProperty("hibernate.connection.username", "smart_admin");
			config.setProperty("hibernate.connection.password", "smart_derby");
			
			//add mapping classes
			
			
			config.addAnnotatedClass(ConservationArea.class);
			config.addAnnotatedClass(Language.class);
			config.addAnnotatedClass(Employee.class);
			config.addAnnotatedClass(Rank.class);
			config.addAnnotatedClass(Agency.class);
			config.addAnnotatedClass(Label.class);
			
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
	public  final Session openSession(){
		if (sessionFactory == null){
			createSessionFactory();
		}
		return sessionFactory.openSession();
	}
}
