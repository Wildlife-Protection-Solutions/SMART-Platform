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
package org.wcs.smart.asset.plugin;

import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.wcs.smart.asset.AssetPlugIn;
import org.wcs.smart.asset.internal.Messages;
import org.wcs.smart.asset.model.AssetModuleSettings;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.upgrade.IDatabaseUpgrader;
import org.wcs.smart.upgrade.UpgradeEngine;

/**
 * Asset upgrade operations while upgrade/restore backup.
 * 
 * @author Emily
 * @since 3.0.0
 */
public class AssetDatabaseUpgrader implements IDatabaseUpgrader {

	@Override
	public void upgrade(IProgressMonitor monitor) throws Exception {
		monitor.subTask(Messages.AssetDatabaseUpgrader_TaskName);
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				Map<String, String> versions = UpgradeEngine.getVersions(session);
				if (versions == null) throw new IllegalStateException("Database versions not found."); //shouldn't happy //$NON-NLS-1$
				String currentPluginVersion = versions.get(AssetPlugIn.PLUGIN_ID);
				
				if (currentPluginVersion == null) {
					monitor.subTask(Messages.AssetDatabaseUpgrader_SubTaskName);
					(new AddAssetJob()).installPlugin(session);
				}else{
					upgrade(currentPluginVersion, session);
				}
				session.getTransaction().commit();
			}catch (Exception ex){
				session.getTransaction().rollback();
				throw ex;
			}
		}
		monitor.done();
	}
	
	/**
	 * Upgrades from the currentVersion to the most recent version.
	 * @param currentVersion
	 * @param session is active transaction
	 */
	public static final void upgrade(String currentVersion, Session session){
		//nothing to do
		if (currentVersion.equals(AssetPlugIn.DB_VERSION_1)) {
			upgradeV1toV2(session);
		}
	}
	
	private static void upgradeV1toV2(Session session) {
		String[] sql = new String[]{
				//primary date field
				"create table smart.asset_deployment_disruption(uuid char(16) for bit data not null, asset_deployment_uuid char(16) for bit data not null, start_date timestamp not null, end_date timestamp not null, comment varchar(32672), primary key (uuid))", //$NON-NLS-1$
				
				"alter table smart.asset_deployment_disruption add constraint assdep_uuid_fk foreign key (asset_deployment_uuid) REFERENCES smart.asset_deployment (uuid) DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				
				"GRANT SELECT ON smart.asset_deployment_disruption TO ANALYST", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.asset_deployment_disruption TO MANAGER", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.asset_deployment_disruption TO DATA_ENTRY", //$NON-NLS-1$
				
				"ALTER TABLE smart.asset_metadata_mapping ADD COLUMN state varchar(10)", //$NON-NLS-1$
				"UPDATE smart.asset_metadata_mapping SET state = 'ENABLED'", //$NON-NLS-1$
				"ALTER TABLE smart.asset_metadata_mapping ALTER COLUMN state set not null", //$NON-NLS-1$
				
				//add buffers to station/location objects
				"ALTER TABLE smart.asset_station add column buffer double precision", //$NON-NLS-1$
				"ALTER TABLE smart.asset_station_location add column buffer double precision", //$NON-NLS-1$
				
				"CREATE FUNCTION smart.to_double( stringvalue varchar(32000) ) RETURNS double precision PARAMETER STYLE JAVA NO SQL LANGUAGE JAVA EXTERNAL NAME 'org.wcs.smart.asset.plugin.AssetDatabaseUpgrader.toDouble'", //$NON-NLS-1$

				"UPDATE smart.asset_station set buffer = (select smart.to_double(c.value) from smart.asset_module_settings c where c.keyid = 'station_buffer' and c.ca_uuid = smart.asset_station.ca_uuid)", //$NON-NLS-1$
				"UPDATE smart.asset_station set buffer = " + AssetModuleSettings.STATION_BUFFER_DEFAULT_VALUE + " where buffer is null or buffer < 0", //$NON-NLS-1$ //$NON-NLS-2$
				
				"UPDATE smart.asset_station_location set buffer = (select smart.to_double(c.value) from smart.asset_module_settings c, smart.ASSET_STATION d where c.ca_uuid = d.ca_uuid and c.keyid = 'location_buffer' and d.uuid = smart.asset_station_location.station_uuid)", //$NON-NLS-1$
				"UPDATE smart.asset_station_location set buffer = " + AssetModuleSettings.LOCATION_BUFFER_DEFAULT_VALUE + " where buffer is null or buffer < 0", //$NON-NLS-1$ //$NON-NLS-2$
				
				"DROP FUNCTION smart.to_double", //$NON-NLS-1$
				
				"ALTER TABLE smart.asset_station alter column buffer set not null",  //$NON-NLS-1$
				"ALTER TABLE smart.asset_station_location alter column buffer set not null" //$NON-NLS-1$

		};
		
		for (String s : sql){
			session.createNativeQuery(s).executeUpdate();
		}
		
		HibernateManager.setPlugInVersion(AssetPlugIn.PLUGIN_ID, AssetPlugIn.DB_VERSION_2, session);

	}
	
	public static Double toDouble(String value) {
		try {
			return Double.valueOf(value);
		}catch (Exception ex) {
			return -1.0;
		}
	}
}
