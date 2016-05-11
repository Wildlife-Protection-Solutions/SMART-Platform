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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.UuidItem;
import org.wcs.smart.connect.SmartConnect;
import org.wcs.smart.connect.cybertracker.ConnectCtHibernateManager;
import org.wcs.smart.connect.cybertracker.internal.Messages;
import org.wcs.smart.connect.cybertracker.model.ConnectAlert;
import org.wcs.smart.connect.cybertracker.util.AlertLookup;
import org.wcs.smart.cybertracker.export.alert.AlertData;
import org.wcs.smart.cybertracker.export.alert.IAlertProvider;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * Alert provider for CyberTracker by Connect plugin.
 * 
 * @author elitvin
 * @since 4.0.0
 */
public class ConnectCtAlertProvider implements IAlertProvider {

	private AlertLookup lookup;
	
	private String username;
	private String password;
	private String url;

	private String caId;
	
	public ConnectCtAlertProvider(ConfigurableModel model) {
		caId = model.getConservationArea().getUuid().toString();
		if (model.getUuid() != null){
			List<ConnectAlert> alerts = loadAlerts(model);
			lookup = new AlertLookup(alerts);
			if (!lookup.isEmpty()) {
				//we have at lease one alert configured for this model, so we need to init server related fields
				initConnectFields();
			}
		}else{
			//this case we are using the data model with has no alerts
			lookup = new AlertLookup(Collections.emptyList());
		}
	}
	
	private void initConnectFields() {
		Display.getDefault().syncExec(new Runnable(){
			@Override
			public void run() {
				ConnectAlertConfigDialog cd = new ConnectAlertConfigDialog(Display.getDefault().getActiveShell());
				if (cd.open() != Window.CANCEL) {
					url = cd.getServerUrl();
					username = cd.getUsername();
					password = cd.getPassword();
				} else {
					//we don't now server configuration -> alerts will be ignored
					lookup.clear();
					Display.getDefault().syncExec(new Runnable() {
						@Override
						public void run() {
							MessageDialog.openInformation(Display.getDefault().getActiveShell(), Messages.ConnectCtAlertProvider_NoAlertConfigInfo_Title, Messages.ConnectCtAlertProvider_NoAlertConfigInfo_Message);
						}
					});
				}
			}
		});
	}

	@Override
	public List<AlertData> getAlertData(UuidItem item, CmAttribute attribute) {
		List<ConnectAlert> alerts = lookup.getAlerts(item, attribute);
		if (alerts == null || alerts.isEmpty()) {
			return Collections.emptyList();
		}
		List<AlertData> result = new ArrayList<>();
		for (ConnectAlert a : alerts) {
			result.add(createAlertData(a));
		}
		return result;
	}

	private AlertData createAlertData(ConnectAlert a) {
		AlertData data = new AlertData();
		data.setActive(true);
		data.setUrl(url + "/" + SmartConnect.API_URL + "/connectalert"); //$NON-NLS-1$ //$NON-NLS-2$
		data.setUsername(username);
		data.setPassword(password);
		data.setCaId(caId);
		data.setType(a.getType());
		data.setLevel(a.getLevel());
		return data;
	}

	private List<ConnectAlert> loadAlerts(final ConfigurableModel cm) {
		final List<ConnectAlert> resultList = new ArrayList<ConnectAlert>();
		Display.getDefault().syncExec(new Runnable(){
			@Override
			public void run() {
				ProgressMonitorDialog pmd = new ProgressMonitorDialog(Display.getDefault().getActiveShell());
				try {
					pmd.run(true, false, new IRunnableWithProgress() {
						@Override
						public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
							monitor.beginTask(Messages.ConnectCtAlertProvider_LoadAlertsTaskName, 1);
							Session s = HibernateManager.openSession();
							s.beginTransaction();
							resultList.addAll(ConnectCtHibernateManager.getConnectAlerts(cm, s, true));
							s.getTransaction().rollback();
							s.close();
						}
					});
				} catch (Exception e) {
					SmartPlugIn.displayLog(Messages.ConnectCtAlertProvider_LoadAlertsError, e);
				}
			}
		});
		return resultList;
	}
	
}
