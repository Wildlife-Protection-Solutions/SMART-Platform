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
package org.wcs.smart.cybertracker;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ConservationAreaManager;
import org.wcs.smart.cybertracker.model.CyberTrackerProperties;

/**
 * The activator class controls the plug-in life cycle
 */
public class CyberTrackerPlugIn extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.wcs.smart.cybertracker"; //$NON-NLS-1$

	public static final String DB_VERSION_3_0 = "3.0"; //$NON-NLS-1$
	public static final String DB_VERSION_4_0 = "4.0"; //$NON-NLS-1$
	public static final String DB_VERSION = DB_VERSION_4_0; //current active version
	
	//image registry key for cybertracker dialog image
	public static final String CT_WIZARD_BANNER = "org.wcs.smart.cybertracker.wizban"; //$NON-NLS-1$

	/**
	 * Extension id
	 */
	public static final String DATASOURCE_EXTENSION_ID = "org.wcs.smart.cybertracker.datasource"; //$NON-NLS-1$
	public static final String ALERT_EXTENSION_ID = "org.wcs.smart.cybertracker.alert"; //$NON-NLS-1$
	
	/* 
	 * cybertracker encoding protocol 
	 */
	private static final String JSON_ENCODING_TYPE_KEY = "org.wcs.smart.connect.cybertracker.json.protocol";
	
	
	// The shared instance
	private static CyberTrackerPlugIn plugin;
	
	/**
	 * The constructor
	 */
	public CyberTrackerPlugIn() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;

		ConservationAreaManager.getInstance().addDeleteHandler(new CyberTrackerCaDeleteHandler(), CyberTrackerCaDeleteHandler.EXECUTE_ORDER);

		Job j = new CyberTrackerStartupJob();
		j.setRule(SmartPlugIn.PLUGIN_START_MUTEX);
		j.schedule();
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
	public static CyberTrackerPlugIn getDefault() {
		return plugin;
	}

	/**
	 * Log an error message
	 * 
	 * @param message error message
	 * @param t exception thrown or null
	 */
	public static void log(String message, Throwable t){
        getDefault().getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, message, t));
	}
	
	/**
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#initializeImageRegistry(org.eclipse.jface.resource.ImageRegistry)
	 */
	@Override
	protected void initializeImageRegistry(ImageRegistry reg) {
	     reg.put(CT_WIZARD_BANNER, imageDescriptorFromPlugin(PLUGIN_ID, "images/wizban/cybertracker.png")); //$NON-NLS-1$
	}
	
	public static void displayInfo(final String title, final String message) {
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				MessageDialog.openInformation(Display.getDefault().getActiveShell(), title, message);
			}
		});
	}

	public static void displayError(final String title, final String message, Throwable ex) {
		log(message, ex);
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				MessageDialog.openError(Display.getDefault().getActiveShell(), title, message);
			}
		});
	}
	
	
	/**
	 * Reads the data encoding type for cybertracker protocol from preference
	 * store.
	 * 
	 * @return
	 */
	public CyberTrackerProperties.Protocol getDefaultCtEncoding(){
		if (getDefault().getPreferenceStore().contains(JSON_ENCODING_TYPE_KEY)){
			try{
				CyberTrackerProperties.Protocol.valueOf(getDefault().getPreferenceStore().getString(JSON_ENCODING_TYPE_KEY));
			}catch (Exception ex){
				log("Invalid cybertracker protocol: " +getDefault().getPreferenceStore().getString(JSON_ENCODING_TYPE_KEY), ex); //$NON-NLS-1$
			}
			
		}
		//default to compressed JSON
//		return CyberTrackerProperties.Protocol.GEOJSON_COMPRESSED;
		return CyberTrackerProperties.Protocol.GEOJSON;
	}
}
