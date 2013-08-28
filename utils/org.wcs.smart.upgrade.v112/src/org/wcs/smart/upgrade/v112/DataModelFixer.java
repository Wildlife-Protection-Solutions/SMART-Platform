package org.wcs.smart.upgrade.v112;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;

import javax.swing.ProgressMonitor;
import javax.swing.UIManager;

import org.wcs.smart.upgrade.UpgradeDialog;
import org.wcs.smart.upgrade.UpgradeSmartEngine;
import org.wcs.smart.upgrade.ZipUtil;

public class DataModelFixer {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		try {UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());}catch (Exception ex){}
		 
		
		new UpgradeDialog("1.1.0 or 1.1.1", "1.1.2", new UpgradeDialog.IUpgradeAction() {
			@Override
			public File performUpgrade(File file, ProgressMonitor pm) throws Exception{
				return performDbUpgrade(file, pm);
			}
		});
	}
	
	private static File performDbUpgrade(File file, ProgressMonitor pm) throws Exception{
		
		pm.setNote("Un-compressing backup");
		File backupLocation = UpgradeSmartEngine.unzipBackup(file);
		backupLocation.deleteOnExit();
		
		File databaseFile = new File(backupLocation, "smartdb");
		if (!databaseFile.exists()) {
			throw new Exception(
					"ERROR: Invalid backup file.  Does not contain smart database.");
		}
		
		File newBackupFile = new File(file.getParentFile(), file.getName().substring(0,file.getName().lastIndexOf(".")) + ".1.1.2.zip");
		if (newBackupFile.exists()){
			throw new Exception("The output file " +newBackupFile + " already exists.  Please move or delete this file before upgrading the database.");
		}
		
		Connection c = null;
		c = getConnection(databaseFile);
		c.setAutoCommit(false);
			
		try{
			UpgradeSmartEngine.checkVersion("1.1.0", c);
			
			pm.setNote("Processing attribute list items");
			AttributeListItemProcessor alp = new AttributeListItemProcessor();
			alp.fixAttributesListItems(c);
			pm.setProgress(20);
		
			pm.setNote("Processing attribute tree nodes");
			AttributeTreeItemProcessor atp = new AttributeTreeItemProcessor();
			atp.fixAttributeTreeItems(c);
			pm.setProgress(40);
			
			pm.setNote("Processing attributes");
			AttributeProcessor ap = new AttributeProcessor();
			ap.fixAttributes(c);
			pm.setProgress(60);
		
			pm.setNote("Processing categories");
			CategoryProcessor cp = new CategoryProcessor();
			cp.fixCategories(c);
			pm.setProgress(80);
			
			pm.setNote("Processing names");
			NameFixer nf = new NameFixer();
			nf.fixNames(c);
			pm.setProgress(90);
			
			pm.setNote("Saving results");
			
			String sql = "Update smart.db_version set version = '1.1.2'";
			c.createStatement().execute(sql);
			c.commit();
			
			pm.setNote("Creating backup file ");
			ZipUtil.createZip(backupLocation.listFiles(), newBackupFile);
			pm.setProgress(100);
			
			
		}catch (Exception ex){
			c.rollback();
			throw ex;
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
