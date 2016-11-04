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

import java.io.File;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.SmartProperties;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.Language;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.user.UserLevelManager;
import org.wcs.smart.util.GeometryUtils;

/**
 * Manages the smart database.
 * 
 * @author Emily
 *
 */
public class SmartDB {

	/**
	 * CCaa configuration modified event string
	 */
	public static final String CCAA_CONFIGURATION_MODIFIED = "CCAA/MODIFIED"; //$NON-NLS-1$
			
	/**
	 * Database table that contains the plugins and associated versions
	 * associated with the current database schema
	 */
	public static final String PLUGIN_VERSION_TBL = "smart.db_version"; //$NON-NLS-1$
	public static CoordinateReferenceSystem DATABASE_CRS = GeometryUtils.SMART_CRS;
	
	/**
	 * MapSettingsStore user accounts
	 * 
	 * @author Emily
	 *
	 */
	public enum DbUser{
		LOGIN("login", "smrt"),  //$NON-NLS-1$//$NON-NLS-2$
		ANALYST("analyst", "smrt"), //$NON-NLS-1$ //$NON-NLS-2$
		MANAGER("manager", "smrt"), //$NON-NLS-1$ //$NON-NLS-2$
		DATAENTRY("data_entry", "smrt"), //$NON-NLS-1$ //$NON-NLS-2$
		ADMIN("smart_admin", "smart_derby"); //$NON-NLS-1$ //$NON-NLS-2$
		
		private final String username;
		private final String password;
		
		DbUser(String user, String pass){
			this.username = user;
			this.password = pass;
		}
		public String getUserName(){
			return this.username;
		}
		public String getPassword(){
			return this.password;
		}
	};
	
	//current database user account
	private static DbUser current = DbUser.LOGIN;
	private static Employee currentEmployee = null;
	private static String plainTextPassword = null;
	private static ConservationArea currentCa = null;
	private static ConservationAreaConfiguration caConfig = null;
	
	/**
	 * 
	 * @return the current database user
	 */
	public static DbUser getCurrentUser(){
		if (current == null){
			return DbUser.LOGIN;
		}
		return current;
	}

	/**
	 * Loads from the DB the shared user which is used 
	 * for associated shared cross conservation area queries.
	 * 
	 * @return shared user or null if shared user not found
	 */
	public static Employee getSharedEmployee(){
		Session s = HibernateManager.openSession();
		s.beginTransaction();
		try{
			return getSharedEmployee(s);
		}finally{
			try{
				s.getTransaction().rollback();
			}catch (Exception ex){
				//eatme
			}
			s.close();
		}
	}
	/**
	 * Loads from the DB the shared user which is used 
	 * for associated shared cross conservation area queries.
	 * 
	 * @param session current open session
	 * @return shared user or null if shared user not found
	 */
	public static Employee getSharedEmployee(Session session){
		Query q = session.createQuery("From Employee e WHERE e.uuid = :e and e.conservationArea.uuid = :ca"); //$NON-NLS-1$
		q.setParameter("e", Employee.SHARED_UUID); //$NON-NLS-1$
		q.setParameter("ca", ConservationArea.MULTIPLE_CA); //$NON-NLS-1$
		@SuppressWarnings("unchecked")
		List<Employee> es = q.list();
		if (es.size() > 0){
			return es.get(0);
		}
		return null;
	}
	
	
	/**
	 * Determines if the smart database file exists.  This does not
	 * mean it is a valid file.
	 * 
	 * @return true if the smart database exists, false otherwise
	 */
	public static boolean dbExists(){
		String embeddedDb = SmartProperties.getInstance().getProperty(SmartProperties.PROP_SMART_DB);
		File db = new File(embeddedDb); 
		boolean exists = db.exists();
		if (!exists){
			SmartPlugIn.log(Messages.SmartDB_Error_NoSmartDatabase + db.getAbsolutePath().toString(), null);
		}
		return exists;
	}
	
	/**
	 * Get the conservation area the application
	 * is current processing.
	 * @return current conservation area
	 */
	public static ConservationArea getCurrentConservationArea(){
		return currentCa;
	}
	
	public static boolean isMultipleAnalysis(){
		try{
			return getCurrentConservationArea().getIsCcaa();
		}catch (NullPointerException ex){
			return false;
		}
	}
	/**
	 * 
	 * @return the configuration used if the current login
	 * is performing cross conservation area analysis.  Otherwise
	 * it will return <code>null</code>.
	 */
	public static ConservationAreaConfiguration getConservationAreaConfiguration(){
		return caConfig;
	}
	
	
	/**
	 * Sets the login information
	 * @param selectedCa
	 */
	public static void setConservationAreaConfiguration(Employee user, String plainTextPassword,
			ConservationArea ca, 
			ConservationAreaConfiguration configuration) throws Exception{
		currentCa = ca;
		if (currentEmployee == null || !currentEmployee.equals(user)){
			//new user
			current = findDbUser(user);
			HibernateManager.setUserName(current.username, current.password);
		}else{
			current = null;
		}
		currentEmployee = user;
		SmartDB.plainTextPassword = plainTextPassword;
		caConfig = configuration;
	}
	
	public static String getPlainTextPassword(){
		return plainTextPassword;
	}
	
	/**
	 * Get the employee of the current user.
	 * @return current employee
	 */
	public static Employee getCurrentEmployee(){
		return currentEmployee;
	}
	
	/**
	 * Finds the database user for a given employee
	 * 
	 * @param user employee
	 * @return the database user
	 */
	public static DbUser findDbUser(Employee user){

		if (user.supportsUser(UserLevelManager.ADMIN)){
			return DbUser.ADMIN;
		}else if (user.supportsUser(UserLevelManager.MANAGER)){
			return DbUser.MANAGER;
		}else if (user.supportsUser(UserLevelManager.ANALYST)){
			return DbUser.ANALYST;
		}else if (user.supportsUser(UserLevelManager.DATA_ENTRY)){
			return DbUser.DATAENTRY;
		}
		return DbUser.ADMIN;
	}
	
	
	/**
	 * Must be called after current conservation area set
	 * @return the current language of the logged in user
	 */
	public static Language getCurrentLanguage(){
		if (caConfig == null){
			return null;
		}
		return caConfig.getLanguage();
	}
	
}
