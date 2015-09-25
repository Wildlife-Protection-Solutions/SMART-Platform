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
package org.wcs.smart.intelligence.upgrade;

import java.text.MessageFormat;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.intelligence.IntelligencePlugIn;
import org.wcs.smart.intelligence.internal.Messages;
import org.wcs.smart.intelligence.updatesite.OnInstallAction;
import org.wcs.smart.upgrade.IDatabaseUpgrader;
import org.wcs.smart.upgrade.UpgradeEngine;

/**
 * Intelligence upgrade operations while upgrade/restore backup.
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class IntelligenceDatabaseUpgrader implements IDatabaseUpgrader {
	
	/* (non-Javadoc)
	 * @see org.wcs.smart.upgrade.IDatabaseUpgrader#upgrade(org.hibernate.Session, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void upgrade(IProgressMonitor monitor) {

		String currentPluginVersion = null;
		
		Session s = HibernateManager.openSession();
		try{
			Map<String, String> versions = UpgradeEngine.getVersions(s);
			if (versions == null) throw new IllegalStateException("Database versions not found."); //shouldn't happy //$NON-NLS-1$
			currentPluginVersion = versions.get(IntelligencePlugIn.PLUGIN_ID);
		}finally{
			s.close();
		}
		
		if (currentPluginVersion == null) {
			//Entity doesn't present in this configuration
			//we need to perform install database support for the plug-in
			monitor.subTask(Messages.IntelligenceDatabaseUpgrader_UpgradeTask);
			OnInstallAction install = new OnInstallAction();
			install.execute(null);
		
		}else{
			s = HibernateManager.openSession();
			s.beginTransaction();
			try{
				upgrade(currentPluginVersion, s);
				s.getTransaction().commit();
			}catch (final Throwable t){
				if (s.getTransaction().isActive()) s.getTransaction().rollback();
				final String msg = MessageFormat.format(Messages.IntelligenceDatabaseUpgrader_UpgradeError, new Object[]{currentPluginVersion, IntelligencePlugIn.DB_VERSION}) + " \n\n" + t.getMessage(); //$NON-NLS-1$
				Display.getDefault().syncExec(new Runnable(){
					@Override
					public void run() {
						IntelligencePlugIn.displayLog(msg, t);
					}
				});
			}finally{
				s.close();
			}
		}
	}
	/**
	 * Upgrades from the currentVersion to the most recent version.
	 * @param currentVersion
	 * @param session in active transaction
	 */
	public static final void upgrade(String currentVersion, Session session){
		if (currentVersion.equals(IntelligencePlugIn.DB_VERSION_31)){
			upgradeV31ToV32(session);
			upgradeV32ToV40(session);
		}else if (currentVersion.equals(IntelligencePlugIn.DB_VERSION_32)){
			upgradeV32ToV40(session);
		}
	}
	
	private static void upgradeV31ToV32(Session session){
		String[] sql = new String[] {
				"CREATE TABLE smart.informant (uuid CHAR(16) for bit data NOT NULL, ca_uuid CHAR(16) for bit data  NOT NULL, ID varchar(128), IS_ACTIVE BOOLEAN NOT NULL, PRIMARY KEY (UUID))", //$NON-NLS-1$
				"ALTER TABLE smart.informant ADD CONSTRAINT informant_ca_uuid_fk FOREIGN KEY (CA_UUID) REFERENCES smart.conservation_area(UUID) ON UPDATE RESTRICT ON DELETE RESTRICT", //$NON-NLS-1$
				"GRANT SELECT ON smart.informant to data_entry", //$NON-NLS-1$
				"GRANT SELECT ON smart.informant to manager", //$NON-NLS-1$
				"GRANT SELECT ON smart.informant to analyst", //$NON-NLS-1$
				
				"alter table smart.intelligence add column INFORMANT_UUID CHAR(16) for bit data", //$NON-NLS-1$
				"ALTER TABLE smart.intelligence ADD CONSTRAINT intelligence_informant_uuid_fk FOREIGN KEY (INFORMANT_UUID) REFERENCES smart.informant(UUID) ON UPDATE RESTRICT ON DELETE RESTRICT" //$NON-NLS-1$
		};
		
		for (String s : sql){
			session.createSQLQuery(s).executeUpdate();
		}
		HibernateManager.setPlugInVersion(IntelligencePlugIn.PLUGIN_ID, IntelligencePlugIn.DB_VERSION_32, session);
	}
	
	private static void upgradeV32ToV40(Session session){
		@SuppressWarnings("nls")
		String[] sql = new String[] {
				"alter table smart.INTELLIGENCE_SOURCE add constraint intell_source_keyid_unq unique(ca_uuid, keyid) DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				
				"ALTER TABLE SMART.INFORMANT DROP CONSTRAINT INFORMANT_CA_UUID_FK",
				"ALTER TABLE SMART.INTELLIGENCE_ATTACHMENT DROP CONSTRAINT INTELLIGENCE_ATTACHMENT_INTELLIGENCE_UUID_FK",
				"ALTER TABLE SMART.INTELLIGENCE DROP CONSTRAINT INTELLIGENCE_CA_UUID_FK",
				"ALTER TABLE SMART.INTELLIGENCE DROP CONSTRAINT INTELLIGENCE_CREATOR_UUID_FK",
				"ALTER TABLE SMART.INTELLIGENCE DROP CONSTRAINT INTELLIGENCE_INFORMANT_UUID_FK",
				"ALTER TABLE SMART.INTELLIGENCE DROP CONSTRAINT INTELLIGENCE_PATROL_UUID_FK",
				"ALTER TABLE SMART.INTELLIGENCE_POINT DROP CONSTRAINT INTELLIGENCE_POINT_INTELLIGENCE_UUID_FK",
				"ALTER TABLE SMART.INTELLIGENCE_SOURCE DROP CONSTRAINT INTELLIGENCE_SOURCE_CA_UUID_FK",
				"ALTER TABLE SMART.INTELLIGENCE DROP CONSTRAINT INTELLIGENCE_SOURCE_UUID_FK",
				"ALTER TABLE SMART.PATROL_INTELLIGENCE DROP CONSTRAINT PATROL_INTELLIGENCE_INTELLIGENCE_UUID_FK",
				"ALTER TABLE SMART.PATROL_INTELLIGENCE DROP CONSTRAINT PATROL_INTELLIGENCE_PATROL_UUID_FK",
				
				"ALTER TABLE SMART.INFORMANT ADD CONSTRAINT INFORMANT_CA_UUID_FK FOREIGN KEY (CA_UUID) REFERENCES SMART.CONSERVATION_AREA(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE SMART.INTELLIGENCE ADD CONSTRAINT INTELLIGENCE_CA_UUID_FK FOREIGN KEY (CA_UUID) REFERENCES SMART.CONSERVATION_AREA(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", 
				"ALTER TABLE SMART.INTELLIGENCE ADD CONSTRAINT INTELLIGENCE_PATROL_UUID_FK FOREIGN KEY (PATROL_UUID) REFERENCES SMART.PATROL(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE SMART.INTELLIGENCE ADD CONSTRAINT INTELLIGENCE_CREATOR_UUID_FK FOREIGN KEY (CREATOR_UUID) REFERENCES SMART.EMPLOYEE(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE SMART.INTELLIGENCE ADD CONSTRAINT INTELLIGENCE_SOURCE_UUID_FK FOREIGN KEY (SOURCE_UUID) REFERENCES SMART.INTELLIGENCE_SOURCE(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", 
				"ALTER TABLE SMART.INTELLIGENCE ADD CONSTRAINT INTELLIGENCE_INFORMANT_UUID_FK FOREIGN KEY (INFORMANT_UUID) REFERENCES SMART.INFORMANT(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE SMART.INTELLIGENCE_ATTACHMENT ADD CONSTRAINT INTELLIGENCE_ATTACHMENT_INTELLIGENCE_UUID_FK FOREIGN KEY (INTELLIGENCE_UUID) REFERENCES SMART.INTELLIGENCE(UUID)  ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", 
				"ALTER TABLE SMART.INTELLIGENCE_POINT ADD CONSTRAINT INTELLIGENCE_POINT_INTELLIGENCE_UUID_FK FOREIGN KEY (INTELLIGENCE_UUID) REFERENCES SMART.INTELLIGENCE(UUID)  ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", 
				"ALTER TABLE SMART.INTELLIGENCE_SOURCE ADD CONSTRAINT INTELLIGENCE_SOURCE_CA_UUID_FK FOREIGN KEY (CA_UUID) REFERENCES SMART.CONSERVATION_AREA(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", 
				"ALTER TABLE SMART.PATROL_INTELLIGENCE ADD CONSTRAINT PATROL_INTELLIGENCE_PATROL_UUID_FK FOREIGN KEY (PATROL_UUID) REFERENCES SMART.PATROL(UUID)  ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE SMART.PATROL_INTELLIGENCE ADD CONSTRAINT PATROL_INTELLIGENCE_INTELLIGENCE_UUID_FK FOREIGN KEY (INTELLIGENCE_UUID) REFERENCES SMART.INTELLIGENCE(UUID)  ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", 
		};
		for (String s : sql){
			session.createSQLQuery(s).executeUpdate();
		}
		HibernateManager.setPlugInVersion(IntelligencePlugIn.PLUGIN_ID, IntelligencePlugIn.DB_VERSION_40, session);
	}
}
