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
package org.wcs.smart.entity.updatesite;

import java.sql.Connection;
import java.sql.SQLException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.application.DisplayAccess;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.entity.EntityPlugIn;
import org.wcs.smart.entity.internal.Messages;
import org.wcs.smart.entity.upgrade.EntityDatabaseUpgrader;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * Job removes adds entity plug-in related tabled to the database
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class AddEntityJob extends Job {

	public AddEntityJob() {
		super(Messages.AddEntityJob_JobName);
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		//required if run during restore to ensure Display.syncexec calls don't block
		DisplayAccess.accessDisplayDuringStartup();
				
		Session session = HibernateManager.openSession();
		try{
			try{
				session.beginTransaction();
				installPlugin(session);
				session.getTransaction().commit();
			}catch(Exception ex){
				if (session.getTransaction().isActive()) session.getTransaction().rollback();
				Display.getDefault().syncExec(new Runnable(){
					@Override
					public void run() {
						MessageDialog.openError(Display.getDefault().getActiveShell(),
								Messages.AddEntityJob_ErrorTitle,
								Messages.AddEntityJob_ErrorMsg);
					}	
				});
				return new Status(Status.ERROR,EntityPlugIn.PLUGIN_ID, "Error installing plugin tables.", ex); //$NON-NLS-1$
					
			}
		}finally{
			session.close();
		}
		monitor.done();
		return Status.OK_STATUS;
	}

	public void installPlugin(Session session){
		String currentVersion = HibernateManager.getPlugInVersion(EntityPlugIn.PLUGIN_ID, session);
		if (currentVersion == null){
			createTables(session);
			HibernateManager.setPlugInVersion(EntityPlugIn.PLUGIN_ID, EntityPlugIn.DB_VERSION_1, session);
			currentVersion = EntityPlugIn.DB_VERSION_1;
		}
			//run the upgrader to upgrade to the current version
		EntityDatabaseUpgrader.upgrade(currentVersion, session);
	}
	
	private void createTables(Session session){
		final String[] sql = new String[]{
				"CREATE TABLE SMART.ENTITY ( UUID CHAR(16) FOR BIT DATA NOT NULL, ENTITY_TYPE_UUID CHAR(16) FOR BIT DATA NOT NULL, ID VARCHAR(32) NOT NULL, STATUS VARCHAR(16) NOT NULL, ATTRIBUTE_LIST_ITEM_UUID CHAR(16) FOR BIT DATA NOT NULL, X DOUBLE, Y DOUBLE, PRIMARY KEY (UUID))", //$NON-NLS-1$
				"CREATE TABLE SMART.ENTITY_ATTRIBUTE (UUID CHAR(16) FOR BIT DATA NOT NULL, ENTITY_TYPE_UUID CHAR(16) FOR BIT DATA NOT NULL, DM_ATTRIBUTE_UUID CHAR(16) FOR BIT DATA NOT NULL, IS_REQUIRED BOOLEAN DEFAULT false NOT NULL, ATTRIBUTE_ORDER INTEGER NOT NULL, IS_PRIMARY BOOLEAN DEFAULT true NOT NULL, KEYID VARCHAR(128) NOT NULL, PRIMARY KEY (UUID))", //$NON-NLS-1$
				"CREATE TABLE SMART.ENTITY_ATTRIBUTE_VALUE(ENTITY_ATTRIBUTE_UUID CHAR(16) FOR BIT DATA NOT NULL, ENTITY_UUID CHAR(16) FOR BIT DATA NOT NULL, NUMBER_VALUE DOUBLE, STRING_VALUE VARCHAR(1024), LIST_ELEMENT_UUID CHAR(16) FOR BIT DATA, TREE_NODE_UUID CHAR(16) FOR BIT DATA , PRIMARY KEY (ENTITY_ATTRIBUTE_UUID, ENTITY_UUID))", //$NON-NLS-1$
				"CREATE TABLE SMART.ENTITY_TYPE(UUID CHAR(16) FOR BIT DATA NOT NULL, CA_UUID CHAR(16) FOR BIT DATA NOT NULL, KEYID VARCHAR(128) NOT NULL, DATE_CREATED TIMESTAMP NOT NULL, CREATOR_UUID CHAR(16) FOR BIT DATA NOT NULL, STATUS VARCHAR(16) NOT NULL, DM_ATTRIBUTE_UUID CHAR(16) FOR BIT DATA NOT NULL, ENTITY_TYPE VARCHAR(16), PRIMARY KEY (UUID))", //$NON-NLS-1$

				"ALTER TABLE SMART.ENTITY_TYPE ADD CONSTRAINT entity_type_ca_uuid_fk FOREIGN KEY (CA_UUID) REFERENCES SMART.CONSERVATION_AREA(UUID) ON UPDATE RESTRICT ON DELETE RESTRICT", //$NON-NLS-1$
				"ALTER TABLE SMART.ENTITY_ATTRIBUTE ADD CONSTRAINT entity_attribute_dm_attribute_fk FOREIGN KEY (DM_ATTRIBUTE_UUID) REFERENCES SMART.DM_ATTRIBUTE(UUID) ON UPDATE RESTRICT ON DELETE RESTRICT", //$NON-NLS-1$
				"ALTER TABLE SMART.ENTITY_TYPE ADD CONSTRAINT entity_type_dm_attribute_fk FOREIGN KEY (DM_ATTRIBUTE_UUID) REFERENCES SMART.DM_ATTRIBUTE(UUID) ON UPDATE RESTRICT ON DELETE RESTRICT", //$NON-NLS-1$
				"ALTER TABLE SMART.ENTITY_ATTRIBUTE_VALUE ADD CONSTRAINT entity_attribute_value_entity_fk FOREIGN KEY (ENTITY_UUID) REFERENCES SMART.ENTITY(UUID) ON UPDATE RESTRICT ON DELETE RESTRICT", //$NON-NLS-1$
				"ALTER TABLE SMART.ENTITY_ATTRIBUTE_VALUE ADD CONSTRAINT entity_attribute_value_attribute_fk FOREIGN KEY (ENTITY_ATTRIBUTE_UUID) REFERENCES SMART.ENTITY_ATTRIBUTE(UUID) ON UPDATE RESTRICT ON DELETE RESTRICT", //$NON-NLS-1$
				"ALTER TABLE SMART.ENTITY_ATTRIBUTE_VALUE ADD CONSTRAINT entity_attribute_value_listelement_fk FOREIGN KEY (LIST_ELEMENT_UUID) REFERENCES SMART.DM_ATTRIBUTE_LIST(UUID) ON UPDATE RESTRICT ON DELETE RESTRICT", //$NON-NLS-1$
				"ALTER TABLE SMART.ENTITY_ATTRIBUTE_VALUE ADD CONSTRAINT entity_attribute_value_treenode_fk FOREIGN KEY (TREE_NODE_UUID) REFERENCES SMART.DM_ATTRIBUTE_TREE(UUID) ON UPDATE RESTRICT ON DELETE RESTRICT", //$NON-NLS-1$
				"ALTER TABLE SMART.ENTITY ADD CONSTRAINT entity_type_uuid_fk FOREIGN KEY (ENTITY_TYPE_UUID) REFERENCES SMART.ENTITY_TYPE(UUID) ON UPDATE RESTRICT ON DELETE RESTRICT", //$NON-NLS-1$
				"ALTER TABLE SMART.ENTITY ADD CONSTRAINT entity_attribute_list_item_uuid_fk FOREIGN KEY (ATTRIBUTE_LIST_ITEM_UUID) REFERENCES SMART.DM_ATTRIBUTE_LIST(UUID) ON UPDATE RESTRICT ON DELETE RESTRICT", //$NON-NLS-1$
				"ALTER TABLE SMART.ENTITY_ATTRIBUTE ADD CONSTRAINT entity_attribute_type_uuid_fk FOREIGN KEY (ENTITY_TYPE_UUID) REFERENCES SMART.ENTITY_TYPE(UUID) ON UPDATE RESTRICT ON DELETE RESTRICT", //$NON-NLS-1$

				"GRANT SELECT ON SMART.ENTITY_TYPE to manager", //$NON-NLS-1$
				"GRANT SELECT ON SMART.ENTITY_ATTRIBUTE TO manager", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON SMART.ENTITY_ATTRIBUTE_VALUE to manager", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON SMART.ENTITY TO manager", //$NON-NLS-1$

				"GRANT INSERT ON SMART.DM_ATTRIBUTE_LIST to manager", //$NON-NLS-1$
				"GRANT DELETE ON SMART.DM_ATTRIBUTE_LIST to manager", //$NON-NLS-1$
				"GRANT UPDATE ON SMART.DM_ATTRIBUTE_LIST to manager", //$NON-NLS-1$
				"GRANT INSERT ON SMART.CM_ATTRIBUTE_LIST to manager", //$NON-NLS-1$
				"GRANT DELETE ON SMART.CM_ATTRIBUTE_LIST to manager", //$NON-NLS-1$

				"GRANT SELECT ON SMART.ENTITY_TYPE to analyst", //$NON-NLS-1$
				"GRANT SELECT ON SMART.ENTITY_ATTRIBUTE TO analyst", //$NON-NLS-1$
				"GRANT SELECT ON SMART.ENTITY_ATTRIBUTE_VALUE to analyst", //$NON-NLS-1$
				"GRANT SELECT ON SMART.ENTITY TO analyst", //$NON-NLS-1$

				"GRANT SELECT ON SMART.ENTITY_TYPE to data_entry", //$NON-NLS-1$
				"GRANT SELECT ON SMART.ENTITY_ATTRIBUTE TO data_entry", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON SMART.ENTITY_ATTRIBUTE_VALUE to data_entry", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON SMART.ENTITY TO data_entry", //$NON-NLS-1$

				"GRANT INSERT ON SMART.DM_ATTRIBUTE_LIST to data_entry", //$NON-NLS-1$
				"GRANT DELETE ON SMART.DM_ATTRIBUTE_LIST to data_entry", //$NON-NLS-1$
				"GRANT UPDATE ON SMART.DM_ATTRIBUTE_LIST to data_entry", //$NON-NLS-1$
				"GRANT INSERT ON SMART.CM_ATTRIBUTE_LIST to data_entry", //$NON-NLS-1$
				"GRANT DELETE ON SMART.CM_ATTRIBUTE_LIST to data_entry" //$NON-NLS-1$

		};
		
		session.doWork(new Work() {
			@Override
			public void execute(Connection c) throws SQLException {
				for (int i = 0; i < sql.length; i ++){
					c.createStatement().execute(sql[i]);
				}	
			}
		});
	}	
}
