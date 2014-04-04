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
package org.wcs.smart.report;

import java.io.File;
import java.io.IOException;

import org.eclipse.birt.report.designer.ui.ReportPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.ConservationAreaManager;
import org.wcs.smart.query.event.QueryEventManager;
import org.wcs.smart.report.internal.Messages;
import org.wcs.smart.report.library.SmartBirtLibrary;

/**
 * SMART BIRT Reporting plugin
 * 
 * @author egouge
 * @since 1.0.0
 */
public class ReportPlugIn extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.wcs.smart.report"; //$NON-NLS-1$

	public static final String REPORT_DIR = "reports"; //$NON-NLS-1$

	/**
	 * The main query icon
	 */
	public static final String REPORT_ICON = "org.wcs.smart.query.reporticon"; //$NON-NLS-1$

	private ReportQueryListener queryListener = new ReportQueryListener();


	private ReportCaDeleteHandler deleteHandler = new ReportCaDeleteHandler();
	private ReportEmployeeListener employeeListener = new ReportEmployeeListener();
	
	// The shared instance
	private static ReportPlugIn plugin;

	/**
	 * The constructor
	 */
	public ReportPlugIn() {
	}


	@Override
    protected void initializeImageRegistry(ImageRegistry reg) {
		reg.put(REPORT_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/report.png")); //$NON-NLS-1$
	}
	
	/**
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		
		ConservationAreaManager.getInstance().addDeleteHandler(deleteHandler, ReportCaDeleteHandler.EXECUTE_ORDER);
		ConservationAreaManager.getInstance().addEmployeeListener(employeeListener);
		QueryEventManager.getInstance().addListener(queryListener);
	}

	public static void initReports() {

		try {
			ReportPlugin
					.getDefault()
					.getPreferenceStore()
					.setValue(
							ReportPlugin.RESOURCE_PREFERENCE,
							SmartBirtLibrary.getInstance().getLibraryLocation()
									.getCanonicalPath());
		} catch (IOException e) {
			displayLog(
					Messages.ReportPlugIn_Error_InitializingParams+ e.getLocalizedMessage(), e);
		}
	}

	public static File getReportDirectory(ConservationArea ca) {
		return new File(ca.getFileDataStoreLocation()
				+ File.separator
				+ ReportPlugIn.REPORT_DIR + File.separator);
	}

	/**
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		ConservationAreaManager.getInstance().removeEmployeeListener(employeeListener);
		QueryEventManager.getInstance().removeListener(queryListener);
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static ReportPlugIn getDefault() {
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
	 * Logs the given error to the error log.
	 * 
	 * @param message
	 *            message
	 * @param t
	 *            error
	 */
	public static void logSql(String sql) {
		int status = IStatus.INFO;
		getDefault().getLog().log(
				new Status(status, PLUGIN_ID, IStatus.OK, sql, null));
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
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				MessageDialog.openError(Display.getDefault().getActiveShell(),
						Messages.ReportPlugIn_Error_DialogTitle, message);
			}
		});

	}
	
	
	
}
