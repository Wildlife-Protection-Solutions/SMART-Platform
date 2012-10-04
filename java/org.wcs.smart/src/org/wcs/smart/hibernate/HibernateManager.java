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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Criteria;
import org.hibernate.EntityMode;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.persister.entity.Joinable;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.Agency;
import org.wcs.smart.ca.BasemapDefinition;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.Employee.SmartUserLevel;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.ca.Station;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.internal.ca.export.TableInfo;

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
			List<?> results = x.createCriteria(Language.class).add(Restrictions.eq("ca", ca)).add(Restrictions.eq("code", code)).list();
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
	 * For each hibernate mapping which maps 1-1 to a database
	 * table this function returns the information about that
	 * database table and hibernate mapping includes 
	 * the database table name, the mapped hibernate class, and
	 * the conservationarea property of the database table (if
	 * it exists).
	 * 
	 * @return
	 */
	public static List<TableInfo> getTableInformation(){
		if (sessionFactory == null){
			return null;
		}
		
		List<TableInfo> data = new ArrayList<TableInfo>();
		Map<String, ClassMetadata> x = sessionFactory.getAllClassMetadata();
		for (Iterator<ClassMetadata> i = x.values().iterator(); i.hasNext();) {
			ClassMetadata m = i.next();
			if (m instanceof Joinable){
				Joinable j = ((Joinable)m);
				if (((AbstractEntityPersister)m).getRootTableName().equals(j.getTableName())){
					TableInfo info = new TableInfo(m.getMappedClass(EntityMode.POJO), j.getTableName());
					//find conservation area property if available
					for (int k = 0; k < m.getPropertyTypes().length; k ++){
						if (m.getPropertyTypes()[k].getReturnedClass() == ConservationArea.class){
							info.setCaPropertyName(((AbstractEntityPersister)m).getPropertyColumnNames(k)[0]);
						}
					}
					data.add(info);
				}
				
			}
		}
		return data;
	}
	
	/**
	 * Given a hibernate entity class returns the mapped
	 * database table or null if cannot determined mapped
	 * database table.
	 * 
	 * @param hibernateClass
	 * @return
	 */
	public static String getTableName(Class<?> hibernateClass){
		if (sessionFactory == null){
			return null;
		}
		ClassMetadata m = sessionFactory.getClassMetadata(hibernateClass);
		if (m instanceof Joinable){
			return ((Joinable)m).getTableName();
		}
		return null;
		
	}
	
	/**
	 * Closes the current session factory.
	 * 
	 * @param reconnect should be true if the application
	 * is going to re-connect to the database.
	 */
	public static void endSessionFactory(boolean reconnect){
		SmartHibernateManager.endSessionFactory();
		DerbyHibernateExtensions.shutDown(reconnect);
	}
	
	
	/**
	 * Opens a session, loads all conservation areas and
	 * closes the session.
	 * @param session hibernate session
	 * @return a list of conservation areas in the database
	 */
	
	public static List<ConservationArea> getConservationAreas(Session session) {
		@SuppressWarnings("unchecked")
		List<ConservationArea> areas = session.createQuery("from ConservationArea order by id").list();	
		return areas;
	}
	
	/**
	 * Gets all active employees for a given conservation area.
	 * 
	 * @param ca
	 * @param s
	 * @return
	 */
	public static List<Employee> getActiveEmployees(ConservationArea ca, Session s){
		@SuppressWarnings("unchecked")
		List<Employee> results = s.createCriteria(Employee.class).add(Restrictions.eq("conservationArea", ca)).add(Restrictions.isNull("endEmploymentDate")).list();
		return results;
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
			List<?> cnt = session.createQuery(query).setEntity("ca", ca).setString("userId", userName).list();
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
			employee.add( Restrictions.eq("smartUserId", userName).ignoreCase());
			employee.add( Restrictions.eq("smartPassword", password));
			employee.add( Restrictions.eq("conservationArea", ca));
			employee.add(Restrictions.isNull("endEmploymentDate"));
			
			@SuppressWarnings("unchecked")
			List<Employee> people = employee.list();
			tx.commit();
			if (people.size() == 1){
				return people.get(0);
			}else{
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
	private static List<Station> getStations(ConservationArea ca, Session s,
			boolean onlyActive) {
		Criteria st = null;
		st = s.createCriteria(Station.class);
		st.add(Restrictions.eq("conservationArea", ca));
		if (onlyActive) {
			st.add(Restrictions.eq("isActive", true));
		}
		@SuppressWarnings("unchecked")
		List<Station> people = st.list();
		return people;
	}
	
	
	/**
	 * Gets all agencies for a given conservation area 
	 * @param ca conservation area 
	 * @param s session 
	 * @return list of agencies
	 */
	public static List<Agency> getAgencies(ConservationArea ca, Session s){
		Criteria st = null;
		st = s.createCriteria(Agency.class);
		st.add( Restrictions.eq("conservationArea", ca));
		@SuppressWarnings("unchecked")
		List<Agency> people = st.list();		
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
		List<?> results = session.createSQLQuery(query).list();
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
	 * given employee to ensures that there is always one 
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
	@SuppressWarnings("unchecked")
	public static DataModel loadDataModel(ConservationArea ca, Session s){
		try{
			List<Category> rootCategories = s.createCriteria(Category.class)
					.add(Restrictions.eq("conservationArea", ca))
					.add(Restrictions.isNull("parent"))
					.addOrder(Order.asc("categoryOrder")).list();
			
			List<Attribute> attribute = s.createCriteria(Attribute.class)
					.add(Restrictions.eq("conservationArea", ca)).list();
			DataModel dm = new DataModel(ca, rootCategories, attribute);
			return dm;
		}catch (final Exception ex){
			SmartPlugIn.displayLog(null, "Cannot load conservation area data model.", ex);
			if (s.getTransaction().isActive()){
				s.getTransaction().rollback();
			}
			s.close();
			return null;
		}
		
	}
	
	
	/**
	 * Searches the database for an employee with the the given id.
	 * 
	 * @param employeeId the employee id
	 * @param ca the conservation area 
	 * @param session hibernate sesion
	 * @return the first employee found with the given id; <code>null</code> if no employee found
	 */
	public static Employee findEmployeeById(String employeeId, ConservationArea ca, Session session){		
		String sql = "FROM Employee WHERE id = :id AND conservationArea = :ca";
		Query query = session.createQuery(sql);
		query.setParameter("id", employeeId);
		query.setParameter("ca", ca);
		
		List<?> results = query.list();
		if (results.size() == 0){
			return null;
		}else{
			return (Employee)results.get(0);
		}
	}
	
	
	/**
	 * Searches the database for an employee with the given first
	 * and last name.
	 * 
	 * @param givenName the given name
	 * @param familyName the family name
	 * @param ca the conservation area to search
	 * @param session 
	 * @return the first employee found with the name given and family names; <code>null</code> if no employee found
	 */
	public static Employee findEmployeeByName(String givenName, String familyName, ConservationArea ca, Session session){		
		String sql = "FROM Employee WHERE givenName = :given AND familyName = :family AND conservationArea = :ca";
		Query query = session.createQuery(sql);
		query.setParameter("given", givenName);
		query.setParameter("family", familyName);
		query.setParameter("ca", ca);
		
		List<?> results = query.list();
		if (results.size() == 0){
			return null;
		}else{
			return (Employee)results.get(0);
		}
	}
	
	/**
	 * Searches the database for an employee with the given id, given, and
	 * family names.
	 * 
	 * @param employeeId employee id
	 * @param givenName the given name 
	 * @param familyName the family name 
	 * @param ca the conservation area to search
	 * @param session
	 * @return the first employee found with the given id, family, and given names.  <code>null</code> if no employee found
	 */
	public static Employee findEmployeeByIdAndName(String employeeId, String givenName, String familyName, ConservationArea ca, Session session){		
		String sql = "FROM Employee WHERE givenName = :given AND familyName = :family AND id = :id AND conservationArea = :ca";
		Query query = session.createQuery(sql);
		query.setParameter("given", givenName);
		query.setParameter("family", familyName);
		query.setParameter("id", employeeId);
		query.setParameter("ca", ca);
		
		List<?> results = query.list();
		if (results.size() == 0){
			return null;
		}else{
			return (Employee)results.get(0);
		}
	}
	
	/**
	 * @param session
	 * @return all basemaps defined for the current conservation area
	 */
	public static List<BasemapDefinition> getBasemaps(Session session){
		String query = "FROM BasemapDefinition WHERE conservationArea = :ca";
		Query q = session.createQuery(query);
		q.setParameter("ca", SmartDB.getCurrentConservationArea());
		return q.list();
	}
	
	
	/**
	 * @param session
	 * @return the default basemap defined for the conservation area or null
	 * if no default specified
	 */
	public static BasemapDefinition getDefaultBasemapDefinition(Session session){
		List<?> defaultmap = session.createCriteria(BasemapDefinition.class).add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())).add(Restrictions.eq("isDefault", true)).list();
		if (defaultmap.size() > 0){
			return (BasemapDefinition) defaultmap.get(0);
		}
		return null;
	}
	
	/**
	 * @param session
	 * @param uuid the basemap uuid
	 * @return the default basemap defined for the conservation area or null
	 * if no default specified
	 */
	public static BasemapDefinition getBasemapDefinition(Session session, byte[] uuid){
		List<?> defaultmap = session.createCriteria(BasemapDefinition.class).add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())).add(Restrictions.eq("uuid", uuid)).list();
		if (defaultmap.size() > 0){
			return (BasemapDefinition) defaultmap.get(0);
		}
		return null;
	}
	

	/**
	 * 
	 * @param session
	 * @return list of projections available for the conservation
	 * area
	 */
	@SuppressWarnings("unchecked")
	public static List<Projection> getCaProjectinList(Session session){
		return ((List<Projection>)session.createCriteria(Projection.class).add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())).list());
	}

	/**
	 * 
	 * @param session
	 * @return the default CRS for the current conservation
	 * area of null of non selected
	 */
	public static Projection getDefaultProjection(Session session){
		List<?> defaults = session.createCriteria(Projection.class).add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())).add(Restrictions.eq("isDeafult", true)).list();
		if (defaults.size() == 0){
			return null;
		}else{
			return (Projection)defaults.get(0);
		}
	}
}
