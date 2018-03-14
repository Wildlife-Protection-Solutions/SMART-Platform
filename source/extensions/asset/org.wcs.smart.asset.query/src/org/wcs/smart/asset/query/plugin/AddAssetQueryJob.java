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
package org.wcs.smart.asset.query.plugin;

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
import org.wcs.smart.asset.query.AssetQueryPlugIn;
import org.wcs.smart.asset.query.internal.Messages;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * Job adds database tables required for asset query plug-in 
 * 
 * @since 1.0.0
 */
public class AddAssetQueryJob extends Job {

	public AddAssetQueryJob() {
		super(Messages.AddAssetQueryJob_AddingAssetJobName);
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		//required if run during restore to ensure Display.syncexec calls don't block
		DisplayAccess.accessDisplayDuringStartup();
				
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try{
				installPlugin(session);
				session.getTransaction().commit();
			}catch(Exception ex){
				if (session.getTransaction().isActive()) session.getTransaction().rollback();
				Display.getDefault().syncExec(new Runnable(){
					@Override
					public void run() {
						MessageDialog.openError(Display.getDefault().getActiveShell(),
								Messages.AddAssetQueryJob_ErrorTitle,
								Messages.AddAssetQueryJob_ErrorMsg);
					}
					
				});
				return new Status(Status.ERROR,AssetQueryPlugIn.PLUGIN_ID, "Error installing plugin tables.", ex); //$NON-NLS-1$
			}
		}
		monitor.done();
		return Status.OK_STATUS;
	}

	public void installPlugin(Session session){
		String currentVersion = HibernateManager.getPlugInVersion(AssetQueryPlugIn.PLUGIN_ID, session);
		if (currentVersion == null){
			createTables(session);
			HibernateManager.setPlugInVersion(AssetQueryPlugIn.PLUGIN_ID, AssetQueryPlugIn.DB_VERSION_1, session);
			currentVersion = AssetQueryPlugIn.DB_VERSION_1;
		}
		AssetQueryDatabaseUpgrader.upgrade(currentVersion, session);
	}
	
	private void createTables(Session session){
		final String[] sql = new String[]{
				"CREATE TABLE SMART.ASSET_OBSERVATION_QUERY(UUID CHAR(16) FOR BIT DATA NOT NULL, ID VARCHAR(6) NOT NULL, CREATOR_UUID CHAR(16) FOR BIT DATA NOT NULL, QUERY_FILTER VARCHAR(32672), CA_FILTER VARCHAR(32672), CA_UUID CHAR(16) FOR BIT DATA NOT NULL, FOLDER_UUID CHAR(16) FOR BIT DATA, COLUMN_FILTER VARCHAR(32672), STYLE LONG VARCHAR, SHARED BOOLEAN NOT NULL, SHOW_DATA_COLUMNS_ONLY BOOLEAN, PRIMARY KEY (UUID) )", //$NON-NLS-1$
				"CREATE TABLE SMART.ASSET_WAYPOINT_QUERY( UUID CHAR(16) FOR BIT DATA NOT NULL, ID VARCHAR(6) NOT NULL, CREATOR_UUID CHAR(16) FOR BIT DATA NOT NULL, QUERY_FILTER VARCHAR(32672), CA_FILTER VARCHAR(32672), CA_UUID CHAR(16) FOR BIT DATA NOT NULL, FOLDER_UUID CHAR(16) FOR BIT DATA, COLUMN_FILTER VARCHAR(32672), SURVEYDESIGN_KEY VARCHAR(128), SHARED BOOLEAN NOT NULL, STYLE LONG VARCHAR, PRIMARY KEY (UUID))", //$NON-NLS-1$
				"CREATE TABLE SMART.ASSET_SUMMARY_QUERY(UUID CHAR(16) FOR BIT DATA NOT NULL, ID VARCHAR(6) NOT NULL, CREATOR_UUID CHAR(16) FOR BIT DATA NOT NULL, QUERY_DEF VARCHAR(32672), CA_FILTER VARCHAR(32672), CA_UUID CHAR(16) FOR BIT DATA NOT NULL, FOLDER_UUID CHAR(16) FOR BIT DATA ,  SHARED BOOLEAN NOT NULL,  STYLE LONG VARCHAR, PRIMARY KEY (UUID))", //$NON-NLS-1$

				"ALTER TABLE SMART.ASSET_OBSERVATION_QUERY ADD CONSTRAINT ASST_OBSERVATION_CREATOR_UUID_FK FOREIGN KEY (CREATOR_UUID) REFERENCES SMART.EMPLOYEE(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE SMART.ASSET_OBSERVATION_QUERY ADD CONSTRAINT ASST_OBSERVATION_CA_UUID_FK FOREIGN KEY (CA_UUID) REFERENCES SMART.CONSERVATION_AREA(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE SMART.ASSET_OBSERVATION_QUERY ADD CONSTRAINT ASST_OBSERVATION_FOLDER_UUID_FK FOREIGN KEY (FOLDER_UUID) REFERENCES SMART.QUERY_FOLDER(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE SMART.ASSET_WAYPOINT_QUERY ADD CONSTRAINT ASST_WAYPOINT_CREATOR_UUID_FK FOREIGN KEY (CREATOR_UUID) REFERENCES SMART.EMPLOYEE(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE SMART.ASSET_WAYPOINT_QUERY ADD CONSTRAINT ASST_WAYPOINT_CA_UUID_FK FOREIGN KEY (CA_UUID) REFERENCES SMART.CONSERVATION_AREA(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE SMART.ASSET_WAYPOINT_QUERY ADD CONSTRAINT ASST_WAYPOINT_FOLDER_UUID_FK FOREIGN KEY (FOLDER_UUID) REFERENCES SMART.QUERY_FOLDER(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE SMART.ASSET_SUMMARY_QUERY ADD CONSTRAINT ASST_SUMMARY_CREATOR_UUID_FK FOREIGN KEY (CREATOR_UUID) REFERENCES SMART.EMPLOYEE(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE",  				 //$NON-NLS-1$
				"ALTER TABLE SMART.ASSET_SUMMARY_QUERY ADD CONSTRAINT ASST_SUMMARY_CA_UUID_FK FOREIGN KEY (CA_UUID) REFERENCES SMART.CONSERVATION_AREA(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE",   				 //$NON-NLS-1$
				"ALTER TABLE SMART.ASSET_SUMMARY_QUERY ADD CONSTRAINT ASST_SUMMARY_FOLDER_UUID_FK FOREIGN KEY (FOLDER_UUID) REFERENCES SMART.QUERY_FOLDER(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE",				 //$NON-NLS-1$

				"GRANT ALL PRIVILEGES ON SMART.ASSET_OBSERVATION_QUERY TO manager", //$NON-NLS-1$
				"GRANT SELECT ON SMART.ASSET_OBSERVATION_QUERY TO data_entry", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON SMART.ASSET_OBSERVATION_QUERY TO analyst", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON SMART.ASSET_WAYPOINT_QUERY TO manager", //$NON-NLS-1$
				"GRANT SELECT ON SMART.ASSET_WAYPOINT_QUERY TO data_entry", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON SMART.ASSET_WAYPOINT_QUERY TO analyst", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON SMART.ASSET_SUMMARY_QUERY TO manager", //$NON-NLS-1$
				"GRANT SELECT ON SMART.ASSET_SUMMARY_QUERY TO data_entry", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON SMART.ASSET_SUMMARY_QUERY TO analyst", //$NON-NLS-1$
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
