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

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.hibernate.query.NativeQuery;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelPermission;
import org.wcs.smart.i2.model.IntelRecordObservationQuery;
import org.wcs.smart.upgrade.IDatabaseUpgrader;
import org.wcs.smart.upgrade.UpgradeEngine;
import org.wcs.smart.util.DerbyUtils;
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
		monitor.subTask(Messages.IntelligenceDatabaseUpgrader_JobName);
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
			upgradeV3toV4(session);
			upgradeV4toV5(session);
		}else if (currentVersion.equals(Intelligence2PlugIn.DB_VERSION_2)){
			upgradeV2toV3(session);
			upgradeV3toV4(session);
			upgradeV4toV5(session);
		}else if (currentVersion.equals(Intelligence2PlugIn.DB_VERSION_3)){
			upgradeV3toV4(session);
			upgradeV4toV5(session);
		}else if (currentVersion.equals(Intelligence2PlugIn.DB_VERSION_4)){
			upgradeV4toV5(session);
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
			"create table smart.i_config_option (uuid char(16) for bit data, ca_uuid char(16) for bit data not null, keyid varchar(32000) not null, value varchar(32000), unique(ca_uuid, keyid), primary key (uuid))", //$NON-NLS-1$
			"ALTER TABLE smart.i_config_option ADD CONSTRAINT intelconfigopcauuid FOREIGN KEY (ca_uuid) REFERENCES SMART.conservation_area(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
			
			"GRANT SELECT on smart.i_config_option TO manager", //$NON-NLS-1$
			"GRANT SELECT on smart.i_config_option TO data_entry", //$NON-NLS-1$
			"GRANT SELECT on smart.i_config_option TO analyst", //$NON-NLS-1$
			
			//employee attribute option
			"ALTER TABLE smart.i_entity_attribute_value add column employee_uuid char(16) for bit data", //$NON-NLS-1$
			"ALTER TABLE smart.i_entity_relationship_attribute_value add column employee_uuid char(16) for bit data", //$NON-NLS-1$
			"ALTER TABLE smart.i_entity_attribute_value ADD CONSTRAINT entityattributevalue_employee_uuid_fk FOREIGN KEY (employee_uuid) REFERENCES SMART.employee(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
			"ALTER TABLE smart.i_entity_relationship_attribute_value ADD CONSTRAINT relationshipattrvalue_employee_uuid_fk FOREIGN KEY (employee_uuid) REFERENCES SMART.employee(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
			
			//entity summary queries
			"CREATE TABLE smart.i_entity_summary_query(uuid char(16) for bit data NOT NULL,ca_uuid char(16) for bit data NOT NULL,query_string long varchar,date_created timestamp NOT NULL,last_modified_date timestamp,created_by char(16) for bit data NOT NULL,last_modified_by char(16) for bit data,PRIMARY KEY (uuid))", //$NON-NLS-1$
			"ALTER TABLE smart.i_entity_summary_query ADD CONSTRAINT ientitysummquery_cauuid_fk FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area (uuid) DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
			"ALTER TABLE smart.i_entity_summary_query ADD CONSTRAINT ientitysummquery_createdby_fk FOREIGN KEY (created_by) REFERENCES smart.employee (uuid) DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
			"ALTER TABLE smart.i_entity_summary_query ADD CONSTRAINT ientitysummquery_modifiedby_fk FOREIGN KEY (last_modified_by) REFERENCES smart.employee (uuid) DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
			
			//entity (record) queries
			"CREATE TABLE smart.i_entity_record_query(uuid char(16) for bit data NOT NULL,ca_uuid char(16) for bit data NOT NULL,query_string long varchar,date_created timestamp NOT NULL,last_modified_date timestamp,created_by char(16) for bit data NOT NULL,last_modified_by char(16) for bit data,PRIMARY KEY (uuid))", //$NON-NLS-1$
			"ALTER TABLE smart.i_entity_record_query ADD CONSTRAINT ientityrecordquery_cauuid_fk FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area (uuid) DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
			"ALTER TABLE smart.i_entity_record_query ADD CONSTRAINT ientityrecordquery_createdby_fk FOREIGN KEY (created_by) REFERENCES smart.employee (uuid) DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
			"ALTER TABLE smart.i_entity_record_query ADD CONSTRAINT ientityrecordquery_modifiedby_fk FOREIGN KEY (last_modified_by) REFERENCES smart.employee (uuid) DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
			
			
			//updates for working set queries
			"ALTER TABLE smart.i_working_set_query DROP CONSTRAINT iworkingsetquery_query_fk", //$NON-NLS-1$
			"ALTER TABLE smart.I_WORKING_SET_QUERY add column query_type varchar(32)", //$NON-NLS-1$
			"UPDATE smart.i_working_set_query set query_type = '" +IntelRecordObservationQuery.KEY + "'",  //$NON-NLS-1$//$NON-NLS-2$
			"ALTER TABLE smart.i_working_set_query alter column query_type set not null", //$NON-NLS-1$
			
			//index on record title
			"create index i_record_title on smart.i_record (title)", //$NON-NLS-1$
			
			//index on attribute list items
			"ALTER TABLE smart.i_attribute_list_item add column list_order integer not null default 0", //$NON-NLS-1$
			
			//relationship diagram related table
			"CREATE TABLE smart.i_diagram_style (uuid CHAR(16) for bit data NOT NULL, ca_uuid CHAR(16) for bit data NOT NULL, IS_DEFAULT BOOLEAN, OPTIONS VARCHAR(2048), PRIMARY KEY (UUID))", //$NON-NLS-1$
			"ALTER TABLE smart.i_diagram_style ADD CONSTRAINT I_DIAGRAM_STYLE_CA_UUID_FK FOREIGN KEY (CA_UUID) REFERENCES SMART.CONSERVATION_AREA(UUID) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
			"GRANT ALL PRIVILEGES ON smart.i_diagram_style to data_entry", //$NON-NLS-1$
			"GRANT ALL PRIVILEGES ON smart.i_diagram_style to manager", //$NON-NLS-1$
			"GRANT ALL PRIVILEGES ON smart.i_diagram_style to analyst", //$NON-NLS-1$
			"GRANT SELECT ON smart.i_diagram_style to login", //$NON-NLS-1$
			
			"CREATE TABLE smart.i_diagram_entity_type_style (uuid CHAR(16) for bit data NOT NULL, style_uuid CHAR(16) for bit data NOT NULL, entity_type_uuid CHAR(16) for bit data NOT NULL, OPTIONS VARCHAR(1024), PRIMARY KEY (UUID))", //$NON-NLS-1$
			"ALTER TABLE smart.i_diagram_entity_type_style ADD CONSTRAINT I_DIAGRAM_ENTITY_TYPE_STYLE_STYLE_UUID_FK FOREIGN KEY (STYLE_UUID) REFERENCES SMART.I_DIAGRAM_STYLE(UUID) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
			"ALTER TABLE smart.i_diagram_entity_type_style ADD CONSTRAINT I_DIAGRAM_ENTITY_TYPE_STYLE_ENTITY_TYPE_UUID_FK FOREIGN KEY (ENTITY_TYPE_UUID) REFERENCES SMART.I_ENTITY_TYPE(UUID) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
			"GRANT ALL PRIVILEGES ON smart.i_diagram_entity_type_style to data_entry", //$NON-NLS-1$
			"GRANT ALL PRIVILEGES ON smart.i_diagram_entity_type_style to manager", //$NON-NLS-1$
			"GRANT ALL PRIVILEGES ON smart.i_diagram_entity_type_style to analyst", //$NON-NLS-1$
			"GRANT SELECT ON smart.i_diagram_entity_type_style to login", //$NON-NLS-1$

			"CREATE TABLE smart.i_diagram_relationship_type_style (uuid CHAR(16) for bit data NOT NULL, style_uuid CHAR(16) for bit data NOT NULL, relationship_type_uuid CHAR(16) for bit data NOT NULL, OPTIONS VARCHAR(1024), PRIMARY KEY (UUID))", //$NON-NLS-1$
			"ALTER TABLE smart.i_diagram_relationship_type_style ADD CONSTRAINT I_DIAGRAM_RELATIONSHIP_TYPE_STYLE_STYLE_UUID_FK FOREIGN KEY (STYLE_UUID) REFERENCES SMART.I_DIAGRAM_STYLE(UUID) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
			"ALTER TABLE smart.i_diagram_relationship_type_style ADD CONSTRAINT I_DIAGRAM_RELATIONSHIP_TYPE_STYLE_RELATIONSHIP_TYPE_UUID_FK FOREIGN KEY (RELATIONSHIP_TYPE_UUID) REFERENCES SMART.I_RELATIONSHIP_TYPE(UUID) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
			"GRANT ALL PRIVILEGES ON smart.i_diagram_relationship_type_style to data_entry", //$NON-NLS-1$
			"GRANT ALL PRIVILEGES ON smart.i_diagram_relationship_type_style to manager", //$NON-NLS-1$
			"GRANT ALL PRIVILEGES ON smart.i_diagram_relationship_type_style to analyst", //$NON-NLS-1$
			"GRANT SELECT ON smart.i_diagram_relationship_type_style to login", //$NON-NLS-1$
			
			//these permissions are to support the events plugin
			//i put them here to keep it easier to manage
			"GRANT ALL PRIVILEGES ON smart.I_RECORD TO data_entry", //$NON-NLS-1$
			"GRANT ALL PRIVILEGES ON smart.I_RECORD_ATTACHMENT TO data_entry", //$NON-NLS-1$
			"GRANT ALL PRIVILEGES ON smart.I_RECORD_ATTRIBUTE_VALUE TO data_entry", //$NON-NLS-1$
			"GRANT ALL PRIVILEGES ON smart.I_RECORD_ATTRIBUTE_VALUE_LIST TO data_entry", //$NON-NLS-1$
			"GRANT ALL PRIVILEGES ON smart.I_ENTITY TO data_entry", //$NON-NLS-1$
			"GRANT ALL PRIVILEGES ON smart.I_ENTITY_ATTACHMENT TO data_entry", //$NON-NLS-1$
			"GRANT ALL PRIVILEGES ON smart.I_ENTITY_ATTRIBUTE_VALUE TO data_entry", //$NON-NLS-1$
			"GRANT ALL PRIVILEGES ON smart.I_ENTITY_LOCATION TO data_entry", //$NON-NLS-1$
			"GRANT ALL PRIVILEGES ON smart.I_ENTITY_RECORD TO data_entry", //$NON-NLS-1$
			"GRANT ALL PRIVILEGES ON smart.I_ENTITY_RELATIONSHIP TO data_entry", //$NON-NLS-1$
			"GRANT ALL PRIVILEGES ON smart.I_ENTITY_RELATIONSHIP_ATTRIBUTE_VALUE TO data_entry", //$NON-NLS-1$
			"GRANT ALL PRIVILEGES ON smart.I_ATTACHMENT TO data_entry", //$NON-NLS-1$
			"GRANT ALL PRIVILEGES ON smart.I_LOCATION TO data_entry", //$NON-NLS-1$
			"GRANT ALL PRIVILEGES ON smart.I_OBSERVATION TO data_entry", //$NON-NLS-1$
			"GRANT ALL PRIVILEGES ON smart.I_OBSERVATION_ATTRIBUTE TO data_entry", //$NON-NLS-1$
			"GRANT SELECT ON smart.i_recordsource to data_entry", //$NON-NLS-1$
			"GRANT SELECT ON smart.i_attribute to data_entry", //$NON-NLS-1$
			"GRANT SELECT ON smart.I_ATTRIBUTE_LIST_ITEM to data_entry", //$NON-NLS-1$
			"grant select on smart.i_entity_type to data_entry", //$NON-NLS-1$
			"grant select on smart.I_ENTITY_TYPE_ATTRIBUTE to data_entry", //$NON-NLS-1$
			"grant select on smart.I_ENTITY_TYPE_ATTRIBUTE_GROUP to data_entry", //$NON-NLS-1$
			"GRANT ALL PRIVILEGES ON smart.I_RECORD TO manager", //$NON-NLS-1$
			"GRANT ALL PRIVILEGES ON smart.I_RECORD_ATTACHMENT TO manager", //$NON-NLS-1$
			"GRANT ALL PRIVILEGES ON smart.I_RECORD_ATTRIBUTE_VALUE TO manager", //$NON-NLS-1$
			"GRANT ALL PRIVILEGES ON smart.I_RECORD_ATTRIBUTE_VALUE_LIST TO manager", //$NON-NLS-1$
			"GRANT ALL PRIVILEGES ON smart.I_ENTITY TO manager", //$NON-NLS-1$
			"GRANT ALL PRIVILEGES ON smart.I_ENTITY_ATTACHMENT TO manager", //$NON-NLS-1$
			"GRANT ALL PRIVILEGES ON smart.I_ENTITY_ATTRIBUTE_VALUE TO manager", //$NON-NLS-1$
			"GRANT ALL PRIVILEGES ON smart.I_ENTITY_LOCATION TO manager", //$NON-NLS-1$
			"GRANT ALL PRIVILEGES ON smart.I_ENTITY_RECORD TO manager", //$NON-NLS-1$
			"GRANT ALL PRIVILEGES ON smart.I_ENTITY_RELATIONSHIP TO manager", //$NON-NLS-1$
			"GRANT ALL PRIVILEGES ON smart.I_ENTITY_RELATIONSHIP_ATTRIBUTE_VALUE TO manager", //$NON-NLS-1$
			"GRANT ALL PRIVILEGES ON smart.I_ATTACHMENT TO manager", //$NON-NLS-1$
			"GRANT ALL PRIVILEGES ON smart.I_LOCATION TO manager", //$NON-NLS-1$
			"GRANT ALL PRIVILEGES ON smart.I_OBSERVATION TO manager", //$NON-NLS-1$
			"GRANT ALL PRIVILEGES ON smart.I_OBSERVATION_ATTRIBUTE TO manager", //$NON-NLS-1$
			"GRANT SELECT ON smart.i_recordsource to manager", //$NON-NLS-1$
			"GRANT SELECT ON smart.i_attribute to manager", //$NON-NLS-1$
			"GRANT SELECT ON smart.I_ATTRIBUTE_LIST_ITEM to maanger", //$NON-NLS-1$
			"grant select on smart.i_entity_type to manager", //$NON-NLS-1$
			"grant select on smart.I_ENTITY_TYPE_ATTRIBUTE to manager", //$NON-NLS-1$
			"grant select on smart.I_ENTITY_TYPE_ATTRIBUTE_GROUP to manager", //$NON-NLS-1$
		};
		
		for (String s : sql) {
			session.createNativeQuery(s).executeUpdate();
		}
		
		HibernateManager.setPlugInVersion(Intelligence2PlugIn.PLUGIN_ID, Intelligence2PlugIn.DB_VERSION_3, session);
	}

	private static void upgradeV3toV4(Session session) {
		String[] sql = new String[]{
				//primary date field
				"alter table smart.i_record ADD COLUMN smart_source varchar(2048)", //$NON-NLS-1$
		};
		for (String s : sql){
			session.createNativeQuery(s).executeUpdate();
		}
		
		HibernateManager.setPlugInVersion(Intelligence2PlugIn.PLUGIN_ID, Intelligence2PlugIn.DB_VERSION_4, session);

	}
	
	
	
	private static void upgradeV4toV5(Session session) {
		
		String profilekey = "profile1"; //$NON-NLS-1$
		String profilename = "Profile 1";  //$NON-NLS-1$
		
		String[] sql = new String[]{
				//primary date field
				"CREATE TABLE smart.i_profile_config(uuid char(16) for bit data not null, ca_uuid char(16) for bit data not null, keyid varchar(128) not null, color int, primary key (uuid))", //$NON-NLS-1$
				"CREATE TABLE smart.i_profile_entity_type( entity_type_uuid char(16) for bit data not null, profile_uuid char(16) for bit data not null, primary key (entity_type_uuid, profile_uuid))" ,  //$NON-NLS-1$
				"CREATE TABLE smart.i_profile_record_source(record_source_uuid char(16) for bit data not null, profile_uuid char(16) for bit data not null, primary key (record_source_uuid, profile_uuid))", //$NON-NLS-1$
				
				"ALTER TABLE smart.i_profile_entity_type ADD CONSTRAINT profileentitytype_entitytypeuuid_fk FOREIGN KEY (entity_type_uuid) REFERENCES smart.i_entity_type (uuid) ON UPDATE RESTRICT ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE smart.i_profile_entity_type ADD CONSTRAINT profileentitytype_profileuuid_fk FOREIGN KEY (profile_uuid) REFERENCES smart.i_profile_config (uuid) ON UPDATE RESTRICT ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE smart.i_profile_record_source ADD CONSTRAINT profilerecordtype_recordsourceuuid_fk FOREIGN KEY (record_source_uuid) REFERENCES smart.I_RECORDSOURCE (uuid) ON UPDATE RESTRICT ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE smart.i_profile_record_source ADD CONSTRAINT profilerecordtype_profileuuid_fk FOREIGN KEY (profile_uuid) REFERENCES smart.i_profile_config (uuid) ON UPDATE RESTRICT ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
		};
		for (String s : sql){
			session.createNativeQuery(s).executeUpdate();
		}
		
		//for each ca we need to create some sort of default profile
		List<?> items = session.createNativeQuery("SELECT uuid FROM smart.conservation_area").list();  //$NON-NLS-1$
		int color = (new Color(51,68,107)).getRGB();
				
		HashMap<UUID, UUID> caProfileUuids = new HashMap<>();
		
		for (Object cauuid : items) {
			UUID uuid = UuidUtils.byteToUUID( (byte[])cauuid );
			if (uuid.equals(ConservationArea.MULTIPLE_CA)) continue;
			
			UUID puuid = UuidUtils.byteToUUID( DerbyUtils.createUuid() );
			session.createNativeQuery("INSERT INTO smart.i_profile_config(uuid, ca_uuid, keyid, color) VALUES(:uuid,:cauuid,:keyid,:color)")  //$NON-NLS-1$
				.setParameter("uuid", puuid) //$NON-NLS-1$
				.setParameter("cauuid",uuid) //$NON-NLS-1$
				.setParameter("color", color) //$NON-NLS-1$
				.setParameter("keyid", profilekey) //$NON-NLS-1$
				.executeUpdate();
	
			caProfileUuids.put(uuid, puuid);
		}
	
		//add name to i18n table
		session.createNativeQuery("INSERT INTO smart.i18n_label (language_uuid, element_uuid, value) " //$NON-NLS-1$
				+ "SELECT a.uuid, b.uuid, '" + profilename + "' FROM smart.language a join smart.i_profile_config b " //$NON-NLS-1$ //$NON-NLS-2$
				+ " on a.ca_uuid = b.ca_uuid").executeUpdate(); //$NON-NLS-1$
			
			
		//link entity types to profiles
		String q = "INSERT INTO smart.i_profile_entity_type(entity_type_uuid, profile_uuid) SELECT "; //$NON-NLS-1$
		q += " a.uuid, b.uuid FROM "; //$NON-NLS-1$
		q += " smart.i_entity_type a, smart.i_profile_config b where a.ca_uuid = b.ca_uuid "; //$NON-NLS-1$
		session.createNativeQuery(q).executeUpdate();
		
		//line record source to profiles
		q = "INSERT INTO smart.i_profile_record_source(record_source_uuid, profile_uuid) SELECT "; //$NON-NLS-1$
		q += " a.uuid, b.uuid FROM "; //$NON-NLS-1$
		q += " smart.i_recordsource a, smart.i_profile_config b where a.ca_uuid = b.ca_uuid "; //$NON-NLS-1$
		session.createNativeQuery(q).executeUpdate();
		
		sql = new String[] {
				"ALTER TABLE smart.i_entity ADD COLUMN profile_uuid char(16) for bit data", //$NON-NLS-1$
				"ALTER TABLE smart.i_entity ADD COLUMN dm_list_item_uuid char(16) for bit data",  //$NON-NLS-1$
				"ALTER TABLE smart.i_entity ADD CONSTRAINT i_entity_dmlistitem_fk FOREIGN KEY (dm_list_item_uuid) REFERENCES smart.dm_attribute_list(uuid) ON UPDATE RESTRICT ON DELETE NO ACTION DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$				
				"ALTER TABLE smart.i_record ADD COLUMN profile_uuid char(16) for bit data", //$NON-NLS-1$
				"ALTER TABLE smart.i_relationship_type ADD COLUMN src_profile_uuid char(16) for bit data", //$NON-NLS-1$
				"ALTER TABLE smart.i_relationship_type ADD COLUMN target_profile_uuid char(16) for bit data", //$NON-NLS-1$
				"ALTER TABLE smart.i_entity_type ADD COLUMN dm_attribute_uuid char(16) for bit data",  //$NON-NLS-1$
				"ALTER TABLE smart.i_entity_type ADD CONSTRAINT i_et_dmatt_fk FOREIGN KEY (dm_attribute_uuid) REFERENCES smart.dm_attribute(uuid) ON UPDATE RESTRICT ON DELETE NO ACTION DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE smart.i_entity_type ADD COLUMN dm_active_filter varchar(32000)", //$NON-NLS-1$
				
				"update smart.i_entity set profile_uuid = (select b.uuid from smart.I_PROFILE_CONFIG b where b.ca_uuid = smart.i_entity.ca_uuid)", //$NON-NLS-1$
				"update smart.i_record set profile_uuid = (select b.uuid from smart.I_PROFILE_CONFIG b where b.ca_uuid = smart.i_record.ca_uuid)", //$NON-NLS-1$
				
				"update smart.i_relationship_type set src_profile_uuid = (select b.uuid from smart.I_PROFILE_CONFIG b where b.ca_uuid = smart.i_relationship_type.ca_uuid)", //$NON-NLS-1$
				"update smart.i_relationship_type set target_profile_uuid = src_profile_uuid", //$NON-NLS-1$
				
				"ALTER TABLE smart.i_entity ALTER COLUMN profile_uuid set not null", //$NON-NLS-1$
				"ALTER TABLE smart.i_entity ADD CONSTRAINT i_entity_profileuuid_fk FOREIGN KEY (profile_uuid) REFERENCES smart.i_profile_config (uuid) ON UPDATE RESTRICT ON DELETE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE smart.i_record ALTER COLUMN profile_uuid set not null", //$NON-NLS-1$
				"ALTER TABLE smart.i_record ADD CONSTRAINT i_record_profileuuid_fk FOREIGN KEY (profile_uuid) REFERENCES smart.i_profile_config (uuid) ON UPDATE RESTRICT ON DELETE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE smart.i_relationship_type ALTER COLUMN src_profile_uuid SET not null", //$NON-NLS-1$
				"ALTER TABLE smart.i_relationship_type ALTER COLUMN target_profile_uuid SET not null", //$NON-NLS-1$
				"ALTER TABLE smart.i_relationship_type ADD CONSTRAINT i_rtype_srcprofileuuid_fk FOREIGN KEY (src_profile_uuid) REFERENCES smart.i_profile_config (uuid) ON UPDATE RESTRICT ON DELETE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE smart.i_relationship_type ADD CONSTRAINT i_rtype_trgprofileuuid_fk FOREIGN KEY (target_profile_uuid) REFERENCES smart.i_profile_config (uuid) ON UPDATE RESTRICT ON DELETE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				
				
				"ALTER TABLE smart.i_entity_record_query add column profile_filter varchar(32672)", //$NON-NLS-1$
				//"ALTER TABLE smart.i_entity_record_query drop column query_filter",
				"ALTER TABLE smart.i_entity_summary_query add column profile_filter varchar(32672)", //$NON-NLS-1$
				"ALTER TABLE smart.i_record_obs_query add column profile_filter varchar(32672)", //$NON-NLS-1$
				
				"UPDATE smart.I_ENTITY_RECORD_QUERY set profile_filter = '" + profilekey + "'",  //$NON-NLS-1$ //$NON-NLS-2$
				"UPDATE smart.i_entity_summary_query set profile_filter = '" + profilekey + "'",  //$NON-NLS-1$ //$NON-NLS-2$
				"UPDATE smart.i_record_obs_query set profile_filter = '" + profilekey + "'", //$NON-NLS-1$ //$NON-NLS-2$
				
				//entity summary queries
				"CREATE TABLE smart.i_record_summary_query(uuid char(16) for bit data NOT NULL,ca_uuid char(16) for bit data NOT NULL,query_string long varchar,date_created timestamp NOT NULL,last_modified_date timestamp,created_by char(16) for bit data NOT NULL,last_modified_by char(16) for bit data, profile_filter varchar(32672), PRIMARY KEY (uuid))", //$NON-NLS-1$
				"ALTER TABLE smart.i_record_summary_query ADD CONSTRAINT irecordsummquery_cauuid_fk FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area (uuid) DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE smart.i_record_summary_query ADD CONSTRAINT irecordsummquery_createdby_fk FOREIGN KEY (created_by) REFERENCES smart.employee (uuid) DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE smart.i_record_summary_query ADD CONSTRAINT irecordsummquery_modifiedby_fk FOREIGN KEY (last_modified_by) REFERENCES smart.employee (uuid) DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$

				//record query			
				"CREATE TABLE smart.i_record_query(uuid char(16) for bit data NOT NULL,ca_uuid char(16) for bit data NOT NULL,query_string long varchar,date_created timestamp NOT NULL,last_modified_date timestamp,created_by char(16) for bit data NOT NULL,last_modified_by char(16) for bit data, profile_filter varchar(32672), PRIMARY KEY (uuid))", //$NON-NLS-1$
				"ALTER TABLE smart.i_record_query ADD CONSTRAINT irecordquery2_cauuid_fk FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area (uuid) DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE smart.i_record_query ADD CONSTRAINT irecordquery2_createdby_fk FOREIGN KEY (created_by) REFERENCES smart.employee (uuid) DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE smart.i_record_query ADD CONSTRAINT irecordquery2_modifiedby_fk FOREIGN KEY (last_modified_by) REFERENCES smart.employee (uuid) DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$

				"CREATE TABLE smart.i_permission(employee_uuid char(16) for bit data not null, profile_uuid char(16) for bit data not null, permissions integer not null, primary key (employee_uuid, profile_uuid))", //$NON-NLS-1$
				"ALTER TABLE smart.i_permission ADD CONSTRAINT ipermission_empuuid_fk FOREIGN KEY (employee_uuid) REFERENCES smart.employee(uuid) ON UPDATE RESTRICT ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE smart.i_permission ADD CONSTRAINT ipermission_profileuuid_fk FOREIGN KEY (profile_uuid) REFERENCES smart.i_profile_config(uuid) ON UPDATE RESTRICT ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				
				"ALTER TABLE smart.i_profile_config ADD CONSTRAINT profile_config_ca_uuid_fk FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area (uuid) ON UPDATE RESTRICT ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				
				"ALTER TABLE smart.i_recordsource_attribute ADD COLUMN keyid varchar(128)", //$NON-NLS-1$
				
				"GRANT ALL PRIVILEGES ON smart.i_permission to data_entry", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.i_permission to manager", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.i_permission to analyst", //$NON-NLS-1$
		};
		for (String s : sql) session.createNativeQuery(s).executeUpdate();
		
		
		HashMap<UUID, Set<String>> usedkeys = new HashMap<>();
		
		//attribute sources
		List<?> results = session.createNativeQuery("select a.uuid, b.keyid, a.source_uuid from smart.I_RECORDSOURCE_ATTRIBUTE a join smart.i_attribute b on a.attribute_uuid = b.uuid").list(); //$NON-NLS-1$
		for (Object x : results) {
			Object[] data = (Object[])x;
			
			UUID uuid = UuidUtils.byteToUUID((byte[])data[0]);
			UUID srcuuid = UuidUtils.byteToUUID((byte[])data[2]);
			String keyid = (String) data[1];
		
			if (!usedkeys.containsKey(srcuuid)) usedkeys.put(srcuuid, new HashSet<>());
			
			Set<String> used = usedkeys.get(srcuuid);
			String root = keyid;
			int i = 1;
			while(used.contains(keyid)) {
				keyid = root + i;
				i++;
			}
			used.add(keyid);
			
			session.createNativeQuery("UPDATE smart.i_recordsource_attribute SET keyid = '" + keyid + "' WHERE uuid = :uuid") //$NON-NLS-1$ //$NON-NLS-2$
				.setParameter("uuid", uuid) //$NON-NLS-1$
				.executeUpdate();
		}
		//repeat for entity sources
		results = session.createNativeQuery("select a.uuid, b.keyid, a.source_uuid from smart.I_RECORDSOURCE_ATTRIBUTE a join smart.i_entity_type b on a.entity_type_uuid = b.uuid").list(); //$NON-NLS-1$
		for (Object x : results) {
			Object[] data = (Object[])x;
			
			UUID uuid = UuidUtils.byteToUUID((byte[])data[0]);
			UUID srcuuid = UuidUtils.byteToUUID((byte[])data[2]);
			String keyid = (String) data[1];
		
			if (!usedkeys.containsKey(srcuuid)) usedkeys.put(srcuuid, new HashSet<>());
			
			Set<String> used = usedkeys.get(srcuuid);
			String root = keyid;
			int i = 1;
			while(used.contains(keyid)) {
				keyid = root + i;
				i++;
			}
			used.add(keyid);
			
			session.createNativeQuery("UPDATE smart.i_recordsource_attribute SET keyid = '" + keyid + "' WHERE uuid = :uuid") //$NON-NLS-1$ //$NON-NLS-2$
				.setParameter("uuid", uuid) //$NON-NLS-1$
				.executeUpdate();
		}		

		session.createNativeQuery("alter table smart.i_recordsource_attribute alter column keyid not null").executeUpdate(); //$NON-NLS-1$

		//need to map old permissions to new ones
		//remove old permission from employee
		results = session.createNativeQuery("select uuid, ca_uuid, smartuserlevel FROM smart.EMPLOYEE where smartuserlevel is not null").list(); //$NON-NLS-1$
		for(Object x : results) {
			Object[] data = (Object[])x;
			
			UUID cauuid = UuidUtils.byteToUUID((byte[])data[1]);
			if (cauuid.equals(ConservationArea.MULTIPLE_CA)) continue;
			
			UUID uuid = UuidUtils.byteToUUID((byte[])data[0]);
			String userlevel = (String)data[2];
			String[] parts = userlevel.split(","); //$NON-NLS-1$
			
			List<String> newparts = new ArrayList<>();
			Set<Integer> intelpermissions = new HashSet<>();
			
			for (String bit : parts) {
				if (bit.equalsIgnoreCase("INTEL_ANALYST")) { //$NON-NLS-1$
					newparts.add("INTEL_ADMIN"); //$NON-NLS-1$
					intelpermissions.add(IntelPermission.ADMIN);
					
				}else if (bit.toUpperCase(Locale.ROOT).startsWith("INTEL_")) { //$NON-NLS-1$
					if(!newparts.contains("INTEL_USER")) newparts.add("INTEL_USER"); //$NON-NLS-1$ //$NON-NLS-2$
					
					if (bit.equalsIgnoreCase("INTEL_ENTITY_CREATE")) intelpermissions.add(IntelPermission.ENTITY_CREATE); //$NON-NLS-1$
					if (bit.equalsIgnoreCase("INTEL_ENTITY_DELETE")) intelpermissions.add(IntelPermission.ENTITY_DELETE); //$NON-NLS-1$
					if (bit.equalsIgnoreCase("INTEL_ENTITY_EDIT")) intelpermissions.add(IntelPermission.ENTITY_EDIT); //$NON-NLS-1$
					if (bit.equalsIgnoreCase("INTEL_ENTITY_VIEW")) intelpermissions.add(IntelPermission.ENTITY_VIEW); //$NON-NLS-1$
					
					if (bit.equalsIgnoreCase("INTEL_RECORD_CREATE")) intelpermissions.add(IntelPermission.RECORD_CREATE); //$NON-NLS-1$
					if (bit.equalsIgnoreCase("INTEL_RECORD_DELETE")) intelpermissions.add(IntelPermission.RECORD_DELETE); //$NON-NLS-1$
					if (bit.equalsIgnoreCase("INTEL_RECORD_EDIT")) intelpermissions.add(IntelPermission.RECORD_EDIT_NOTSTATUS); //$NON-NLS-1$
					if (bit.equalsIgnoreCase("INTEL_RECORD_EDIT_WITH_STATUS")) intelpermissions.add(IntelPermission.RECORD_EDIT_ALL); //$NON-NLS-1$
					if (bit.equalsIgnoreCase("INTEL_RECORD_VIEW")) intelpermissions.add(IntelPermission.RECORD_VIEW); //$NON-NLS-1$
					
					if (bit.equalsIgnoreCase("INTEL_QUERY_ALL")) intelpermissions.add(IntelPermission.QUERY); //$NON-NLS-1$
					if (bit.equalsIgnoreCase("INTEL_READ_ONLY")) intelpermissions.add(IntelPermission.READ_ONLY); //$NON-NLS-1$
					
				}else {
					newparts.add(bit);
				}
			}
			if (intelpermissions.isEmpty()) continue;
			
			if (newparts.contains("INTEL_ADMIN") && newparts.contains("INTEL_USER")) newparts.remove("INTEL_USER"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			
			if (intelpermissions.contains(IntelPermission.ADMIN)) {
				//remove all others
				intelpermissions.clear();
				intelpermissions.add(IntelPermission.ADMIN);
			}
			
			int permission = 0;
			for (Integer i : intelpermissions) permission = permission | i;

			//insert permission
			session.createNativeQuery("INSERT INTO smart.i_permission (employee_uuid, profile_uuid, permissions) VALUES(:euuid, :puuid, :p)") //$NON-NLS-1$
				.setParameter("euuid", uuid) //$NON-NLS-1$
				.setParameter("puuid", caProfileUuids.get(cauuid)) //$NON-NLS-1$
				.setParameter("p", permission) //$NON-NLS-1$
				.executeUpdate();
			
			//update employee
			StringBuilder sb = new StringBuilder();
			sb.append(newparts.get(0));
			for (int i = 1; i < newparts.size(); i ++) {
				sb.append(","); //$NON-NLS-1$
				sb.append(newparts.get(i));
			}
			session.createNativeQuery("UPDATE smart.employee set smartuserlevel = :ul where uuid = :uuid") //$NON-NLS-1$
				.setParameter("ul", sb.toString()) //$NON-NLS-1$
				.setParameter("uuid", uuid) //$NON-NLS-1$
				.executeUpdate();
			
		}
		
		HibernateManager.setPlugInVersion(Intelligence2PlugIn.PLUGIN_ID, Intelligence2PlugIn.DB_VERSION_5, session);

	}
}
