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
package org.wcs.smart.tests;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.SessionFactoryImplementor;
import org.wcs.smart.ca.Agency;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.Label;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.Rank;
import org.wcs.smart.ca.datamodel.Aggregation;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.ca.datamodel.DmObject;

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
public class Hibernate {

	private static SessionFactory sessionFactory = null;
	
	private static final void createSessionFactory(){
		
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
			config.addAnnotatedClass(DmObject.class);
			config.addAnnotatedClass(Category.class);
			config.addAnnotatedClass(Attribute.class);
			config.addAnnotatedClass(CategoryAttribute.class);
			config.addAnnotatedClass(Aggregation.class);
			config.addAnnotatedClass(AttributeListItem.class);
			config.addAnnotatedClass(AttributeTreeNode.class);
			
			
			
			sessionFactory = config.buildSessionFactory();
				
			if (!((SessionFactoryImplementor)sessionFactory).getSettings().getDialect().supportsSequences()){
				//fail
				throw new IllegalStateException("You can't use this database - it does not support sequences");
			}				
		}
	}
	
	/** Copied from HibernateManager **/
	private static NumberFormat ID_FORMATTER = new DecimalFormat("00000");
	public static void generateEmployeeId(Employee e, Session session){

		Calendar c = Calendar.getInstance();
		c.setTime(e.getBirthDate());
		int year = c.get(Calendar.YEAR);
		
		String query = (((SessionFactoryImplementor)sessionFactory).getSettings().getDialect().getSequenceNextValString("smart.smart_user_id_seq"));
		List results = session.createSQLQuery(query).list();
		e.setId(year + "" + ID_FORMATTER.format(results.get(0)));
	}
	
	/**
	 * Users are required to close the session when they are done with it.
	 * @return
	 */
	public static Session openSession(){
		if (sessionFactory == null){
			createSessionFactory();
		}
		return sessionFactory.openSession();
	}
}

