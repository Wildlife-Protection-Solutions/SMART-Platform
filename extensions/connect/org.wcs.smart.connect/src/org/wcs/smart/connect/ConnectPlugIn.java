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
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ConservationAreaManager;
import org.wcs.smart.changetracking.ChangeLogInstaller;
import org.wcs.smart.connect.internal.CaConnectDeleteHandler;
import org.wcs.smart.connect.internal.CaReplicationDeleteHandler;
import org.wcs.smart.connect.internal.EmployeeDeleteHandler;
import org.wcs.smart.connect.model.ConnectUser;
import org.wcs.smart.connect.model.PasswordAesManager;
import org.wcs.smart.hibernate.SmartDB;

/**
 * The activator class controls the plug-in life cycle
 */
public class ConnectPlugIn extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.wcs.smart.connect"; //$NON-NLS-1$

	public static final String DB_VERSION_1 = "1.0"; //$NON-NLS-1$
	public static final String DB_VERSION = DB_VERSION_1; //current version
	
	// The shared instance
	private static ConnectPlugIn plugin;
	private EmployeeDeleteHandler employeeDelete = new EmployeeDeleteHandler();
	
	public static final String LOCAL_CHANGES_ICON = "org.wcs.smart.connect.local.changes"; //$NON-NLS-1$
	public static final String LOCAL_ERROR_ICON = "org.wcs.smart.connect.local.error"; //$NON-NLS-1$
	public static final String LOCAL_OK_ICON = "org.wcs.smart.connect.local.ok"; //$NON-NLS-1$
	public static final String LOCAL_PROCESSING_ICON = "org.wcs.smart.connect.server.processing"; //$NON-NLS-1$
	public static final String SERVER_CHANGES_ICON = "org.wcs.smart.connect.server.changes"; //$NON-NLS-1$
	public static final String SERVER_ERROR_ICON = "org.wcs.smart.connect.server.icon"; //$NON-NLS-1$
	public static final String SERVER_PROCESSING_ICON = "org.wcs.smart.connect.server.processing"; //$NON-NLS-1$
	public static final String SERVER_OK_ICON = "org.wcs.smart.connect.server.ok"; //$NON-NLS-1$
	public static final String REFRESH_ICON = "org.wcs.smart.connect.refresh"; //$NON-NLS-1$
	
	public static final String CONNECT_URL_PREF = "org.wcs.smart.connect.url.pref"; //$NON-NLS-1$
	
	public static final String CONNECT_ALERT_TYPE_CACHE_PREF = "org.wcs.smart.connect.alert.types"; //$NON-NLS-1$
	
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
		ConservationAreaManager.getInstance().addDeleteHandler(new CaReplicationDeleteHandler(), CaReplicationDeleteHandler.EXECUTE_ORDER);
		ConservationAreaManager.getInstance().addDeleteHandler(new CaConnectDeleteHandler(), CaConnectDeleteHandler.EXECUTE_ORDER);
		ConservationAreaManager.getInstance().addEmployeeListener(employeeDelete);
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
		
		ConservationAreaManager.getInstance().removeEmployeeListener(employeeDelete);
		
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
		SmartPlugIn.displayError(message, t);
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
	
	public static String decryptPassword(ConnectUser user, String passwordKey) throws Exception{
		if (user.getConnectPassword() == null){
			return null;
		}
		if (user.getConnectPassword().isEmpty()){
			return user.getConnectPassword();
		}
		return PasswordAesManager.getInstance().decryptPassword(user.getConnectPassword(), passwordKey);
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
	
    protected void initializeImageRegistry(ImageRegistry reg) {
        // spec'ed to do nothing
    	reg.put(LOCAL_CHANGES_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/local_changes.png")); //$NON-NLS-1$
    	reg.put(LOCAL_ERROR_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/local_error.png")); //$NON-NLS-1$
    	reg.put(LOCAL_OK_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/local_ok.png")); //$NON-NLS-1$
    	reg.put(LOCAL_PROCESSING_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/local_processing.png"));   //$NON-NLS-1$
    
	    reg.put(SERVER_CHANGES_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/server_changes.png")); //$NON-NLS-1$
		reg.put(SERVER_ERROR_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/server_error.png")); //$NON-NLS-1$
		reg.put(SERVER_OK_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/server_ok.png")); //$NON-NLS-1$
		reg.put(SERVER_PROCESSING_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/server_processing.png")); //$NON-NLS-1$
		reg.put(REFRESH_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/refresh.png")); //$NON-NLS-1$
    }
}
