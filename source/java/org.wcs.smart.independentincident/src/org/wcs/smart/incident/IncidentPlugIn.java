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
package org.wcs.smart.incident;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.hibernate.Session;
import org.osgi.framework.BundleContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.wcs.smart.SmartContext;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.TelemetryManager;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.ConservationAreaManager;
import org.wcs.smart.incident.internal.Messages;
import org.wcs.smart.incident.patrol.IncidentToPatrolProcessor;
import org.wcs.smart.incident.patrol.IncidentToPatrolProcessorJob;
import org.wcs.smart.incident.ui.IncidentEditor;
import org.wcs.smart.patrol.PatrolEventManager;
import org.wcs.smart.patrol.PatrolEventManager.EventType;
import org.wcs.smart.patrol.PatrolEventManager.IPatrolEventListener;

/**
 * The activator class for the incident plugin.
 * 
 */
public class IncidentPlugIn extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.wcs.smart.independentincident"; //$NON-NLS-1$

	public static final String INCIDENT_ICON = "org.wcs.smart.incident.ICON"; //$NON-NLS-1$
	public static final String INTEGRATE_ICON = "org.wcs.smart.incident.integrate.ICON"; //$NON-NLS-1$
	public static final String INCIDENT32_ICON = "org.wcs.smart.incident.ICON32"; //$NON-NLS-1$

	public static final String DB_VERSION_2 = "2.0"; //$NON-NLS-1$
	public static final String DB_VERSION_1 = "1.0"; //$NON-NLS-1$
	public static final String DB_VERSION = DB_VERSION_2;
	
	// The shared instance
	private static IncidentPlugIn plugin;
	
	/**
	 * The constructor
	 */
	public IncidentPlugIn() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		
		SmartContext.INSTANCE.setClass(IIncidentLabelProvider.class, new IncidentLabelProvider());
		
		IPatrolEventListener scheduleIncidentProcessing = new IPatrolEventListener() {			
			@Override
			public void eventFired(int attributeChanged, Object source) {
				//wait 5 seconds for other events then run 
				//processing job
				IncidentToPatrolProcessorJob.getInstance().schedule(5_000);
			}
		};
		
		PatrolEventManager.getInstance().addListener(EventType.PATROL_ADDED, scheduleIncidentProcessing);
		PatrolEventManager.getInstance().addListener(EventType.PATROL_MODIFIED, scheduleIncidentProcessing);
		PatrolEventManager.getInstance().addListener(EventType.PATROL_SAVED, scheduleIncidentProcessing);
		
		
		IEventBroker eventBroker = EclipseContextFactory.getServiceContext(IncidentPlugIn.getDefault().getBundle().getBundleContext()).get(IEventBroker.class);
		eventBroker.subscribe(SmartPlugIn.E4_SYNC_DOWNLOAD_DONE, new EventHandler() {
			
			@Override
			public void handleEvent(Event event) {
				Object[] data = (Object[]) event.getProperty(IEventBroker.DATA);
				if (data == null) return;
				if (data[1] == null || !(data[1] instanceof ConservationArea)) return;
				if (data[0] == null || !(data[0] instanceof Session)) return;
				
				ConservationArea ca = (ConservationArea)data[1];
				Session session = (Session) data[0];
					
				try {
					(new IncidentToPatrolProcessor(ca, false)).doWork(session);
				} catch (Exception e) {
					IncidentPlugIn.displayLog(e.getMessage(), e);
				}
			}
		});
		
		ConservationAreaManager.getInstance().addDeleteHandler(new IncidentCaDeleteHandler(), 32);

		TelemetryManager.INSTANCE.registerPartId(IncidentEditor.ID, TelemetryManager.Key.INCIDENT_VIEW);

	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static IncidentPlugIn getDefault() {
		return plugin;
	}

	@Override
    protected void initializeImageRegistry(ImageRegistry reg) {
		reg.put(INCIDENT_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/incident.png")); //$NON-NLS-1$
		reg.put(INTEGRATE_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/integrate_incident.png")); //$NON-NLS-1$
		reg.put(INCIDENT32_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/incident@2x.png")); //$NON-NLS-1$
	}
	
	public static void log(String message, Throwable t){
		int status = t instanceof Exception || message != null ? IStatus.ERROR : IStatus.WARNING;
        getDefault().getLog().log(new Status(status, PLUGIN_ID, IStatus.OK, message, t));
	}

	/**
	 * Displays an error message to the user and logs the
	 * message.
	 * 
	 * @param message  Error message to display
	 * @param t exception to log
	 */
	public static void displayLog(final String message, Throwable t){
		log(message, t);
		Display.getDefault().syncExec(new Runnable(){
			@Override
			public void run() {
				MessageDialog.openError(Display.getDefault().getActiveShell(), Messages.IncidentPlugIn_ErrorDialogTitle, message);
			}
		});
		
	}
	
}
