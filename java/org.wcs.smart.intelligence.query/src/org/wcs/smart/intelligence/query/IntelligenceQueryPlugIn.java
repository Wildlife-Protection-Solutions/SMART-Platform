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
package org.wcs.smart.intelligence.query;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.wcs.smart.SmartContext;
import org.wcs.smart.intelligence.query.internal.IntelligenceQueryLabelProvider;
import org.wcs.smart.intelligence.query.internal.Messages;
import org.wcs.smart.intelligence.query.model.IntelligenceQueryColumnProvider;

/**
 * The activator class controls the plug-in life cycle
 */
public class IntelligenceQueryPlugIn extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.wcs.smart.intelligence.query"; //$NON-NLS-1$

	public static final String INTEL_SUMMARY_ICON = "org.wcs.smart.intelligence.query.summary"; //$NON-NLS-1$
	public static final String INTEL_RECORD_ICON = "org.wcs.smart.intelligence.query.record"; //$NON-NLS-1$
	
	public static final String DB_VERSION_1 = "1.0"; //$NON-NLS-1$
	public static final String DB_VERSION_2 = "2.0"; //$NON-NLS-1$
	public static final String DB_VERSION = DB_VERSION_2; //current version
	
	// The shared instance
	private static IntelligenceQueryPlugIn plugin;
	
	/**
	 * The constructor
	 */
	public IntelligenceQueryPlugIn() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		
		SmartContext.INSTANCE.setClass(IIntelligenceQueryLabelProvider.class, new IntelligenceQueryLabelProvider());
		SmartContext.INSTANCE.setClass(IIntelligenceQueryColumnProvider.class, new IntelligenceQueryColumnProvider());
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
	public static IntelligenceQueryPlugIn getDefault() {
		return plugin;
	}

	@Override
    protected void initializeImageRegistry(ImageRegistry reg) {
		reg.put(INTEL_SUMMARY_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/intelligence_summary.png")); //$NON-NLS-1$
		reg.put(INTEL_RECORD_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/intelligence_record.png")); //$NON-NLS-1$	
	}
	

	/**
	 * Logs the given error to the error log.
	 * 
	 * @param message
	 *            message
	 * @param t
	 *            error
	 */
	public static void log(String message, Throwable t) {
		int status = t instanceof Exception || message != null ? IStatus.ERROR
				: IStatus.WARNING;
		getDefault().getLog().log(
				new Status(status, PLUGIN_ID, IStatus.OK, message, t));
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
				MessageDialog.openError(Display.getDefault().getActiveShell(),
						Messages.IntelligenceQueryPlugIn_ErrorDialogTitle, message);
			}
			
		});
		
	}	
}
