/*
 * Copyright (C) 2015 Wildlife Conservation Society
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
package org.wcs.smart.connect.hibernate;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.servlet.ServletContext;

import org.hibernate.Interceptor;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.connect.model.Alert;
import org.wcs.smart.connect.model.AlertFilterDefault;
import org.wcs.smart.connect.model.AlertType;
import org.wcs.smart.connect.model.ConservationAreaInfo;
import org.wcs.smart.connect.model.EmployeeInfo;
import org.wcs.smart.connect.model.MapLayer;
import org.wcs.smart.connect.model.SharedLink;
import org.wcs.smart.connect.model.SimpleConservationAreaList;
import org.wcs.smart.connect.model.SmartRole;
import org.wcs.smart.connect.model.SmartUser;
import org.wcs.smart.connect.model.SmartUserRole;
import org.wcs.smart.connect.model.StyleConfiguration;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.util.I18nUtil;

/**
 * Hibernate manager for SMART Connect application.
 * @author Emily
 *
 */
public class HibernateManager {
	
	public static final String DATABASE_VERSION = "7.0.0"; //$NON-NLS-1$
	public static final String FILESTORE_VERSION = "7.0.0"; //$NON-NLS-1$
	
	public static final String CONTEXT_KEY = "SessionFactory"; //$NON-NLS-1$
	
	/**
	 * Creates a new session from the given context.  If you are
	 * accessing SMART Objects (data model etc.) you should use the
	 * getSession(ServletContext, Locale) function to ensure the
	 * names are displayed in the correct locale.
	 * 
	 * @param context
	 * @return
	 */
	public static Session getSession(ServletContext context){
		return getSessionFactory(context).getCurrentSession();
	}
	
	public static Session openNewSession(ServletContext context, Locale l){
		I18nUtil.setLocale(l);
		return getSessionFactory(context).openSession();
	}
	
	public static Session getSession(ServletContext context, Locale l, Interceptor interceptor){
		I18nUtil.setLocale(l);
		Session session = getSessionFactory(context).withOptions().interceptor(interceptor).openSession();
		
		if (interceptor instanceof AttachmentInterceptor) {
			((AttachmentInterceptor)interceptor).setSession(session, l);
		}
		return session;
	}
	
	/**
	 * Creates a new session from the given context that uses the given
	 * locale for loading database labels. 
	 *  
	 * @param context
	 * @param l
	 * @return
	 */
	public static Session getSession(ServletContext context, Locale l){
		I18nUtil.setLocale(l);
		return getSession(context);
	}
	
	/**
	 * Returns the session factory.
	 * 
	 * @param context
	 * @return
	 */
	public static SessionFactory getSessionFactory(ServletContext context){
		return ((SessionFactory)context.getAttribute(CONTEXT_KEY));
	}
	
	/**
	 * Finds the smart user from the given username.
	 * 
	 * @param session
	 * @param username
	 * @return
	 */
	public static SmartUser getUser(Session session, String username){
		SmartUser su = QueryFactory.buildQuery(session,  SmartUser.class, "username", username).uniqueResult(); //$NON-NLS-1$
		return su;
	}
	
	/**
	 * Finds the smart user's role(s) from the given username.
	 * 
	 * @param session
	 * @param username
	 * @return
	 */
	public static List<SmartUserRole> getUserRoles(Session session, String username){
		return QueryFactory.buildQuery(session,  SmartUserRole.class, "id.username", username).list(); //$NON-NLS-1$
	}
	
	/**
	 * Finds all users in the database.
	 * 
	 * @param session
	 * @return
	 */
	public static List<SmartUser> getUsers(Session session){
		CriteriaBuilder cb = session.getCriteriaBuilder();
		CriteriaQuery<SmartUser> c = cb.createQuery(SmartUser.class);
		Root<SmartUser> from = c.from(SmartUser.class);
		c.orderBy(cb.asc(from.get("username"))); //$NON-NLS-1$
		return session.createQuery(c).list();
	}

	/**
	 * 
	 * @param session
	 * @return a list of conservation areas in the system
	 */
	public static List<ConservationAreaInfo> getConservationAreaInfos(Session session) {
		return QueryFactory.buildQuery(session,  ConservationAreaInfo.class).list(); 
	}

