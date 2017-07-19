package org.wcs.smart.upgrade.v600;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.upgrade.IDatabaseUpgrader;
import org.wcs.smart.upgrade.UpgradeEngine;
import org.wcs.smart.util.DerbyUtils;

public class Upgrader500To600 implements IDatabaseUpgrader { 
	private Exception thrownException = null;

	@Override
	public void upgrade(final IProgressMonitor monitor) throws Exception {
		monitor.beginTask(Messages.Upgrader500To600_ProgressMessage, 1);
		thrownException = null;
		final Session s = HibernateManager.openSession();
		try {
			s.doWork(new Work() {
				@Override
				public void execute(Connection c) throws SQLException {
					try {
						c.setAutoCommit(false);
						upgrade(c, s, monitor);
						c.setAutoCommit(true);
					} catch (final Exception e) {
						thrownException = new Exception(Messages.Upgrader500To600_ErrorMessage, e);
					}
				}
			});

		} finally {
			s.close();
		}
		if (thrownException != null)
			throw thrownException;

		monitor.done();
	}

	private void upgrade(Connection c, Session session, IProgressMonitor monitor)
			throws Exception {

		String[] sql = new String[] {
				"ALTER TABLE smart.patrol_leg ADD COLUMN mandate_uuid char(16) for bit data", //$NON-NLS-1$
				"UPDATE smart.patrol_leg SET mandate_uuid = (SELECT p.mandate_uuid FROM smart.patrol p WHERE p.uuid = smart.patrol_leg.patrol_uuid)", //$NON-NLS-1$
				"ALTER TABLE SMART.PATROL_LEG ADD CONSTRAINT MANDATE_UUID_FK FOREIGN KEY (MANDATE_UUID) REFERENCES SMART.PATROL_MANDATE(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE smart.patrol_leg ALTER COLUMN mandate_uuid SET NOT NULL", //$NON-NLS-1$
				"ALTER TABLE smart.patrol DROP COLUMN mandate_uuid", //$NON-NLS-1$

		};

		for (String s : sql) {
			SmartPlugIn.logInfo(s);
			c.createStatement().execute(s);
		}
		upgradeConfigurableModel(c);

		//create qa plugin tables
		QaPlugInInstaller.createTables(session, c);
		
		/* VERSION UDATE */
		String ssql = "update smart.db_version set version = '" + UpgradeEngine.UpgradeFromVersion.V600.toVersion + "' where plugin_id = 'org.wcs.smart'"; //$NON-NLS-1$ //$NON-NLS-2$
		c.createStatement().execute(ssql);
		c.commit();
	}

	private void upgradeConfigurableModel(Connection c) throws Exception {
		String[] sql = new String[] {
				"CREATE TABLE smart.cm_attribute_config(uuid char(16) for bit data not null, cm_uuid char(16) for bit data not null, dm_attribute_uuid char(16) for bit data not null, display_mode varchar(10), is_default boolean, primary key (uuid))", //$NON-NLS-1$
				"ALTER TABLE smart.cm_attribute_config ADD CONSTRAINT CM_ATTRIBUTE_CONFIG_CM_UUID_FK FOREIGN KEY (CM_UUID) REFERENCES SMART.CONFIGURABLE_MODEL(UUID) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE smart.cm_attribute_config ADD CONSTRAINT CM_ATTRIBUTE_CONFIG_DM_ATTRIBUTE_UUID_FK FOREIGN KEY (DM_ATTRIBUTE_UUID) REFERENCES SMART.DM_ATTRIBUTE(UUID) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$

				"GRANT ALL PRIVILEGES ON smart.cm_attribute_config TO manager", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.cm_attribute_config TO data_entry", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.cm_attribute_config TO analyst", //$NON-NLS-1$

				"alter table smart.cm_attribute add column config_uuid char(16) for bit data", //$NON-NLS-1$
				"ALTER TABLE smart.cm_attribute ADD CONSTRAINT CM_ATTRIBUTE_CONFIG_UUID_FK FOREIGN KEY (CONFIG_UUID) REFERENCES SMART.CM_ATTRIBUTE_CONFIG(UUID) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$

				"alter table smart.cm_attribute_list add column config_uuid char(16) for bit data", //$NON-NLS-1$
				"ALTER TABLE SMART.CM_ATTRIBUTE_LIST ADD CONSTRAINT CM_ATTRIBUTE_LIST_CONFIG_UUID_FK FOREIGN KEY (CONFIG_UUID) REFERENCES SMART.CM_ATTRIBUTE_CONFIG(UUID) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$ 

				"alter table smart.cm_attribute_tree_node add column config_uuid char(16) for bit data", //$NON-NLS-1$
				"ALTER TABLE SMART.CM_ATTRIBUTE_TREE_NODE ADD CONSTRAINT CM_ATTRIBUTE_TREE_NODE_CONFIG_UUID_FK FOREIGN KEY (CONFIG_UUID) REFERENCES SMART.CM_ATTRIBUTE_CONFIG(UUID) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$ 
		};

		for (String s : sql) {
			SmartPlugIn.logInfo(s);
			c.createStatement().execute(s);
		}
		
		populateConfigs(c, "smart.CM_ATTRIBUTE_LIST"); //$NON-NLS-1$
		populateConfigs(c, "smart.CM_ATTRIBUTE_TREE_NODE"); //$NON-NLS-1$
		
		sql = new String[] {
				"drop table SMART.CM_DM_ATTRIBUTE_SETTINGS", //$NON-NLS-1$

				"alter table smart.cm_attribute_list drop column CM_ATTRIBUTE_UUID", //$NON-NLS-1$
				"alter table smart.cm_attribute_list drop column DM_ATTRIBUTE_UUID", //$NON-NLS-1$
				"alter table smart.cm_attribute_list drop column CM_UUID", //$NON-NLS-1$
				"alter table smart.cm_attribute_list alter column config_uuid SET NOT NULL", //$NON-NLS-1$

				"alter table smart.cm_attribute_tree_node drop column CM_ATTRIBUTE_UUID", //$NON-NLS-1$
				"alter table smart.cm_attribute_tree_node drop column DM_ATTRIBUTE_UUID", //$NON-NLS-1$
				"alter table smart.cm_attribute_tree_node drop column CM_UUID", //$NON-NLS-1$
				"alter table smart.cm_attribute_tree_node alter column config_uuid SET NOT NULL", //$NON-NLS-1$

				"delete from smart.CM_ATTRIBUTE_OPTION where OPTION_ID = 'DISPLAY_MODE' OR OPTION_ID = 'CUSTOM_CONFIG'", //$NON-NLS-1$
		};

		for (String s : sql) {
			SmartPlugIn.logInfo(s);
			c.createStatement().execute(s);
		}
	}

