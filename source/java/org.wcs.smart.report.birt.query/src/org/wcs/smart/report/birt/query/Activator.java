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
package org.wcs.smart.report.birt.query;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.wcs.smart.data.oda.smart.query.common.PatrolReportQueryListener;
import org.wcs.smart.data.oda.smart.ui.internal.Messages;
import org.wcs.smart.query.event.QueryEventManager;

public class Activator extends  AbstractUIPlugin{

	// The plug-in ID
	public static final String PLUGIN_ID = "org.wcs.smart.report.birt.query"; //$NON-NLS-1$

	public static final String TABLE_ICON = "org.wcs.smart.report.birt.query.table"; //$NON-NLS-1$
	
	// The shared instance
	private static Activator plugin;
	
	private static BundleContext context;

	private PatrolReportQueryListener queryListener = new PatrolReportQueryListener();

	
	static BundleContext getContext() {
		return context;
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext bundleContext) throws Exception {
		Activator.context = bundleContext;
		plugin = this;
		QueryEventManager.getInstance().addListener(queryListener);
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext bundleContext) throws Exception {
		Activator.context = null;
		plugin = null;
		QueryEventManager.getInstance().removeListener(queryListener);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
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
	 * Displays an error message to the user and logs the message.
	 * 
	 * @param message
	 *            Error message to display
	 * @param t
	 *            exception to log
	 */
	public static void displayLog(final String message, Throwable t) {
		log(message, t);
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				MessageDialog.openError(Display.getDefault().getActiveShell(),
						Messages.Activator_Error_DialogTitle, message);
			}
		});

	}

	@Override
	protected void initializeImageRegistry(ImageRegistry reg) {
		super.initializeImageRegistry(reg);
		reg.put(TABLE_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "icons/obj16/table.png")); //$NON-NLS-1$
	}
}
