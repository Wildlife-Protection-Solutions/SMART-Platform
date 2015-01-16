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
package org.wcs.smart.intelligence;

import java.io.File;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.wcs.smart.ca.ConservationAreaManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.intelligence.internal.Messages;
import org.wcs.smart.patrol.PatrolManager;

/**
 * The activator class controls the plug-in life cycle
 */
public class IntelligencePlugIn extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.wcs.smart.intelligence"; //$NON-NLS-1$

	//Version of Data Structures required for current implementation
	public static final String DB_VERSION = "3.2"; //$NON-NLS-1$
	
	
	// The shared instance
	private static IntelligencePlugIn plugin;

	/**
	 * Image descriptor key for intelligence filter icon
	 */
	public static final String INTELLIGENCE_FILTER_ICON = "org.wsc.smart.intelligence.INTELLIGENCE_FILTER"; //$NON-NLS-1$
	/**
	 * Image descriptor key for intelligence icon
	 */
	public static final String INTELLIGENCE_ICON = "org.wsc.smart.intelligence.INTELLIGENCE"; //$NON-NLS-1$

	public static final String  INTELLIGENCE_DIR = "intelligence"; //$NON-NLS-1$
	
	/**
	 * The constructor
	 */
	public IntelligencePlugIn() {
	}

	@Override
	protected void initializeImageRegistry(ImageRegistry reg) {
		super.initializeImageRegistry(reg);
		reg.put(INTELLIGENCE_FILTER_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/etool16/filter.png")); //$NON-NLS-1$			
		reg.put(INTELLIGENCE_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/etool16/intelligence.png")); //$NON-NLS-1$			
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		
		PatrolManager.getInstance().addDeleteHandler(new PatrolDeleteHandler(), PatrolDeleteHandler.EXECUTE_ORDER);
		ConservationAreaManager.getInstance().addDeleteHandler(new CaDeleteHandler(), CaDeleteHandler.EXECUTE_ORDER);
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
	public static IntelligencePlugIn getDefault() {
		return plugin;
	}


	/**
	 * Gets the location of intelligence files within the smart file store.
	 * If the directory does not exist it will create it.
	 * @return
	 */
	public File getIntelligenceDirectory() {
		File f =  new File(SmartDB.getCurrentConservationArea().getFileDataStoreLocation() 
				+ File.separator + INTELLIGENCE_DIR + File.separator);
		if (!f.exists()) {
			f.mkdir();
		}
		return f;
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
				MessageDialog.openError(Display.getDefault().getActiveShell(), Messages.IntelligencePlugIn_ErrorDialog_Title, message);
			}
			
		});
		
	}	
}
