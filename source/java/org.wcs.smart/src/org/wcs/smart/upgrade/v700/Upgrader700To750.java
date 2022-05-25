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
package org.wcs.smart.upgrade.v700;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.upgrade.AbstractInteralDatabaseUpgrader;
import org.wcs.smart.upgrade.UpgradeEngine;

/**
 * 7.0.0 to 7.5.0 upgrader
 * 
 * @author Emily
 *
 */
public class Upgrader700To750 extends AbstractInteralDatabaseUpgrader { 
	
	private Exception thrownException = null;

	@Override
	public void upgrade(final IProgressMonitor monitor) throws Exception {
		
		monitor.subTask(MessageFormat.format(Messages.Upgrader700To741_UpgradeMsg, UpgradeEngine.UpgradeFromVersion.V700.fromVersion, UpgradeEngine.UpgradeFromVersion.V750.toVersion));  
		thrownException = null;
		try(Session s = HibernateManager.openSession()){
			s.doWork(new Work() {
				@Override
				public void execute(Connection c) throws SQLException {
					s.beginTransaction();
					try {
						c.setAutoCommit(false);
						upgrade(c, monitor);
						c.setAutoCommit(true);
						s.getTransaction().commit();
					} catch (final Exception e) {
						thrownException = new Exception(MessageFormat.format(Messages.Upgrader700To741_UpgradeErrorMsage, UpgradeEngine.UpgradeFromVersion.V700.fromVersion, UpgradeEngine.UpgradeFromVersion.V750.toVersion), e); 
					}
				}
			});
		}
		if (thrownException != null)
			throw thrownException;

		monitor.done();
	}

