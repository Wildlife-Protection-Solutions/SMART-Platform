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


import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.List;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.engine.SessionFactoryImplementor;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.Agency;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.Employee.SmartUserLevel;
import org.wcs.smart.ca.Station;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.DataModel;

/**
 * Hibernate manager to manage database connections.
 * 
 * @author Emily
 *
 */
public class HibernateManager extends SmartHibernateManager{

	/**
	 * 
	 * @return the language for the given code
	 */
	public static Language findLanguage(String code, ConservationArea ca){
		Session x = openSession();
		Transaction tx = x.beginTransaction();
		try {
			List results = x.createCriteria(Language.class).add(Restrictions.eq("ca", ca)).add(Restrictions.eq("code", code)).list();
			if (results.size() > 0){
				return (Language)results.get(0);
			}else{
				results = x.createCriteria(Language.class).add(Restrictions.eq("ca", ca)).add(Restrictions.eq("default", true)).list();
				if (results.size() > 0){
					return (Language) results.get(0);
				}
			}
			return null;
		}finally{
			tx.rollback();
			x.close();
		}
	}
	
	
	/**
	 * Opens a session, loads all conservation areas and
	 * closes the session.
	 * 
	 * @return a list of conservation areas in the database
	 */
	public static List<ConservationArea> getConservationAreas() {
		Session x = openSession();
		Transaction tx = x.beginTransaction();
		try {
			List<ConservationArea> areas = x.createQuery("from ConservationArea order by id").list();	
			tx.commit();
			return areas;
		} finally {
			x.close();
		}
	}
	
	/**
	 * Gets all active employees for a given conservation area.
	 * 
	 * @param ca
	 * @param s
	 * @return
	 */
	public static List<Employee> getActiveEmployees(ConservationArea ca, Session s){
		s.beginTransaction();
		try {
			List<Employee> results = s.createCriteria(Employee.class).add(Restrictions.eq("conservationArea", ca)).add(Restrictions.isNull("endEmploymentDate")).list();
			s.getTransaction().commit();
			return results;
		}catch (Exception ex){
			s.getTransaction().rollback();
			s.close();
			SmartPlugIn.displayLog(null, "Could not load active employees. " + ex.getMessage(), ex);
			
		}
		return null;
	}

	/**
	 * Opens a session and validates that no other users
	 * for the given conservation area have the username.
	 * 
	 * @param userName  username to validate
	 * @param ca conservation area
	 * @return true if no other usernames; true if username already exists 
	 */
	public static boolean validateUserIdUnique(String userName, ConservationArea ca, Session session){
		Transaction tx = session.beginTransaction();
		try{
			String query = "select count(*) from Employee where conservationArea = :ca and smartUserId = :userId";
			List cnt = session.createQuery(query).setEntity("ca", ca).setString("userId", userName).list();
			boolean ok = false;
			if ( (Long) cnt.get(0) > 0){
				ok = false;
			}else{
				ok = true;
			}
			tx.rollback();
			return ok;
		}catch (Exception ex){
			tx.rollback();
			session.close();
			SmartPlugIn.displayLog(null, "Error validating user id.", ex);
		}
		return false;
	}
	
	/**
	 * Validates the username and password for a given conservation area.
	 * 
	 * @param userName user name
	 * @param password password
	 * @param ca the conservation area
	 * @return the Employee associated with the username and password; null if nobody found
	 * @throws Exception
	 */
	public static Employee validateUser(String userName, String password, ConservationArea ca) throws Exception{
		Session x = HibernateManager.openSession();
		Transaction tx = x.beginTransaction();
		try{
			Criteria employee = x.createCriteria(Employee.class);
			employee.add( Restrictions.eq("smartUserId", userName));
			employee.add( Restrictions.eq("smartPassword", password));
			employee.add( Restrictions.eq("conservationArea", ca));
			employee.add(Restrictions.isNull("endEmploymentDate"));
			
			List<Employee> people = employee.list();
			tx.commit();
			if (people.size() == 1){
				return people.get(0);
			}else{
				MessageDialog.openInformation(Display.getCurrent().getActiveShell(), "Error", "The username and password does not exist for the selected conservation area.");
				return null;
			}
			
		}catch (Exception ex){
			tx.rollback();
			throw ex;
		}finally{
			x.close();
		}
	}
	
	/**
	 * Gets all stations (active and in-active)
	 * 
	 * @param ca
	 * @param s
	 * @return
	 */
	public static List<Station> getStations(ConservationArea ca, Session s){
		return getStations(ca, s, false);
	}
	
	/**
	 * Get only active stations.
	 * 
	 * @param ca
	 * @param s
	 * @return
	 */
	public static List<Station> getActiveStations(ConservationArea ca, Session s){
		return getStations(ca, s, true);
	}
	/**
	 * Returns a list of all the stations for a given conservation area.
	 * 
	 * @param ca conservation area
	 * @param s session 
	 * @param onlyActive if only active stations should be returned
	 * 
	 * @return list of stations
	 */
	private static List<Station> getStations(ConservationArea ca, Session s, boolean onlyActive){
		   Transaction tx = s.beginTransaction();

			Criteria st =null;
			st = s.createCriteria(Station.class);
			st.add( Restrictions.eq("conservationArea", ca));
			if (onlyActive){
				st.add(Restrictions.eq("isActive", true));
			}
			List<Station> people = st.list();
			tx.commit();

			return people;
	}
	
	
	/**
	 * Gets all agencies for a given conservation area 
	 * @param ca conservation area 
	 * @param s session 
	 * @return list of agencies
	 */
	public static List<Agency> getAgencies(ConservationArea ca, Session s){
	   Transaction tx = s.beginTransaction();
		Criteria st = null;
		st = s.createCriteria(Agency.class);
		st.add( Restrictions.eq("conservationArea", ca));
		List<Agency> people = st.list();		
		tx.commit();
		return people;		
	}
	
	

