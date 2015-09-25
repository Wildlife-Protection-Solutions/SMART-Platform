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
package org.wcs.smart.entity.upgrade;

import java.text.MessageFormat;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.entity.EntityPlugIn;
import org.wcs.smart.entity.internal.Messages;
import org.wcs.smart.entity.updatesite.OnInstallAction;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.upgrade.IDatabaseUpgrader;
import org.wcs.smart.upgrade.UpgradeEngine;

/**
 * Entity upgrade operations while upgrade/restore backup.
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class EntityDatabaseUpgrader implements IDatabaseUpgrader {

	@Override
	public void upgrade(IProgressMonitor monitor) {
		
		String currentPluginVersion = null;
		Session s = HibernateManager.openSession();
		try{
			Map<String, String> versions = UpgradeEngine.getVersions(s);
			if (versions == null) throw new IllegalStateException("Database versions not found."); //shouldn't happy //$NON-NLS-1$
			currentPluginVersion = versions.get(EntityPlugIn.PLUGIN_ID);
		}finally{
			s.close();
		}
		
		if (currentPluginVersion == null) {
			//Entity doesn't present in this configuration
			//we need to perform install database support for the plug-in
			monitor.subTask(Messages.EntityDatabaseUpgrader_UpgradeTask);
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
				final String msg = MessageFormat.format(Messages.EntityDatabaseUpgrader_UpgradeError, new Object[]{currentPluginVersion, EntityPlugIn.DB_VERSION}) + " \n\n" + t.getMessage(); //$NON-NLS-1$
				Display.getDefault().syncExec(new Runnable(){
					@Override
					public void run() {
						EntityPlugIn.displayLog(msg, t);
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
		if (currentVersion.equals(EntityPlugIn.DB_VERSION_1)){
			upgradeV1ToV2(session);
		}
	}
	
	private static void upgradeV1ToV2(Session session){
		@SuppressWarnings("nls")
		String[] sql = new String[]{
				"alter table smart.ENTITY_ATTRIBUTE add constraint entity_attribute_keyid_unq unique(entity_type_uuid, keyid) DEFERRABLE INITIALLY IMMEDIATE",
				"alter table smart.ENTITY_TYPE add constraint entity_type_keyid_unq unique(ca_uuid, keyid) DEFERRABLE INITIALLY IMMEDIATE",
				
				"ALTER TABLE SMART.ENTITY_ATTRIBUTE DROP CONSTRAINT ENTITY_ATTRIBUTE_DM_ATTRIBUTE_FK",
				"ALTER TABLE SMART.ENTITY DROP CONSTRAINT ENTITY_ATTRIBUTE_LIST_ITEM_UUID_FK",
				"ALTER TABLE SMART.ENTITY_ATTRIBUTE DROP CONSTRAINT ENTITY_ATTRIBUTE_TYPE_UUID_FK",
				"ALTER TABLE SMART.ENTITY_ATTRIBUTE_VALUE DROP CONSTRAINT ENTITY_ATTRIBUTE_VALUE_ATTRIBUTE_FK",
				"ALTER TABLE SMART.ENTITY_ATTRIBUTE_VALUE DROP CONSTRAINT ENTITY_ATTRIBUTE_VALUE_ENTITY_FK",
				"ALTER TABLE SMART.ENTITY_ATTRIBUTE_VALUE DROP CONSTRAINT ENTITY_ATTRIBUTE_VALUE_LISTELEMENT_FK",
				"ALTER TABLE SMART.ENTITY_ATTRIBUTE_VALUE DROP CONSTRAINT ENTITY_ATTRIBUTE_VALUE_TREENODE_FK",
				"ALTER TABLE SMART.ENTITY_TYPE DROP CONSTRAINT ENTITY_TYPE_CA_UUID_FK",
				"ALTER TABLE SMART.ENTITY_TYPE DROP CONSTRAINT ENTITY_TYPE_DM_ATTRIBUTE_FK",
				"ALTER TABLE SMART.ENTITY DROP CONSTRAINT ENTITY_TYPE_UUID_FK",
				
				"ALTER TABLE SMART.ENTITY ADD CONSTRAINT ENTITY_TYPE_UUID_FK FOREIGN KEY (ENTITY_TYPE_UUID) REFERENCES SMART.ENTITY_TYPE(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE SMART.ENTITY ADD CONSTRAINT ENTITY_ATTRIBUTE_LIST_ITEM_UUID_FK FOREIGN KEY (ATTRIBUTE_LIST_ITEM_UUID) REFERENCES SMART.DM_ATTRIBUTE_LIST(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE SMART.ENTITY_ATTRIBUTE ADD CONSTRAINT ENTITY_ATTRIBUTE_DM_ATTRIBUTE_FK FOREIGN KEY (DM_ATTRIBUTE_UUID) REFERENCES SMART.DM_ATTRIBUTE(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE SMART.ENTITY_ATTRIBUTE ADD CONSTRAINT ENTITY_ATTRIBUTE_TYPE_UUID_FK FOREIGN KEY (ENTITY_TYPE_UUID) REFERENCES SMART.ENTITY_TYPE(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE SMART.ENTITY_ATTRIBUTE_VALUE ADD CONSTRAINT ENTITY_ATTRIBUTE_VALUE_ENTITY_FK FOREIGN KEY (ENTITY_UUID) REFERENCES SMART.ENTITY(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE SMART.ENTITY_ATTRIBUTE_VALUE ADD CONSTRAINT ENTITY_ATTRIBUTE_VALUE_ATTRIBUTE_FK FOREIGN KEY (ENTITY_ATTRIBUTE_UUID) REFERENCES SMART.ENTITY_ATTRIBUTE(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE SMART.ENTITY_ATTRIBUTE_VALUE ADD CONSTRAINT ENTITY_ATTRIBUTE_VALUE_LISTELEMENT_FK FOREIGN KEY (LIST_ELEMENT_UUID) REFERENCES SMART.DM_ATTRIBUTE_LIST(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE SMART.ENTITY_ATTRIBUTE_VALUE ADD CONSTRAINT ENTITY_ATTRIBUTE_VALUE_TREENODE_FK FOREIGN KEY (TREE_NODE_UUID) REFERENCES SMART.DM_ATTRIBUTE_TREE(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE SMART.ENTITY_TYPE ADD CONSTRAINT ENTITY_TYPE_CA_UUID_FK FOREIGN KEY (CA_UUID) REFERENCES SMART.CONSERVATION_AREA(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE SMART.ENTITY_TYPE ADD CONSTRAINT ENTITY_TYPE_DM_ATTRIBUTE_FK FOREIGN KEY (DM_ATTRIBUTE_UUID) REFERENCES SMART.DM_ATTRIBUTE(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE",
		};
		for (String s : sql){
			session.createSQLQuery(s).executeUpdate();
		}
		HibernateManager.setPlugInVersion(EntityPlugIn.PLUGIN_ID, EntityPlugIn.DB_VERSION_2, session);
	}
}