	private void populateConfigs(Connection c, String tableName) throws Exception {
		try (ResultSet rs = c.createStatement().executeQuery("select distinct CM_UUID, CM_ATTRIBUTE_UUID, DM_ATTRIBUTE_UUID from " + tableName)) { //$NON-NLS-1$
			while (rs.next()) {
				byte[] cm_uuid = rs.getBytes(1);
				byte[] cma_uuid = rs.getBytes(2);
				byte[] dma_uuid = rs.getBytes(3);
				byte[] cfg_uuid = DerbyUtils.createUuid();

				if (cma_uuid != null) {
					//this is custom config
					insertConfig(c, cfg_uuid, cm_uuid, getDmAttributeForCmAttribute(c, cma_uuid), getDisplayModeForCustomCmAttribute(c, cma_uuid), false);

					PreparedStatement ps_upd = c.prepareStatement("UPDATE " + tableName + " SET CONFIG_UUID = ? WHERE CM_UUID = ? AND CM_ATTRIBUTE_UUID = ? AND DM_ATTRIBUTE_UUID IS NULL"); //$NON-NLS-1$ //$NON-NLS-2$
					ps_upd.setBytes(1, cfg_uuid);
					ps_upd.setBytes(2, cm_uuid);
					ps_upd.setBytes(3, cma_uuid);
					ps_upd.executeUpdate();

					PreparedStatement ps_cma_upd = c.prepareStatement("UPDATE smart.cm_attribute SET CONFIG_UUID = ? WHERE UUID = ?"); //$NON-NLS-1$
					ps_cma_upd.setBytes(1, cfg_uuid);
					ps_cma_upd.setBytes(2, cma_uuid);
					ps_cma_upd.executeUpdate();

				} else if (dma_uuid != null) {
					//this is default config
					insertConfig(c, cfg_uuid, cm_uuid, dma_uuid, getDisplayModeForDefaultAttribute(c, cm_uuid, dma_uuid), true);

					PreparedStatement ps_upd = c.prepareStatement("UPDATE " + tableName + " SET CONFIG_UUID = ? WHERE CM_UUID = ? AND CM_ATTRIBUTE_UUID IS NULL AND DM_ATTRIBUTE_UUID = ?"); //$NON-NLS-1$ //$NON-NLS-2$
					ps_upd.setBytes(1, cfg_uuid);
					ps_upd.setBytes(2, cm_uuid);
					ps_upd.setBytes(3, dma_uuid);
					ps_upd.executeUpdate();

					PreparedStatement ps_cma_upd = c.prepareStatement("UPDATE smart.cm_attribute SET CONFIG_UUID = ? WHERE CONFIG_UUID IS NULL and ATTRIBUTE_UUID = ? AND NODE_UUID IN (select uuid from smart.cm_node where cm_uuid = ?)"); //$NON-NLS-1$
					ps_cma_upd.setBytes(1, cfg_uuid);
					ps_cma_upd.setBytes(2, dma_uuid);
					ps_cma_upd.setBytes(3, cm_uuid);
					ps_cma_upd.executeUpdate();
				}
			}
		}
	}

