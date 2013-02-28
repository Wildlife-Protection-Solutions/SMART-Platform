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

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;

import org.apache.derby.impl.jdbc.EmbedSQLException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.SmartProperties;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.hibernate.SmartHibernateManager;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.ui.internal.ca.create.CreateCaWizard;

/**
 * This class contains some of the basic functions required
 * for starting the smart application.
 * 
 * @author Emily Gouge
 *
 */
public class SmartStartUp {
	
	public static void initDb(){
		SmartHibernateManager.setDatabaseParameter(SmartProperties.getInstance().getProperty(SmartProperties.PROP_SMART_DB));
	}
	
	
	/**
	 * Gets a list of conservation areas from the database.  
	 * 
	 * It will exit the program if an error occurs which trying
	 * to load the conservation areas.
	 * 
	 * 
	 * @return list of conservation areas in the database
	 */
	public static List<ConservationArea> getConservationAreas(){
		//check that the database exists
		if (!SmartDB.dbExists()){
			//log error message and exit
			throw new IllegalStateException(
				MessageFormat.format(Messages.SmartStartUp_Error_NoSmartDb, new Object[]{SmartProperties.getInstance().getProperty(SmartProperties.PROP_SMART_DB)}));
		}
		try{
			Session session = HibernateManager.openSession();
			session.beginTransaction();
			try{
				return HibernateManager.getConservationAreas(session);
			}finally{
				session.getTransaction().rollback();
				session.close();
			}
		}catch (Exception ex){
			if (checkAlreadyRunning(ex)){
				throw new IllegalStateException(Messages.SmartStartUp_MultiConnectError);
			}else{
				throw new IllegalStateException(Messages.SmartStartUp_ConnectError + ex.getLocalizedMessage(), ex);
			}
		}
	}
	
	/*
	 * Check for specific error code that is thrown if multiple
	 * applications trying to open the same database connection.
	 */
	private static boolean checkAlreadyRunning(Throwable ex){
		if (ex instanceof EmbedSQLException){
			EmbedSQLException sqlex = (EmbedSQLException) ex;
			if (sqlex.getSQLState().equalsIgnoreCase("XSDB6") && sqlex.getErrorCode() == 45000){ //$NON-NLS-1$
				return true;
			}
			
		}
		if (ex.getCause() == null){
			return false;
		}
		return checkAlreadyRunning(ex.getCause());
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
		
		if (Arrays.equals(ca.getUuid(), ConservationArea.MULTIPLE_CA)) {
			// we are performing cross-ca analysis and need to do something
			// differen
			List<ConservationArea> areas;
			try {
				areas = HibernateManager.findConservationAreas(userName,
						password);

				if (areas == null || areas.size() == 0) {
					MessageDialog
							.openInformation(
									Display.getCurrent().getActiveShell(),
									"Error",
									"The provided username/password does not have permissions to access any of the conservation areas.");
					return false;
				} else if (areas.size() == 1) {
					MessageDialog
							.openInformation(
									Display.getCurrent().getActiveShell(),
									"Error",
									"The current username/password only has access to a single conservation area.  To perform cross-conservation analaysis you must have the same user account with manager, analyst or admin permissions in each conservation area you with to query");
					return false;
				} else {
					
					//set the employee to the employee for the first CA
					Employee e = HibernateManager.validateUser(userName, password, areas.get(0));
					if (e == null){
						MessageDialog.openInformation(Display.getCurrent().getActiveShell(), Messages.SmartStartUp_ErrorDialog_Title, Messages.SmartStartUp_Error_LoginFail);
						return false;
					}
					//	disconnect from the database & setup correct user level
					HibernateManager.endSessionFactory(true);
					SmartDB.setCurrentUser(e, ca);
					SmartDB.setSelectedCas(areas);
					return true;
				}
			} catch (Exception ex) {
				SmartPlugIn.displayLog(null,
						Messages.SmartStartUp_Error_LoginError, ex);
				return false;
			}
		}else{
			try{
				Employee e = HibernateManager.validateUser(userName, password, ca);
				if (e == null){
					MessageDialog.openInformation(Display.getCurrent().getActiveShell(), Messages.SmartStartUp_ErrorDialog_Title, Messages.SmartStartUp_Error_LoginFail);
					return false;
				}
				//	disconnect from the database & setup correct user level
				HibernateManager.endSessionFactory(true);
				SmartDB.setCurrentUser(e, ca);
			
				HibernateManager.openSession();
				return true;
			}catch (Exception ex){
				SmartPlugIn.displayLog(null, Messages.SmartStartUp_Error_LoginError, ex);
			}
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
		CreateCaWizard wizard = new CreateCaWizard();
		WizardDialog wd = new WizardDialog(parent, wizard);
		wd.create();
		wd.open();
		return wizard.isCompletedOk();
		
	}
}
