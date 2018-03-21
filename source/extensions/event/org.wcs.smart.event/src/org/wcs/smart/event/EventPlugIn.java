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
package org.wcs.smart.event;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.wcs.smart.ca.ConservationAreaManager;

/**
 * The activator class controls the plug-in life cycle
 */
public class EventPlugIn extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.wcs.smart.event"; //$NON-NLS-1$

	// The shared instance
	private static EventPlugIn plugin;
	
	public static final String ICON_ACTION_TYPE = "org.wcs.smart.event.icon.actiontype"; //$NON-NLS-1$
	public static final String ICON_ACTION = "org.wcs.smart.event.icon.action"; //$NON-NLS-1$
	public static final String ICON_ACTION_EVENT = "org.wcs.smart.event.icon.actionevent"; //$NON-NLS-1$
	public static final String ICON_FILTER = "org.wcs.smart.event.icon.filter"; //$NON-NLS-1$
	public static final String ICON_DELETE_MINI = "org.wcs.smart.event.icon.deletemini"; //$NON-NLS-1$
	
	public static final String DB_VERSION_1 = "1.0.0"; //$NON-NLS-1$
	
	public static final String DB_VERSION = DB_VERSION_1;
	/**
	 * The constructor
	 */
	public EventPlugIn() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		
		ConservationAreaManager.getInstance().addDeleteHandler(new ConservationAreaDeleteHandler(), ConservationAreaDeleteHandler.EXECUTE_ORDER);
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
	public static EventPlugIn getDefault() {
		return plugin;
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
				MessageDialog.openError(Display.getDefault().getActiveShell(), "Error", message); //$NON-NLS-1$
			}
			
		});
		
	}	
	
	@Override
	protected void initializeImageRegistry(ImageRegistry reg) {
		super.initializeImageRegistry(reg);
		
		reg.put(ICON_ACTION_TYPE, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/action_type.png")); //$NON-NLS-1$
		reg.put(ICON_ACTION, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/action.png")); //$NON-NLS-1$
		reg.put(ICON_ACTION_EVENT, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/action_event.png")); //$NON-NLS-1$
		reg.put(ICON_FILTER, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/event_filter.png")); //$NON-NLS-1$
		reg.put(ICON_DELETE_MINI, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/delete_mini.png")); //$NON-NLS-1$
	}
}
