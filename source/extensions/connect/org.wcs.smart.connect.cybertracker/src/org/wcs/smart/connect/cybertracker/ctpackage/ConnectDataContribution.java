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
import org.json.simple.JSONObject;
import org.wcs.smart.connect.cybertracker.internal.Messages;
import org.wcs.smart.connect.cybertracker.model.CtConnectPackageMetadata;
import org.wcs.smart.connect.cybertracker.model.CtPackageAlert;
import org.wcs.smart.cybertracker.export.IPackageUiContribution;
import org.wcs.smart.cybertracker.model.AbstractCtPackage;
import org.wcs.smart.cybertracker.model.ICtPackage;
import org.wcs.smart.cybertracker.model.MetadataFieldValue;
import org.wcs.smart.util.UuidUtils;

import jakarta.ws.rs.NotFoundException;

/**
 * Contributing alerts and data upload package items 
 * 
 * @author Emily
 *
 */
public class ConnectDataContribution extends AbstractConnectPackageContribution {


	//urls for package status and package
	private static final String DATA_URL = "/noa/cybertracker/data/"; //$NON-NLS-1$
	private static final String ALERT_URL = "/noa/cybertracker/alert/"; //$NON-NLS-1$
	
	public ConnectDataContribution() {
	}

	@Override
	public IPackageUiContribution getUiController() {
		return new ConnectDataUiController();
	}

	@SuppressWarnings("unchecked")
	@Override
	public PackageContribution packageFiles(ICtPackage ctpackage, IEclipseContext context, IProgressMonitor monitor) throws IOException {
		if (!(ctpackage instanceof AbstractCtPackage)) return null;
		AbstractCtPackage apackage = (AbstractCtPackage) ctpackage;

		
		//for backward compatibility
		//if we have data_upload metadata field but not the use_connect then set the use_connect
		//field to the value of the data_upload
		MetadataFieldValue useconnect = null;
		MetadataFieldValue dataupload = null;
		for (MetadataFieldValue mv : apackage.getMetadataValues()) {
			if (mv.getMetadataKey().equalsIgnoreCase(CtConnectPackageMetadata.Properties.USE_CONNECT.name())) {
				useconnect = mv;
			}else if (mv.getMetadataKey().equalsIgnoreCase(CtConnectPackageMetadata.Properties.DATA_UPLOAD.name())) {
				dataupload = mv;
			}
		}
		if (useconnect == null) {
			MetadataFieldValue mv = new MetadataFieldValue();
			mv.setConservationArea(apackage.getConservationArea());
			mv.setCtPackage(apackage);
			mv.setMetadataKey(CtConnectPackageMetadata.Properties.USE_CONNECT.name());
			mv.setVisible(false);
			apackage.getMetadataValues().add(mv);
			
			if (dataupload == null || !dataupload.getBooleanValue()) {
				mv.setBooleanValue(false);
			}else {
				mv.setBooleanValue(true);
			}
		}
		
		String fieldIdMetadata = null;
		boolean requiresConnect = false;
		for (MetadataFieldValue mv : apackage.getMetadataValues()) {
			if (mv.getMetadataKey().equals(CtConnectPackageMetadata.Properties.USE_CONNECT.name())) {
				if (mv.getBooleanValue()) {
					requiresConnect = true;
				}
			}else if (mv.getMetadataKey().equals(CtConnectPackageMetadata.Properties.POSITION_UPLOAD.name())) {
				if (mv.getBooleanValue()) {
					requiresConnect=true;
				}
			}else if (mv.getMetadataKey().equals(ICtPackage.FIELD_IDENTIFIER_KEY)) {
				fieldIdMetadata = mv.getStringValue();
			}
		}
		if (!requiresConnect) return null;
		
		String[] parts = null;
		try {
			parts = super.getServerDetails(context, ctpackage.getConservationArea(), false, PackageType.PRIVATE);
		}catch (NotFoundException ex) {
			throw new IOException(Messages.ConnectDataContribution_CaNotFound);
		}catch (Exception ex) {
			throw new IOException(ex);
		}
		if (parts == null) throw new IOException(Messages.ConnectDataContribution_ConnectRequired);
		
		String url = parts[0];
		String apikey = parts[1];
				
		PackageContribution cc = new PackageContribution();
		
		JSONObject dataserver = new JSONObject();
		dataserver.put(SmartMobilePackageFields.JSON_URLKEY, url + DATA_URL + UuidUtils.uuidToString( ctpackage.getConservationArea().getUuid()) );
		dataserver.put(SmartMobilePackageFields.JSON_APIKEY, apikey);		
		
		for (MetadataFieldValue mv : apackage.getMetadataValues()) {
			if (mv.getMetadataKey().equals(CtConnectPackageMetadata.Properties.USE_CONNECT.name())) {
				if (mv.getBooleanValue()) {
					cc.addProfileMetadata(SmartMobilePackageFields.DATA_SERVER_JSONKEY, dataserver);
				}
				
			}else if (mv.getMetadataKey().equals(CtConnectPackageMetadata.Properties.DATA_UPLOAD.name())) {
				if (mv.getBooleanValue()) {
					dataserver.put(SmartMobilePackageFields.FREQUENCY_MIN_JSONKEY, Integer.valueOf( mv.getStringValue() ) );
				}
			}else if (mv.getMetadataKey().equals(CtConnectPackageMetadata.Properties.POSITION_UPLOAD.name())) {
				if (mv.getBooleanValue()) {
					JSONObject pingalert = new JSONObject();
					
					pingalert.put(SmartMobilePackageFields.FREQUENCY_MIN_JSONKEY, Integer.valueOf( mv.getStringValue() ) );
					pingalert.put(SmartMobilePackageFields.JSON_URLKEY, url + ALERT_URL);
					pingalert.put(SmartMobilePackageFields.JSON_APIKEY, apikey);
					
					JSONObject metadata = new JSONObject();
					metadata.put(SmartMobilePackageFields.TYPEUUID_JSONKEY, mv.getUuidValue().toString());
					metadata.put(SmartMobilePackageFields.CAUUID_JSONKEY, mv.getConservationArea().getUuid().toString());
					metadata.put(SmartMobilePackageFields.LEVEL_JSONKEY, CtPackageAlert.Level.ONE.value);
					if (fieldIdMetadata != null && !fieldIdMetadata.isBlank() ) {
						metadata.put(SmartMobilePackageFields.FIELDID_JSONKEY, fieldIdMetadata);
					}
					
					pingalert.put(SmartMobilePackageFields.METADATA_JSONKEY, metadata);
					cc.addProfileMetadata(SmartMobilePackageFields.POSITION_UPDATES_JSONKEY, pingalert);
				}
			}
			
		}
		
		return cc;
		
	}

	@Override
	public void createDetails(Composite parent, ICtPackage ctpackage, Session session) {		
	}

}

