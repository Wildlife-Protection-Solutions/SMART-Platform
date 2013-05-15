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
package org.wcs.smart.upgrade;

import java.io.File;

/**
 * Tools to upgrade a smart database.
 * 
 * @author Emily
 *
 */
public class UpgradeSmart {

	/**
	 * This program takes four arguments:
	 * 1. old version
	 * 2. new version
	 * 3. upgrade script which upgrades from the old version to the new version.
	 * This script must be a valid sql script with sql commands terminated by ;
	 * 4  SMART system backup file to upgrade.
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length != 4){
			System.err.println("ERROR: Invalid arguments.  The following arguments must be provided:");
			System.err.println("<oldVersion> <newVersion> <updateScript> <SMARTBackupFile>");
			System.err.println("oldVersion - the database version being upgraded from");
			System.err.println("newVersion - the database version being upgraded to");
			System.err.println("updateScript - the database upgrade sql script to upgrade from old to new version");
			System.err.println("SMARTBackupFile - the SMART backup file to upgrade");
			return;
		}
		String oldVersion = args[0];
		String newVersion = args[1];
		String upgradeScript = args[2];
		String dbBackup = args[3];

		File backupFile = new File(dbBackup);
		if (!backupFile.exists()){
			System.out.println("ERROR: Backup file " + backupFile.getAbsolutePath() + " does not exist");
			return;
		}
		
		File scriptFile = new File(upgradeScript);
		if (!scriptFile.exists()){
			System.out.println("ERROR: Upgrade script " + upgradeScript + " does not exist");
			return;
		}
		
		UpgradeSmartEngine engine = new UpgradeSmartEngine();
		try{
			File upgradedFile = engine.processBackupfile(backupFile, oldVersion, newVersion, scriptFile);
			System.out.println("---------------------------------------");
			System.out.println("UPGRADE COMPLETED SUCCESSFULLY.");
			System.out.println("Upgraded backup file: " + upgradedFile.getAbsolutePath());
		}catch (Exception ex){
			System.out.println("-------------------");
			System.out.println("Stack Trace:");
			ex.printStackTrace(System.out);
			System.out.println("-------------------");
			System.out.println(ex.getMessage());
			
		}

	}

}
