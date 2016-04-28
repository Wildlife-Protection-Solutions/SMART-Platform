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
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.UuidItem;
import org.wcs.smart.connect.cybertracker.ConnectCtHibernateManager;
import org.wcs.smart.connect.cybertracker.model.ConnectAlert;
import org.wcs.smart.cybertracker.export.alert.AlertData;
import org.wcs.smart.cybertracker.export.alert.IAlertProvider;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * Alert provider for CyberTracker by Connect plugin.
 * 
 * @author elitvin
 * @since 4.0.0
 */
public class ConnectCtAlertProvider implements IAlertProvider {
	
	private List<ConnectAlert> alerts;
	private String username;
	private String password;
	private String url;

	public ConnectCtAlertProvider(ConfigurableModel model) {
		alerts = loadAlerts(model);
		// TODO: fix init logic
		username = "user";
		password = "pwd";
		url = "http://127.0.0.1/test";
	}

	@Override
	public List<AlertData> getAlertData(UuidItem item) {
		//TODO: implement some lookup logic for fast search
		//TODO: need to match both item and attribute
		List<AlertData> result = new ArrayList<>();
		for (ConnectAlert a : alerts) {
			if (item.equals(a.getAlertItem())) {
				result.add(createAlertData(a));
			}
		}
		return result;
	}

	private AlertData createAlertData(ConnectAlert a) {
		AlertData data = new AlertData();
		data.setActive(true);
		data.setUrl(url);
		data.setUsername(username);
		data.setPassword(password);
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
							monitor.beginTask("Loading SMART Connect Alerts", 1);
							Session s = HibernateManager.openSession();
							s.beginTransaction();
							resultList.addAll(ConnectCtHibernateManager.getConnectAlerts(cm, s, false));
							s.getTransaction().rollback();
							s.close();
						}
					});
				} catch (Exception e) {
					SmartPlugIn.displayLog("Error occurs while loading SMART Connect Alerts.", e);
				}
			}
		});
		return resultList;
	}
	
}