	private void upgrade(Connection c, IProgressMonitor monitor)
			throws Exception {
		
		//these constraints need to be dropped but were not named in upgrade script
		//so have to get the name to drop them
		
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT fc.constraintname "); //$NON-NLS-1$
		sb.append("FROM sys.sysconstraints fc "); //$NON-NLS-1$
		sb.append("JOIN sys.sysforeignkeys f ON f.constraintid = fc.constraintid "); //$NON-NLS-1$
		sb.append("JOIN sys.sysconglomerates fg ON fg.conglomerateid = f.conglomerateid "); //$NON-NLS-1$
		sb.append("JOIN sys.systables ft ON ft.tableid = fg.tableid "); //$NON-NLS-1$
		sb.append("JOIN sys.sysschemas fs ON ft.schemaid = fs.schemaid "); //$NON-NLS-1$
		sb.append("JOIN sys.sysconstraints pc ON pc.constraintid = f.keyconstraintid "); //$NON-NLS-1$
		sb.append("JOIN sys.sysschemas ps ON pc.schemaid = ps.schemaid "); //$NON-NLS-1$
		sb.append("WHERE fc.type = 'F' "); //$NON-NLS-1$
		sb.append("and ft.tablename = 'WP_OBSERVATION_ATTRIBUTES_LIST' "); //$NON-NLS-1$
		sb.append("and fs.schemaname='SMART'"); //$NON-NLS-1$
		
		List<String> names = new ArrayList<>();
		try(Statement s = c.createStatement()){
			try(ResultSet rs = s.executeQuery(sb.toString())){
				while(rs.next()) {
					names.add(rs.getString(1));
				}
			}
		}
		for(String s : names) {
			sb = new StringBuilder();
			sb.append("ALTER TABLE smart.wp_observation_attributes_list "); //$NON-NLS-1$
			sb.append("DROP CONSTRAINT \""); //$NON-NLS-1$
			sb.append(s);
			sb.append("\""); //$NON-NLS-1$
			
			try(Statement st = c.createStatement()){
				SmartPlugIn.logInfo(sb.toString());
				st.execute(sb.toString());
			}
		}
		
		
		String[] sql = new String[] {
				//fix cases where employee linked to last_modified_by was removed
				"update smart.WAYPOINT set last_modified_by = null where last_modified_by not in (select uuid from smart.employee)", //$NON-NLS-1$
				"alter table smart.waypoint add constraint waypoint_last_modified_fk foreign key (last_modified_by) references smart.employee (uuid) on delete set null deferrable initially immediate", //$NON-NLS-1$

				"alter table smart.OBSERVATION_ATTACHMENT add column signature_type_uuid char(16) for bit data", //$NON-NLS-1$
				
				//constraint nightmare. have to drop all these constraints and re-create them
				//in order to be able to add signature foreign key constraint
				"alter table smart.dm_category drop constraint dm_category_ca_uuid_fk", //$NON-NLS-1$
				"alter table smart.DM_ATTRIBUTE drop constraint dm_attribute_ca_uuid_fk", //$NON-NLS-1$
				"ALTER table smart.wp_observation DROP CONSTRAINT wo_ob_group_uuid_fk", //$NON-NLS-1$
				"ALTER table smart.wp_observation_group DROP CONSTRAINT wo_obs_grp_wp_uuid_fk", //$NON-NLS-1$
				"alter table smart.wp_observation DROP constraint obs_employee_uuid_fk", //$NON-NLS-1$
				"alter table smart.wp_observation DROP constraint observation_category_uuid_fk", //$NON-NLS-1$
				"ALTER TABLE SMART.OBSERVATION_ATTACHMENT DROP CONSTRAINT OBSERVATION_ATTACHMENT_OBS_UUID_FK", //$NON-NLS-1$
				"ALTER TABLE SMART.WP_OBSERVATION_ATTRIBUTES DROP CONSTRAINT OBS_ATTRIBUTE_OBS_UUID_FK", //$NON-NLS-1$
				"ALTER TABLE smart.WP_ATTACHMENTS  DROP CONSTRAINT wp_attachments_signature_type_fk", //$NON-NLS-1$
				"ALTER TABLE smart.dm_attribute DROP CONSTRAINT dmatt_iconuuid_fk", //$NON-NLS-1$
				"ALTER TABLE smart.dm_attribute_list DROP CONSTRAINT dmattlist_iconuuid_fk", //$NON-NLS-1$
				"ALTER TABLE smart.dm_attribute_tree DROP CONSTRAINT dmatttree_iconuuid_fk", //$NON-NLS-1$
				"ALTER TABLE smart.dm_category DROP CONSTRAINT dmcat_iconuuid_fk", //$NON-NLS-1$
				"ALTER TABLE smart.signature_type  DROP CONSTRAINT signature_type_ca_uuid_fk", //$NON-NLS-1$
				"ALTER TABLE smart.icon DROP CONSTRAINT icon_cauuid_fk", //$NON-NLS-1$

				"ALTER table smart.wp_observation ADD CONSTRAINT wo_ob_group_uuid_fk FOREIGN KEY (wp_group_uuid) REFERENCES smart.wp_observation_group (uuid) ON UPDATE RESTRICT ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER table smart.wp_observation_group ADD CONSTRAINT wo_obs_grp_wp_uuid_fk FOREIGN KEY (wp_uuid) REFERENCES smart.waypoint (uuid) ON UPDATE RESTRICT ON DELETE CASCADE  DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"alter table smart.wp_observation add constraint obs_employee_uuid_fk foreign key (employee_uuid) REFERENCES smart.employee (uuid) ON UPDATE RESTRICT ON DELETE RESTRICT  DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"alter table smart.wp_observation add constraint observation_category_uuid_fk foreign key (category_uuid) REFERENCES smart.dm_category (uuid) ON UPDATE RESTRICT ON DELETE RESTRICT  DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE SMART.OBSERVATION_ATTACHMENT ADD CONSTRAINT OBSERVATION_ATTACHMENT_OBS_UUID_FK FOREIGN KEY (OBS_UUID) REFERENCES SMART.WP_OBSERVATION(UUID)  ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE SMART.WP_OBSERVATION_ATTRIBUTES ADD CONSTRAINT OBS_ATTRIBUTE_OBS_UUID_FK FOREIGN KEY (OBSERVATION_UUID) REFERENCES SMART.WP_OBSERVATION(UUID)  ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"alter table smart.wp_observation_attributes_list ADD CONSTRAINT wp_ob_att_list_ob_att_uuid_fk FOREIGN KEY (observation_attribute_uuid) REFERENCES smart.wp_observation_attributes(uuid) on DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"alter table smart.wp_observation_attributes_list ADD CONSTRAINT wp_ob_att_list_list_uuid_fk FOREIGN KEY (list_element_uuid) REFERENCES smart.dm_attribute_list(uuid) ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"alter table smart.dm_category add constraint dm_category_ca_uuid_fk foreign key (ca_uuid) references smart.CONSERVATION_AREA(uuid) ON UPDATE NO ACTION ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"alter table smart.DM_ATTRIBUTE add constraint dm_attribute_ca_uuid_fk foreign key (ca_uuid) references smart.CONSERVATION_AREA(uuid) ON UPDATE NO ACTION ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE smart.WP_ATTACHMENTS  ADD CONSTRAINT wp_attachments_signature_type_fk FOREIGN KEY (signature_type_uuid) REFERENCES smart.signature_type(uuid) ON DELETE SET NULL ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"alter table smart.observation_attachment ADD CONSTRAINT observation_attachment_sig_fk foreign key (signature_type_uuid)  references smart.signature_type(uuid) ON DELETE SET NULL ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE smart.signature_type  ADD CONSTRAINT signature_type_ca_uuid_fk FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area(uuid) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE smart.dm_attribute ADD CONSTRAINT dmatt_iconuuid_fk FOREIGN KEY (icon_uuid) REFERENCES smart.icon(uuid) ON DELETE SET NULL ON UPDATE RESTRICT  DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE smart.dm_attribute_list ADD CONSTRAINT dmattlist_iconuuid_fk FOREIGN KEY (icon_uuid) REFERENCES smart.icon(uuid) ON DELETE SET NULL ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE smart.dm_attribute_tree ADD CONSTRAINT dmatttree_iconuuid_fk FOREIGN KEY (icon_uuid) REFERENCES smart.icon(uuid) ON DELETE SET NULL ON UPDATE RESTRICT  DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE smart.dm_category ADD CONSTRAINT dmcat_iconuuid_fk FOREIGN KEY (icon_uuid) REFERENCES smart.icon(uuid) ON DELETE SET NULL ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE smart.icon ADD CONSTRAINT icon_cauuid_fk FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area(uuid) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
		
				"update smart.i18n_label set value = 'Hyaena Brown' WHERE  value = 'Hyaena rown' and element_uuid in (select uuid from smart.icon where keyid = 'hyaena_rown')", //$NON-NLS-1$
		};

		for (String s : sql) {
			SmartPlugIn.logInfo(s);
			c.createStatement().execute(s);
		}
		
		//remove configurable model labels that match data model labels
		sb = new StringBuilder();
		sb.append("SELECT va.element_uuid, va.language_uuid "); //$NON-NLS-1$
		sb.append("FROM smart.cm_node a, smart.i18n_label va, smart.i18n_label ca "); //$NON-NLS-1$
		sb.append("WHERE a.category_uuid is not null AND "); //$NON-NLS-1$
		sb.append(" ca.element_uuid = a.category_uuid AND "); //$NON-NLS-1$
		sb.append(" va.element_uuid = a.uuid AND "); //$NON-NLS-1$
		sb.append(" va.language_uuid = ca.language_uuid AND "); //$NON-NLS-1$
		sb.append(" va.value = ca.value "); //$NON-NLS-1$
		
		try(Statement s = c.createStatement();
			ResultSet rs = s.executeQuery(sb.toString());
			PreparedStatement delete = c.prepareStatement("DELETE FROM smart.i18n_label WHERE element_uuid = ? and language_uuid = ?");){ //$NON-NLS-1$

			int cnt = 0;
			while(rs.next()) {
				byte[] element = rs.getBytes(1);
				byte[] language = rs.getBytes(2);
				
				delete.setBytes(1, element);
				delete.setBytes(2, language);
				delete.addBatch();
				cnt ++;
				if (cnt == 500) {
					cnt = 0;
					delete.executeBatch();
				}
			}
			delete.executeBatch();
			
		}
		sb = new StringBuilder();
		sb.append("SELECT va.element_uuid, va.language_uuid "); //$NON-NLS-1$
		sb.append("FROM smart.cm_attribute a, smart.i18n_label va, smart.i18n_label ca "); //$NON-NLS-1$
		sb.append("WHERE a.attribute_uuid is not null AND "); //$NON-NLS-1$
		sb.append(" ca.element_uuid = a.attribute_uuid AND "); //$NON-NLS-1$
		sb.append(" va.element_uuid = a.uuid AND "); //$NON-NLS-1$
		sb.append(" va.language_uuid = ca.language_uuid AND "); //$NON-NLS-1$
		sb.append(" va.value = ca.value "); //$NON-NLS-1$
		
		try(Statement s = c.createStatement();
			ResultSet rs = s.executeQuery(sb.toString());
			PreparedStatement delete = c.prepareStatement("DELETE FROM smart.i18n_label WHERE element_uuid = ? and language_uuid = ?");){ //$NON-NLS-1$

			int cnt = 0;
			while(rs.next()) {
				byte[] element = rs.getBytes(1);
				byte[] language = rs.getBytes(2);
				
				delete.setBytes(1, element);
				delete.setBytes(2, language);
				delete.addBatch();
				cnt ++;
				if (cnt == 500) {
					cnt = 0;
					delete.executeBatch();
				}
			}
			delete.executeBatch();
			
		}
		
		/* VERSION UDATE */
		String ssql = "update smart.db_version set version = '" + UpgradeEngine.UpgradeFromVersion.V750.toVersion + "' where plugin_id = 'org.wcs.smart'"; //$NON-NLS-1$ //$NON-NLS-2$
		c.createStatement().execute(ssql);
	}

}