	private static NumberFormat ID_FORMATTER = new DecimalFormat("00000");
	
	/**
	 * Must be called inside of a transaction.
	 * 
	 * Generates the next employee id for a new employee.
	 * @param e the employee
	 * @param session the database session
	 */
	public static void generateEmployeeId(Employee e, Session session){

		Calendar c = Calendar.getInstance();
		c.setTime(e.getBirthDate());
		int year = c.get(Calendar.YEAR);
		
		String query = (((SessionFactoryImplementor)sessionFactory).getSettings().getDialect().getSequenceNextValString("smart.smart_user_id_seq"));
		List results = session.createSQLQuery(query).list();
		e.setId(year + "" + ID_FORMATTER.format(results.get(0)));
	}
	
	/**
	 * Saves the new conservation area to the database.
	 * 
	 * @param newCa
	 * @throws Exception
	 */
	public static void saveNewConservationArea(ConservationArea newCa) throws Exception{
		Session s = HibernateManager.openSession();
		Transaction tx = s.beginTransaction();
		try {
			//save conservation area
			s.save(newCa);
			for(Employee e: newCa.getEmployees()){
				generateEmployeeId(e, s);
				s.save(e);
			}			
			tx.commit();
		} catch (Exception ex) {
			tx.rollback();
			throw ex;
		} finally {
			s.close();
		}
	}
	
	
	public static void processesError(Throwable exception){
		SmartPlugIn.log("Database Error", exception);
		
		if (exception.getMessage().contains("ERROR XSDB6")){
			MessageDialog.openError(Display.getCurrent().getActiveShell(), "Error", "Another application is currently accessing the database.  Please ensure no other SMART applications are running then restart SMART.");		
		}else{
			MessageDialog.openError(Display.getCurrent().getActiveShell(), "Error", "An unknown error occurred while connecting to the database.  Please ensure no other SMART applications are running and restart the application.  If the problem persists Do something.");
		}
	}
	
	/**
	 * Validates the changes to the 
	 * given employee to ensures that there is always have one 
	 * admin user in the database.
	 * <p>
	 * If the employee that is being updated is the same
	 * as the current logged in employee the database is checked to ensure
	 * that changes to the employee does not cause all
	 * admin users for a given conservation area to be removed.
	 * </p>
	 * <p>
	 * This function should be called inside the transaction
	 * that updates the employee.
	 * </p>
	 * <p>
	 * This function makes the assumption that the current logged
	 * in user is an admin user.
	 * </p>
	 * 
	 * @param s database session
	 * @param e employee being updated
	 * @return an error message if the employee should not be updated, false if updates
	 * can be made.
	 */
	public static String validateSmartUserChanges(Session session, Employee e){
		//we only care if we are modifying ourself.
		Criteria crit = session.createCriteria(Employee.class);
		crit.add(Restrictions.isNull("endEmploymentDate"));
		crit.add(Restrictions.ne("uuid", e.getUuid()));
		crit.add(Restrictions.eq("smartUserLevel", SmartUserLevel.ADMIN));
		crit.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea()));
		crit.setProjection(Projections.rowCount());
			
		int num = ((Long)crit.list().get(0)).intValue();
		if (num > 0){
			return null;
		}
		//no other users so I must be an active smart admin user
		if (e.getEndEmploymentDate() != null){
			return "This user cannot be terminated as there would be no other admin users.  Please create another admin account before terminating this user.";
		}
		if (e.getSmartUserLevel() == null || ! e.getSmartUserLevel().equals(Employee.SmartUserLevel.ADMIN)){
			return "This user must be an ADMIN SMART user as there are no other ADMIN smart users in the database.";
		}
			
		return null;
	}
	
	
	/**
	 * Loads a data model for a given conservation area from the database.
	 * <p>
	 * Does not use a transaction; you must open a transaction
	 * before calling this method if you require a transaction.
	 * 
	 * </p>
	 * 
	 * @param ca Conservation area
	 * @param s database connection
	 * @return data model loaded or <code>null</code> if error occurred
	 */
	public static DataModel loadDataModel(ConservationArea ca, Session s){
		try{
			List<Category> rootCategories = s.createCriteria(Category.class).add(Restrictions.eq("conservationArea", ca)).add(Restrictions.isNull("parent")).list();
			List<Attribute> attribute = s.createCriteria(Attribute.class).add(Restrictions.eq("conservationArea", ca)).list();
			DataModel dm = new DataModel(ca, rootCategories, attribute);
			return dm;
		}catch (final Exception ex){
			s.close();
			Display.getDefault().asyncExec(new Runnable(){
				@Override
				public void run() {
					SmartPlugIn.displayLog(null, "Cannot load conservation area data model.", ex);
				}});
		
			return null;
		}
		
	}
}
