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
package org.wcs.smart.connect.dataqueue;

import java.io.File;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.hibernate.Session;
import org.osgi.framework.BundleContext;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ConservationAreaManager;
import org.wcs.smart.connect.ConnectDatastore;
import org.wcs.smart.connect.ConnectServerManager;
import org.wcs.smart.connect.ConnectServerManager.IConnectServerEventHandler;
import org.wcs.smart.connect.dataqueue.internal.CaDataQueueDeleteHandler;
import org.wcs.smart.connect.dataqueue.internal.process.DataQueueManager;
import org.wcs.smart.hibernate.SmartDB;

/**
 * The activator class controls the plug-in life cycle for the
 * smart connect data queue plugin.
 */
public class ConnectDataQueuePlugin extends AbstractUIPlugin {

	
	public static final String DATA_QUEUE_DIR = ConnectDatastore.CONNECT_FILESTORE_DIR + File.separator + "dataqueue";
	// The plug-in ID
	public static final String PLUGIN_ID = "org.wcs.smart.connect.dataqueue"; //$NON-NLS-1$
	public static final String DB_VERSION_1 = "1.0";
	public static final String DB_VERSION = DB_VERSION_1;

	public static final String ERROR_ICON = "org.wcs.smart.connect.dataqueue.icon.error"; //$NON-NLS-1$
	public static final String QUEUED_ICON = "org.wcs.smart.connect.dataqueue.icon.queued"; //$NON-NLS-1$
	public static final String PROCESSING_ICON = "org.wcs.smart.connect.dataqueue.icon.processing"; //$NON-NLS-1$
	public static final String COMPLETE_ICON = "org.wcs.smart.connect.dataqueue.icon.complete"; //$NON-NLS-1$
	public static final String COMPLETE_WARN_ICON = "org.wcs.smart.connect.dataqueue.icon.completewarn"; //$NON-NLS-1$
	
	public static final String DQ_ERROR_ICON = "org.wcs.smart.connect.dataqueue.icon.dqerror"; //$NON-NLS-1$
	public static final String DQ_OK_ICON = "org.wcs.smart.connect.dataqueue.icon.dqok"; //$NON-NLS-1$
	public static final String DQ_WARN_ICON = "org.wcs.smart.connect.dataqueue.icon.dqwarn"; //$NON-NLS-1$
	public static final String DQ_PROCESSING_ICON = "org.wcs.smart.connect.dataqueue.icon.dqprocessing"; //$NON-NLS-1$
	public static final String DQ_INACTIVE_ICON = "org.wcs.smart.connect.dataqueue.incon.dqinactive"; //$NON-NLS-1$
	
	// The shared instance
	private static ConnectDataQueuePlugin plugin;
	
	/**
	 * The constructor
	 */
	public ConnectDataQueuePlugin() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		
		ConservationAreaManager.getInstance().addDeleteHandler(new CaDataQueueDeleteHandler(), CaDataQueueDeleteHandler.EXECUTE_ORDER);
		ConnectServerManager.INSTANCE.addHandler(new IConnectServerEventHandler() {
			@Override
			public void beforeDelete(Session session) throws Exception{
				DataQueueManager.INSTANCE.deleteDataQueue(SmartDB.getCurrentConservationArea(), session);
			}
		});
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
	public static ConnectDataQueuePlugin getDefault() {
		return plugin;
	}

	/**
	 * Displays an error message to the user and logs the message.
	 * 
	 * @param message  Error message to display
	 * @param t exception to log
	 */
	public static void displayLog(final String message, Throwable t){
		log(message, t);
		SmartPlugIn.displayError(message, t);
	}
	
	public static void log(String message, Throwable t){
		int status = t instanceof Exception || message != null ? IStatus.ERROR : IStatus.WARNING;
		getDefault().getLog().log(new Status(status, PLUGIN_ID, IStatus.OK, message, t));
	}
	
	/**
	 * Get image descriptors for the clear button.
	 */
	@Override
	 protected void initializeImageRegistry(ImageRegistry reg) {
		super.initializeImageRegistry(reg);
	     reg.put(ERROR_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/error_icon.png")); //$NON-NLS-1$
	     reg.put(QUEUED_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/queued_icon.png")); //$NON-NLS-1$
	     reg.put(PROCESSING_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/processing_icon.png")); //$NON-NLS-1$
	     reg.put(COMPLETE_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/complete_icon.png")); //$NON-NLS-1$
	     reg.put(COMPLETE_WARN_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/completewarn_icon.png")); //$NON-NLS-1$
	    
	     reg.put(DQ_ERROR_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/dq_error.png")); //$NON-NLS-1$
	     reg.put(DQ_OK_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/dq_ok.png")); //$NON-NLS-1$
	     reg.put(DQ_WARN_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/dq_warn.png")); //$NON-NLS-1$
	     reg.put(DQ_PROCESSING_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/dq_processing.png")); //$NON-NLS-1$
	     reg.put(DQ_INACTIVE_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/dq_inactive.png")); //$NON-NLS-1$	     
	}
}
