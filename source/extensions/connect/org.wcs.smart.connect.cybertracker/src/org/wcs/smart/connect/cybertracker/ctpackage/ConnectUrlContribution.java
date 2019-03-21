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
import org.eclipse.swt.widgets.Composite;
import org.hibernate.Session;
import org.wcs.smart.connect.ConnectHibernateManager;
import org.wcs.smart.connect.model.ConnectServer;
import org.wcs.smart.cybertracker.export.IPackageContribution;
import org.wcs.smart.cybertracker.export.IPackageUiContribution;
import org.wcs.smart.cybertracker.model.ICtPackage;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * This contribution adds the status_url and download_url
 * links to the project json file.  Only added if a
 * connect server is configured.
 * 
 * @author Emily
 *
 */
public class ConnectUrlContribution implements IPackageContribution {

	public ConnectUrlContribution() {
	}

	@Override
	public IPackageUiContribution getUiController() {
		return null;
	}

	@Override
	public PackageContribution packageFiles(ICtPackage ctpackage, IProgressMonitor monitor) throws IOException {
		PackageContribution cc = new PackageContribution();
		
		String url = null;
		try(Session s = HibernateManager.openSession()){
			ConnectServer cs = ConnectHibernateManager.getConnectServer(s);
			if (cs == null) return cc;
			
			url = cs.getServerUrl();
		}catch (Exception ex) {
			ex.printStackTrace();
		}
		//get connect url
		if (url == null) return cc;
		
		cc.setProjectMetadata("status_url:", url + "/api/cybertracker/" + ctpackage.getUuid().toString());
		cc.setProjectMetadata("download_url:", url + "/api/cybertracker/packages/" + ctpackage.getUuid().toString());
		
		return cc;
	}

	@Override
	public void createDetails(Composite parent, ICtPackage ctpackage, Session session) {
		
	}

}