	/**
	 * 
	 * @param session
	 * @return a list of conservation areas in the system
	 * 
	 * Only Include actual Desktop CAs with DATA (exclude 'CCAA', 'NO DATA' and 'Uploading')
	 */
	public static List<ConservationAreaInfo> getConservationAreaInfosWithoutCCAA(Session session, boolean includeDataOnly) {
		
		CriteriaBuilder cb = session.getCriteriaBuilder();
		CriteriaQuery<ConservationAreaInfo> c = cb.createQuery(ConservationAreaInfo.class);
		Root<ConservationAreaInfo> from = c.from(ConservationAreaInfo.class);
		
		Predicate[] p = new Predicate[includeDataOnly ? 2 : 1];
		p[0] = cb.notEqual(from.get("uuid"), ConservationAreaInfo.CCAA_UUID); //$NON-NLS-1$
		if (includeDataOnly) {
			p[1] = cb.equal(from.get("status"), ConservationAreaInfo.Status.DATA); //$NON-NLS-1$
		}
		c.where(cb.and(p));
		return session.createQuery(c).list();
	}

	
	/**
	 * 
	 * @param session
	 * @param caUuid
	 * @return a specific conservation area 
	 */
	public static ConservationAreaInfo getConservationAreaInfo(Session session, UUID caUuid) {
		return session.get(ConservationAreaInfo.class, caUuid);
	}
	
	public static List<AlertType> getAlertTypes(Session session) {
		CriteriaBuilder cb = session.getCriteriaBuilder();
		CriteriaQuery<AlertType> c = cb.createQuery(AlertType.class);
		Root<AlertType> from = c.from(AlertType.class);
		c.where(cb.notEqual(from.get("uuid"), AlertType.NULL_TYPE)); //$NON-NLS-1$
		c.orderBy(cb.asc(from.get("label"))); //$NON-NLS-1$
		return session.createQuery(c).list();	
	}

	public static List<Alert> getAlerts(Session session) {
		return QueryFactory.buildQuery(session,  Alert.class).list();
	}

	public static Alert getAlert(Session session, UUID alertUuid) {
		return session.get(Alert.class, alertUuid);
	}

	public static AlertType getAlertType(Session session, UUID typeUuid) {
		CriteriaBuilder cb = session.getCriteriaBuilder();
		CriteriaQuery<AlertType> c = cb.createQuery(AlertType.class);
		Root<AlertType> from = c.from(AlertType.class);
		c.where(cb.and(
				cb.equal(from.get("uuid"),  typeUuid), //$NON-NLS-1$
				cb.notEqual(from.get("uuid"), AlertType.NULL_TYPE) //$NON-NLS-1$
				));
		return session.createQuery(c).uniqueResult();
	}
	
	public static AlertType getAlertTypeIncludeUnknown(Session session, UUID typeUuid) {
		return session.get(AlertType.class, typeUuid);
	}

	public static Alert getAlertByUserId(Session session, String userGenId) {
		return QueryFactory.buildQuery(session, Alert.class, "userGeneratedId", userGenId).uniqueResult(); //$NON-NLS-1$
	}

	public static List<Alert> getAlertsByCa(Session session, UUID caUuid) {
		return QueryFactory.buildQuery(session, Alert.class, "ca.uuid", caUuid).list(); //$NON-NLS-1$
	}
	
	public static List<MapLayer> getMapLayers(Session session) {
		CriteriaBuilder cb = session.getCriteriaBuilder();
		CriteriaQuery<MapLayer> c = cb.createQuery(MapLayer.class);
		Root<MapLayer> from = c.from(MapLayer.class);
		c.orderBy(cb.asc(from.get("layerOrder"))); //$NON-NLS-1$
		return session.createQuery(c).list();
	}

	public static MapLayer getMapLayer(Session session, UUID layerUuid) {
		return QueryFactory.buildQuery(session, MapLayer.class, "uuid", layerUuid).uniqueResult(); //$NON-NLS-1$
	}

	public static MapLayer getMapLayerByLayerName(Session session, String layerName) {
		return QueryFactory.buildQuery(session, MapLayer.class, "layerName", layerName).uniqueResult(); //$NON-NLS-1$
	}

	public static List<AlertFilterDefault> getAlertFilterDefaults(Session session) {
		List<AlertFilterDefault> defaults = QueryFactory.buildQuery(session, AlertFilterDefault.class).list(); 
		if(defaults.size() == 0){
			AlertFilterDefault d = new AlertFilterDefault();
			d.setDefaultPastHours(24);
			d.setStartingLat(0);
			d.setStartingLong(30);
			d.setStartingZoomLevel(3);
			d.setSecondsRefresh(60);
			defaults.add(d);
		}
		return defaults;
	}
	
