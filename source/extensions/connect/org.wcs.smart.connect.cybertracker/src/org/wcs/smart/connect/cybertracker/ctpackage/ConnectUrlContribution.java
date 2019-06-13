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
import org.eclipse.swt.widgets.Composite;
import org.hibernate.Session;
import org.wcs.smart.cybertracker.export.IPackageUiContribution;
import org.wcs.smart.cybertracker.model.ICtPackage;

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

	//json keys for status/package urls
	private static final String JSON_STATUS_KEY = "status_url"; //$NON-NLS-1$
	private static final String JSON_DOWNLOAD_KEY = "download_url"; //$NON-NLS-1$
	
	
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
				parts = super.getServerDetails(context, ctpackage.getConservationArea());
			}catch (Exception ex) {
				throw new IOException(ex);
			}
			if (parts == null) return null;
			String url = parts[0];
			String apikey = parts[1];
			
			cc.setProjectMetadata(JSON_STATUS_KEY, url + STATUS_URL + ctpackage.getUuid().toString());
			cc.setProjectMetadata(JSON_DOWNLOAD_KEY, url + PACKAGE_URL + ctpackage.getUuid().toString()); 
			cc.setProjectMetadata(JSON_APIKEY, apikey);
		}
		
		return cc;
	}

	@Override
	public void createDetails(Composite parent, ICtPackage ctpackage, Session session) {
		
	}

}
