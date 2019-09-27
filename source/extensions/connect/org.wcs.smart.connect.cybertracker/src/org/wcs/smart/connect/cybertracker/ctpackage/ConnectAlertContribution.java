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
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.wcs.smart.connect.cybertracker.internal.Messages;
import org.wcs.smart.connect.cybertracker.model.CtConnectPackageMetadata;
import org.wcs.smart.connect.cybertracker.model.CtPackageAlert;
import org.wcs.smart.cybertracker.export.IPackageUiContribution;
import org.wcs.smart.cybertracker.model.AbstractCtPackage;
import org.wcs.smart.cybertracker.model.ICtPackage;
import org.wcs.smart.cybertracker.model.MetadataFieldValue;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * Alert contribution to CyberTracker packages
 * @author Emily
 *
 */
public class ConnectAlertContribution extends AbstractConnectPackageContribution {

	private static final String ALERTS_JSONKEY = "ALERTS"; //$NON-NLS-1$
	private static final String CMATTRIBUTE_TREENODE_UUID_JSONKEY = "CMATTRIBUTE_TREENODE_UUID"; //$NON-NLS-1$
	private static final String CMATTRIBUTE_LISTITEM_UUID_JSONKEY = "CMATTRIBUTE_LISTITEM_UUID"; //$NON-NLS-1$
	private static final String CMATTRIBUTE_UUID_JSONKEY = "CMATTRIBUTE_UUID"; //$NON-NLS-1$
	private static final String CMNODE_UUID_JSONKEY = "CMNODE_UUID"; //$NON-NLS-1$
	private static final String TYPEUUID_JSONKEY = "TYPEUUID"; //$NON-NLS-1$
	private static final String CA_JSONKEY = "CAUUID"; //$NON-NLS-1$
	private static final String LEVEL_JSONKEY = "LEVEL"; //$NON-NLS-1$
	private static final String METADATA_JSONKEY = "METADATA"; //$NON-NLS-1$

	private static final String ALERT_URL = "/noa/cybertracker/alert/"; //$NON-NLS-1$
	
	public ConnectAlertContribution() {
	}

	@Override
	public IPackageUiContribution getUiController() {
		return new ConnectAlertUiController();
	}

	@SuppressWarnings("unchecked")
	@Override
	public PackageContribution packageFiles(ICtPackage ctpackage, IEclipseContext context, IProgressMonitor monitor) throws IOException {
		if (!(ctpackage instanceof AbstractCtPackage)) return null;
		
		AbstractCtPackage apackage = (AbstractCtPackage) ctpackage;
		boolean requiresconnect = false;
		for (MetadataFieldValue v : apackage.getMetadataValues()) {
			if (v.getMetadataKey().equals(CtConnectPackageMetadata.Properties.CONNECT_ALERT.name())) {
				requiresconnect = true;
				break;
			}
		}
		if (!requiresconnect) return null;
		
		String[] parts = null;
		try {
			parts = super.getServerDetails(context, ctpackage.getConservationArea(), false);
		}catch (Exception ex) {
			throw new IOException(ex);
		}
		if (parts == null) throw new IOException(Messages.ConnectAlertContribution_ConnectRequired);
		
		String url = parts[0];
		String apikey = parts[1];

		PackageContribution cc = new PackageContribution();

		JSONArray alerts = new JSONArray();
		try(Session s = HibernateManager.openSession()){
			for (CtPackageAlert palert: CtPackageAlert.fromString(apackage, s)) { 
				JSONObject alert = new JSONObject();
						
				alert.put(CMNODE_UUID_JSONKEY, palert.getCmNode().getUuid().toString());
				if (palert.getCmAttribute() != null)
					alert.put(CMATTRIBUTE_UUID_JSONKEY, palert.getCmAttribute().getUuid().toString());
				if (palert.getCmAttributeListItem() != null) 
					alert.put(CMATTRIBUTE_LISTITEM_UUID_JSONKEY, palert.getCmAttributeListItem().getUuid().toString());
				if (palert.getCmAttributeTreeNode() != null) 
					alert.put(CMATTRIBUTE_TREENODE_UUID_JSONKEY, palert.getCmAttributeTreeNode().getUuid().toString());
				
				alert.put(JSON_URLKEY, url + ALERT_URL);
				alert.put(JSON_APIKEY, apikey);
				
				JSONObject metadata = new JSONObject();
				metadata.put(TYPEUUID_JSONKEY, palert.getType().toString());
				metadata.put(CA_JSONKEY, apackage.getConservationArea().getUuid().toString());
				metadata.put(LEVEL_JSONKEY, palert.getLevel().value);
				
				alert.put(METADATA_JSONKEY, metadata);
				
				alerts.add(alert);
			}
		}
		cc.addProfileMetadata(ALERTS_JSONKEY, alerts);
		
		return cc;
	}

	@Override
	public void createDetails(Composite parent, ICtPackage ctpackage, Session session) {

	}

}
