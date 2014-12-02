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
package org.wcs.smart.upgrade.v300;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.upgrade.IDatabaseUpgrader;
import org.wcs.smart.upgrade.UpgradeEngine;

/**
 * Upgrade SMART from version 2.0.0 to 3.0.0.
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class Upgrader200To300 implements IDatabaseUpgrader {

	public void upgrade(Session s, IProgressMonitor monitor) {
		monitor.subTask(Messages.Upgrader200To300_SubTask_Name);
		s.doWork(new Work() {
			@Override
			public void execute(Connection c) throws SQLException {
				try {
					c.setAutoCommit(false);
					upgrade200To300(c);
				} catch (final Exception e) {
					Display.getDefault().syncExec(new Runnable(){
						@Override
						public void run() {
							SmartPlugIn.displayLog(Display.getDefault().getActiveShell(), Messages.Upgrader200To300_Error, e);
						}
					});
				} finally {
					c.setAutoCommit(true);
				}
			}
		});
	}

	private static void upgrade200To300(Connection c) throws Exception {
		InputStream in = Upgrader200To300.class.getClassLoader().getResourceAsStream("org/wcs/smart/upgrade/v300/version_3.0.0.sql"); //$NON-NLS-1$
		UpgradeEngine.runScript(c, in);
		
		in = Upgrader200To300.class.getClassLoader().getResourceAsStream("org/wcs/smart/upgrade/v300/intelligence_source_pre.sql"); //$NON-NLS-1$
		UpgradeEngine.runScript(c, in);			

		List<CaData> areas = getConservationAreas(c);
		for (CaData ca : areas) {
			createSource(c, ca, "Patrol", "patrol"); //$NON-NLS-1$ //$NON-NLS-2$
			createSource(c, ca, "Public", "public"); //$NON-NLS-1$ //$NON-NLS-2$
			createSource(c, ca, "Informant", "informant"); //$NON-NLS-1$ //$NON-NLS-2$
			createSource(c, ca, "CET", "cet"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		
		in = Upgrader200To300.class.getClassLoader().getResourceAsStream("org/wcs/smart/upgrade/v300/intelligence_source_post.sql"); //$NON-NLS-1$
		UpgradeEngine.runScript(c, in);			
	
//		UpgradeSmartEngine.checkVersion2("3.0.0", c); //$NON-NLS-1$
		c.commit();
	}

	private static List<CaData> getConservationAreas(Connection c) throws SQLException {
		PreparedStatement pst = c.prepareStatement("select ca.uuid, lng.uuid from smart.CONSERVATION_AREA ca left join smart.LANGUAGE lng on ca.uuid = lng.CA_UUID WHERE ca.uuid <> ? and lng.ISDEFAULT = true"); //$NON-NLS-1$
		pst.setBytes(1, ConservationArea.MULTIPLE_CA);
		ResultSet rs = pst.executeQuery();
		List<CaData> areas = new ArrayList<CaData>();
		while (rs.next()) {
			CaData data = new CaData();
			data.uuid = rs.getBytes(1);
			data.defaultLangUuid = rs.getBytes(2);
			areas.add(data);
		}
		return areas;
	}

	private static void createSource(Connection c, CaData ca, final String name, final String keyId) throws SQLException {
		//the same approach is used in hibernate to generate uuids (see StandardRandomStrategy)
		UUID uuidEx = UUID.randomUUID();
		byte[] uuid = transform(uuidEx);
		
		PreparedStatement pst = c.prepareStatement("INSERT INTO smart.intelligence_source (UUID, CA_UUID, KEYID, IS_ACTIVE) VALUES (?, ?, ?, ?)"); //$NON-NLS-1$
		pst.setBytes(1, uuid);
		pst.setBytes(2, ca.uuid);
		pst.setString(3, keyId);
		pst.setBoolean(4, true);
		pst.execute();
		
		pst = c.prepareStatement("INSERT INTO smart.I18N_LABEL (LANGUAGE_UUID, ELEMENT_UUID, VALUE) VALUES (?, ?, ?)"); //$NON-NLS-1$
		pst.setBytes(1, ca.defaultLangUuid);
		pst.setBytes(2, uuid);
		pst.setString(3, name);
		pst.execute();
		
		pst = c.prepareStatement("UPDATE smart.intelligence SET source_uuid = ? where ca_uuid = ? and source = ?"); //$NON-NLS-1$
		pst.setBytes(1, uuid);
		pst.setBytes(2, ca.uuid);
		pst.setString(3, keyId.toUpperCase());
		pst.executeUpdate();
		
	}

	//copy from org.hibernate.type.descriptor.java.ToBytesTransformer
	private static byte[] transform(UUID uuid) {
		byte[] bytes = new byte[16];
		System.arraycopy( fromLong( uuid.getMostSignificantBits() ), 0, bytes, 0, 8 );
		System.arraycopy( fromLong( uuid.getLeastSignificantBits() ), 0, bytes, 8, 8 );
		return bytes;
	}

	//copy from org.hibernate.internal.util.BytesHelper
	private static byte[] fromLong(long longValue) {
		byte[] bytes = new byte[8];
		bytes[0] = (byte) ( longValue >> 56 );
		bytes[1] = (byte) ( ( longValue << 8 ) >> 56 );
		bytes[2] = (byte) ( ( longValue << 16 ) >> 56 );
		bytes[3] = (byte) ( ( longValue << 24 ) >> 56 );
		bytes[4] = (byte) ( ( longValue << 32 ) >> 56 );
		bytes[5] = (byte) ( ( longValue << 40 ) >> 56 );
		bytes[6] = (byte) ( ( longValue << 48 ) >> 56 );
		bytes[7] = (byte) ( ( longValue << 56 ) >> 56 );
		return bytes;
	}
	
	private static class CaData {
		byte[] uuid;
		byte[] defaultLangUuid;
	}
	
}
