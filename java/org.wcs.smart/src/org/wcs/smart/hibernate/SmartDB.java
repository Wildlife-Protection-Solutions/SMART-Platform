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

import org.eclipse.core.runtime.Platform;
import org.wcs.smart.SmartProperties;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.Language;

/**
 * Manages the smart database.
 * 
 * @author Emily
 *
 */
public class SmartDB {

	/**
	 * Database user accounts
	 * 
	 * @author Emily
	 *
	 */
	public enum DbUser{
		LOGIN("login", "smrt"),
		ANALYST("analyst", "smrt"),
		MANAGER("manager", "smrt"),
		DATAENTRY("data_entry", "smrt"),
		ADMIN("smart_admin", "smart_derby");
		
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
	private static ConservationArea currentCa = null;
	private static Language currentLanguage = null;;
	
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
	 * 
	 * @param user database user account
	 */
	public static void setCurrentUser(Employee user, ConservationArea ca){
		currentEmployee = user;
		currentCa = ca;
		if (currentEmployee != null){
			current = findDbUser(user);
			HibernateManager.setUserName(current.username, current.password);
		}else{
			current = null;
		}
			
		getCurrentLanguage();
	}
	
	
	/**
	 * Determines if the smart database file exists.  This does not
	 * mean it is a valid file.
	 * 
	 * @return true if the smart database exists, false otherwise
	 */
	public static boolean dbExists(){
		String embeddedDb = SmartProperties.getInstance().getProperty(SmartProperties.SMART_DB_KEY);
		File db = new File(embeddedDb); 
		return db.exists();
	}
	
	/**
	 * Get the conservation area the application
	 * is current processing.
	 * @return current conservation area
	 */
	public static ConservationArea getCurrentConservationArea(){
		return currentCa;
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
		if (user.getSmartUserLevel() == Employee.SmartUserLevel.MANAGER){
			return DbUser.MANAGER;
		}else if (user.getSmartUserLevel() == Employee.SmartUserLevel.ADMIN){
			return DbUser.ADMIN;
		}else if (user.getSmartUserLevel() == Employee.SmartUserLevel.DATA_ENTRY){
			return DbUser.DATAENTRY;
		}else if (user.getSmartUserLevel() == Employee.SmartUserLevel.ANALYST){
			return DbUser.ANALYST;
		}
		return DbUser.LOGIN;
	}
	
	
	/**
	 * Must be called after current conservation area set
	 * @return the current language of the logged in user
	 */
	public static Language getCurrentLanguage(){
		if (currentLanguage == null){
			currentLanguage = HibernateManager.findLanguage(Platform.getNL(), getCurrentConservationArea());
		}
		return currentLanguage;
		
	}
	

}
