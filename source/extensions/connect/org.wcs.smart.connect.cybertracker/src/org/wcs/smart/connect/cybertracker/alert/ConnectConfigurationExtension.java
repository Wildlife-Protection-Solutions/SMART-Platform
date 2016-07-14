/*
 * Copyright (C) 2012 Wildlife Conservation Society
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
package org.wcs.smart.connect.cybertracker.alert;

import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.cybertracker.export.alert.IAlertProvider;
import org.wcs.smart.cybertracker.export.alert.ICtConfigurationExtension;
import org.wcs.smart.cybertracker.export.alert.IDataTargetProvider;
import org.wcs.smart.dataentry.model.ConfigurableModel;

/**
 * Configuration extension by Connect for CyberTracker.  This provides both
 * alerts and data target providers
 * 
 * @author elitvin
 * @since 4.0.0
 */
public class ConnectConfigurationExtension implements ICtConfigurationExtension {
	
	private IDataTargetProvider dprovider;
	private IAlertProvider aprovider;
	private String[] connect = null;
	
	@Override
	public synchronized IAlertProvider getAlertProvider(ConfigurableModel model, Session session) {
		if (aprovider == null){
			aprovider = new ConnectCtAlertProvider(model, this);
		}
		return aprovider;
	}
	
	@Override
	public synchronized IDataTargetProvider getDataTargetProvider(ConfigurableModel model, Session session) throws Exception{
		if (dprovider == null){
			dprovider = new ConnectCtDataTargetProvider(model, this, session);
		}
		return dprovider;
	}

	/**
	 * Gets Connect server information.  Dialog is displayed to user once, then cached results
	 * are provided.  
	 * @return String array {connectUrl, connectUsername, connectPassword}.  If the user presses cancel this returns an empty array.
	 */
	public synchronized String[] getConnectData(){
		if (connect == null){
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					ConnectAlertConfigDialog cd = new ConnectAlertConfigDialog(Display.getDefault().getActiveShell());
					if (cd.open() != Window.CANCEL) {
						connect = new String[]{cd.getServerUrl(), cd.getUsername(), cd.getPassword()};
					}else{
						//	could not configure for whatever reason;
						connect = new String[]{};
					}
				}
			});
		}
		return connect;
	}

}
