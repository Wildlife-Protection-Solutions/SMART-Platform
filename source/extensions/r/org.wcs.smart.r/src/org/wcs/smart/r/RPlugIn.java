/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.r;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.wcs.smart.ca.ConservationAreaManager;
import org.wcs.smart.r.plugin.DeleteCaHandler;

/**
 * The activator class controls the plug-in life cycle
 */
public class RPlugIn extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.wcs.smart.r"; //$NON-NLS-1$

	// The shared instance
	private static RPlugIn plugin;
	
	/*
	 * As the plugin evolves updates may require changes to the
	 * database.  The plugin database version is stored in the database
	 * so the upgrade script know which version to upgrade from.
	 * Initially there will be no upgrades to perform so the these
	 * two versions will be the same.
	 */
	/**
	 * Version 1.0 of the database tables.
	 */
	public static final String DB_VERSION_1 = "1.0"; //$NON-NLS-1$
	
	/**
	 * The current version of the database tables
	 */
	public static final String DB_VERSION = DB_VERSION_1;
  
	public static final String ICON_UP = "org.wcs.smart.r.ICON_UP"; //$NON-NLS-1$
	public static final String ICON_DOWN = "org.wcs.smart.r.ICON_DOWN"; //$NON-NLS-1$
	public static final String ICON_R = "org.wcs.smart.r.ICON_R"; //$NON-NLS-1$
	
	
	/**
	 * The constructor
	 */
	public RPlugIn() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		
		ConservationAreaManager.getInstance().addDeleteHandler(new DeleteCaHandler(), DeleteCaHandler.EXECUTE_ORDER);
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
	public static RPlugIn getDefault() {
		return plugin;
	}

	/**
	 * Writes an error message to the log file.  
	 * @param message the error message to write to the log file. 
	 * @param t the exception thrown or null 
	 */
	public static void log(String message, Throwable t){
		int status = t instanceof Exception || message != null ? IStatus.ERROR : IStatus.WARNING;
        getDefault().getLog().log(new Status(status, PLUGIN_ID, IStatus.OK, message, t));
	}
	
	/**
	 * Logs the error to the log file and then displays 
	 * the error message to the user.  Can be called from
	 * outside the Display thread.
	 * 
	 * @param message the error message to display
	 * @param t the exception to log or null
	 */
	public static void displayLog(final String message, Throwable t){
		log(message, t);
		Display.getDefault().syncExec(new Runnable(){
			@Override
			public void run() {
				MessageDialog.openError(Display.getDefault().getActiveShell(), "Error", message); //$NON-NLS-1$
			}
			
		});
		
	}	
	
	@Override
	 protected void initializeImageRegistry(ImageRegistry reg) {
		super.initializeImageRegistry(reg);
	    reg.put(ICON_DOWN, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/down.png")); //$NON-NLS-1$
	    reg.put(ICON_UP, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/up.png")); //$NON-NLS-1$
	    reg.put(ICON_R, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/rlogo_16.png")); //$NON-NLS-1$
	}
	
}

