package org.wcs.smart.connect.cybertracker.ctpackage;

import java.io.IOException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Composite;
import org.hibernate.Session;
import org.wcs.smart.cybertracker.export.IPackageContribution;
import org.wcs.smart.cybertracker.export.IPackageUiContribution;
import org.wcs.smart.cybertracker.model.ICtPackage;

public class ConnectAlertContribution implements IPackageContribution {

	public ConnectAlertContribution() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public IPackageUiContribution getUiController() {
		return new ConnectAlertUiController();
	}

	@Override
	public PackageContribution packageFiles(ICtPackage ctpackage, IProgressMonitor monitor) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void createDetails(Composite parent, ICtPackage ctpackage, Session session) {
		// TODO Auto-generated method stub

	}

}
