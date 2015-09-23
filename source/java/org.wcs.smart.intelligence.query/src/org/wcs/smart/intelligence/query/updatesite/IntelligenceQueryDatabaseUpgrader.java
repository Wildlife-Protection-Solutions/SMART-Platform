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
package org.wcs.smart.intelligence.query.updatesite;

import java.text.MessageFormat;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.intelligence.query.IntelligenceQueryPlugIn;
import org.wcs.smart.intelligence.query.internal.Messages;
import org.wcs.smart.intelligence.updatesite.OnInstallAction;
import org.wcs.smart.upgrade.IDatabaseUpgrader;
import org.wcs.smart.upgrade.UpgradeEngine;

/**
 * Entity query upgrade operations while upgrade/restore backup.
 * 
 * @author egouge
 * @since 3.1.0
 */
public class IntelligenceQueryDatabaseUpgrader implements IDatabaseUpgrader {

	@Override
	public void upgrade(IProgressMonitor monitor) {
		Map<String, String> versions = null;
		Session s = HibernateManager.openSession();
		try{
			versions = UpgradeEngine.getVersions(s);
		
			if (versions == null) {
				//we don't know what is happening with database
				//it is some kind of error or wrong database version
				return;
			}
			final String currentVersion = versions.get(IntelligenceQueryPlugIn.PLUGIN_ID);
			if (currentVersion == null) {
				//Entity doesn't present in this configuration
				//we need to perform install database support for the plug-in
				monitor.subTask(Messages.IntelligenceQueryDatabaseUpgrader_InstallStatu);
				OnInstallAction install = new OnInstallAction();
				install.execute(null);
			}else{
				try{
					upgrade(currentVersion, s);
				}catch (final Throwable t){
					Display.getDefault().syncExec(new Runnable(){
						@Override
						public void run() {
							IntelligenceQueryPlugIn.displayLog(MessageFormat.format(
									"Error upgrading the intelligence query plugin database tables from version {0} to version {1}.", new Object[]{currentVersion, IntelligenceQueryPlugIn.DB_VERSION}) + " \n\n" + t.getMessage(), t); //$NON-NLS-1$ //$NON-NLS-2$
						}
					});
				}
			}
		}finally{
			s.close();
		}
	}
	
	/**
	 * Upgrades from the currentVersion to the most recent version.
	 * @param currentVersion
	 * @param session
	 */
	public static final void upgrade(String currentVersion, Session session){
		if (currentVersion.equals(IntelligenceQueryPlugIn.DB_VERSION_1)){
			upgradeV1ToV2(session);
		}
	}
	
	private static void upgradeV1ToV2(Session session) {
		
		String[] sql = new String[] {
				"ALTER TABLE SMART.INTEL_RECORD_QUERY DROP CONSTRAINT INTEL_RECORD_QUERY_CA_UUID_FK",
				"ALTER TABLE SMART.INTEL_RECORD_QUERY DROP CONSTRAINT INTEL_RECORD_QUERY_CREATOR_UUID_FK",
				"ALTER TABLE SMART.INTEL_RECORD_QUERY DROP CONSTRAINT INTEL_RECORD_QUERY_FOLDER_UUID_FK",
				"ALTER TABLE SMART.INTEL_SUMMARY_QUERY DROP CONSTRAINT INTEL_SUMMARY_QUERY_CA_UUID_FK",
				"ALTER TABLE SMART.INTEL_SUMMARY_QUERY DROP CONSTRAINT INTEL_SUMMARY_QUERY_CREATOR_UUID_FK",
				"ALTER TABLE SMART.INTEL_SUMMARY_QUERY DROP CONSTRAINT INTEL_SUMMARY_QUERY_FOLDER_UUID_FK",

				"ALTER TABLE SMART.INTEL_RECORD_QUERY ADD CONSTRAINT INTEL_RECORD_QUERY_CREATOR_UUID_FK FOREIGN KEY (CREATOR_UUID) REFERENCES SMART.EMPLOYEE(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE SMART.INTEL_RECORD_QUERY ADD CONSTRAINT INTEL_RECORD_QUERY_FOLDER_UUID_FK FOREIGN KEY (FOLDER_UUID) REFERENCES SMART.QUERY_FOLDER(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", 
				"ALTER TABLE SMART.INTEL_RECORD_QUERY ADD CONSTRAINT INTEL_RECORD_QUERY_CA_UUID_FK FOREIGN KEY (CA_UUID) REFERENCES SMART.CONSERVATION_AREA(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE SMART.INTEL_SUMMARY_QUERY ADD CONSTRAINT INTEL_SUMMARY_QUERY_CREATOR_UUID_FK FOREIGN KEY (CREATOR_UUID) REFERENCES SMART.EMPLOYEE(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE SMART.INTEL_SUMMARY_QUERY ADD CONSTRAINT INTEL_SUMMARY_QUERY_FOLDER_UUID_FK FOREIGN KEY (FOLDER_UUID) REFERENCES SMART.QUERY_FOLDER(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", 
				"ALTER TABLE SMART.INTEL_SUMMARY_QUERY ADD CONSTRAINT INTEL_SUMMARY_QUERY_CA_UUID_FK FOREIGN KEY (CA_UUID) REFERENCES SMART.CONSERVATION_AREA(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE" 
		};
		
		session.beginTransaction();
		try{
			for (String s : sql){
				session.createSQLQuery(s).executeUpdate();
			}
		
			HibernateManager.setPlugInVersion(IntelligenceQueryPlugIn.PLUGIN_ID, IntelligenceQueryPlugIn.DB_VERSION_2, session);
			session.getTransaction().commit();
		}finally{
			if (session.getTransaction().isActive()){
				session.getTransaction().rollback();
			}
		}
	}
}
