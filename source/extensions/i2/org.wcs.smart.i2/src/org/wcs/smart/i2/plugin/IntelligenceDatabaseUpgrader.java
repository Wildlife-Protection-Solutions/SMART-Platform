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
package org.wcs.smart.i2.plugin;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.hibernate.query.NativeQuery;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.upgrade.IDatabaseUpgrader;
import org.wcs.smart.upgrade.UpgradeEngine;
import org.wcs.smart.util.UuidUtils;

/**
 * Plan upgrade operations while upgrade/restore backup.
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class IntelligenceDatabaseUpgrader implements IDatabaseUpgrader {

	@Override
	public void upgrade(IProgressMonitor monitor) throws Exception {
		monitor.beginTask(Messages.IntelligenceDatabaseUpgrader_JobName, 1);
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				Map<String, String> versions = UpgradeEngine.getVersions(session);
				if (versions == null) throw new IllegalStateException("Database versions not found."); //shouldn't happy //$NON-NLS-1$
				String currentPluginVersion = versions.get(Intelligence2PlugIn.PLUGIN_ID);
				
				if (currentPluginVersion == null) {
					monitor.subTask(Messages.IntelligenceDatabaseUpgrader_TaskName);
					(new AddIntelligenceJob()).installPlugin(session);
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
		if (currentVersion.equals(Intelligence2PlugIn.DB_VERSION_1)){
			upgradeV1toV2(session);
			upgradeV2toV3(session);
		}else if (currentVersion.equals(Intelligence2PlugIn.DB_VERSION_2)){
			upgradeV2toV3(session);
		}
	}
	
	private static void upgradeV1toV2(Session session) {
		String[] sql = new String[]{
				//primary date field
				"alter table smart.i_record ADD COLUMN primary_date timestamp", //$NON-NLS-1$
				"update smart.i_record set primary_date = (select a.maxdatetime from (select record_uuid, max(datetime) as maxdatetime from smart.I_LOCATION group by record_uuid) a where a.record_uuid = smart.i_record.uuid )", //$NON-NLS-1$
				"update smart.i_record set primary_date = date_created where primary_date is null", //$NON-NLS-1$
				"alter table smart.i_record ALTER COLUMN primary_date NOT NULL", //$NON-NLS-1$
				//unique constraint for attribute key id
				"alter table smart.i_attribute add constraint ca_attribute_key_unq unique(ca_uuid, keyid)", //$NON-NLS-1$
				//unique constraint for entity type id
				"alter table smart.i_entity_type add constraint ca_entity_type_key_unq unique(ca_uuid, keyid)", //$NON-NLS-1$
				//unique relationship types
				"alter table smart.i_relationship_type add constraint ca_relationship_type_key_unq unique(ca_uuid, keyid)", //$NON-NLS-1$
				"alter table smart.i_relationship_group add constraint ca_relationship_group_type_key_unq unique(ca_uuid, keyid)", //$NON-NLS-1$
				//record source type key
				"alter table smart.i_recordsource add constraint ca_recordsource_type_key_unq unique(ca_uuid, keyid)" //$NON-NLS-1$
		};
		for (String s : sql){
			session.createNativeQuery(s).executeUpdate();
		}
	}
	
	private static void upgradeV2toV3(Session session) {
		//convert "INTEL_DATA_ENTRY" to "INTEL_RECORD_VIEW,INTEL_RECORD_EDIT,INTEL_RECORD_CREATE,INTEL_ENTITY_VIEW"
		List<?> employees = session.createNativeQuery("select uuid, smartuserlevel from smart.employee where smartuserlevel is not null").list(); //$NON-NLS-1$
		for (Object e : employees) {
			UUID uuid = UuidUtils.byteToUUID( (byte[])((Object[])e)[0]);
			String permissions = (String)((Object[])e)[1];
			
			
			String[] parts = permissions.split(Employee.USER_LEVEL_SEP);
			HashSet<String> newParts = new HashSet<>();
			boolean modified = false;
			for (String part : parts) {
				if (part.equalsIgnoreCase("INTEL_DATA_ENTRY")) { //$NON-NLS-1$
					newParts.add("INTEL_RECORD_CREATE"); //$NON-NLS-1$
					newParts.add("INTEL_RECORD_VIEW"); //$NON-NLS-1$
					newParts.add("INTEL_RECORD_EDIT"); //$NON-NLS-1$
					newParts.add("INTEL_ENTITY_VIEW"); //$NON-NLS-1$
					newParts.add("INTEL_QUERY_ALL"); //$NON-NLS-1$
					modified = true;
				}else {
					newParts.add(part);
				}
			}
			if (modified) {
				StringBuilder newPermission = new StringBuilder();
				for (String s : newParts) {
					newPermission.append(s);
					newPermission.append(Employee.USER_LEVEL_SEP);
				}
				newPermission.deleteCharAt(newPermission.length() - 1);
				NativeQuery<?> update = session.createNativeQuery("Update smart.employee set smartuserlevel = :userlevel WHERE uuid = :uuid"); //$NON-NLS-1$
				update.setParameter("uuid", uuid); //$NON-NLS-1$
				update.setParameter("userlevel", newPermission.toString()); //$NON-NLS-1$
				update.executeUpdate();
			}
		}
		
		String[] sql = new String[] {
			"create table smart.i_config_option (uuid char(16) for bit data, ca_uuid char(16) for bit data not null, keyid varchar(32000) not null, value varchar(32000), unique(ca_uuid, keyid), primary key (uuid))",
			"ALTER TABLE SMART.i_config_option ADD CONSTRAINT intelconfigopcauuid FOREIGN KEY (ca_uuid) REFERENCES SMART.conservation_area(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE",
		};
		
		for (String s : sql) {
			session.createNativeQuery(s).executeUpdate();
		}
		
		HibernateManager.setPlugInVersion(Intelligence2PlugIn.PLUGIN_ID, Intelligence2PlugIn.DB_VERSION_3, session);
	}
	
}
