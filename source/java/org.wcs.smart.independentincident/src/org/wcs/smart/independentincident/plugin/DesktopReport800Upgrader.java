package org.wcs.smart.independentincident.plugin;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.common.control.WarningDialog;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.incident.IncidentReport800Upgrader;
import org.wcs.smart.incident.internal.Messages;
import org.wcs.smart.upgrade.AbstractInteralDatabaseUpgrader;

public class DesktopReport800Upgrader extends AbstractInteralDatabaseUpgrader {
	
	@Override
	public void upgrade(IProgressMonitor monitor) throws Exception {
		try (Session session = HibernateManager.openSession()){
			session.beginTransaction();
			
			List<String> warnings = (new IncidentReport800Upgrader()).upgrade(session);
			if (warnings.size() > 0){
				Display.getDefault().syncExec(new Runnable(){
					@Override
					public void run() {
						WarningDialog wd = new WarningDialog(Display.getDefault().getActiveShell(), Messages.DesktopReport800Upgrader_ReportUpdateDialogTitle, Messages.DesktopReport800Upgrader_ReportUpgradeMessage, warnings);
						wd.open();
					}});
			}
			
			session.getTransaction().commit();
		}
	}

}
