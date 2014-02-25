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

import java.awt.Component;
import java.io.File;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.swing.JOptionPane;
import javax.swing.UIManager;

import org.wcs.smart.upgrade.CustomProgressMonitor;
import org.wcs.smart.upgrade.UpgradeDialog;
import org.wcs.smart.upgrade.UpgradeSmartEngine;
import org.wcs.smart.upgrade.ZipUtil;
import org.wcs.smart.upgrade.v200.SmartUpgrader;

/**
 * Tool for upgrading to SMART 3.0.0.
 * 
 * @author elitvin
 * @since 2.0.0
 */
public class SmartUpdater300 {

	private static final String TARGET_VERSION = "3.0.0"; //$NON-NLS-1$
	private static final byte[] MULTIPLE_CA = new byte[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());}catch (Exception ex){}
		 
		
		new UpgradeDialog("1.1.2, 1.1.3, 2.0.1, 2.0.2", TARGET_VERSION, new UpgradeDialog.IUpgradeAction() { //$NON-NLS-1$
			@Override
			public File performUpgrade(File file, CustomProgressMonitor pm) throws Exception{
				return performDbUpgrade(file, pm);
			}

			@Override
			public boolean checkInputFile(File file, Component dialog) {
				File newBackupFile = getBackupFileName(file);
				
				if (newBackupFile.exists()){
					int x = JOptionPane.showOptionDialog(dialog, 
							"<html><p style='width:400px'>The output file to be created " + newBackupFile.toString() + " already exists.  Do you want to overwrite it?</p></html>", "Overwrite", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null, null, null);   //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
					if (x != 0){
						return false;
					}else{
						if (!newBackupFile.delete()){
							JOptionPane.showMessageDialog(dialog, "Could not delete file.","Upgrade", JOptionPane.ERROR_MESSAGE);  //$NON-NLS-1$//$NON-NLS-2$
							return false;
						}
						return true;
					}
					
				}
				return true;
			}
			
		});
	}

	private static File performDbUpgrade(File file, CustomProgressMonitor pm) throws Exception{
		
		pm.setNote("Un-compressing backup"); //$NON-NLS-1$
		File backupLocation = UpgradeSmartEngine.unzipBackup(file);
		backupLocation.deleteOnExit();
		
		File databaseFile = new File(backupLocation, "smartdb"); //$NON-NLS-1$
		if (!databaseFile.exists()) {
			throw new Exception(
					"ERROR: Invalid backup file.  Does not contain smart database."); //$NON-NLS-1$
		}
		
		File newBackupFile = getBackupFileName(file);
		if (newBackupFile.exists()){
			throw new Exception("The output file " +newBackupFile + " already exists.  Please move or delete this file before upgrading the database."); //$NON-NLS-1$ //$NON-NLS-2$
		}
		
		Connection c = null;
		c = getConnection(databaseFile);
		c.setAutoCommit(false);
		
		try{
			pm.setNote("Determining source version");
			String x = UpgradeSmartEngine.getVersion(c);
			if (x.equals("1.1.2")){
				pm.setNote("Upgrading from 1.x.x to 3.x.x");
				SmartUpgrader.upgrade112To200(c);
				upgrade200To300(c);
			}else if (x.equals("2.0.0")){
				pm.setNote("Upgrading from 2.x.x to 3.x.x");
				upgrade200To300(c);
			}
			
			pm.setNote("Creating backup file "); //$NON-NLS-1$
			ZipUtil.createZip(backupLocation.listFiles(), newBackupFile);
			pm.setProgress(100);

		}catch (Exception ex){
			c.rollback();
			throw ex;
		}finally{
			c.close();
		}
		return newBackupFile;
		
	}

	public static void upgrade200To300(Connection c) throws Exception {
		
		UpgradeSmartEngine.checkVersion("2.0.0", c); //$NON-NLS-1$
		
		InputStream in = SmartUpdater300.class.getClassLoader().getResourceAsStream("org/wcs/smart/upgrade/v300/version_3.0.0.sql"); //$NON-NLS-1$
		UpgradeSmartEngine.runScript(c, in);
		
		in = SmartUpdater300.class.getClassLoader().getResourceAsStream("org/wcs/smart/upgrade/v300/intelligence_source_pre.sql"); //$NON-NLS-1$
		UpgradeSmartEngine.runScript(c, in);			

		List<CaData> areas = getConservationAreas(c);
		for (CaData ca : areas) {
			createSource(c, ca, "Patrol", "patrol"); //$NON-NLS-1$ //$NON-NLS-2$
			createSource(c, ca, "Public", "public"); //$NON-NLS-1$ //$NON-NLS-2$
			createSource(c, ca, "Informant", "informant"); //$NON-NLS-1$ //$NON-NLS-2$
			createSource(c, ca, "CET", "cet"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		
		in = SmartUpdater300.class.getClassLoader().getResourceAsStream("org/wcs/smart/upgrade/v300/intelligence_source_post.sql"); //$NON-NLS-1$
		UpgradeSmartEngine.runScript(c, in);			
	
		UpgradeSmartEngine.checkVersion2("3.0.0", c); //$NON-NLS-1$
		c.commit();
	}
	
	private static Connection getConnection(File databaseFile) throws Exception{
		String driver = "org.apache.derby.jdbc.EmbeddedDriver";  //$NON-NLS-1$
		Class.forName(driver).newInstance();
		
		String queryString = "jdbc:derby:" + databaseFile.getAbsolutePath() + ";user=smart_admin;password=smart_derby";  //$NON-NLS-1$//$NON-NLS-2$
		
		Connection c = DriverManager.getConnection(queryString);
		return c;
	}

	private static File getBackupFileName(File file){
		return new File(file.getParentFile(), file.getName().substring(0,file.getName().lastIndexOf(".")) + "." + TARGET_VERSION +".zip");   //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
	}
	

	private static List<CaData> getConservationAreas(Connection c) throws SQLException {
		PreparedStatement pst = c.prepareStatement("select ca.uuid, lng.uuid from smart.CONSERVATION_AREA ca left join smart.LANGUAGE lng on ca.uuid = lng.CA_UUID WHERE ca.uuid <> ? and lng.ISDEFAULT = true"); //$NON-NLS-1$
		pst.setBytes(1, MULTIPLE_CA);
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
