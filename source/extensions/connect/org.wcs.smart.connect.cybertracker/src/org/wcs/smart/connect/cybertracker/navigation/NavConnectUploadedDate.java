package org.wcs.smart.connect.cybertracker.navigation;

import java.text.DateFormat;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
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
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.SmartConnect;
import org.wcs.smart.connect.cybertracker.ctpackage.CtConnectClient;
import org.wcs.smart.connect.cybertracker.internal.Messages;
import org.wcs.smart.connect.cybertracker.model.CyberTrackerNavigationProxy;
import org.wcs.smart.connect.ui.server.ConnectDialog;
import org.wcs.smart.cybertracker.ctpackage.ui.ICtPackagePropertyProvider.IPropertyListener;
import org.wcs.smart.cybertracker.model.NavigationLayer;
import org.wcs.smart.cybertracker.navigation.INavigationLayerProperty;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.UuidUtils;

public class NavConnectUploadedDate implements INavigationLayerProperty {

	private SmartConnect connect = null;
	
	private HashMap<UUID, String> values = null;
	
	private List<IPropertyListener> listeners = new ArrayList<>();
	private AtomicBoolean isScheduled = new AtomicBoolean(false);
	
	@Inject
	private IEclipseContext context;

	
	@Override
	public String getName() {
		return "Connect Upload Date";
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
			return "No Connection";
		}
		if (values.containsKey(layer.getUuid())) return values.get(layer.getUuid());
		return "";
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

	private Job loadProperties = new Job("Loading navigation target properties") {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			if (connect == null) {
				if (context == null || context.get(SmartConnect.class) == null) {
					Display.getDefault().syncExec(new Runnable() {
						@Override
						public void run() {
							ConnectDialog cd = new ConnectDialog(Display.getCurrent().getActiveShell(), true) {
								@Override
								protected Control createDialogArea(Composite parent) {
									setTitle("Navigation Layers");
									getShell().setText("Navigation Layers");
									setMessage("Configure SMART Connect details");	
									return super.createDialogArea(parent);
								}	
							};
							
							if (cd.open() == Window.OK) {
								connect = cd.getConnection();
								if (context != null) context.set(SmartConnect.class, connect);
							}
						}
					});
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
