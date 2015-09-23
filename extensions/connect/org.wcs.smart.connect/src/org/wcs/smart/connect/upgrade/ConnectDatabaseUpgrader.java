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
package org.wcs.smart.connect.upgrade;

import java.text.MessageFormat;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.updatesite.OnInstallAction;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.upgrade.IDatabaseUpgrader;
import org.wcs.smart.upgrade.UpgradeEngine;

/**
 * Entity upgrade operations while upgrade/restore backup.
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class ConnectDatabaseUpgrader implements IDatabaseUpgrader {

	/* (non-Javadoc)
	 * @see org.wcs.smart.upgrade.IDatabaseUpgrader#upgrade(org.hibernate.Session, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void upgrade(IProgressMonitor monitor) {
		Map<String, String> versions = null;
		Session s = HibernateManager.openSession();
		try{
			versions = UpgradeEngine.getVersions(s);
		
			if (versions == null) {
				//we don't know what is happening with database
				//it is some kind of error or wrong database version
				return;
			}
			final String currentVersion = versions.get(ConnectPlugIn.PLUGIN_ID);
			if (currentVersion == null) {
				//Entity doesn't present in this configuration
				//we need to perform install database support for the plug-in
				monitor.subTask("Installing Connect PlugIn database tables.");
				OnInstallAction install = new OnInstallAction();
				install.execute(null);
			}else{
				try{
					upgrade(currentVersion, s);
				}catch (final Throwable t){
					Display.getDefault().syncExec(new Runnable(){
						@Override
						public void run() {
							ConnectPlugIn.displayLog(MessageFormat.format(
									"Error upgrading the connect plugin database tables from version {0} to version {1}.", new Object[]{currentVersion, ConnectPlugIn.DB_VERSION}) + " \n\n" + t.getMessage(), t); //$NON-NLS-1$ //$NON-NLS-2$
						}
					});
				}
			}
		}finally{
			s.close();
		}
	}
	
	
	public static final void upgrade(String currentVersion, Session session){
	}

}
