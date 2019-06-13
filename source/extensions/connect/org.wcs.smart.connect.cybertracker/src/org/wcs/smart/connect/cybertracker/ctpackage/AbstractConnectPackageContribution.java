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

import java.util.HashMap;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.connect.SmartConnect;
import org.wcs.smart.connect.cybertracker.internal.Messages;
import org.wcs.smart.connect.ui.server.ConnectDialog;
import org.wcs.smart.cybertracker.export.IPackageContribution;
import org.wcs.smart.util.UuidUtils;

public abstract class AbstractConnectPackageContribution  implements IPackageContribution {
	
	protected static final String JSON_URLKEY = "url"; //$NON-NLS-1$
	protected static final String JSON_APIKEY = "apikey"; //$NON-NLS-1$

	private SmartConnect connect = null;
	
	/**
	 * Two element array; first is the connect url the second is the ct api key.
	 * return null if details cannot be obtained
	 * 
	 * @param context
	 * @return
	 */
	protected String[] getServerDetails(IEclipseContext context, ConservationArea ca) throws Exception{
		connect = (SmartConnect) context.get(SmartConnect.class);

		if (connect == null) {
			//prompt for server details
			Display.getDefault().syncExec(()->{
				ConnectDialog cd = new ConnectDialog(Display.getCurrent().getActiveShell(), true) {
					@Override
					protected Control createDialogArea(Composite parent) {
						setTitle(Messages.ConnectCtPackageProperties_Title);
						getShell().setText(Messages.ConnectCtPackageProperties_Title);
						setMessage(Messages.ConnectCtPackageProperties_Message);	
						return super.createDialogArea(parent);
					}	
				};
				
				if (cd.open() == Window.OK) {
					connect = cd.getConnection();
					context.set(SmartConnect.class, connect);
				}
			});
			if (connect == null) {
				throw new Exception("A Connection to SMART Connect is required to export this package.");
			}
		}
		
		String apikey = (String) context.get("org.wcs.smart.connect.cybertracker.apikey");
		if (apikey == null) {
			ResteasyClient client = connect.getClient();
			ResteasyWebTarget target = client.target(connect.getServer().getServerUrl() + SmartConnect.API_URL);
			CtConnectClient simple = target.proxy(CtConnectClient.class);
			apikey = simple.getApiKey(UuidUtils.uuidToString(ca.getUuid()));
		}
		
		if (apikey == null) {
			throw new Exception("A CyberTracker SMART Connect API Key could not be found.  This package cannot be exported without an api key.");
		}
		context.set("org.wcs.smart.connect.cybertracker.apikey", apikey);
		
		return new String[] {connect.getServer().getServerUrl(), apikey};
	}

}
