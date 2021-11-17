/*
 * Copyright (C) 2021 Wildlife Conservation Society
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

import java.text.MessageFormat;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.changetracking.ChangeLogInstaller;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.upgrade.UpgradeEngine.UpgradeFromVersion;

/**
 * Class will upgrade the core tables in the 
 * SMART database to the current version.
 * 
 * This class was introduced to support changes to the database
 * without have to do complete backup/restore process. Users can
 * upgrade plugins then the database will be upgraded on the first
 * launch.  I tried to do this via p2 like the other plugins but
 * couldn't figure it out. When using need to think
 * about coordinating updates with Connect.
 * 
 * @author Emily
 *
 */
public class StartUpDatabaseUpgrader   {

	public void doUpgrade (String currentVersion) throws Exception {
		
		try {
			SmartPlugIn.log("Performing upgrade of core tables", null); //$NON-NLS-1$
			
			UpgradeFromVersion fromVersion = null;
			for (UpgradeFromVersion v : UpgradeFromVersion.values()){
				if (v.fromVersion.equals(currentVersion)){
					fromVersion = v;
				}
			}
			
			int startIndex = -1;
			for (int i = 0; i < UpgradeFromVersion.values().length; i ++) {
				if (UpgradeFromVersion.values()[i] == fromVersion) {
					startIndex = i;
				}
			}
			
			if (startIndex < 0) {
				//throw an error
			}
			
			SmartPlugIn.log(MessageFormat.format("Upgrading core table from {0}", fromVersion.fromVersion), null); //$NON-NLS-1$
			//disable change tracking - we don't want to log changes for this
			boolean isChangeLogInstalled = ChangeLogInstaller.INSTANCE.isChangeLoggingEnabled();
			ChangeLogInstaller.INSTANCE.setEnabled(false);
			try {
				for (int i = startIndex; i < UpgradeFromVersion.values().length; i ++) {
					IDatabaseUpgrader upgrader = UpgradeFromVersion.values()[i].upgradeEngine.getDeclaredConstructor().newInstance();
					upgrader.upgrade(new NullProgressMonitor());
				}
			}finally {
				ChangeLogInstaller.INSTANCE.setEnabled(isChangeLogInstalled);
			}
		}catch (Exception ex) {
			throw new Exception(Messages.StartUpDatabaseUpgrader_UpgradeError + ex.getMessage(), ex);
		}
	}


}
