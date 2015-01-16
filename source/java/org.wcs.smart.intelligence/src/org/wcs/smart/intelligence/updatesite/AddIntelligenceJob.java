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
package org.wcs.smart.intelligence.updatesite;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.application.DisplayAccess;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.intelligence.IntelligencePlugIn;
import org.wcs.smart.intelligence.internal.Messages;
import org.wcs.smart.intelligence.upgrade.IIntelligenceUpgrader;
import org.wcs.smart.intelligence.upgrade.UpgradeFromVersion;

/**
 * Job removes adds intelligence related tabled to the database
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class AddIntelligenceJob extends Job {

	
	public AddIntelligenceJob() {
		super(Messages.AddIntelligenceJob_Title);
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		//required if run during restore to ensure Display.syncexec calls don't block
		DisplayAccess.accessDisplayDuringStartup();
				
		Session session = HibernateManager.openSession();
		try{
			final String dbVersion = HibernateManager.getPlugInVersion(IntelligencePlugIn.PLUGIN_ID, session);

			if (dbVersion != null && dbVersion.equals(IntelligencePlugIn.DB_VERSION)){
				//database version matches expected version 
				return Status.OK_STATUS;
			}
			
			UpgradeFromVersion fromVersion = UpgradeFromVersion.fromString(dbVersion);
			if (fromVersion == null) {
				Display.getDefault().syncExec(new Runnable(){
					@Override
					public void run() {
						MessageDialog.openError(
								Display.getDefault().getActiveShell(),
								Messages.AddIntelligenceJob_UpgradeError,
								MessageFormat.format(Messages.AddIntelligenceJob_CannotUpgrade, dbVersion));
					}
				});
				return new Status(IStatus.ERROR, IntelligencePlugIn.PLUGIN_ID, 1, "", null); //$NON-NLS-1$
			}
			
			
			//find the index of the current from version; then
			//run all upgrades from that index to upgrade to the 
			//current version
			int startIndex = 0;
			for (int i = 0; i < UpgradeFromVersion.values().length; i++){
				if (fromVersion == UpgradeFromVersion.values()[i]){
					startIndex = i ;
					break;
				}
			}
			for (int i = startIndex; i < UpgradeFromVersion.values().length; i ++){
				UpgradeFromVersion v = UpgradeFromVersion.values()[i];
				IIntelligenceUpgrader upgrader = v.createUpgradeEngine();
				if (upgrader == null || !upgrader.upgrade(session, monitor)) {
					//some error occurred, no need to continue; message was reported to user by upgrader
					return new Status(IStatus.ERROR, IntelligencePlugIn.PLUGIN_ID, 1, "", null); //$NON-NLS-1$
				}
			}

		} finally {
			session.close();
		}
		return Status.OK_STATUS;
	}
}
