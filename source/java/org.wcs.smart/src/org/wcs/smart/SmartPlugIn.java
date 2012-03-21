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
package org.wcs.smart;

import java.util.List;
import java.util.Locale;

import net.refractions.udig.catalog.CatalogPlugin;
import net.refractions.udig.catalog.IResolve;
import net.refractions.udig.catalog.IService;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.wcs.smart.ca.ConservationAreaManager;

/**
 * The activator class controls the plug-in life cycle
 */
public class SmartPlugIn extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.wcs.smart"; //$NON-NLS-1$

	// The shared instance
	private static SmartPlugIn plugin;
	
	/**
	 * Image descriptor key for smart employee
	 */
	public static final String SMART_EMPLOYEE_ICON = "org.wsc.smart.SMART_EMPLOYEE"; //$NON-NLS-1$

	/**
	 * Image descriptor key for non-smart user employee
	 */
	public static final String EMPLOYEE_ICON = "org.wsc.smart.EMPLOYEE"; //$NON-NLS-1$
	
	/**
	 * The constructor
	 */
	public SmartPlugIn() {

	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		
		// add delete handler
		ConservationAreaManager.getInstance().addDeleteHandler(new DeleteConservationAreaHandler(),DeleteConservationAreaHandler.EXECUTE_ORDER);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		try {
			//TODO: review this requirement
			// clean out the catalog
			List<IResolve> members = CatalogPlugin.getDefault().getLocalCatalog().members(null);
			for (IResolve member : members) {
				CatalogPlugin.getDefault().getLocalCatalog().remove((IService) member.resolve(IService.class, null));
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static SmartPlugIn getDefault() {
		return plugin;
	}

	public static void log(String message, Throwable t){
		int status = t instanceof Exception || message != null ? IStatus.ERROR : IStatus.WARNING;
        getDefault().getLog().log(new Status(status, PLUGIN_ID, IStatus.OK, message, t));
	}
	
	public static void logInfo(String message){
		getDefault().getLog().log(new Status(IStatus.OK, PLUGIN_ID, IStatus.INFO, message, null));
	}
	

	
	/**
	 * Displays an error message to the user and logs the
	 * message.
	 * 
	 * @param message  Error message to display
	 * @param t exception to log
	 */
	public static void displayLog(Shell currentShell, String message, Throwable t){
		log(message, t);
		if (currentShell == null){
			if (Display.getCurrent() != null && Display.getCurrent().getActiveShell() != null){
				currentShell = Display.getCurrent().getActiveShell();
			}
		}
		MessageDialog.openError(currentShell, "Error", message);
	}
	
	/**
	 * Displays an error message to the user, logs the error and
	 * exits the program with an error code of 1.
	 * 
	 * @param message the message to display to the user
	 * @param t optionally exception
	 */
	public static void displayLogExit(String message, Throwable t){
		log(message, t);
		MessageDialog.openError(Display.getCurrent().getActiveShell(), "Error", message);
		System.exit(1);
	}
	
	
	public static String lookupLocaleName(String nl){
		String[] bits = nl.split("_");
		for (int i = 0; i < Locale.getAvailableLocales().length;i++){
			Locale l  = Locale.getAvailableLocales()[i];
			if (bits[0].equals(l.getLanguage()) && 
					bits[1].equals(l.getCountry())){
				return l.getDisplayName();
			}
		}
		return null;
		
	}
	
	

}
