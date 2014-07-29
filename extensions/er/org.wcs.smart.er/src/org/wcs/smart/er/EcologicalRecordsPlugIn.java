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
package org.wcs.smart.er;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
/**
 * The activator class controls the plug-in life cycle
 */
public class EcologicalRecordsPlugIn extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.wcs.smart.er"; //$NON-NLS-1$

	//The current db version
	public static final String DB_VERSION = "1.0"; //$NON-NLS-1$
	
	// The shared instance
	private static EcologicalRecordsPlugIn plugin;
	
	public static final String SURVEY_DESIGN_ICON = "org.wcs.smart.surveydesign.icon"; //$NON-NLS-1$
	public static final String MISSION_ICON = "org.wcs.smart.fieldsurvey.icon"; //$NON-NLS-1$
	public static final String SURVEY_ICON = "org.wcs.smart.survey.icon"; //$NON-NLS-1$
	
	/**
	 * The constructor
	 */
	public EcologicalRecordsPlugIn() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
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
	public static EcologicalRecordsPlugIn getDefault() {
		return plugin;
	}

    public static void log(String message, Throwable t){
		int status = t instanceof Exception || message != null ? IStatus.ERROR : IStatus.WARNING;
        getDefault().getLog().log(new Status(status, PLUGIN_ID, IStatus.OK, message, t));
	}
	
    
    protected void initializeImageRegistry(ImageRegistry reg) {
     	super.initializeImageRegistry(reg);
     	reg.put(SURVEY_DESIGN_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/survey_design.png")); //$NON-NLS-1$
     	reg.put(SURVEY_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/survey.png")); //$NON-NLS-1$
     	reg.put(MISSION_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/mission.png")); //$NON-NLS-1$
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
				MessageDialog.openError(Display.getDefault().getActiveShell(), "Error", message);
			}
			
		});
		
	}
}