	public static StyleConfiguration getStyleConfiguration(Session session) {
		return QueryFactory.buildQuery(session, StyleConfiguration.class).uniqueResult(); 
	}
	
	public static List<StyleConfiguration> getStyleConfigurations(Session session) {
		return QueryFactory.buildQuery(session, StyleConfiguration.class).list(); 
	}

	public static Object getInactiveUsers(Session s) {
		ArrayList<SmartUser> users = (ArrayList<SmartUser>) HibernateManager.getUsers(s);
		List<SmartUser> inactiveUsers = new ArrayList<SmartUser>(); 
		for(SmartUser user: users){
			List<SmartUserRole> roles = HibernateManager.getUserRoles(s, user.getUsername());
			if(roles.isEmpty()){
				inactiveUsers.add(user);
			}
		}
		return inactiveUsers;
	}

	public static ArrayList<SmartUser> getActiveUsers(Session s) {
		ArrayList<SmartUser> users = (ArrayList<SmartUser>) HibernateManager.getUsers(s);
		ArrayList<SmartUser> activeUsers = new ArrayList<SmartUser>(); 
		for(SmartUser user: users){
			List<SmartUserRole> roles = HibernateManager.getUserRoles(s, user.getUsername());
			if(!roles.isEmpty()){
				activeUsers.add(user);
			}
		}
		return activeUsers;
	}

	/**
	 * Returns the system role (not the connect roles).
	 * 
	 * @param s
	 * @return
	 */
	public static SmartRole getSmartRole(Session s) {
		return QueryFactory.buildQuery(s, SmartRole.class, "roleName", SmartRole.SYSTEM_ROLE_NAME).uniqueResult();  //$NON-NLS-1$
	}

	public static SharedLink getSharedLink(Session s, UUID uuid) {
		return QueryFactory.buildQuery(s, SharedLink.class, "uuid", uuid).uniqueResult(); //$NON-NLS-1$
	}

	/**
	* Returns all desktop user accounts
	*/
	public static List<EmployeeInfo> getDesktopUsers(Session s) {
		
		List<Employee> results = s.createQuery("FROM Employee e WHERE e.smartUserId is not null AND e.conservationArea.uuid != :ccaauuid and e.endEmploymentDate is null", Employee.class) //$NON-NLS-1$
				.setParameter("ccaauuid", ConservationArea.MULTIPLE_CA) //$NON-NLS-1$
				. list( );
		ArrayList<EmployeeInfo> l = new ArrayList<>();
		for (Employee e : results) {
			l.add(new EmployeeInfo(e.getSmartUserId(), e.getConservationArea().getName(),  e.getConservationArea().getUuid()));
		}
		return l;
	}

	/**
	* Returns all desktop user accounts with that username, could be in multiple CAs
	 * @return 
	*/
	public static ArrayList<SimpleConservationAreaList> getDesktopUserAllCas(Session s, String username) {
		List<ConservationArea> cas = s.createQuery("SELECT e.conservationArea FROM Employee e WHERE e.smartUserId = :username AND e.endEmploymentDate is null", ConservationArea.class) //$NON-NLS-1$
				.setParameter("username", username) //$NON-NLS-1$
				.list();
		
		ArrayList<SimpleConservationAreaList> l = new ArrayList<>();
		for (ConservationArea ca : cas) {
			l.add(new SimpleConservationAreaList(ca.getNameLabel(), ca.getUuid()));
		}
		return l;
	}
	
	/**
	* Returns the desktop user accounts with that username in the CA, if it isn't deactivated already.
	*/
	public static Employee getDesktopUserInCa(Session s, String username, String cauuid) {
		UUID uuid = UUID.fromString(cauuid);
		return QueryFactory.buildQuery(s, Employee.class,
				new Object[] {"smartUserId", username}, //$NON-NLS-1$
				new Object[] {"conservationArea.uuid", uuid}) //$NON-NLS-1$ 
				.uniqueResult();  
	}

	/**
	 * Find the conservation area associated with the provided conservation area uuid
	 * @param s
	 * @param caUuid
	 * @return
	 */
	public static ConservationArea getConservationArea(Session s, UUID caUuid) {
		return s.get(ConservationArea.class,  caUuid);
	}
	
}