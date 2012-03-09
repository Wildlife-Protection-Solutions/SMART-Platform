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
package org.wcs.smart.startup;

import java.util.List;

import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.internal.ca.create.CreateCaWizard;

/**
 * This class contains some of the basic functions required
 * for starting the smart application.
 * 
 * @author Emily Gouge
 *
 */
public class SmartStartUp {
	
	/**
	 * Gets a list of conservation areas from the database.  
	 * 
	 * It will exit the program if an error occurs which trying
	 * to load the conservation areas.
	 * 
	 * 
	 * @return list of conservation areas in the database
	 */
	public static List<ConservationArea>  getConservationAreas(){
		//check that the database exists
		if (!SmartDB.dbExists()){
			//log error message and exit
			SmartPlugIn.displayLogExit("No SMART database exists.  The application needs to be re-installed or the database restored manually.", new IllegalStateException("No SMART database."));
		}
		
		try{
			return HibernateManager.getConservationAreas();
		}catch (Throwable t){
			SmartPlugIn.displayLogExit("Could not load conservation areas from database.", t);
		}
		return null;
	}
	
	/**
	 * Logs a user into a given conservation area database.
	 * 
	 * @param ca conservation area to log into
	 * @param userName user name
	 * @param password password 
	 * @return true if successfully logged in, false otherwise
	 */
	public static boolean login(ConservationArea ca, String userName, String password ){
		try{
			Employee e = HibernateManager.validateUser(userName, password, ca);
			if (e == null){
				return false;
			}
			//disconnect from the database & setup correct user level
			HibernateManager.endSessionFactory();
			SmartDB.setCurrentUser(e, ca);
			return true;
		}catch (Exception ex){
			SmartPlugIn.displayLog(null, "Error logging in user", ex);
		}
		return false;
	}
	
		
	/**
	 * Opens the create new conservation area wizard.
	 * 
	 * @return true if the wizard completes without error; false if an error occurs while
	 * completing the wizard.
	 * 
	 * @param parent wizard parent shell
	 */
	public static boolean openCreateNewCaWizard(Shell parent){
		//TODO: surround with try/catch
		CreateCaWizard wizard = new CreateCaWizard();
		WizardDialog wd = new WizardDialog(parent, wizard);
		wd.create();
		wd.open();
		return wizard.isCompletedOk();
		
	}
}
