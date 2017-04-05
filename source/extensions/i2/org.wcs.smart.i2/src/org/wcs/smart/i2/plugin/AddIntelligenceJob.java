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

import java.sql.Connection;
import java.sql.SQLException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.application.DisplayAccess;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.internal.Messages;

/**
 * Adds and or upgrades intelligence plugin
 * 
 * @author Emily
 *
 */
public class AddIntelligenceJob extends Job {

	public AddIntelligenceJob() {
		super(Messages.AddIntelligenceJob_JobName);
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		//required if run during restore to ensure Display.syncexec calls don't block
		DisplayAccess.accessDisplayDuringStartup();
				
		Session session = HibernateManager.openSession();
		try{
			session.beginTransaction();
			installPlugin(session);
			session.getTransaction().commit();
		} catch (final Exception ex) {
			if (session.getTransaction().isActive()) session.getTransaction().rollback();
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					Intelligence2PlugIn.displayLog(Messages.AddIntelligenceJob_InstallError, ex);
				}
			});
			return new Status(IStatus.ERROR, Intelligence2PlugIn.PLUGIN_ID, 1, Messages.AddIntelligenceJob_InstallError, ex); 
		} finally {
			session.close();
		}
		monitor.done();
		return Status.OK_STATUS;
	}

	public void installPlugin(Session session){
		String currentVersion = HibernateManager.getPlugInVersion(Intelligence2PlugIn.PLUGIN_ID, session);
		if (currentVersion == null){
			createTables(session);
			currentVersion = Intelligence2PlugIn.DB_VERSION_1;
		}
		
		IntelligenceDatabaseUpgrader.upgrade(Intelligence2PlugIn.DB_VERSION_1, session);
	}
	
	@SuppressWarnings("nls")
	private void createTables(Session session){
		String[] sql = new String[]{
				 // Create Tables
				 "CREATE TABLE smart.i_attachment(uuid char(16) for bit data NOT NULL,ca_uuid char(16) for bit data NOT NULL,date_created timestamp NOT NULL,created_by char(16) for bit data NOT NULL,description varchar(2048),filename varchar(1024) NOT NULL,PRIMARY KEY (uuid))",
				 "CREATE TABLE smart.i_attribute(uuid char(16) for bit data NOT NULL,keyid varchar(128) NOT NULL,type char(8) NOT NULL,ca_uuid char(16) for bit data NOT NULL,PRIMARY KEY (uuid))",
				 "CREATE TABLE smart.i_attribute_list_item(uuid char(16) for bit data NOT NULL,attribute_uuid char(16) for bit data NOT NULL,keyid varchar(128) NOT NULL,PRIMARY KEY (uuid))",
				 "CREATE TABLE smart.i_entity(uuid char(16) for bit data NOT NULL,ca_uuid char(16) for bit data NOT NULL,date_created timestamp NOT NULL,date_modified timestamp,created_by char(16) for bit data NOT NULL,last_modified_by char(16) for bit data,primary_attachment_uuid char(16) for bit data,entity_type_uuid char(16) for bit data NOT NULL,comment long varchar,PRIMARY KEY (uuid))",
				 "CREATE TABLE smart.i_entity_attachment(entity_uuid char(16) for bit data NOT NULL,attachment_uuid char(16) for bit data NOT NULL,PRIMARY KEY (entity_uuid, attachment_uuid))",
				 "CREATE TABLE smart.i_entity_attribute_value(entity_uuid char(16) for bit data NOT NULL,attribute_uuid char(16) for bit data NOT NULL,string_value varchar(1024),double_value double,double_value2 double,list_item_uuid char(16) for bit data,metaphone varchar(32600), PRIMARY KEY (entity_uuid, attribute_uuid))",
				 "CREATE TABLE smart.i_entity_location(entity_uuid char(16) for bit data NOT NULL,location_uuid char(16) for bit data NOT NULL,PRIMARY KEY (entity_uuid, location_uuid))",
				 "CREATE TABLE smart.i_entity_record(entity_uuid char(16) for bit data NOT NULL,record_uuid char(16) for bit data NOT NULL,PRIMARY KEY (entity_uuid, record_uuid))",
				 "CREATE TABLE smart.i_entity_relationship(uuid char(16) for bit data NOT NULL,src_entity_uuid char(16) for bit data NOT NULL,relationship_type_uuid char(16) for bit data NOT NULL,target_entity_uuid char(16) for bit data NOT NULL,source varchar(16) not null,source_uuid char(16) for bit data,PRIMARY KEY (uuid))",
				 "CREATE TABLE smart.i_entity_relationship_attribute_value(entity_relationship_uuid char(16) for bit data NOT NULL,attribute_uuid char(16)for bit data NOT NULL,string_value varchar(1024),double_value double,double_value2 double,list_item_uuid char(16) for bit data,PRIMARY KEY (entity_relationship_uuid, attribute_uuid))",
				 "CREATE TABLE smart.i_entity_search(uuid char(16) for bit data NOT NULL,search_string long varchar,ca_uuid char(16) for bit data NOT NULL,PRIMARY KEY (uuid))",
				 "CREATE TABLE smart.i_entity_type(uuid char(16) for bit data NOT NULL,keyid varchar(128) NOT NULL,ca_uuid char(16) for bit data NOT NULL,id_attribute_uuid char(16) for bit data NOT NULL,icon blob,birt_template varchar(4096),PRIMARY KEY (uuid))",
				 "CREATE TABLE smart.i_entity_type_attribute(entity_type_uuid char(16)for bit data  NOT NULL,attribute_uuid char(16) for bit data NOT NULL,attribute_group_uuid char(16) for bit data,seq_order integer not null,PRIMARY KEY (entity_type_uuid, attribute_uuid))",
				 "CREATE TABLE smart.i_entity_type_attribute_group(uuid char(16) for bit data NOT NULL,entity_type_uuid char(16) for bit data not null,seq_order integer not null,PRIMARY KEY (uuid))",
				 "CREATE TABLE smart.i_location(uuid char(16) for bit data NOT NULL,ca_uuid char(16) for bit data NOT NULL,geometry blob NOT NULL,datetime timestamp,comment varchar(4096),id varchar(1028),record_uuid char(16) for bit data,PRIMARY KEY (uuid))",
				 "CREATE TABLE smart.i_observation(uuid char(16) for bit data NOT NULL,location_uuid char(16) for bit data NOT NULL,category_uuid char(16)for bit data ,PRIMARY KEY (uuid))",
				 "CREATE TABLE smart.i_observation_attribute(observation_uuid char(16) for bit data NOT NULL,attribute_uuid char(16) for bit data NOT NULL,list_element_uuid char(16) for bit data,tree_node_uuid char(16) for bit data,string_value varchar(1024),double_value double,PRIMARY KEY (observation_uuid, attribute_uuid))",
				 "CREATE TABLE smart.i_record(uuid char(16) for bit data NOT NULL,ca_uuid char(16) for bit data NOT NULL,source_uuid char(16) for bit data, title varchar(1024) NOT NULL,date_created timestamp NOT NULL,last_modified_date timestamp,created_by char(16) for bit data NOT NULL,last_modified_by char(16) for bit data,date_exported timestamp,status varchar(16) NOT NULL,description long varchar,comment long varchar, PRIMARY KEY (uuid))",
				 "CREATE TABLE smart.i_record_attachment(record_uuid char(16) for bit data NOT NULL,attachment_uuid char(16) for bit data NOT NULL,PRIMARY KEY (record_uuid, attachment_uuid))",
				 "CREATE TABLE smart.i_record_obs_query(uuid char(16) for bit data NOT NULL,ca_uuid char(16) for bit data NOT NULL,style long varchar,query_string long varchar,column_filter long varchar,date_created timestamp NOT NULL,last_modified_date timestamp,created_by char(16) for bit data NOT NULL,last_modified_by char(16) for bit data,PRIMARY KEY (uuid))",
				 "CREATE TABLE smart.i_relationship_type_attribute(relationship_type_uuid char(16) for bit data NOT NULL,attribute_uuid char(16) for bit data NOT NULL,seq_order integer not null,PRIMARY KEY (relationship_type_uuid, attribute_uuid))",
				 "CREATE TABLE smart.i_relationship_group(uuid char(16) for bit data NOT NULL,ca_uuid char(16) for bit data NOT NULL,keyid varchar(128) NOT NULL,PRIMARY KEY (uuid))",
				 "CREATE TABLE smart.i_relationship_type(uuid char(16) for bit data NOT NULL,keyid varchar(128) NOT NULL,ca_uuid char(16) for bit data NOT NULL,icon blob,relationship_group_uuid char(16) for bit data,src_entity_type char(16) for bit data,target_entity_type char(16) for bit data,PRIMARY KEY (uuid))",
				 "CREATE TABLE smart.i_working_set(uuid char(16) for bit data NOT NULL,ca_uuid char(16) for bit data NOT NULL,date_created timestamp NOT NULL,last_modified_date timestamp,created_by char(16) for bit data NOT NULL,last_modified_by char(16) for bit data,entity_date_filter varchar(1024),PRIMARY KEY (uuid))",
				 "CREATE TABLE smart.i_working_set_entity(working_set_uuid char(16) for bit data NOT NULL,entity_uuid char(16) for bit data NOT NULL,map_style long varchar,is_visible boolean not null default true,PRIMARY KEY (working_set_uuid, entity_uuid))",
				 "CREATE TABLE smart.i_working_set_query(working_set_uuid char(16) for bit data NOT NULL,query_uuid char(16) for bit data NOT NULL,date_filter varchar(1024),map_style long varchar,is_visible boolean not null default true,PRIMARY KEY (working_set_uuid, query_uuid))",
				 "CREATE TABLE smart.i_working_set_record(working_set_uuid char(16) for bit data NOT NULL,record_uuid char(16) for bit data NOT NULL,map_style long varchar,is_visible boolean not null default true,PRIMARY KEY (working_set_uuid, record_uuid))",
				 
				"CREATE TABLE smart.i_record_attribute_value(uuid char(16) for bit data NOT NULL, record_uuid char(16) for bit data NOT NULL,attribute_uuid char(16) for bit data NOT NULL,string_value varchar(1024),double_value double, double_value2 double,PRIMARY KEY (uuid), UNIQUE(record_uuid, attribute_uuid))",
				"CREATE TABLE smart.i_record_attribute_value_list(value_uuid char(16) for bit data not null, element_uuid char(16) for bit data not null, primary key (value_uuid, element_uuid))",
				"CREATE TABLE smart.i_recordsource_attribute(uuid char(16) for bit data, source_uuid char(16) for bit data NOT NULL,attribute_uuid char(16) for bit data, entity_type_uuid char(16) for bit data, seq_order integer,  is_multi boolean, PRIMARY KEY(uuid), UNIQUE (source_uuid, attribute_uuid, entity_type_uuid))",
				"CREATE TABLE smart.i_recordsource (uuid char(16) for bit data not null, ca_uuid char(16) for bit data not null, keyid varchar(128) not null, icon blob, PRIMARY KEY (uuid))",
					
				 // Create Foreign Keys
				"ALTER TABLE smart.i_location ADD CONSTRAINT ilocation_cauuid_fk FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.i_location ADD CONSTRAINT location_recorduuid_fk FOREIGN KEY (record_uuid) REFERENCES smart.i_record (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.i_entity_search ADD CONSTRAINT ientitysearch_cauuid_fk FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.i_attribute ADD CONSTRAINT iattribute_cauuid_fk FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.i_record ADD CONSTRAINT irecord_cauuid_fk FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.i_record ADD CONSTRAINT irecord_createdby_fk FOREIGN KEY (created_by) REFERENCES smart.employee (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.i_record ADD CONSTRAINT irecord_modifiedby_fk FOREIGN KEY (lasT_modified_by) REFERENCES smart.employee (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.i_entity_type ADD CONSTRAINT ientitytype_cauuid_fk FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.i_entity_type ADD CONSTRAINT ientitytype_idattributeuuid_fk FOREIGN KEY (id_attribute_uuid) REFERENCES smart.i_attribute (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.i_attachment ADD CONSTRAINT iattachment_cauuid_fk FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.i_attachment ADD CONSTRAINT iattachment_createdby_fk FOREIGN KEY (created_by) REFERENCES smart.employee (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.i_relationship_type ADD CONSTRAINT irelationshiptype_cauuid_fk FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.i_working_set ADD CONSTRAINT iworkingset_cauuid_fk FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.i_working_set ADD CONSTRAINT iworkingset_createdby_fk FOREIGN KEY (created_by) REFERENCES smart.employee (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.i_working_set ADD CONSTRAINT iworkingset_lastmodifiedby_fk FOREIGN KEY (last_modified_by) REFERENCES smart.employee (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.i_entity ADD CONSTRAINT ientity_cauuid_fk FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.i_record_obs_query ADD CONSTRAINT irecordquery_cauuid_fk FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.i_record_obs_query ADD CONSTRAINT irecordquery_createdby_fk FOREIGN KEY (created_by) REFERENCES smart.employee (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.i_record_obs_query ADD CONSTRAINT irecordquery_modifiedby_fk FOREIGN KEY (last_modified_by) REFERENCES smart.employee (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.i_observation_attribute ADD CONSTRAINT iobservationattribute_attributeuuid_fk FOREIGN KEY (attribute_uuid) REFERENCES smart.DM_ATTRIBUTE (UUID) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.i_observation_attribute ADD CONSTRAINT iobservationattribute_list_fk FOREIGN KEY (list_element_uuid) REFERENCES smart.DM_ATTRIBUTE_LIST (UUID) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.i_observation_attribute ADD CONSTRAINT iobservationattribute_tree_fk FOREIGN KEY (tree_node_uuid) REFERENCES smart.DM_ATTRIBUTE_TREE (UUID) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.i_record_attachment ADD CONSTRAINT irecordattachment_attchment_fk FOREIGN KEY (attachment_uuid) REFERENCES smart.i_attachment (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.i_entity_attachment ADD CONSTRAINT ientityattachment_attchment_fk FOREIGN KEY (attachment_uuid) REFERENCES smart.i_attachment (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.i_entity_attribute_value ADD CONSTRAINT ientityattribute_attribute_fk FOREIGN KEY (attribute_uuid) REFERENCES smart.i_attribute (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.i_attribute_list_item ADD CONSTRAINT iattributelist_attribute_fk FOREIGN KEY (attribute_uuid) REFERENCES smart.i_attribute (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.i_relationship_type_attribute ADD CONSTRAINT irelationshipattribute_attribute_fk FOREIGN KEY (attribute_uuid) REFERENCES smart.i_attribute (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.i_entity_type_attribute ADD CONSTRAINT ientitytypeattribute_attribute_fk FOREIGN KEY (attribute_uuid) REFERENCES smart.i_attribute (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.i_entity_type_attribute ADD CONSTRAINT iattributegroupuuid_fk FOREIGN KEY (attribute_group_uuid) REFERENCES smart.i_entity_type_attribute_group (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.i_entity_type_attribute_group ADD CONSTRAINT ientitytypeattributegroupentitytypeuuid_fk FOREIGN KEY (entity_type_uuid) REFERENCES smart.i_entity_type (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.i_entity_relationship_attribute_value ADD CONSTRAINT ientityrelationshipattribute_attribute_fk FOREIGN KEY (attribute_uuid) REFERENCES smart.i_attribute (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.i_entity_relationship_attribute_value ADD CONSTRAINT ientityrelationshipattribute_list_fk FOREIGN KEY (list_item_uuid) REFERENCES smart.i_attribute_list_item (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.i_entity_attribute_value ADD CONSTRAINT ientityattributevalue_list_fk FOREIGN KEY (list_item_uuid) REFERENCES smart.i_attribute_list_item (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.i_entity_relationship ADD CONSTRAINT ientityrelationship_srcentity_fk FOREIGN KEY (src_entity_uuid) REFERENCES smart.i_entity (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.i_entity_relationship ADD CONSTRAINT ientityrelationship_targetentity_fk FOREIGN KEY (target_entity_uuid) REFERENCES smart.i_entity (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.i_entity_record ADD CONSTRAINT ientityrecord_entity_fk FOREIGN KEY (entity_uuid) REFERENCES smart.i_entity (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.i_working_set_entity ADD CONSTRAINT iworkingsetentity_entity_fk FOREIGN KEY (entity_uuid) REFERENCES smart.i_entity (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.i_entity_attribute_value ADD CONSTRAINT ientityattributevalue_entity_fk FOREIGN KEY (entity_uuid) REFERENCES smart.i_entity (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.i_entity_attachment ADD CONSTRAINT ientityattachment_entity_fk FOREIGN KEY (entity_uuid) REFERENCES smart.i_entity (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.i_entity_location ADD CONSTRAINT ientitylocation_entity_fk FOREIGN KEY (entity_uuid) REFERENCES smart.i_entity (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.i_entity_relationship_attribute_value ADD CONSTRAINT ientityrelationshipattribute_entityrelationship_fk FOREIGN KEY (entity_relationship_uuid) REFERENCES smart.i_entity_relationship (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.i_entity_type_attribute ADD CONSTRAINT ientitytypeattribute_entitytype_fk FOREIGN KEY (entity_type_uuid) REFERENCES smart.i_entity_type (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.i_entity ADD CONSTRAINT ientity_entitytype_fk FOREIGN KEY (entity_type_uuid) REFERENCES smart.i_entity_type (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.i_entity ADD CONSTRAINT ientity_createdby_fk FOREIGN KEY (created_by) REFERENCES smart.employee (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.i_entity ADD CONSTRAINT ientity_lastmodifiedby_fk FOREIGN KEY (last_modified_by) REFERENCES smart.employee (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.i_entity_location ADD CONSTRAINT ientitylocation_location_fk FOREIGN KEY (location_uuid) REFERENCES smart.i_location (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.i_observation ADD CONSTRAINT iobservation_location_fk FOREIGN KEY (location_uuid) REFERENCES smart.i_location (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.i_observation ADD CONSTRAINT iobservation_category_fk FOREIGN KEY (category_uuid) REFERENCES smart.dm_category (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.i_observation_attribute ADD CONSTRAINT iobservationattribute_observation_fk FOREIGN KEY (observation_uuid) REFERENCES smart.i_observation (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.i_entity_record ADD CONSTRAINT ientityrecord_record_fk FOREIGN KEY (record_uuid) REFERENCES smart.i_record (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.i_working_set_record ADD CONSTRAINT iworkingsetrecord_record_fk FOREIGN KEY (record_uuid) REFERENCES smart.i_record (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.i_record_attachment ADD CONSTRAINT irecordattachment_record_fk FOREIGN KEY (record_uuid) REFERENCES smart.i_record (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.i_working_set_query ADD CONSTRAINT iworkingsetquery_query_fk FOREIGN KEY (query_uuid) REFERENCES smart.i_record_obs_query (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.i_relationship_type ADD CONSTRAINT irelationshiptype_group_fk FOREIGN KEY (relationship_group_uuid) REFERENCES smart.i_relationship_group (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.i_relationship_type_attribute ADD CONSTRAINT irelationshipattribute_type_fk FOREIGN KEY (relationship_type_uuid) REFERENCES smart.i_relationship_type (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.I_RELATIONSHIP_TYPE add constraint I_RELATIONSHIP_TYPE_SRC_TYPE_FK  FOREIGN KEY (src_entity_type) REFERENCES smart.I_ENTITY_TYPE(uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.I_RELATIONSHIP_TYPE add constraint I_RELATIONSHIP_TYPE_TRG_TYPE_FK  FOREIGN KEY (target_entity_type) REFERENCES smart.I_ENTITY_TYPE(uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.i_entity_relationship ADD CONSTRAINT ientityrelationship_type_fk FOREIGN KEY (relationship_type_uuid) REFERENCES smart.i_relationship_type (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.i_working_set_query ADD CONSTRAINT iworkingsetquery_workingset_fk FOREIGN KEY (working_set_uuid) REFERENCES smart.i_working_set (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.i_working_set_record ADD CONSTRAINT iworkingsetrecord_workingset_fk FOREIGN KEY (working_set_uuid) REFERENCES smart.i_working_set (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.i_working_set_entity ADD CONSTRAINT iworkginsetentity_workingset_fk FOREIGN KEY (working_set_uuid) REFERENCES smart.i_working_set (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.i_relationship_group ADD CONSTRAINT relationshipgroup_cauuid_fk FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area (uuid) DEFERRABLE INITIALLY IMMEDIATE",

				"ALTER TABLE smart.i_recordsource ADD CONSTRAINT irecordsource_cauuid_fk FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.i_recordsource_attribute ADD CONSTRAINT irecordsourceattribute_sourceuuid_fk FOREIGN KEY (source_uuid) REFERENCES smart.i_recordsource (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.i_recordsource_attribute ADD CONSTRAINT irecordsourceattribute_attributeuuid_fk FOREIGN KEY (attribute_uuid) REFERENCES smart.i_attribute (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.i_recordsource_attribute ADD CONSTRAINT irecordsourceattribute_entitytypeuuid_fk FOREIGN KEY (entity_type_uuid) REFERENCES smart.i_entity_type (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.i_record_attribute_value ADD CONSTRAINT irecordattvalue_sourceuuid_fk FOREIGN KEY (record_uuid) REFERENCES smart.i_record (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.i_record_attribute_value ADD CONSTRAINT irecordattvalue_attributeuuid_fk FOREIGN KEY (attribute_uuid) REFERENCES smart.i_recordsource_attribute (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.i_record ADD CONSTRAINT irecord_sourceuuid_fk FOREIGN KEY (source_uuid) REFERENCES smart.i_recordsource (uuid) DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE smart.i_record_attribute_value_list ADD CONSTRAINT i_recordattributelist_valueuuid_fk FOREIGN KEY (value_uuid) REFERENCES smart.i_record_attribute_value (uuid) DEFERRABLE INITIALLY IMMEDIATE",
								 
				 // FUNCTIONS AND TRIGGERS FOR METAPHONE FUZZY SEARCH
				 "create function smart.metaphoneContains (metaphone varchar(4), searchstring varchar(32600))  returns boolean LANGUAGE JAVA PARAMETER STYLE JAVA DETERMINISTIC NO SQL RETURNS NULL ON NULL INPUT EXTERNAL NAME 'org.wcs.smart.i2.search.DerbyFuzzyFunctions.metaphoneContains'",
				 "GRANT EXECUTE ON FUNCTION smart.metaphoneContains TO admin,data_entry,manager,analyst",
		};
		
		session.doWork(new Work(){
			@Override
			public void execute(Connection connection) throws SQLException {
				for (String s : sql){
					Intelligence2PlugIn.log(s, null);
					connection.createStatement().executeUpdate(s);
				}
			}
			
		});
		HibernateManager.setPlugInVersion(Intelligence2PlugIn.PLUGIN_ID, Intelligence2PlugIn.DB_VERSION_1, session);
	}
	
}
