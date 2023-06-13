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


import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.Collator;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.geotools.referencing.CRS;
import org.hibernate.Interceptor;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.internal.SessionImpl;
import org.hibernate.jdbc.Work;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;
import org.mindrot.jbcrypt.BCrypt;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.wcs.smart.ProjectionUtils;
import org.wcs.smart.SmartContext;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.Agency;
import org.wcs.smart.ca.Area;
import org.wcs.smart.ca.Area.AreaType;
import org.wcs.smart.ca.BasemapDefinition;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.ICaCreateHandler;
import org.wcs.smart.ca.IconItem;
import org.wcs.smart.ca.IconManager;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.ca.Station;
import org.wcs.smart.ca.UuidItem;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.ca.datamodel.SimpleDataModel;
import org.wcs.smart.ca.export.TableInfo;
import org.wcs.smart.ca.icon.Icon;
import org.wcs.smart.ca.icon.IconFile;
import org.wcs.smart.hibernate.SmartDB.DbUser;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.user.UserLevelManager;
import org.wcs.smart.util.I18nUtil;
import org.wcs.smart.util.ReprojectUtils;
import org.wcs.smart.util.SharedUtils;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.EntityType;

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

	/**
	 * Opens a new sessions.  Users should close sessions
	 * when done.
	 * 
	 * @return
	 */
	public synchronized static Session openSession(){
		return openSession(null);
	}
	
	public static Icon loadIcon(IconItem item) {
		
		if (item == null || item.getIcon() == null) return null;
		if (item.getIcon().getUuid() == null) return item.getIcon();
		try (Session session = HibernateManager.openSession()) {
			Icon icon = session.get(Icon.class, item.getIcon().getUuid());
			if (icon == null) return item.getIcon();
			for (IconFile file : icon.getFiles()) {
				file.computeFileLocation(session);
				file.getIconSet().getIsDefault();
			}
			return icon;
		} catch (Exception ex) {
			return null;
		}
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
		Session session = null;
		
		if (interceptor == null){
			session = SmartHibernateManager.openSession();
		}else{
			session = SmartHibernateManager.openSession(interceptor);
		}
		
		SmartContext.INSTANCE.setClass(SessionFactory.class, session.getSessionFactory());
		
		return session;
	}
	
	
	/**
	 * 
	 * @return the language for the given code associated with the conservation area
	 * <code>null</code> if not found
	 */
	public static Language findLanguage(Session x, Locale l, ConservationArea ca){
		x.beginTransaction();
		try {
			//match language and country code
			String fullCode = I18nUtil.localeToString(l);
			
			ConservationArea thisca = x.get(ConservationArea.class, ca.getUuid());
			if (thisca == null) return null;
			
			//match language and country code
			for (Language lang : thisca.getLanguages()) {
				if (lang.getCode().equals(fullCode)) return lang;
			}
			
			//match language only
			for (Language lang : thisca.getLanguages()) {
				if (lang.getCode().equals(l.getLanguage())) return lang;
			}
			
			//find default
			for (Language lang : thisca.getLanguages()) {
				if (lang.isDefault()) return lang;
			}
			
			return null;
		}finally{
			x.getTransaction().rollback();
		}
	}
	
	/**
	 * Loads all the areas for the given area type
	 * @param areaType area type to load area for
	 * @param session
	 * @return
	 */
	public static List<Area> loadAreas(AreaType areaType, Session session){
		return QueryFactory.buildQuery(session, Area.class, 
				new Object[]{"conservationArea", SmartDB.getCurrentConservationArea()},  //$NON-NLS-1$
				new Object[]{"type", areaType}).getResultList(); //$NON-NLS-1$
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
	public static Area findArea(AreaType type, String key, Session session){
		return QueryFactory.buildQuery(session, Area.class, 
				new Object[]{"conservationArea", SmartDB.getCurrentConservationArea()},  //$NON-NLS-1$
				new Object[]{"keyId", key},  //$NON-NLS-1$
				new Object[]{"type", type}).uniqueResult(); //$NON-NLS-1$
	}
	
	/**
	 * Compresses all smart database tables
	 * 
	 * @param session
	 * @param monitor
	 */
	public static void compressTables(Session session, SubMonitor monitor) {
		session.doWork(new Work() {
			
			@Override
			public void execute(Connection connection) throws SQLException {
				List<TableInfo> all = getTableInformation();
				SubMonitor sub = SubMonitor.convert(monitor, "", all.size()); //$NON-NLS-1$

				for (TableInfo ti : getTableInformation()) {
					try {
						sub.subTask(MessageFormat.format(Messages.HibernateManager_TaskName, ti.getTableName()));
						String[] bits = ti.getTableName().split("\\."); //$NON-NLS-1$
						String q = "CALL SYSCS_UTIL.SYSCS_COMPRESS_TABLE('" + bits[0].toUpperCase() + "', '" + bits[1].toUpperCase() + "', 1)"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						connection.createStatement().execute(q);
						connection.commit();
						sub.worked(1);
					}catch (Exception ex) {
						SmartPlugIn.log(ex.getMessage(),ex);
					}
				}
			}
		});
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
		
		
		try (EntityManager em = sessionFactory.createEntityManager()){
			for (EntityType<?> t : em.getMetamodel().getEntities()) {
				Object entityExample = null;
				try {
					//skip abstract classes
					if (Modifier.isAbstract( t.getJavaType().getModifiers() )) continue;
					entityExample = t.getJavaType().getConstructor().newInstance();
				} catch (ReflectiveOperationException e) {
					throw new RuntimeException(e);
				}
				EntityPersister p = em.unwrap(SessionImpl.class).getEntityPersister(null, entityExample);
				
				if (p instanceof AbstractEntityPersister) {
					AbstractEntityPersister info = (AbstractEntityPersister) p;
				
					if (info instanceof Joinable) {
						Joinable j = (Joinable)info;
						if (info.getRootTableName().equals(j.getTableName())){
							TableInfo tableInfo = new TableInfo(info.getMappedClass(), j.getTableName());
							//find conservation area property if available
							for (int k = 0; k < info.getPropertyTypes().length; k ++){
								if (info.getPropertyTypes()[k].getReturnedClass() == ConservationArea.class){
									tableInfo.setCaPropertyName(((AbstractEntityPersister)info).getPropertyColumnNames(k)[0]);
									break;
								}
							}
							data.add(tableInfo);
						}
					}
				}else {
					throw new RuntimeException(MessageFormat.format("Cannot determine entity details for type {0}.", t.getJavaType().toString()));
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
		
		try (EntityManager em = sessionFactory.createEntityManager()){
			Object entityExample = null;
			try {
				entityExample = hibernateClass.getConstructor().newInstance();
			} catch (ReflectiveOperationException e) {
				throw new RuntimeException(e);
			}
			EntityPersister p = em.unwrap(SessionImpl.class).getEntityPersister(null, entityExample);
			
			if (p instanceof AbstractEntityPersister) {
				AbstractEntityPersister info = (AbstractEntityPersister) p;
				return info.getRootTableName();
				
			}
		}
		
		//return null
        throw new RuntimeException("Unexpected persister type; a subtype of AbstractEntityPersister expected.");
		
//		MetamodelImplementor mi = (MetamodelImplementor)((EntityManagerFactory)sessionFactory).getMetamodel();
//		AbstractEntityPersister info = ((AbstractEntityPersister)mi.entityPersister(hibernateClass));
//		ClassMetadata m = info.getClassMetadata();
//		if (m instanceof Joinable){
//			return ((Joinable)m).getTableName();
//		}
		//return null;
		
	}
	
	/**
	 * Closes the current session factory.
	 * 
	 * @param reconnect should be true if the application
	 * is going to re-connect to the database.
	 * @throws Exception 
	 */
	public static void endSessionFactory(boolean reconnect, boolean force) throws Exception{
		SmartHibernateManager.endSessionFactory(force);
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
	public static List<ConservationArea> getConservationAreas(Session session) {		
		Query<ConservationArea> query = session.createQuery("from ConservationArea WHERE uuid != :uuid Order by lower(id)", ConservationArea.class);	 //$NON-NLS-1$
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
		CriteriaBuilder cb = s.getCriteriaBuilder();
		CriteriaQuery<Employee> c = cb.createQuery(Employee.class);
		Root<Employee> root = c.from(Employee.class);
		c.where(cb.and(
				cb.equal(root.get("conservationArea"), ca), //$NON-NLS-1$
				cb.isNull(root.get("endEmploymentDate")) //$NON-NLS-1$
				));
		return s.createQuery(c).getResultList();
	}

	/**
	 * Gets all employees for a given conservation area.
	 * 
	 * @param ca
	 * @param s
	 * @return
	 */
	public static List<Employee> getAllEmployees(ConservationArea ca, Session s){
		ConservationArea thisCa = s.get(ConservationArea.class, ca.getUuid());
		return thisCa.getEmployees();
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
			//TODO: test this
			String query = "select count(*) from Employee where conservationArea = :ca and UPPER(smartUserId) = UPPER(:userId)"; //$NON-NLS-1$
			List<Long> cnt = session.createQuery(query, Long.class)
					.setParameter("ca", ca) //$NON-NLS-1$
					.setParameter("userId", userName).list(); //$NON-NLS-1$ 
			boolean ok = false;
			if ( cnt.get(0) > 0){
				ok = false;
			}else{
				ok = true;
			}
			tx.rollback();
			return ok;
		}catch (Exception ex){
			tx.rollback();
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
	public static List<ConservationArea> findConservationAreas(String userName, String password) throws Exception{
		try(Session s = HibernateManager.openSession()){
		
			String query = "FROM Employee WHERE UPPER(smartUserId) = UPPER(:userId)"; //$NON-NLS-1$
			List<Employee> es = s.createQuery(query, Employee.class)
				.setParameter("userId", userName) //$NON-NLS-1$
				.getResultList();

			List<ConservationArea> areas = new ArrayList<ConservationArea>();
			for (Employee e : es){
				if (!e.getConservationArea().getIsCcaa() && e.getEndEmploymentDate() == null){
					if (e.getSmartUserLevelKeys() != null && !e.getSmartUserLevelKeys().isEmpty()) {
//					if (UserLevelManager.INSTANCE.supportsUser(e, UserLevelManager.ADMIN, UserLevelManager.MANAGER, UserLevelManager.ANALYST)){
						if (validatePassword(password, e)){
							areas.add(e.getConservationArea());
						}
					}
				}
			}
			areas.sort((a,b)-> Collator.getInstance().compare(a.getName(), b.getName()));
			return areas;
			
		}
	}
	
	public static boolean validatePassword(String password, Employee e){
		return validatePassword(password, e.getSmartPassword());
	}
	
	public static boolean validatePassword(String password, String employeePassword){
		if (employeePassword == null) return false;
		return BCrypt.checkpw(password, employeePassword);
	}
	
	public static String generatePassword(String password){
		return SharedUtils.generatePassword(password);
	}
	
	/**
	 * Validates the username and password for a given conservation area.
	 * If the given conservation area is the CCAA, it checks for a matching username
	 * password in any other non-ccaa but returns the user associated with ccaa and the same username.
	 * The Employee must still be active.
	 * 
	 * @param userName user name
	 * @param password password
	 * @param ca the conservation area
	 * @return the Employee associated with the username and password; null if nobody found
	 * @throws Exception
	 */
	public static Employee validateUser(String userName, String password, ConservationArea ca) {
		
		try(Session s = HibernateManager.openSession()){
			
			String query = "FROM Employee WHERE conservationArea = :ca and UPPER(smartUserId) = UPPER(:userId) and endEmploymentDate is null"; //$NON-NLS-1$
			List<Employee> people = s.createQuery(query, Employee.class)
					.setParameter("ca", ca) //$NON-NLS-1$
					.setParameter("userId", userName) //$NON-NLS-1$
					.list();
			
			if (people.size() == 1){
				Employee user = people.get(0);
				
				if (!ca.getIsCcaa()){
					if (validatePassword(password, user)){
						return user;
					}
					return null;
				}else{
					//check passwords for all users with matching userids
					query = "FROM Employee WHERE conservationArea != :ca and UPPER(smartUserId) = UPPER(:userId) and endEmploymentDate is null"; //$NON-NLS-1$
					List<Employee> otherUsers = s.createQuery(query, Employee.class)
							.setParameter("ca", ca) //$NON-NLS-1$
							.setParameter("userId", userName) //$NON-NLS-1$
							.list();
					
					for (Employee o : otherUsers){
						if (validatePassword(password, o)){
							return user;
						}
					}
					return null;
				}
			}
			return null;
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
		Object[][] fields = new Object[onlyActive?2:1][2];
		fields[0] = new Object[] {"conservationArea", ca}; //$NON-NLS-1$
		if (onlyActive) {
			fields[1] = new Object[] {"isActive", true}; //$NON-NLS-1$
		}
		return QueryFactory.buildQuery(s, Station.class, fields).getResultList();
	}
	
	
	/**
	 * Gets all agencies for a given conservation area 
	 * @param ca conservation area 
	 * @param s session 
	 * @return list of agencies
	 */
	public static List<Agency> getAgencies(ConservationArea ca, Session s){
		CriteriaBuilder cb = s.getCriteriaBuilder();
		CriteriaQuery<Agency> c = cb.createQuery(Agency.class);
		Root<Agency> root = c.from(Agency.class);
		c.where(cb.equal(root.get("conservationArea"), ca)); //$NON-NLS-1$
		
		return s.createQuery(c).getResultList();
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
		int year = LocalDate.now().getYear();
		if (e.getBirthDate() != null) {
			year = e.getBirthDate().getYear();
		}else {
			year = e.getStartEmploymentDate().getYear();
		}
		
		String query = HibernateUtil.getHibernateCurrentDialect(session)
				.getSequenceSupport()
				.getSequenceNextValString("smart.smart_user_id_seq"); //$NON-NLS-1$

		List<?> results = session.createNativeQuery(query, Long.class).list();
		e.setId(year + ID_FORMATTER.format(results.get(0)));
	}
	
	/**
	 * Saves the new conservation area to the database.
	 * 
	 * @param newCa
	 * @throws Exception
	 */
	public static void saveNewConservationArea(ConservationArea newCa, Path logoFile) throws Exception{
		
		/* need to login as admin user to create CA */
		HibernateManager.endSessionFactory(true);
		SmartHibernateManager.setUserName(DbUser.ADMIN.getUserName(), DbUser.ADMIN.getPassword());
		
		try(Session s = HibernateManager.openSession()) {
			s.beginTransaction();
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
				
				//create icons
				IconManager.INSTANCE.createDefaultIconSet(s, newCa);
				s.flush();
				
				//fire extension points
				List<ICaCreateHandler> extensions = getCreateExtensions();
				for (ICaCreateHandler handler : extensions) {
					handler.afterCreate(newCa, s);
				}
				
				s.flush();
				
				newCa.setLogo(logoFile);
				
				s.getTransaction().commit();
			} catch (Exception ex) {
				s.getTransaction().rollback();
				throw ex;
			}
		} finally {
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
		
		CriteriaBuilder cb = session.getCriteriaBuilder();
		CriteriaQuery<Employee> c = cb.createQuery(Employee.class);
		Root<Employee> root = c.from(Employee.class);
		c.where(cb.and(
				cb.equal(root.get("conservationArea"), SmartDB.getCurrentConservationArea()), //$NON-NLS-1$
				cb.isNull(root.get("endEmploymentDate")), //$NON-NLS-1$
				cb.notEqual(root.get("uuid"), e.getUuid()), //$NON-NLS-1$
				cb.isNotNull(root.get("smartUserLevelKeys")) //$NON-NLS-1$
				));
		
		List<Employee> otherEmployees = session.createQuery(c).getResultList();
		for (Employee other : otherEmployees){
			//some other employee in this ca is admin; we don't have anything to worry about
			if (UserLevelManager.INSTANCE.supportsUser(other, UserLevelManager.ADMIN)) return null;
		}
		
		//no other admin users so I must be an active smart admin user
		if (e.getEndEmploymentDate() != null){
			return Messages.HibernateManager_Error_CannotDeleteLastAdminUser;
		}
		if (!UserLevelManager.INSTANCE.supportsUser(e, UserLevelManager.ADMIN)){
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
	public static DataModel loadDataModel(ConservationArea ca, Session session){
		try{
			SimpleDataModel simple = SimpleDataModel.loadDataModel(ca, session);
			DataModel dm = new DataModel(ca, simple.getCategories(), simple.getAttributes());
			return dm;
		}catch (final Exception ex){
			SmartPlugIn.displayLog(Messages.HibernateManager_Error_LoadingDataModel, ex);
			
			if (session.getTransaction().isActive()){
				session.getTransaction().rollback();
			}
			session.close();
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
		Query<Employee> query = session.createQuery(sql, Employee.class);
		query.setParameter("id", employeeId); //$NON-NLS-1$
		query.setParameter("ca", ca); //$NON-NLS-1$
		
		List<Employee> results = query.list();
		if (results.size() == 0){
			return null;
		}else{
			return results.get(0);
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
		Query<Employee> query = session.createQuery(sql, Employee.class);
		query.setParameter("given", givenName); //$NON-NLS-1$
		query.setParameter("family", familyName); //$NON-NLS-1$
		query.setParameter("ca", ca); //$NON-NLS-1$
		
		List<Employee> results = query.list();
		if (results.size() == 0){
			return null;
		}else{
			return results.get(0);
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
		Query<Employee> query = session.createQuery(sql, Employee.class);
		query.setParameter("given", givenName); //$NON-NLS-1$
		query.setParameter("family", familyName); //$NON-NLS-1$
		query.setParameter("id", employeeId); //$NON-NLS-1$
		query.setParameter("ca", ca); //$NON-NLS-1$
		
		List<Employee> results = query.list();
		if (results.size() == 0){
			return null;
		}else{
			return results.get(0);
		}
	}
	
	/**
	 * @param session
	 * @return all basemaps defined for the current conservation area
	 */
	public static List<BasemapDefinition> getBasemaps(Session session){
		String query = "FROM BasemapDefinition WHERE conservationArea = :ca"; //$NON-NLS-1$
		Query<BasemapDefinition> q = session.createQuery(query, BasemapDefinition.class);
		q.setParameter("ca", SmartDB.getCurrentConservationArea()); //$NON-NLS-1$
		return q.list();
	}
	
	
	/**
	 * @param session
	 * @return the default basemap defined for the conservation area or null
	 * if no default specified
	 */
	public static BasemapDefinition getDefaultBasemapDefinition(Session session){
		
		CriteriaBuilder cb = session.getCriteriaBuilder();
		CriteriaQuery<BasemapDefinition> c = cb.createQuery(BasemapDefinition.class);
		Root<BasemapDefinition> root = c.from(BasemapDefinition.class);
		c.where(cb.and(
				cb.equal(root.get("conservationArea"), SmartDB.getCurrentConservationArea()), //$NON-NLS-1$
				cb.equal(root.get("isDefault"), true) //$NON-NLS-1$
				));
		return session.createQuery(c).uniqueResult();
	}
	
	/**
	 * @param session
	 * @param uuid the basemap uuid
	 * @return the  basemap with the uuid or null if not found
	 */
	public static BasemapDefinition getBasemapDefinition(Session session, UUID uuid){
		return session.get(BasemapDefinition.class, uuid);
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
	public static List<Projection> getCaProjectionList(ConservationArea ca, Session session){
		CriteriaBuilder cb = session.getCriteriaBuilder();
		CriteriaQuery<Projection> c = cb.createQuery(Projection.class);
		Root<Projection> root = c.from(Projection.class);
		c.where(cb.equal(root.get("conservationArea"), ca)); //$NON-NLS-1$
		
		return session.createQuery(c).getResultList();
	}

	
	/**
	 * 
	 * @param session
	 * @return the default CRS for the current conservation
	 * area of null of non selected
	 */
	public static Projection getDefaultProjection(Session session){
		CriteriaBuilder cb = session.getCriteriaBuilder();
		CriteriaQuery<Projection> query = cb.createQuery(Projection.class);
		Root<Projection> root = query.from(Projection.class);
		query.where(cb.and(
				cb.equal(root.get("conservationArea"), SmartDB.getCurrentConservationArea()), //$NON-NLS-1$
				cb.equal(root.get("isDefault"), true) //$NON-NLS-1$
				));
		return session.createQuery(query).uniqueResult();
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
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT version FROM "); //$NON-NLS-1$
		sb.append(SmartDB.PLUGIN_VERSION_TBL);
		sb.append("WHERE plugin_id = ");  //$NON-NLS-1$
		sb.append("'" + pluginId + "'"); //$NON-NLS-1$ //$NON-NLS-2$
				
		NativeQuery<String> query = s.createNativeQuery(sb.toString(), String.class);
		List<String> versions = query.list();
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
			NativeQuery<?> query = s.createNativeQuery("DELETE FROM " + SmartDB.PLUGIN_VERSION_TBL + " WHERE plugin_id = '" + pluginId + "'"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			query.executeUpdate();
		}else{
			String x = getPlugInVersion(pluginId, s);
			if (x == null){
				//insert new
				NativeQuery<?> query = s.createNativeQuery("INSERT INTO " + SmartDB.PLUGIN_VERSION_TBL + " (plugin_id, version) VALUES ('" + pluginId + "', '" + newVersion + "')"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				query.executeUpdate();
			}else{
				NativeQuery<?> query = s.createNativeQuery("UPDATE " + SmartDB.PLUGIN_VERSION_TBL + " SET version = '" + newVersion + "' WHERE plugin_id = '" + pluginId + "'"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				query.executeUpdate();
			}
		}
	}
	
	/**
	 * Get the current viewing projection for the current Conservation
	 * Area
	 * @return
	 */
	public static Projection getCurrentViewProjection() {
		try(Session s = HibernateManager.openSession()) {
			return getCurrentViewProjection(s);
		}
	}

	/**
	 * Get the current viewing coordinate reference system for the
	 * current conservation area. 
	 * @return
	 */
	public static CoordinateReferenceSystem getCurrentViewCRS() {
		try(Session s = HibernateManager.openSession()) {
			Projection p = getCurrentViewProjection(s);
			CoordinateReferenceSystem crs = ReprojectUtils.stringToCrs(p.getDefinition());
			return crs;
		}catch (Exception ex) {
			SmartPlugIn.log(ex.getMessage(), ex);
			return SmartDB.DATABASE_CRS;
		}
	}
	
	/**
	 * Get the current viewing projection for the current Conservation
	 * Area
	 * @return
	 */
	public static Projection getCurrentViewProjection(Session s) {
		return ProjectionUtils.INSTANCE.getCurrentViewProjection(s, SmartDB.getCurrentConservationArea());
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
	

	public static void load(Collection<? extends NamedItem> items) {
		if (items == null) return;
		items.forEach(e->e.getNames().size());
	}
	
	public static void save(Session session, Collection<? extends UuidItem> items) {
		if (items == null) return;
		items.forEach(item->{if (item.getUuid() == null) session.persist(item);});
	}
	
	public static <T extends UuidItem> T saveOrMerge(Session session, T item) {
		if (item == null) return null;
		
		if (item.getUuid() == null) {
			session.persist(item);
			return item;
		}else {
			return session.merge(item);
		}
	}
}


