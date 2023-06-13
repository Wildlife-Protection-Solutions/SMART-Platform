/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.connect.cybertracker.ctpackage;

import java.io.IOException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.connect.cybertracker.internal.Messages;
import org.wcs.smart.cybertracker.export.IPackageUiContribution;
import org.wcs.smart.cybertracker.model.ICtPackage;

import jakarta.ws.rs.NotFoundException;

/**
 * This contribution adds the status_url and download_url
 * links to the project json file.  Only added if a
 * connect server is configured.  These urls identify the location 
 * of the package and/or all packages on the connect server (for installing or
 * updating the app on the device).
 * 
 * @author Emily
 *
 */
public class ConnectUrlContribution extends AbstractConnectPackageContribution {

	//urls for package status and package
	private static final String STATUS_URL = "/noa/cybertracker/packages/info/"; //$NON-NLS-1$
	private static final String PACKAGE_URL = "/noa/cybertracker/packages/"; //$NON-NLS-1$
	private static final String NAVIGATION_URL = "/noa/cybertracker/navigation/"; //$NON-NLS-1$
	
	public ConnectUrlContribution() {
	}

	@Override
	public IPackageUiContribution getUiController() {
		return null;
	}

	@Override
	public PackageContribution packageFiles(ICtPackage ctpackage, IEclipseContext context, IProgressMonitor monitor) throws IOException {
		PackageContribution cc = new PackageContribution();
		
		//get connect url
		if (ctpackage.getUuid() != null) {
			String[] parts = null;
			try {
				parts = super.getServerDetails(context, ctpackage.getConservationArea(), true, PackageType.PRIVATE);
			}catch (NotFoundException ex) {
				//ca not found on server
				//warn user but allow export to proceed
				Display.getDefault().syncExec(()->{
					MessageDialog.openInformation(Display.getDefault().getActiveShell(), 
							Messages.ConnectUrlContribution_Warning,
							Messages.ConnectUrlContribution_CaNotFound);						
				});
				return null;
			}catch (Exception ex) {
				throw new IOException(ex);
			}
			if (parts == null) {
				//user hit cancel
				Display.getDefault().syncExec(()->{
					MessageDialog.openInformation(Display.getDefault().getActiveShell(), Messages.ConnectUrlContribution_Warning, Messages.ConnectUrlContribution_AutoUpdateMsg);
				});
				return null;
			}
			String url = parts[0];
			String apikey = parts[1];
			
			cc.setProjectMetadata(SmartMobilePackageFields.JSON_STATUS_KEY, url + STATUS_URL + ctpackage.getUuid().toString());
			cc.setProjectMetadata(SmartMobilePackageFields.NAVLAYERS_JSONKEY, url + NAVIGATION_URL);
			cc.setProjectMetadata(SmartMobilePackageFields.JSON_DOWNLOAD_KEY, url + PACKAGE_URL + ctpackage.getUuid().toString()); 
			cc.setProjectMetadata(SmartMobilePackageFields.JSON_APIKEY, apikey);
			
			
		}
		
		return cc;
	}

	@Override
	public void createDetails(Composite parent, ICtPackage ctpackage, Session session) {
		
	}

}
