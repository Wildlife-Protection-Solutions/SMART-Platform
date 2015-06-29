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
package org.wcs.smart.upgrade.v321;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.id.UUIDGenerationStrategy;
import org.hibernate.id.UUIDGenerator;
import org.hibernate.id.uuid.StandardRandomStrategy;
import org.hibernate.jdbc.Work;
import org.hibernate.type.BinaryType;

/**
 * Performs upgrade for configurable model from version 3.2.0 to 3.2.1
 * 
 * @author elitvin
 * @since 3.2.0
 */
public class CmUpgrader320To321 {
	
	private Session session;
	private UUIDGenerator uuidGenerator;
	
	public void upgrade(Session s) {
		this.session = s;
		uuidGenerator = null;
		
		s.doWork(new Work() {
			@Override
			public void execute(Connection c) throws SQLException {
				upgrade(c);
			}
		});
	}
	
	void upgrade(Connection c) throws SQLException {
		try(ResultSet cm_rs = c.createStatement().executeQuery("select UUID from smart.CONFIGURABLE_MODEL")){ //$NON-NLS-1$
			PreparedStatement ps = c.prepareStatement("select distinct cma.ATTRIBUTE_UUID from smart.CM_ATTRIBUTE cma left join smart.CM_NODE node on cma.NODE_UUID = node.UUID left join smart.DM_ATTRIBUTE dma on cma.ATTRIBUTE_UUID = dma.UUID where dma.ATT_TYPE = 'LIST' and node.CM_UUID = ?"); //$NON-NLS-1$
			while (cm_rs.next()) {
				byte[] cm_uuid = cm_rs.getBytes(1);
				ps.setBytes(1, cm_uuid);
				try(ResultSet dma_rs = ps.executeQuery()){
					while (dma_rs.next()) {
						byte[] dma_uuid = dma_rs.getBytes(1);
						buildDefaultList(c, cm_uuid, dma_uuid);
					}
				}
			}
		}
	}

	private void buildDefaultList(Connection c, byte[] cm_uuid, byte[] dma_uuid) throws SQLException {
		PreparedStatement psa = c.prepareStatement("select uuid, list_order from smart.DM_ATTRIBUTE_LIST where ATTRIBUTE_UUID = ? and IS_ACTIVE"); //$NON-NLS-1$
		psa.setBytes(1, dma_uuid);

		try(ResultSet dm_node_rs = psa.executeQuery()){
			PreparedStatement ps = c.prepareStatement("select UUID from smart.cm_attribute_list where CM_UUID = ? and LIST_ELEMENT_UUID = ?"); //$NON-NLS-1$
			PreparedStatement insert_ps = c.prepareStatement("insert into smart.cm_attribute_list (UUID, CM_UUID, LIST_ELEMENT_UUID, IS_ACTIVE, LIST_ORDER, DM_ATTRIBUTE_UUID) VALUES (?, ?, ?, ?, ?, ?)"); //$NON-NLS-1$
			PreparedStatement update_ps = c.prepareStatement("update smart.cm_attribute_list set DM_ATTRIBUTE_UUID = ?, LIST_ORDER = ? where UUID = ?"); //$NON-NLS-1$
			ps.setBytes(1, cm_uuid);
			while (dm_node_rs.next()) {
				byte[] cm_node_uuid = null;
				byte[] dm_node_uuid = dm_node_rs.getBytes(1);
				int order = dm_node_rs.getInt(2);
				ps.setBytes(2, dm_node_uuid);
				try(ResultSet cm_node_rs = ps.executeQuery()){
					if (cm_node_rs.next()) {
						//some mapping existed, need to update it
						cm_node_uuid = cm_node_rs.getBytes(1);
	
						update_ps.setBytes(1, dma_uuid);
						update_ps.setInt(2, order);
						update_ps.setBytes(3, cm_node_uuid);
						update_ps.execute();
					} else {
						//	there is no mapping, need to create it
						byte[][] obj = new byte[2][];
						obj[0] = cm_uuid;
						obj[1] = dm_node_uuid;
						cm_node_uuid = getNewUuid(obj);
					
						insert_ps.setBytes(1, cm_node_uuid);
						insert_ps.setBytes(2, cm_uuid);
						insert_ps.setBytes(3, dm_node_uuid);
						insert_ps.setBoolean(4, true);
						insert_ps.setInt(5, order);
						insert_ps.setBytes(6, dma_uuid);
						insert_ps.execute();
					}
				}
			}
		}
	}

	private byte[] getNewUuid(Object object) {
		if (uuidGenerator == null) {
			uuidGenerator = UUIDGenerator.buildSessionFactoryUniqueIdentifierGenerator();
			Properties prop = new Properties();
			prop.put(UUIDGenerator.UUID_GEN_STRATEGY, StandardRandomStrategy.INSTANCE);
			prop.put(UUIDGenerator.UUID_GEN_STRATEGY_CLASS, UUIDGenerationStrategy.class.getName());
			uuidGenerator.configure(new BinaryType(), prop, null);
		}

		byte[] uuid = (byte[]) uuidGenerator.generate((SessionImplementor) session, object);
		return uuid;
	}

}
