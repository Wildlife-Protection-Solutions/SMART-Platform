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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
import org.wcs.smart.connect.cybertracker.internal.Messages;
import org.wcs.smart.connect.cybertracker.model.CyberTrackerPackageProxy;
import org.wcs.smart.connect.model.ConnectServer;
import org.wcs.smart.connect.model.ConnectUser;
import org.wcs.smart.connect.ui.server.ConnectDialog;
import org.wcs.smart.cybertracker.ctpackage.ui.ICtPackageProperty;
import org.wcs.smart.cybertracker.ctpackage.ui.ICtPackagePropertyProvider;
import org.wcs.smart.cybertracker.model.ICtPackage;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Connect Cybertracker package property provider.  Provides
 * the connect date and version numbers.
 * 
 * @author Emily
 *
 */
public class ConnectCtPackageProperties implements ICtPackagePropertyProvider {

	private ConnectDateProperty p1 = new ConnectDateProperty();
	private ConnectVersionProperty p2 = new ConnectVersionProperty();
	private List<ICtPackageProperty> pps = (List<ICtPackageProperty>) List.of(p1,p2);
	
	private SmartConnect connect = null;
	
	private HashMap<UUID, String[]> values = null;
	
	private List<IPropertyListener> listeners = new ArrayList<>();
	private AtomicBoolean isScheduled = new AtomicBoolean(false);
	
	@Inject
	private IEclipseContext context;
	
	public ConnectCtPackageProperties() {
	}

	@Override
	public String getName() {
		return Messages.ConnectCtPackageProperties_ConnectDetailsLabel;
	}
	
	@Override
	public List<ICtPackageProperty> getProperties() {
		return pps;
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

	private String getValue(ICtPackage ctpackage, int index) {
		
		if (values == null) {
			if (!isScheduled.getAndSet(true)) {
				loadProperties.schedule();
				return DialogConstants.LOADING_TEXT;
			}else if (loadProperties.getState() == Job.RUNNING  || loadProperties.getState() == Job.WAITING){
				return DialogConstants.LOADING_TEXT;
			}else {
				return Messages.ConnectCtPackageProperties_NoConnect;
			}
		}else {
			String data[] = values.get(ctpackage.getUuid());
			if (data == null) {
				return ""; //$NON-NLS-1$
			}else {
				return data[index];
			}
		}
	}
	

	private Job loadProperties = new Job(Messages.ConnectCtPackageProperties_LoadingConnectProperties) {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			if (connect == null) {
				if (context == null || context.get(SmartConnect.class) == null) {
					
					ConnectServer cs = null;
					ConnectUser user = null;
					try(Session s = HibernateManager.openSession()){
						cs = ConnectHibernateManager.getConnectServer(s);
						user = ConnectHibernateManager.getConnectUser(SmartDB.getCurrentEmployee(), s);			

					}
					if (cs != null) {
						if (user != null && !user.getConnectPassword().isBlank()) {
							try {
								SmartConnect temp = SmartConnect.findInstance(cs, user.getConnectUsername(), ConnectPlugIn.decryptPassword(user));
								if (temp != null) {
									String error = temp.validateUser();
									if (error == null) {
										connect = temp;
									}
								}
							}catch (Exception ex) {}
						}
						
						if (connect == null) {
							Display.getDefault().syncExec(new Runnable() {
								@Override
								public void run() {
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
									}
								}
							});
						}
						if (context != null && connect != null) context.set(SmartConnect.class, connect);

					}
				} else {
					connect = context.get(SmartConnect.class);
				}
			}
			if (connect == null) {
				fireEvents();
				return Status.OK_STATUS;
			}
			
			 HashMap<UUID, String[]> properties = new HashMap<>();
			 
			//load results into connect browser
			ResteasyClient client = connect.getClient();
			ResteasyWebTarget target = client.target(connect.getServer().getServerUrl() + SmartConnect.API_URL);
			CtConnectClient simple = target.proxy(CtConnectClient.class);
			
			try {
				List<CyberTrackerPackageProxy> proxies = simple.getCtPackages( SmartDB.getCurrentConservationArea().getUuid().toString() );
				
				for (CyberTrackerPackageProxy p : proxies) {
					UUID uuid = p.getUuid();
					String revision = p.getVersion();
					
					String r = revision.substring(0, 32);
					SimpleDateFormat sdf = new SimpleDateFormat(ICtPackage.PACKAGE_DATE_FORMAT);
					String v = Messages.ConnectCtPackageProperties_ErrorLabel;
					try {
						Date d = sdf.parse(revision.substring(33));
						v = DateFormat.getDateTimeInstance().format(d);
					}catch (Exception ex) {
						ex.printStackTrace();
					}
					
					properties.put(uuid, new String[] {r,v});
					
				}
				values = properties;	
			}catch (Exception ex) {
				ConnectPlugIn.displayLog(Messages.ConnectCtPackageProperties_ErrorMessage + ex.getMessage(), ex);
			}
			
				
			fireEvents();
			return Status.OK_STATUS;
		}
		
	};
	
	
	private class ConnectDateProperty implements ICtPackageProperty{

		@Override
		public String getValue(ICtPackage ctpackage) {
			return ConnectCtPackageProperties.this.getValue(ctpackage, 1);
		}

		@Override
		public String getLongName() {
			return Messages.ConnectCtPackageProperties_DateColumn;
		}
		@Override
		public String getShortName() {
			return Messages.ConnectCtPackageProperties_DatePropertyShortName;
		}
	}
	
	private class ConnectVersionProperty implements ICtPackageProperty{

		@Override
		public String getValue(ICtPackage ctpackage) {
			return ConnectCtPackageProperties.this.getValue(ctpackage, 0);
		}

		@Override
		public String getLongName() {
			return Messages.ConnectCtPackageProperties_VersionColumn;
		}
		@Override
		public String getShortName() {
			return Messages.ConnectCtPackageProperties_VersionPropertyShortName;
		}
		@Override
		public boolean showInSummaryTable() {
			return false;
		}
	}

	
}
