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
package org.wcs.smart.connect;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.hibernate.Session;
import org.osgi.framework.BundleContext;
import org.wcs.smart.ca.ConservationAreaManager;
import org.wcs.smart.changetracking.ChangeLogInstaller;
import org.wcs.smart.connect.model.ConnectUser;
import org.wcs.smart.connect.model.PasswordAesManager;
import org.wcs.smart.connect.replication.DerbyReplicationManager;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;

/**
 * The activator class controls the plug-in life cycle
 */
public class ConnectPlugIn extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.wcs.smart.connect"; //$NON-NLS-1$

	public static final String DB_VERSION_1 = "1.0";
	public static final String DB_VERSION = DB_VERSION_1; //current version
	
	// The shared instance
	private static ConnectPlugIn plugin;
	
	/**
	 * The constructor
	 */
	public ConnectPlugIn() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		ConservationAreaManager.getInstance().addDeleteHandler(new CaConnectDeleteHandler(), CaConnectDeleteHandler.EXECUTE_ORDER);
		ChangeLogInstaller.INSTANCE.setEnabled(true);
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
		
		SmartConnect.closeAll();
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static ConnectPlugIn getDefault() {
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
		Display.getDefault().syncExec(new Runnable(){
			@Override
			public void run() {
				MessageDialog.openError(Display.getDefault().getActiveShell(),
						"Error", message);
			}
		});
	}
	
	public static void log(int status, String message, Throwable t){
        getDefault().getLog().log(new Status(status, PLUGIN_ID, IStatus.OK, message, t));
	}
	
	public static void log(String message, Throwable t){
		int status = t instanceof Exception || message != null ? IStatus.ERROR : IStatus.WARNING;
		log(status, message, t);
	}

	/**
	 * Decrypt a connect user password.
	 * 
	 * @param user
	 * @return
	 * @throws Exception
	 */
	public static String decryptPassword(ConnectUser user) throws Exception{
		if (user.getConnectPassword() == null){
			return null;
		}
		if (user.getConnectPassword().isEmpty()){
			return user.getConnectPassword();
		}
		String key = SmartDB.getPlainTextPassword();
		return PasswordAesManager.getInstance().decryptPassword(user.getConnectPassword(), key);
	}

	/**
	 * Encrypt a password.
	 * 
	 * @param password
	 * @return
	 * @throws Exception
	 */
	public static String encryptPassword(String password) throws Exception{
		String key = SmartDB.getPlainTextPassword();
		return PasswordAesManager.getInstance().encryptPassword(password, key);
	}
}