	private void insertConfig(Connection c, byte[] cfg_uuid, byte[] cm_uuid, byte[] dma_uuid, String displayMode, boolean isDefault) throws Exception {
		PreparedStatement ps_cfg = c.prepareStatement("INSERT INTO smart.CM_ATTRIBUTE_CONFIG (UUID, CM_UUID, DM_ATTRIBUTE_UUID, DISPLAY_MODE, IS_DEFAULT) VALUES (?, ?, ?, ?, ?)"); //$NON-NLS-1$
		ps_cfg.setBytes(1, cfg_uuid);
		ps_cfg.setBytes(2, cm_uuid);
		ps_cfg.setBytes(3, dma_uuid);
		ps_cfg.setString(4, displayMode);
		ps_cfg.setBoolean(5, isDefault);
		ps_cfg.executeUpdate();

		//assigning name for the config
		byte[] lng_uuid = null;
		PreparedStatement ps_lng = c.prepareStatement("select lng.UUID from smart.LANGUAGE lng join smart.CONFIGURABLE_MODEL cm on lng.CA_UUID = cm.CA_UUID where lng.ISDEFAULT and cm.UUID = ?"); //$NON-NLS-1$
		ps_lng.setBytes(1, cm_uuid);
		try (ResultSet rs = ps_lng.executeQuery()) {
			if (rs.next()) {
				lng_uuid = rs.getBytes(1);
			} else {
				throw new Exception("Unable to detect default language while upgrading configurable model."); //$NON-NLS-1$
			}
		}

		String cfgName = "Configuration"; //$NON-NLS-1$ Some default that will be owerwritten
		PreparedStatement ps_name = c.prepareStatement("select VALUE from smart.I18N_LABEL where ELEMENT_UUID = ? AND LANGUAGE_UUID = ?"); //$NON-NLS-1$
		ps_name.setBytes(1, dma_uuid);
		ps_name.setBytes(2, lng_uuid);
		try (ResultSet rs = ps_name.executeQuery()) {
			if (rs.next()) {
				cfgName = rs.getString(1);
			}
		}
		if (!isDefault) {
			int customCount = 0;
			PreparedStatement ps_count = c.prepareStatement("select count(UUID) from smart.CM_ATTRIBUTE_CONFIG where DM_ATTRIBUTE_UUID = ? AND not IS_DEFAULT AND CM_UUID = ?"); //$NON-NLS-1$
			ps_count.setBytes(1, dma_uuid);
			ps_count.setBytes(2, cm_uuid);
			try (ResultSet rs = ps_count.executeQuery()) {
				if (rs.next()) {
					customCount = rs.getInt(1);
				}
			}
			cfgName = "Custom " + cfgName + " " + customCount; //$NON-NLS-1$ //$NON-NLS-2$
		}
		PreparedStatement ps_lbl = c.prepareStatement("INSERT INTO smart.I18N_LABEL (LANGUAGE_UUID, ELEMENT_UUID, VALUE) VALUES (?, ?, ?)"); //$NON-NLS-1$
		ps_lbl.setBytes(1, lng_uuid);
		ps_lbl.setBytes(2, cfg_uuid);
		ps_lbl.setString(3, cfgName);
		ps_lbl.executeUpdate();
	}

	private String getDisplayModeForDefaultAttribute(Connection c, byte[] cm_uuid, byte[] dma_uuid) throws SQLException {
		PreparedStatement ps = c.prepareStatement("select DISPLAY_MODE from smart.CM_DM_ATTRIBUTE_SETTINGS where CM_UUID = ? AND DM_ATTRIBUTE_UUID = ?"); //$NON-NLS-1$
		ps.setBytes(1, cm_uuid);
		ps.setBytes(2, dma_uuid);
		try (ResultSet rs = ps.executeQuery()) {
			return rs.next() ? rs.getString(1) : null;
		}
	}

	private String getDisplayModeForCustomCmAttribute(Connection c, byte[] cma_uuid) throws SQLException {
		PreparedStatement ps = c.prepareStatement("select STRING_VALUE from smart.CM_ATTRIBUTE_OPTION where OPTION_ID = 'DISPLAY_MODE' AND CM_ATTRIBUTE_UUID = ?"); //$NON-NLS-1$
		ps.setBytes(1, cma_uuid);
		try (ResultSet rs = ps.executeQuery()) {
			return rs.next() ? rs.getString(1) : null;
		}
	}

	private byte[] getDmAttributeForCmAttribute(Connection c, byte[] cma_uuid) throws SQLException {
		PreparedStatement ps = c.prepareStatement("select ATTRIBUTE_UUID from smart.CM_ATTRIBUTE where UUID = ?"); //$NON-NLS-1$
		ps.setBytes(1, cma_uuid);
		try (ResultSet rs = ps.executeQuery()) {
			return rs.next() ? rs.getBytes(1) : null;
		}
	}

}
