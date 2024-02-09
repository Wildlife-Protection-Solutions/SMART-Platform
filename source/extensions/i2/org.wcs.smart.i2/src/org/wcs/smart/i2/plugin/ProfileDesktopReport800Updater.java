package org.wcs.smart.i2.plugin;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.common.control.WarningDialog;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.i2.ProfileReport800Upgrader;
import org.wcs.smart.upgrade.AbstractInteralDatabaseUpgrader;

public class ProfileDesktopReport800Updater  {
	
	public void upgrade(Session session) throws Exception {
			
		List<String> warnings = (new ProfileReport800Upgrader()).upgrade(session);
		if (warnings.size() > 0){
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					WarningDialog wd = new WarningDialog(Display.getDefault().getActiveShell(), "Incident Report Upgrade", "The following incident templates could not be upgraded. You may need to reset these to print incidents to PDF.", warnings);
					wd.open();
				}});
		}
	}
}
