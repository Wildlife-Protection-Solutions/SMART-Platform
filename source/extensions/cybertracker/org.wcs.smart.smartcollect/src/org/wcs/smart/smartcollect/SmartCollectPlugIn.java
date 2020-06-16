/*
 * Copyright (C) 2020 Wildlife Conservation Society
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
package org.wcs.smart.smartcollect;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.osgi.framework.BundleContext;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.ConservationAreaManager;
import org.wcs.smart.ca.ICaDeleteHandler;
import org.wcs.smart.smartcollect.internal.Messages;
import org.wcs.smart.smartcollect.model.ISmartCollectLabelProvider;

/**
 * The activator class controls the plug-in life cycle
 */
public class SmartCollectPlugIn extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.wcs.smart.smartcollect"; //$NON-NLS-1$
	public static final String SMARTCOLLECT_ICON = "smartcollecticon"; //$NON-NLS-1$
	public static final String SMARTCOLLECT32_ICON = "smartcollecticon32"; //$NON-NLS-1$
	
	/**
	 * Version 1.0 of the database tables.
	 */
	public static final String DB_VERSION_1 = "1.0"; //$NON-NLS-1$
	
	/**
	 * The current version of the database tables
	 */
	public static final String DB_VERSION = DB_VERSION_1;
	
	// The shared instance
	private static SmartCollectPlugIn plugin;
	
	/**
	 * The constructor
	 */
	public SmartCollectPlugIn() {
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		
		SmartContext.INSTANCE.setClass(ISmartCollectLabelProvider.class, new SmartCollectLabelProvider());
		
		//delete packages when plugin deleted
		//waypoints are deleted elsewhere
		ICaDeleteHandler deleteHandler = new ICaDeleteHandler() {
			@Override
			public void beforeDelete(ConservationArea ca, Session session, IProgressMonitor monitor) throws Exception {
				Query<?> q = session.createQuery("delete from SmartCollectPackage where conservationArea = :ca"); //$NON-NLS-1$
				q.setParameter("ca", ca); //$NON-NLS-1$
				q.executeUpdate();
			}
		};
		ConservationAreaManager.getInstance().addDeleteHandler(deleteHandler, 1);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static SmartCollectPlugIn getDefault() {
		return plugin;
	}
	
	@Override
    protected void initializeImageRegistry(ImageRegistry reg) {
		reg.put(SMARTCOLLECT_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/smartcollect.16.png")); //$NON-NLS-1$
		reg.put(SMARTCOLLECT32_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/smartcollect.32.png")); //$NON-NLS-1$
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
				MessageDialog.openError(Display.getDefault().getActiveShell(), Messages.SmartCollectPlugIn_ErrorDialogTitle, message);
			}
		});
		
	}
}
