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
import org.json.simple.JSONObject;
import org.wcs.smart.connect.ConnectHibernateManager;
import org.wcs.smart.connect.cybertracker.model.CtConnectPackageMetadata;
import org.wcs.smart.connect.cybertracker.model.CtPackageAlert;
import org.wcs.smart.connect.model.ConnectServer;
import org.wcs.smart.cybertracker.export.IPackageContribution;
import org.wcs.smart.cybertracker.export.IPackageUiContribution;
import org.wcs.smart.cybertracker.model.AbstractCtPackage;
import org.wcs.smart.cybertracker.model.ICtPackage;
import org.wcs.smart.cybertracker.model.MetadataFieldValue;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.util.UuidUtils;

/**
 * Contributing alerts and data upload package items 
 * 
 * @author Emily
 *
 */
public class ConnectDataContribution implements IPackageContribution {

	private static final String POSITION_UPDATES_JSONKEY = "POSITION_UPDATES"; //$NON-NLS-1$
	private static final String METADATA_JSONKEY = "METADATA"; //$NON-NLS-1$
	private static final String LEVEL_JSONKEY = "LEVEL"; //$NON-NLS-1$
	private static final String CAUUID_JSONKEY = "CAUUID"; //$NON-NLS-1$
	private static final String TYPEUUID_JSONKEY = "TYPEUUID"; //$NON-NLS-1$
	private static final String DATA_SERVER_JSONKEY = "DATA_SERVER"; //$NON-NLS-1$
	private static final String URL_JSONKEY = "URL"; //$NON-NLS-1$
	private static final String FREQUENCY_MIN_JSONKEY = "FREQUENCY_MIN"; //$NON-NLS-1$

	public ConnectDataContribution() {
	}

	@Override
	public IPackageUiContribution getUiController() {
		return new ConnectDataUiController();
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

		for (MetadataFieldValue mv : apackage.getMetadataValues()) {
			if (mv.getMetadataKey().equals(CtConnectPackageMetadata.Properties.DATA_UPLOAD.name())) {
				if (mv.getBooleanValue()) {
					JSONObject dataserver = new JSONObject();
					dataserver.put(FREQUENCY_MIN_JSONKEY, Integer.valueOf( mv.getStringValue() ) );
					dataserver.put(URL_JSONKEY, url + "/api/ctdata/" + UuidUtils.uuidToString( ctpackage.getConservationArea().getUuid() ));
					//TODO: api key
					cc.addProfileMetadata(DATA_SERVER_JSONKEY, dataserver);
				}
			}else if (mv.getMetadataKey().equals(CtConnectPackageMetadata.Properties.POSITION_UPLOAD.name())) {
				if (mv.getBooleanValue()) {
					JSONObject pingalert = new JSONObject();
					
					pingalert.put(FREQUENCY_MIN_JSONKEY, Integer.valueOf( mv.getStringValue() ) );
					pingalert.put(URL_JSONKEY, url + "/api/connectalert/");
					//TODO: api key
					
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
