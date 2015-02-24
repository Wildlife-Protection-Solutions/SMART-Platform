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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;


import org.apache.derby.iapi.error.StandardException;
//import org.apache.derby.impl.jdbc.EmbedSQLException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.SmartProperties;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.Language;
import org.wcs.smart.hibernate.ConservationAreaConfiguration;
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
	
	/**
	 * Initializes the db and checks the version.
	 * @return <code>true</code> if successful, <code>false</code> if error
	 * occurs and application should exit.
	 */
	public static void initDb() throws Exception{
		SmartHibernateManager.setDatabaseParameter(SmartProperties.getInstance().getProperty(SmartProperties.PROP_SMART_DB));
		
		//check that the database exists
		if (!SmartDB.dbExists()){
			throw new Exception (MessageFormat.format(Messages.SmartStartUp_Error_NoSmartDb, new Object[]{SmartProperties.getInstance().getProperty(SmartProperties.PROP_SMART_DB)}));
		}
		
		try{
			SmartPlugIn.versionCheck();
		}catch (Exception ex){
			if (checkAlreadyRunning(ex)){
				throw new IllegalStateException(Messages.SmartStartUp_MultiConnectError);
			}else{
				throw ex;
			}
		}

	}
	
	
	/**
	 * Gets a list of conservation areas from the database.  
	 * 
	 * It will exit the program if an error occurs which trying
	 * to load the conservation areas.
	 * 
	 * @param includeCcaa if the ccaa conservation area should be included
	 * in the results
	 * @return list of conservation areas in the database
	 */
	public static List<Object> getConservationAreas(boolean includeCcaa){
		try{
			Session session = HibernateManager.openSession();
			session.beginTransaction();
			try{
				List<Object> results = new ArrayList<Object>();
				results.addAll(HibernateManager.getConservationAreas(session));
				
				if (results.size() > 1 && includeCcaa){
					List<?> tmp = session.createCriteria(ConservationArea.class).add(Restrictions.eq("uuid", ConservationArea.MULTIPLE_CA)).list(); //$NON-NLS-1$
					if (tmp.size() > 0){
						results.add(Messages.SmartStartUp_AnalysisLoginSepeartor);
						results.add((ConservationArea)tmp.get(0));
					}
				}
				return results;
			}finally{
				session.getTransaction().rollback();
				session.close();
			}
		}catch (Exception ex){
			throw new IllegalStateException(Messages.SmartStartUp_ConnectError + ex.getLocalizedMessage(), ex);
		}
	}
	
	/**
	 * Gets a list of conservation areas from the database.  
	 * 
	 * It will exit the program if an error occurs which trying
	 * to load the conservation areas.
	 * 
	 * @param includeCcaa if the ccaa conservation area should be included
	 * in the results
	 * @return list of conservation areas in the database
	 */
	public static void connectToDb() throws Exception{
		//check that the database exists
		if (!SmartDB.dbExists()){
			//log error message and exit
			throw new Exception(
				MessageFormat.format(Messages.SmartStartUp_Error_NoSmartDb, new Object[]{SmartProperties.getInstance().getProperty(SmartProperties.PROP_SMART_DB)}));
		}
		try{
			Session session = HibernateManager.openSession();
			session.close();
		}catch (Exception ex){
			if (checkAlreadyRunning(ex)){
				throw new Exception(Messages.SmartStartUp_MultiConnectError);
			}else{
				throw new Exception(Messages.SmartStartUp_ConnectError + ex.getLocalizedMessage(), ex);
			}
		}
	}
	
	/*
	 * Check for specific error code that is thrown if multiple
	 * applications trying to open the same database connection.
	 */
	private static boolean checkAlreadyRunning(Throwable ex){
		Throwable t = ex;
		while(t != null){
			if (t instanceof StandardException){
				StandardException se = (StandardException)t;
				if (se.getSQLState().equalsIgnoreCase("XSDB6") && se.getErrorCode() == 45000){ //$NON-NLS-1$
					return true;
				}
			}
			t = t.getCause();
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
			// different
			List<ConservationArea> areas;
			try {
				areas = HibernateManager.findConservationAreas(userName,
						password);

				if (areas == null || areas.size() == 0) {
					MessageDialog
							.openInformation(
									Display.getCurrent().getActiveShell(),
									Messages.SmartStartUp_ErrorDialogTitle,
									Messages.SmartStartUp_NoCaAccess);
					return false;
				} else if (areas.size() == 1) {
					MessageDialog
							.openInformation(
									Display.getCurrent().getActiveShell(),
									Messages.SmartStartUp_ErrorDialogTitle,
									Messages.SmartStartUp_SingleCaAccess);
					return false;
				} else {
					
					//set the employee to the employee for the first CA
					List<Employee> users = new ArrayList<Employee>();
					for (ConservationArea thisCa : areas){
						Employee e = HibernateManager.validateUser(userName, password, thisCa);
						if (e == null){
							MessageDialog.openInformation(Display.getCurrent().getActiveShell(), Messages.SmartStartUp_ErrorDialog_Title, Messages.SmartStartUp_Error_LoginFail);
							return false;
						}
						users.add(e);
					}
					
					Session session = HibernateManager.openSession();
					session.beginTransaction();
					try{
						List<?> results = session.createCriteria(Language.class).add(Restrictions.eq("ca", ca)).add(Restrictions.eq("code", Locale.getDefault().getLanguage())).list(); //$NON-NLS-1$ //$NON-NLS-2$
						if (results.size() == 0){
							Language lang = new Language();
							lang.setCa(ca);
							lang.setDefault(false);
							lang.setCode(Locale.getDefault().getLanguage());
							
							ca.getLanguages().add(lang);
							session.saveOrUpdate(lang);
						}
						session.getTransaction().commit();
					}catch (Exception ex){
						session.getTransaction().rollback();
						SmartPlugIn.log(ex.getMessage(), ex);
					}finally{
						session.close();
					}
					
					
					//	disconnect from the database & setup correct user level
					
					HibernateManager.endSessionFactory(true);					
					SmartDB.setCurrentUser(users.get(0), ca);
					ConservationAreaConfiguration config = new ConservationAreaConfiguration(areas, users);
					SmartDB.setConservationAreaConfiguration(config);
										
					return true;
				}
			} catch (Exception ex) {
				SmartPlugIn.displayLog(Messages.SmartStartUp_Error_LoginError, ex);
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
			

				ConservationAreaConfiguration config = new ConservationAreaConfiguration(Collections.singleton(ca), Collections.singleton(e));
				SmartDB.setConservationAreaConfiguration(config);
				HibernateManager.openSession();
				return true;
			}catch (Exception ex){
				SmartPlugIn.displayLog(Messages.SmartStartUp_Error_LoginError, ex);
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
