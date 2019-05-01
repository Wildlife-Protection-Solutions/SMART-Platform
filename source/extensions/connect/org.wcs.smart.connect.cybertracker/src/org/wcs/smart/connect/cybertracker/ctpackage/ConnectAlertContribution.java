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
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.wcs.smart.connect.ConnectHibernateManager;
import org.wcs.smart.connect.cybertracker.model.CtPackageAlert;
import org.wcs.smart.connect.model.ConnectServer;
import org.wcs.smart.cybertracker.export.IPackageContribution;
import org.wcs.smart.cybertracker.export.IPackageUiContribution;
import org.wcs.smart.cybertracker.model.AbstractCtPackage;
import org.wcs.smart.cybertracker.model.ICtPackage;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * Alert contribution to CyberTracker packages
 * @author Emily
 *
 */
public class ConnectAlertContribution implements IPackageContribution {

	public ConnectAlertContribution() {
	}

	@Override
	public IPackageUiContribution getUiController() {
		return new ConnectAlertUiController();
	}

	@SuppressWarnings("unchecked")
	@Override
	public PackageContribution packageFiles(ICtPackage ctpackage, IProgressMonitor monitor) throws IOException {
		if (!(ctpackage instanceof AbstractCtPackage)) return null;
		
		String url = null;
		try(Session s = HibernateManager.openSession()){
			ConnectServer cs = ConnectHibernateManager.getConnectServer(s);
			if (cs == null) return null;
			
			url = cs.getServerUrl();
		}catch (Exception ex) {
			ex.printStackTrace();
		}
		
		AbstractCtPackage apackage = (AbstractCtPackage) ctpackage;
		PackageContribution cc = new PackageContribution();

		JSONArray alerts = new JSONArray();
		try(Session s = HibernateManager.openSession()){
			for (CtPackageAlert palert: CtPackageAlert.fromString(apackage, s)) { 
				JSONObject alert = new JSONObject();
						
				alert.put("CMNODE_UUID", palert.getCmNode().getUuid().toString());
				if (palert.getCmAttribute() != null)
					alert.put("CMATTRIBUTE_UUID", palert.getCmAttribute().getUuid().toString());
				if (palert.getCmAttributeListItem() != null) 
					alert.put("CMATTRIBUTE_LISTITEM_UUID", palert.getCmAttributeListItem().getUuid().toString());
				if (palert.getCmAttributeTreeNode() != null) 
					alert.put("CMATTRIBUTE_TREENODE_UUID", palert.getCmAttributeTreeNode().getUuid().toString());
				
				//TODO: fix this
				alert.put("URL", url + "/api/connectalert/");
				//TODO: api key
						
				JSONObject metadata = new JSONObject();
				metadata.put("TYPEUUID", palert.getType().toString());
				metadata.put("CAUUID", apackage.getConservationArea().getUuid().toString());
				metadata.put("LEVEL", palert.getLevel().value);
				
				alert.put("METADATA", metadata);
				
				alerts.add(alert);
			}
		}
		cc.addProfileMetadata("ALERTS", alerts);
		
		return cc;
	}

	@Override
	public void createDetails(Composite parent, ICtPackage ctpackage, Session session) {

	}

}
