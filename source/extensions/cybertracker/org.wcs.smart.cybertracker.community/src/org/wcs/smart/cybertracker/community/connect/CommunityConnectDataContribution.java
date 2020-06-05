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
package org.wcs.smart.cybertracker.community.connect;

import java.io.IOException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.swt.widgets.Composite;
import org.hibernate.Session;
import org.json.simple.JSONObject;
import org.wcs.smart.connect.cybertracker.ctpackage.AbstractConnectPackageContribution;
import org.wcs.smart.connect.cybertracker.ctpackage.ConnectDataUiController;
import org.wcs.smart.connect.cybertracker.model.CtConnectPackageMetadata;
import org.wcs.smart.connect.cybertracker.model.CtPackageAlert;
import org.wcs.smart.cybertracker.community.model.CommunityCtPackage;
import org.wcs.smart.cybertracker.export.IPackageUiContribution;
import org.wcs.smart.cybertracker.model.AbstractCtPackage;
import org.wcs.smart.cybertracker.model.ICtPackage;
import org.wcs.smart.cybertracker.model.MetadataFieldValue;
import org.wcs.smart.util.UuidUtils;

/**
 * Contributing alerts and data upload package items 
 * 
 * @author Emily
 *
 */
public class CommunityConnectDataContribution extends AbstractConnectPackageContribution {

	private static final String POSITION_UPDATES_JSONKEY = "POSITION_UPDATES"; //$NON-NLS-1$
	private static final String METADATA_JSONKEY = "METADATA"; //$NON-NLS-1$
	private static final String LEVEL_JSONKEY = "LEVEL"; //$NON-NLS-1$
	private static final String CAUUID_JSONKEY = "CAUUID"; //$NON-NLS-1$
	private static final String TYPEUUID_JSONKEY = "TYPEUUID"; //$NON-NLS-1$
	private static final String DATA_SERVER_JSONKEY = "DATA_SERVER"; //$NON-NLS-1$
	
	private static final String FREQUENCY_MIN_JSONKEY = "FREQUENCY_MIN"; //$NON-NLS-1$

	//urls for package status and package
	private static final String DATA_URL = "/noa/cybertracker/data/"; //$NON-NLS-1$
	private static final String ALERT_URL = "/noa/cybertracker/alert/"; //$NON-NLS-1$
	
	public CommunityConnectDataContribution() {
	}

	@Override
	public IPackageUiContribution getUiController() {
		return new ConnectDataUiController(false);
	}

	@SuppressWarnings("unchecked")
	@Override
	public PackageContribution packageFiles(ICtPackage ctpackage, IEclipseContext context, IProgressMonitor monitor) throws IOException {
		if (!(ctpackage instanceof CommunityCtPackage)) return null;
		CommunityCtPackage apackage = (CommunityCtPackage) ctpackage;

		boolean requiresConnect = false;
		for (MetadataFieldValue mv : apackage.getMetadataValues()) {
			if (mv.getMetadataKey().equals(CtConnectPackageMetadata.Properties.DATA_UPLOAD.name())) {
				if (mv.getBooleanValue()) {
					requiresConnect=true;
					break;
				}
			}else if (mv.getMetadataKey().equals(CtConnectPackageMetadata.Properties.POSITION_UPLOAD.name())) {
				if (mv.getBooleanValue()) {
					requiresConnect=true;
					break;
				}
			}
		}
		if (!requiresConnect) return null;
		
		String[] parts = null;
		try {
			parts = super.getServerDetails(context, ctpackage.getConservationArea(), false, PackageType.PUBLIC);
		}catch (Exception ex) {
			throw new IOException(ex);
		}
		if (parts == null) throw new IOException("This package requires a SMART Connect server connection to export.  The package includes Connect data uploads.");
		
		String url = parts[0];
		String apikey = parts[1];
				
		PackageContribution cc = new PackageContribution();
		
		for (MetadataFieldValue mv : apackage.getMetadataValues()) {
			if (mv.getMetadataKey().equals(CtConnectPackageMetadata.Properties.DATA_UPLOAD.name())) {
				if (mv.getBooleanValue()) {
					JSONObject dataserver = new JSONObject();
					dataserver.put(FREQUENCY_MIN_JSONKEY, Integer.valueOf( mv.getStringValue() ) );
					dataserver.put(JSON_URLKEY, url + DATA_URL + UuidUtils.uuidToString( ctpackage.getConservationArea().getUuid()) );
					dataserver.put(JSON_APIKEY, apikey);
					
					cc.addProfileMetadata(DATA_SERVER_JSONKEY, dataserver);
				}
			}else if (mv.getMetadataKey().equals(CtConnectPackageMetadata.Properties.POSITION_UPLOAD.name())) {
				if (mv.getBooleanValue()) {
					JSONObject pingalert = new JSONObject();
					
					pingalert.put(FREQUENCY_MIN_JSONKEY, Integer.valueOf( mv.getStringValue() ) );
					pingalert.put(JSON_URLKEY, url + ALERT_URL);
					pingalert.put(JSON_APIKEY, apikey);
					
					JSONObject metadata = new JSONObject();
					metadata.put(TYPEUUID_JSONKEY, mv.getUuidValue().toString());
					metadata.put(CAUUID_JSONKEY, mv.getConservationArea().getUuid().toString());
					metadata.put(LEVEL_JSONKEY, CtPackageAlert.Level.ONE.value);
					
					pingalert.put(METADATA_JSONKEY, metadata);
					cc.addProfileMetadata(POSITION_UPDATES_JSONKEY, pingalert);
				}
			}
			
		}
		
		return cc;
		
	}

	@Override
	public void createDetails(Composite parent, ICtPackage ctpackage, Session session) {
	}

}

