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
package org.wcs.smart.entity;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.wcs.smart.ca.ConservationAreaManager;
import org.wcs.smart.entity.internal.Messages;

/**
 * The activator class controls the plug-in life cycle
 */
public class EntityPlugIn extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.wcs.smart.entity"; //$NON-NLS-1$

	//The current db version
	public static final String DB_VERSION = "1.0"; //$NON-NLS-1$
	
	
	// The shared instance
	private static EntityPlugIn plugin;
	
	private EntityCaDeleteHandler deleteCaHandler;
	
	public static final String ENTITY_TYPE_ICON = "org.wcs.smart.entitytype.icon"; //$NON-NLS-1$
	public static final String ENTITY_TYPE_WIZBAN = "org.wcs.smart.entitytype.wizban"; //$NON-NLS-1$
	public static final String CONFIGURATION_ICON = "org.wcs.smart.entity.configuration.icon"; //$NON-NLS-1$
	public static final String SIGHTINGS_ICON = "org.wcs.smart.entity.sightings.icon"; //$NON-NLS-1$
	
	/**
	 * The constructor
	 */
	public EntityPlugIn() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		
		deleteCaHandler = new EntityCaDeleteHandler();
		ConservationAreaManager.getInstance().addDeleteHandler(deleteCaHandler,
				EntityCaDeleteHandler.EXECUTE_ORDER);
		
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
	public static EntityPlugIn getDefault() {
		return plugin;
	}

    protected void initializeImageRegistry(ImageRegistry reg) {
     	super.initializeImageRegistry(reg);
     	reg.put(ENTITY_TYPE_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/entitytype.png")); //$NON-NLS-1$
     	reg.put(CONFIGURATION_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/configuration.png")); //$NON-NLS-1$
     	reg.put(ENTITY_TYPE_WIZBAN, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/wizban/entity.png")); //$NON-NLS-1$
     	reg.put(SIGHTINGS_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/sightings.png")); //$NON-NLS-1$
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
	public static void displayLog(final String message, Throwable t){
		log(message, t);
		Display.getDefault().syncExec(new Runnable(){

			@Override
			public void run() {
				MessageDialog.openError(Display.getDefault().getActiveShell(), Messages.EntityPlugIn_ErrorDialogTitle, message);
			}
			
		});
		
	}
}
