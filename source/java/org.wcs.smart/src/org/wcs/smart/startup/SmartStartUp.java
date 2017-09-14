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
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.apache.derby.iapi.error.StandardException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.wcs.smart.ILoginHandler;
import org.wcs.smart.LoginLogEntry;
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
import org.wcs.smart.user.UserLevelManager;

/**
 * This class contains some of the basic functions required
 * for starting the smart application.
 * 
 * @author Emily Gouge
 *
 */
public class SmartStartUp {
	
	public static final String LOGIN_EXT_ID = "org.wcs.smart.caLogin"; //$NON-NLS-1$
	
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
		
		//check version
		try{
			SmartPlugIn.versionCheck();
		}catch (Exception ex){
			if (checkAlreadyRunning(ex)){
				throw new IllegalStateException(Messages.SmartStartUp_MultiConnectError);
			}else{
				throw ex;
			}
		}
		
		//run any other start scripts
		List<IDatabaseStartupRunner> runners = new ArrayList<IDatabaseStartupRunner>();
		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(IDatabaseStartupRunner.EXTENSION_ID);
		for (IConfigurationElement e : config) {	
			if (e.getName().equals(IDatabaseStartupRunner.CLASS_ATTRIBUTE_NAME)){
				IDatabaseStartupRunner runner = (IDatabaseStartupRunner) e.createExecutableExtension("class"); //$NON-NLS-1$
				runners.add(runner);
			}
		}
		for (IDatabaseStartupRunner runner: runners){
			try(Session s = HibernateManager.openSession()){
				runner.run(s);
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
		try(Session session = HibernateManager.openSession()){
			
			session.beginTransaction();
			try{
				List<Object> results = new ArrayList<Object>();
				results.addAll(HibernateManager.getConservationAreas(session));
				
				if (results.size() > 1 && includeCcaa){
					CriteriaBuilder cb = session.getCriteriaBuilder();
					CriteriaQuery<ConservationArea> query = cb.createQuery(ConservationArea.class);
					Root<ConservationArea> from = query.from(ConservationArea.class);
					query.where(cb.equal(from.get("uuid"), ConservationArea.MULTIPLE_CA)); //$NON-NLS-1$
					
					List<?> tmp = session.createQuery(query).getResultList();
					if (tmp.size() > 0){
						results.add(Messages.SmartStartUp_AnalysisLoginSepeartor);
						results.add((ConservationArea)tmp.get(0));
					}
				}
				return results;
			}finally{
				session.getTransaction().rollback();
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
		try(Session session = HibernateManager.openSession()){
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
		if (ca.getIsCcaa()){
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
					
					
					Employee ccaaUser = null;
					try(Session session = HibernateManager.openSession()){
						
						session.beginTransaction();
						try {
							Query<Employee> e = session.createQuery("FROM Employee WHERE conservationArea = :ca and upper(smartUserId) = :id", Employee.class); //$NON-NLS-1$
							e.setParameter("ca", ca); //$NON-NLS-1$
							e.setParameter("id", users.get(0).getSmartUserId().toUpperCase()); //$NON-NLS-1$
							ccaaUser = e.uniqueResult();
							
							if (ccaaUser == null){
								ccaaUser = new Employee();
								ccaaUser.setGender(Employee.DB_MALE);
								ccaaUser.setSmartUserId(users.get(0).getSmartUserId());
								ccaaUser.setGivenName(ccaaUser.getSmartUserId());
								ccaaUser.setFamilyName(""); //$NON-NLS-1$
								ccaaUser.setStartEmploymentDate(new Date());
								ccaaUser.setId(ccaaUser.getSmartUserId());
								ccaaUser.setConservationArea(ca);
								ccaaUser.setSmartUserLevel(UserLevelManager.INSTANCE.getUserLevels().values()); //give them all seeing access
								session.save(ccaaUser);
								session.flush();
							}
							
							Query<Language> q = session.createQuery("FROM Language WHERE ca = :ca and code = :code", Language.class); //$NON-NLS-1$
							q.setParameter("ca", ca); //$NON-NLS-1$
							q.setParameter("code", Locale.getDefault().getLanguage()); //$NON-NLS-1$
							List<Language> results = q.getResultList(); 
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
							return false;
						}
					}
					
					ConservationAreaConfiguration config = null;
					try(Session s = HibernateManager.openSession()){
						config = new ConservationAreaConfiguration(ca, areas, ccaaUser, users, s);
					}
					//	disconnect from the database & setup correct user level
					SmartDB.setConservationAreaConfiguration(ccaaUser, password, ca, config);
					
					//Record this Login in the login-log
					recordLogin(ccaaUser, ca);
					
					try (Session s = HibernateManager.openSession()){
					}
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

				ConservationAreaConfiguration config = null;
				try(Session s = HibernateManager.openSession()){
					config = new ConservationAreaConfiguration(ca, Collections.singleton(ca),e, Collections.singleton(e), s);
				}
				//	disconnect from the database & setup correct user level
				SmartDB.setConservationAreaConfiguration(e, password, ca, config);
				
				//Record this Login in the login-log
				recordLogin(e, ca);
				
				try(Session s = HibernateManager.openSession()){}

			}catch (Exception ex){
				SmartPlugIn.displayLog(Messages.SmartStartUp_Error_LoginError + ": " + ex.getMessage(), ex); //$NON-NLS-1$
				return false;
			}
		}
		
		//run login handlers
		List<ILoginHandler> handlers = new ArrayList<ILoginHandler>();
		try{
			IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(LOGIN_EXT_ID);
			for (IConfigurationElement e : config) {	
				if (e.getName().equals("loginHandler")){ //$NON-NLS-1$
					ILoginHandler handler = (ILoginHandler) e.createExecutableExtension("clazz"); //$NON-NLS-1$
					handlers.add(handler);
				}
			}
		}catch (Exception ex){
			String error = MessageFormat.format(Messages.SmartStartUp_CannotLogin + "\n\n" + "{1}.", ca.getName(), ex.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
			SmartPlugIn.displayLog(error, ex);
			return false;
		}
		for (ILoginHandler h : handlers){
			try{
				h.onLogin();
			}catch (Exception ex){
				String error = MessageFormat.format(Messages.SmartStartUp_CannotLogin + "\n\n" + Messages.SmartStartUp_LoginHandlerError, ca.getName(), h.getClass().getName(), ex.getMessage()); //$NON-NLS-1$
				SmartPlugIn.displayLog(error, ex);
				return false;
			}
		}
		
		return true;
	}
	
	
	private static void recordLogin(Employee e, ConservationArea ca) {
		try(Session s = HibernateManager.openSession()){
			s.beginTransaction();
			try {
				LoginLogEntry l = new LoginLogEntry();
				l.setCaId(ca.getId());
				l.setCaName(ca.getName());
				l.setSmartUserId(e.getSmartUserId());
				l.setUserLevels(e.getSmartUserLevelKeys());
				s.save(l);
				s.getTransaction().commit();
			}catch (Exception ex) {
				SmartPlugIn.log(ex.getMessage(),  ex);
				s.getTransaction().rollback();
			}
		}
		
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
