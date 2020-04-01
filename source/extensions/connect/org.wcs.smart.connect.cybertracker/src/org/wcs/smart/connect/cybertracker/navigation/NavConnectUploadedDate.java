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
package org.wcs.smart.connect.cybertracker.navigation;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.wcs.smart.connect.ConnectHibernateManager;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.SmartConnect;
import org.wcs.smart.connect.cybertracker.ctpackage.CtConnectClient;
import org.wcs.smart.connect.cybertracker.internal.Messages;
import org.wcs.smart.connect.cybertracker.model.CyberTrackerNavigationProxy;
import org.wcs.smart.connect.model.ConnectServer;
import org.wcs.smart.connect.ui.server.ConnectDialog;
import org.wcs.smart.cybertracker.ctpackage.ui.ICtPackagePropertyProvider.IPropertyListener;
import org.wcs.smart.cybertracker.model.NavigationLayer;
import org.wcs.smart.cybertracker.navigation.INavigationLayerProperty;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Navigation layer uploaded property date.
 * 
 * @author Emily
 *
 */
public class NavConnectUploadedDate implements INavigationLayerProperty {

	private SmartConnect connect = null;
	
	private HashMap<UUID, String> values = null;
	
	private List<IPropertyListener> listeners = new ArrayList<>();
	private AtomicBoolean isScheduled = new AtomicBoolean(false);
	
	@Inject
	private IEclipseContext context;

	
	@Override
	public String getName() {
		return Messages.NavConnectUploadedDate_PropertyName;
	}

	@Override
	public String getValue(NavigationLayer layer) {
		if (values == null) {
			if (!isScheduled.getAndSet(true)) {
				loadProperties.schedule();
				return DialogConstants.LOADING_TEXT;
			}else if (loadProperties.getState() == Job.RUNNING  || loadProperties.getState() == Job.WAITING){
				return DialogConstants.LOADING_TEXT;
			}
			return Messages.NavConnectUploadedDate_NoConnect;
		}
		if (values.containsKey(layer.getUuid())) return values.get(layer.getUuid());
		return ""; //$NON-NLS-1$
	}

	
	@Override
	public void refresh() {
		isScheduled.set(false);
		values = null;
	}

	@Override
	public void addPropertyUpdatedListener(IPropertyListener listener) {
		listeners.add(listener);
	}

	private void fireEvents() {
		listeners.forEach(l->l.propertyUpdated());
	}

	private Job loadProperties = new Job(Messages.NavConnectUploadedDate_jobname) {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			if (connect == null) {
				if (context == null || context.get(SmartConnect.class) == null) {
					ConnectServer cs = null;
					try(Session s = HibernateManager.openSession()){
						cs = ConnectHibernateManager.getConnectServer(s);
					}
					if (cs != null) {
						Display.getDefault().syncExec(new Runnable() {
							@Override
							public void run() {
								ConnectDialog cd = new ConnectDialog(Display.getCurrent().getActiveShell(), true) {
									@Override
									protected Control createDialogArea(Composite parent) {
										setTitle(Messages.NavConnectUploadedDate_ConnectDialogTitle);
										getShell().setText(Messages.NavConnectUploadedDate_ConnectDialogTitle);
										setMessage(Messages.NavConnectUploadedDate_ConnectDialogMsg);	
										return super.createDialogArea(parent);
									}	
								};
								
								if (cd.open() == Window.OK) {
									connect = cd.getConnection();
									if (context != null) context.set(SmartConnect.class, connect);
								}
							}
						});
					}
				}else {
					connect = context.get(SmartConnect.class);
				}
			}
			if (connect == null) {
				fireEvents();
				return Status.OK_STATUS;
			}
			
			HashMap<UUID, String> properties = new HashMap<>();
			 
			//load results into connect browser
			ResteasyClient client = connect.getClient();
			ResteasyWebTarget target = client.target(connect.getServer().getServerUrl() + SmartConnect.API_URL);
			CtConnectClient simple = target.proxy(CtConnectClient.class);
			
			try {
				List<CyberTrackerNavigationProxy> proxies = simple.getNavigationLayers( SmartDB.getCurrentConservationArea().getUuid().toString() );
				for (CyberTrackerNavigationProxy p : proxies) {
					properties.put(p.getUuid(), DateFormat.getDateTimeInstance().format( p.getUploadedDate()) );
				}
				values = properties;	
			}catch (Exception ex) {
				ConnectPlugIn.displayLog(Messages.ConnectCtPackageProperties_ErrorMessage + ex.getMessage(), ex);
			}
			
			fireEvents();
			return Status.OK_STATUS;
		}
		
	};
	
	
}
