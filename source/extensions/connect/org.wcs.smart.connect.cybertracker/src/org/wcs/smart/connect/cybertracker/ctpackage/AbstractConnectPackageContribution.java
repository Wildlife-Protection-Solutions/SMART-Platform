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
	
	public static final String CT_API_CONTEXT_KEY = "org.wcs.smart.connect.cybertracker.apikey"; //$NON-NLS-1$
	

	private SmartConnect connect = null;
	
	public enum PackageType{
		PRIVATE,
		PUBLIC
	};
	
	/**
	 * 
	 * @param context
	 * @return Two element array; first is the connect url the second is the ct api key; null if the user cancels
	 * @throws exception if cannot connect (invalid username etc.)
	 */
	protected String[] getServerDetails(IEclipseContext context, ConservationArea ca, boolean canskip, PackageType type) throws Exception{
		connect = (SmartConnect) context.get(SmartConnect.class);

		if (connect == null) {
			//prompt for server details
			Display.getDefault().syncExec(()->{
				ConnectDialog cd = new ConnectDialog(Display.getCurrent().getActiveShell(), true) {
					@Override
					protected Control createDialogArea(Composite parent) {
						setTitle(Messages.AbstractConnectPackageContribution_ConnectTitle);
						getShell().setText(Messages.AbstractConnectPackageContribution_ConnectTitle);
						setMessage(Messages.AbstractConnectPackageContribution_ConnectMsg);	
						return super.createDialogArea(parent);
					}	
					
					@Override
					public void createButtonsForButtonBar(Composite parent){
						super.createButtonsForButtonBar(parent);
						if (canskip) {
							getButton(CANCEL).setText(Messages.AbstractConnectPackageContribution_SkipButtonTxt);
						}
					}
				};
				if (cd.open() == Window.OK) {
					connect = cd.getConnection();
					context.set(SmartConnect.class, connect);
				}
			});
			if (connect == null) {
				return null;
			}
		}
		
		String apikey = (String) context.get(CT_API_CONTEXT_KEY);
		if (apikey == null) {
			ResteasyClient client = connect.getClient();
			ResteasyWebTarget target = client.target(connect.getServer().getServerUrl() + SmartConnect.API_URL);
			CtConnectClient simple = target.proxy(CtConnectClient.class);
			apikey = simple.getApiKey(UuidUtils.uuidToString(ca.getUuid()), type.name());
		}
		
		if (apikey == null) {
			throw new Exception(Messages.AbstractConnectPackageContribution_KeyRequired);
		}
		context.set(CT_API_CONTEXT_KEY, apikey);
		
		return new String[] {connect.getServer().getServerUrl(), apikey};
	}

}
