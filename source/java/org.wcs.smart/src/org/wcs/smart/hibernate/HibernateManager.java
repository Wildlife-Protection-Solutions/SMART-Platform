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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.geotools.referencing.CRS;
import org.hibernate.Criteria;
import org.hibernate.Interceptor;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.persister.entity.Joinable;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.Agency;
import org.wcs.smart.ca.Area;
import org.wcs.smart.ca.Area.AreaType;
import org.wcs.smart.ca.BasemapDefinition;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.Employee.SmartUserLevel;
import org.wcs.smart.ca.ICaCreateHandler;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.ca.Rank;
import org.wcs.smart.ca.Station;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.ca.export.TableInfo;
import org.wcs.smart.common.attachment.ISmartAttachment;
import org.wcs.smart.hibernate.SmartDB.DbUser;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.util.I18nUtil;

/**
 * Hibernate manager to manage database connections.
 * 
 * @author Emily
 *
 */
public class HibernateManager extends SmartHibernateManager{
	
	public static void initContext(){
		if (SmartDB.getCurrentLanguage() != null){
			I18nUtil.setLocale(SmartDB.getCurrentLanguage().getUuid());
		}
		if (SmartDB.getCurrentConservationArea() != null){
			I18nUtil.setCa(SmartDB.getCurrentConservationArea().getUuid());
		}
	}
	public synchronized static Session openSession(){
		initContext();
		
		Session session = SmartHibernateManager.openSession();
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
	public synchronized static Session openSession(Interceptor interceptor){
		initContext();
		Session session = SmartHibernateManager.openSession(interceptor);
		return session;
	}
	
	/**
	 * 
	 * @return the language for the given code associated with the conservation area
	 * <code>null</code> if not found
	 */
	public static Language findLanguage(Session x, Locale l, ConservationArea ca){
		Transaction tx = x.beginTransaction();
		try {
			//match language and country code
			String fullCode = I18nUtil.localeToString(l);
			List<?> results = x.createCriteria(Language.class).add(Restrictions.eq("ca", ca)).add(Restrictions.eq("code", fullCode)).list(); //$NON-NLS-1$ //$NON-NLS-2$
			if (results.size() > 0){
				return (Language)results.get(0);
			}
			//match language only
			results = x.createCriteria(Language.class).add(Restrictions.eq("ca", ca)).add(Restrictions.eq("code", l.getLanguage())).list(); //$NON-NLS-1$ //$NON-NLS-2$
			if (results.size() > 0){
				return (Language)results.get(0);
			}
			
			//find default
			results = x.createCriteria(Language.class).add(Restrictions.eq("ca", ca)).add(Restrictions.eq("default", true)).list(); //$NON-NLS-1$ //$NON-NLS-2$
			if (results.size() > 0){
				return (Language) results.get(0);
			}
			
			return null;
		}finally{
			tx.rollback();
			x.close();
		}
	}
	
	/**
	 * Loads all the areas for the given area type
	 * @param areaType area type to load area for
	 * @param session
	 * @return
	 */
	public static List<Area> loadAreas(AreaType areaType, Session session){
		@SuppressWarnings("unchecked")
		List<Area> items = session
			.createCriteria(Area.class)
			.add(Restrictions.eq(
					"conservationArea", //$NON-NLS-1$
				SmartDB.getCurrentConservationArea()))
				.add(Restrictions.eq("type", areaType)).list(); //$NON-NLS-1$
	
		return items;
	}
	
	/**
	 * Finds the Area of the provided type with the given key for the
	 * current conservation area.  IF multiple area are found (this should not
	 * happen if keys are maintained correctly) the first area is returned.
	 * 
	 * @param type area type
	 * @param key area key
	 * @param session
	 * @return matching area or <code>null</code> if not found
	 */
	@SuppressWarnings("unchecked")
	public static Area findArea(AreaType type, String key, Session session){
		List<Area> matching = session
				.createCriteria(Area.class)
				.add(Restrictions.eq("conservationArea", //$NON-NLS-1$
						SmartDB.getCurrentConservationArea()))
				.add(Restrictions.eq("keyId", key)) //$NON-NLS-1$
				.add(Restrictions.eq("type", type)).list(); //$NON-NLS-1$
		if (matching.size() == 0){
			return null;
		}else{
			return matching.get(0);
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
					TableInfo info = new TableInfo(m.getMappedClass(), j.getTableName());
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
	 * Loads all public conservation areas in the database.
	 * <p>This will not return the default CA used for cross-ca
	 * analysis</p>
	 * 
	 * @param session hibernate session
	 * @return a list of conservation areas in the database
	 */
	@SuppressWarnings("unchecked")
	public static List<ConservationArea> getConservationAreas(Session session) {		
		Query query = session.createQuery("from ConservationArea WHERE uuid != :uuid Order by lower(id)");	 //$NON-NLS-1$
		query.setParameter("uuid", ConservationArea.MULTIPLE_CA); //$NON-NLS-1$
		List<ConservationArea> areas = query.list();
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
		List<Employee> results = s.createCriteria(Employee.class).add(Restrictions.eq("conservationArea", ca)).add(Restrictions.isNull("endEmploymentDate")).list(); //$NON-NLS-1$ //$NON-NLS-2$
		return results;
	}

	/**
	 * Gets all active employees for a given conservation area.
	 * 
	 * @param ca
	 * @param s
	 * @return
	 */
	public static List<Employee> getAllEmployees(ConservationArea ca, Session s){
		@SuppressWarnings("unchecked")
		List<Employee> results = s.createCriteria(Employee.class).add(Restrictions.eq("conservationArea", ca)).list(); //$NON-NLS-1$
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
			String query = "select count(*) from Employee where conservationArea = :ca and smartUserId = :userId"; //$NON-NLS-1$
			List<?> cnt = session.createQuery(query).setEntity("ca", ca).setString("userId", userName).list(); //$NON-NLS-1$ //$NON-NLS-2$
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
			SmartPlugIn.displayLog(Messages.HibernateManager_Error_ValidatingUserId, ex);
		}
		return false;
	}
	/**
	 * Determines all conservation areas accessible by
	 * the user with the given username and password.
	 * <p>
	 * The username/password must exist as a manager, admin, or analyst account
	 * in the conservation area.
	 * </p>
	 * 
	 * 
	 * @param userName user name
	 * @param password password
	 * @return list of conservation areas 
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public static List<ConservationArea> findConservationAreas(String userName, String password) throws Exception{
		Session x = HibernateManager.openSession();
		Transaction tx = x.beginTransaction();
		try{
			String hql = "SELECT e.conservationArea from Employee e where smartUserId = :userid and smartPassword = :password and smartUserLevel IN (:users) ORDER BY e.conservationArea.name"; //$NON-NLS-1$
			Query q = x.createQuery(hql);
			q.setParameter("userid", userName); //$NON-NLS-1$
			q.setParameter("password", password); //$NON-NLS-1$
			q.setParameterList("users", new Integer[]{Employee.SmartUserLevel.ADMIN.ordinal(), Employee.SmartUserLevel.ANALYST.ordinal(), Employee.SmartUserLevel.MANAGER.ordinal()}); //$NON-NLS-1$
			List<ConservationArea> areas = q.list(); 
			return areas;
			
		}catch (Exception ex){
			tx.rollback();
			throw ex;
		}finally{
			x.close();
		}
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
			employee.add( Restrictions.eq("smartUserId", userName).ignoreCase()); //$NON-NLS-1$
			employee.add( Restrictions.eq("smartPassword", password)); //$NON-NLS-1$
			employee.add( Restrictions.eq("conservationArea", ca)); //$NON-NLS-1$
			employee.add(Restrictions.isNull("endEmploymentDate")); //$NON-NLS-1$
			
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
		st.add(Restrictions.eq("conservationArea", ca)); //$NON-NLS-1$
		if (onlyActive) {
			st.add(Restrictions.eq("isActive", true)); //$NON-NLS-1$
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
		st.add( Restrictions.eq("conservationArea", ca)); //$NON-NLS-1$
		@SuppressWarnings("unchecked")
		List<Agency> people = st.list();		
		return people;		
	}
	
	public static List<Rank> getRanksByAgency(ConservationArea ca, Session s,Agency agt){
		Criteria st = null;
		st = s.createCriteria(Agency.class);
		st.add( Restrictions.eq("conservationArea", ca)); //$NON-NLS-1$
		st.add( Restrictions.eq("Agency", agt)); //$NON-NLS-1$
		@SuppressWarnings("unchecked")
		List<Rank> ranks = st.list();		
		return ranks;		
	}

	private static NumberFormat ID_FORMATTER = new DecimalFormat("00000"); //$NON-NLS-1$
	
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
		
		String query = (((SessionFactoryImplementor)sessionFactory).getDialect().getSequenceNextValString("smart.smart_user_id_seq")); //$NON-NLS-1$
		List<?> results = session.createSQLQuery(query).list();
		e.setId(year + ID_FORMATTER.format(results.get(0)));
	}
	
	/**
	 * Saves the new conservation area to the database.
	 * 
	 * @param newCa
	 * @throws Exception
	 */
	public static void saveNewConservationArea(ConservationArea newCa) throws Exception{
		
		/* need to login as admin user to create CA */
		HibernateManager.endSessionFactory(true);
		SmartHibernateManager.setUserName(DbUser.ADMIN.getUserName(), DbUser.ADMIN.getPassword());
		
		Session s = HibernateManager.openSession();
		Transaction tx = s.beginTransaction();
		try {
			//save conservation area
			s.save(newCa);
			for(Employee e: newCa.getEmployees()){
				generateEmployeeId(e, s);
				s.save(e);
			}		
			
			//create initial default projection
			CoordinateReferenceSystem crs = CRS.decode("EPSG:4326"); //$NON-NLS-1$
			Projection prj = new Projection();
			prj.setConservationArea(newCa);
			String code = "unknown"; //$NON-NLS-1$
			try{
				code = CRS.lookupIdentifier(crs.getName().getAuthority(), crs, true);
			}catch (Exception ex){
				
			}
			prj.setName(crs.getName().getCode() + " [" + crs.getName().getCodeSpace() + ": " + code + "]"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			prj.setDefinition(crs.toWKT());
			prj.setIsDefault(true);
			s.save(prj);
			
			//fire extension points
			List<ICaCreateHandler> extensions = getCreateExtensions();
			for (ICaCreateHandler handler : extensions) {
				handler.afterCreate(newCa, s);
			}
			
			tx.commit();
		} catch (Exception ex) {
			tx.rollback();
			throw ex;
		} finally {
			s.close();
			HibernateManager.endSessionFactory(true);	
			SmartHibernateManager.setUserName(DbUser.LOGIN.getUserName(), DbUser.LOGIN.getPassword());
		}
	}

	/**
	 * @return list of {@link ICaCreateHandler} extension points
	 */
	private static List<ICaCreateHandler> getCreateExtensions() {
		if (Platform.getExtensionRegistry() == null) return Collections.emptyList();
		List<ICaCreateHandler> items = new ArrayList<ICaCreateHandler>();
		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(ICaCreateHandler.EXTENSION_ID);
		try {
			for (IConfigurationElement e : config) {
				items.add((ICaCreateHandler)e.createExecutableExtension("class")); //$NON-NLS-1$
			}
		}catch (Exception ex){
			SmartPlugIn.log(ex.getMessage(), ex);
		}
		return items;
	}
	
	
	public static void processesError(Throwable exception){
		SmartPlugIn.log("Database Error", exception); //$NON-NLS-1$
		
		if (exception.getMessage().contains("ERROR XSDB6")){ //$NON-NLS-1$
			MessageDialog.openError(Display.getCurrent().getActiveShell(), Messages.HibernateManager_Error_Dialog_Title, Messages.HibernateManager_Error_AnotherAppOpen);		
		}else{
			MessageDialog.openError(Display.getCurrent().getActiveShell(), Messages.HibernateManager_Error_Dialog_Title, Messages.HibernateManager_Error_DBConnection);
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
		crit.add(Restrictions.isNull("endEmploymentDate")); //$NON-NLS-1$
		crit.add(Restrictions.ne("uuid", e.getUuid())); //$NON-NLS-1$
		crit.add(Restrictions.eq("smartUserLevel", SmartUserLevel.ADMIN)); //$NON-NLS-1$
		crit.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())); //$NON-NLS-1$
		crit.setProjection(Projections.rowCount());
			
		int num = ((Long)crit.list().get(0)).intValue();
		if (num > 0){
			return null;
		}
		//no other users so I must be an active smart admin user
		if (e.getEndEmploymentDate() != null){
			return Messages.HibernateManager_Error_CannotDeleteLastAdminUser;
		}
		if (e.getSmartUserLevel() == null || ! e.getSmartUserLevel().equals(Employee.SmartUserLevel.ADMIN)){
			return Messages.HibernateManager_CannotChangeLastAdminUser;
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
					.add(Restrictions.eq("conservationArea", ca)) //$NON-NLS-1$
					.add(Restrictions.isNull("parent")) //$NON-NLS-1$
					.addOrder(Order.asc("categoryOrder")).list(); //$NON-NLS-1$
			
			List<Attribute> attribute = s.createCriteria(Attribute.class)
					.add(Restrictions.eq("conservationArea", ca)).list(); //$NON-NLS-1$
			DataModel dm = new DataModel(ca, rootCategories, attribute);
			return dm;
		}catch (final Exception ex){
			SmartPlugIn.displayLog(Messages.HibernateManager_Error_LoadingDataModel, ex);
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
		String sql = "FROM Employee WHERE id = :id AND conservationArea = :ca"; //$NON-NLS-1$
		Query query = session.createQuery(sql);
		query.setParameter("id", employeeId); //$NON-NLS-1$
		query.setParameter("ca", ca); //$NON-NLS-1$
		
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
		String sql = "FROM Employee WHERE givenName = :given AND familyName = :family AND conservationArea = :ca"; //$NON-NLS-1$
		Query query = session.createQuery(sql);
		query.setParameter("given", givenName); //$NON-NLS-1$
		query.setParameter("family", familyName); //$NON-NLS-1$
		query.setParameter("ca", ca); //$NON-NLS-1$
		
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
		String sql = "FROM Employee WHERE givenName = :given AND familyName = :family AND id = :id AND conservationArea = :ca"; //$NON-NLS-1$
		Query query = session.createQuery(sql);
		query.setParameter("given", givenName); //$NON-NLS-1$
		query.setParameter("family", familyName); //$NON-NLS-1$
		query.setParameter("id", employeeId); //$NON-NLS-1$
		query.setParameter("ca", ca); //$NON-NLS-1$
		
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
	@SuppressWarnings("unchecked")
	public static List<BasemapDefinition> getBasemaps(Session session){
		String query = "FROM BasemapDefinition WHERE conservationArea = :ca"; //$NON-NLS-1$
		Query q = session.createQuery(query);
		q.setParameter("ca", SmartDB.getCurrentConservationArea()); //$NON-NLS-1$
		return q.list();
	}
	
	
	/**
	 * @param session
	 * @return the default basemap defined for the conservation area or null
	 * if no default specified
	 */
	public static BasemapDefinition getDefaultBasemapDefinition(Session session){
		List<?> defaultmap = session.createCriteria(BasemapDefinition.class)
				.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())) //$NON-NLS-1$
				.add(Restrictions.eq("isDefault", true)).list(); //$NON-NLS-1$
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
	public static BasemapDefinition getBasemapDefinition(Session session, UUID uuid){
		List<?> defaultmap = session.createCriteria(BasemapDefinition.class)
				.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())) //$NON-NLS-1$
				.add(Restrictions.eq("uuid", uuid)).list(); //$NON-NLS-1$
		if (defaultmap.size() > 0){
			return (BasemapDefinition) defaultmap.get(0);
		}
		return null;
	}
	

	/**
	 * 
	 * @param session
	 * @return list of projections available for the current conservation area
	 * area
	 */
	public static List<Projection> getCaProjectionList(Session session){
		return getCaProjectionList(SmartDB.getCurrentConservationArea(), session);
	}

	/**
	 * 
	 * @param session
	 * @return list of projections available for the given conservation
	 * area
	 */
	@SuppressWarnings("unchecked")
	public static List<Projection> getCaProjectionList(ConservationArea ca, Session session){
		return ((List<Projection>)session.createCriteria(Projection.class).add(Restrictions.eq("conservationArea", ca)).list()); //$NON-NLS-1$
	}

	
	/**
	 * 
	 * @param session
	 * @return the default CRS for the current conservation
	 * area of null of non selected
	 */
	public static Projection getDefaultProjection(Session session){
		List<?> defaults = session.createCriteria(Projection.class).add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())).add(Restrictions.eq("isDefault", true)).list(); //$NON-NLS-1$ //$NON-NLS-2$
		if (defaults.size() == 0){
			return null;
		}else{
			return (Projection)defaults.get(0);
		}
	}
	
	
	/**
	 * Reads the version for the given plugin from the database
	 * versions table.  Will return null if plugin not found in
	 * table.
	 * 
	 * @param pluginId
	 * @param s
	 * @return
	 */
	public static String getPlugInVersion(String pluginId, Session s){
		SQLQuery query = s.createSQLQuery("SELECT version FROM " + SmartDB.PLUGIN_VERSION_TBL + " WHERE plugin_id = '" + pluginId + "'"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		List<?> versions = query.list();
		if (versions.size() == 0){
			return null;
		}else{
			return (String)versions.get(0);
		}
	}
	
	/**
	 * Updates the database versions table for the given plugin id.
	 * 
	 * If new version is null then it removes the record from the database versions table.
	 * 
	 * @param pluginId
	 * @param newVersion
	 * @param s
	 */
	public static void setPlugInVersion(String pluginId, String newVersion, Session s){
		if (newVersion == null){
			//delete record
			SQLQuery query = s.createSQLQuery("DELETE FROM " + SmartDB.PLUGIN_VERSION_TBL + " WHERE plugin_id = '" + pluginId + "'"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			query.executeUpdate();
		}else{
			String x = getPlugInVersion(pluginId, s);
			if (x == null){
				//insert new
				SQLQuery query = s.createSQLQuery("INSERT INTO " + SmartDB.PLUGIN_VERSION_TBL + " (plugin_id, version) VALUES ('" + pluginId + "', '" + newVersion + "')"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				query.executeUpdate();
			}else{
				SQLQuery query = s.createSQLQuery("UPDATE " + SmartDB.PLUGIN_VERSION_TBL + " SET version = '" + newVersion + "' WHERE plugin_id = '" + pluginId + "'"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				query.executeUpdate();
			}
		}
	}
	

	
	/**
	 * Evicts all names from the given session
	 * @param session
	 */
	public static void evitNames(NamedItem item, Session session){
		for(org.wcs.smart.ca.Label name: item.getNames()){
			if (name.getElementuuid() != null){
				session.evict(name);
			}
		}
	}
}


