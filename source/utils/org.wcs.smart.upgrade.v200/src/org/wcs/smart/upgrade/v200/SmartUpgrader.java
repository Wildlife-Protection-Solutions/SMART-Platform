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
package org.wcs.smart.upgrade.v200;

import java.awt.Component;
import java.io.File;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;

import javax.swing.JOptionPane;
import javax.swing.UIManager;

import org.wcs.smart.upgrade.CustomProgressMonitor;
import org.wcs.smart.upgrade.UpgradeDialog;
import org.wcs.smart.upgrade.UpgradeSmartEngine;
import org.wcs.smart.upgrade.ZipUtil;

/**
 * Tool for upgrading to SMART 2.0.0.
 * 
 * @author Emily
 *
 */
public class SmartUpgrader {

	private static final String TARGET_VERSION = "2.0.0";
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		try {UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());}catch (Exception ex){}
		 
		
		new UpgradeDialog("1.1.2 or 1.1.3", TARGET_VERSION, new UpgradeDialog.IUpgradeAction() {
			@Override
			public File performUpgrade(File file, CustomProgressMonitor pm) throws Exception{
				return performDbUpgrade(file, pm);
			}

			@Override
			public boolean checkInputFile(File file, Component dialog) {
				File newBackupFile = getBackupFileName(file);
				
				if (newBackupFile.exists()){
					int x = JOptionPane.showOptionDialog(dialog, 
							"<html><p style='width:400px'>The output file to be created " + newBackupFile.toString() + " already exists.  Do you want to overwrite it?</p></html>", "Overwrite", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null, null, null);
					if (x != 0){
						return false;
					}else{
						if (!newBackupFile.delete()){
							JOptionPane.showMessageDialog(dialog, "Could not delete file.","Upgrade", JOptionPane.ERROR_MESSAGE);
							return false;
						}
						return true;
					}
					
				}
				return true;
			}
			
		});
	}
	
	private static File getBackupFileName(File file){
		return new File(file.getParentFile(), file.getName().substring(0,file.getName().lastIndexOf(".")) + "." + TARGET_VERSION +".zip");
	}
	
	private static File performDbUpgrade(File file, CustomProgressMonitor pm) throws Exception{
		
		pm.setNote("Un-compressing backup");
		File backupLocation = UpgradeSmartEngine.unzipBackup(file);
		backupLocation.deleteOnExit();
		
		File databaseFile = new File(backupLocation, "smartdb");
		if (!databaseFile.exists()) {
			throw new Exception(
					"ERROR: Invalid backup file.  Does not contain smart database.");
		}
		
		File newBackupFile = getBackupFileName(file);
		if (newBackupFile.exists()){
			throw new Exception("The output file " +newBackupFile + " already exists.  Please move or delete this file before upgrading the database.");
		}
		
		Connection c = null;
		c = getConnection(databaseFile);
		c.setAutoCommit(false);
			
		try{
			UpgradeSmartEngine.checkVersion("1.1.2", c);
			
			
			InputStream in = SmartUpgrader.class.getClassLoader().getResourceAsStream("org/wcs/smart/upgrade/v200/version_2.0.0.sql");
			UpgradeSmartEngine.runScript(c, in);
			
			in = SmartUpgrader.class.getClassLoader().getResourceAsStream("org/wcs/smart/upgrade/v200/smart-tables-dataentry.sql");
			UpgradeSmartEngine.runScript(c, in);			
			
			//generate keys for required new fields
			KeyGenerator kg = new KeyGenerator();
			kg.generateKeys(c, "smart.patrol_mandate");
			kg.generateKeys(c, "smart.team");
			kg.generateKeys(c, "smart.patrol_transport");
						
			pm.setNote("Creating backup file ");
			ZipUtil.createZip(backupLocation.listFiles(), newBackupFile);
			pm.setProgress(100);
			
			
			UpgradeSmartEngine.checkVersion("2.0.0", c);
			c.commit();
			
		}catch (Exception ex){
			c.rollback();
			throw ex;
		}finally{
			c.close();
		}
		return newBackupFile;
		
	}
	
	private static Connection getConnection(File databaseFile) throws Exception{
		String driver = "org.apache.derby.jdbc.EmbeddedDriver"; 
		Class.forName(driver).newInstance();
		
		String queryString = "jdbc:derby:" + databaseFile.getAbsolutePath() + ";user=smart_admin;password=smart_derby";
		
		Connection c = DriverManager.getConnection(queryString);
		return c;
	}
	
}
