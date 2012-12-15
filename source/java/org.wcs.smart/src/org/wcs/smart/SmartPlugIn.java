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

import net.refractions.udig.catalog.CatalogPlugin;
import net.refractions.udig.catalog.IResolve;
import net.refractions.udig.catalog.IService;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.wcs.smart.ca.BasemapDefinition;
import org.wcs.smart.ca.ConservationAreaManager;
import org.wcs.smart.ca.DeleteConservationAreaHandler;
import org.wcs.smart.internal.Messages;

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
	public static final String SMART_EMPLOYEE_ICON = "org.wsc.smart.SMART_EMPLOYEE_ICON"; //$NON-NLS-1$

	/**
	 * Image descriptor key for non-smart user employee
	 */
	public static final String EMPLOYEE_ICON = "org.wsc.smart.EMPLOYEE_ICON"; //$NON-NLS-1$
	

	/**
	 * Image descriptor key for station
	 */
	public static final String STATION_ICON = "org.wsc.smart.STATION_ICON"; //$NON-NLS-1$
	
	/**
	 * Image descriptor for smart 48x48 icon
	 */
	public static final String SMART_48_ICON = "org.wcs.smart.SMART_48_ICON"; //$NON-NLS-1$
	
	
	/**
	 * Image descriptor for category icon
	 */
	public static final String CATEGORY_ICON = "org.wsc.smart.datamodel.CATEGORY_ICON"; //$NON-NLS-1$
	/**
	 * Image descriptor for attribute text icon
	 */
	public static final String ATTRIBUTE_TEXT_ICON= "org.wsc.smart.datamodel.ATTRIBUTE_TEXT_ICON"; //$NON-NLS-1$
	
	/**
	 * Image descriptor for attribute boolean icon
	 */
	public static final String ATTRIBUTE_BOOLEAN_ICON = "org.wsc.smart.datamodel.ATTRIBUTE_BOOLEAN_ICON"; //$NON-NLS-1$
	/**
	 * Image descriptor for attribute number icon
	 */
	public static final String ATTRIBUTE_NUMBER_ICON= "org.wsc.smart.datamodel.ATTRIBUTE_NUMBER_ICON"; //$NON-NLS-1$
	/**
	 * Image descriptor for attribute list icon
	 */
	public static final String ATTRIBUTE_LIST_ICON= "org.wsc.smart.datamodel.ATTRIBUTE_LIST_ICON"; //$NON-NLS-1$
	/**
	 * Image descriptor for attribute tree icon
	 */
	public static final String ATTRIBUTE_TREE_ICON= "org.wsc.smart.datamodel.ATTRIBUTE_TREE_ICON"; //$NON-NLS-1$
	/**
	 * Image descriptor for data model icon
	 */
	public static final String DATA_MODEL_ICON= "org.wsc.smart.datamodel.DATAMODEL_ICON"; //$NON-NLS-1$
			
	public BasemapDefinition defaultDefinition = null;
	
	/**
	 * The constructor
	 */
	public SmartPlugIn() {

	}
	
	public BasemapDefinition getBasemapSelection(){
		return this.defaultDefinition;
	}
	public void setBasemapSelection(BasemapDefinition definition){
		this.defaultDefinition = definition;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		System.setProperty("org.wcs.smart.version", context.getBundle().getVersion().toString()); //$NON-NLS-1$
		
		
		// add delete handler
		ConservationAreaManager.getInstance().addDeleteHandler(new DeleteConservationAreaHandler(), DeleteConservationAreaHandler.EXECUTE_ORDER);
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
	 * Get image descriptors for the clear button.
	 */
	@Override
	 protected void initializeImageRegistry(ImageRegistry reg) {
	     reg.put(CATEGORY_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/category_obj.gif")); //$NON-NLS-1$
	     reg.put(ATTRIBUTE_TEXT_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/attribute_text.png")); //$NON-NLS-1$
	     reg.put(ATTRIBUTE_NUMBER_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/attribute_number.png")); //$NON-NLS-1$
	     reg.put(ATTRIBUTE_BOOLEAN_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/attribute_boolean.png")); //$NON-NLS-1$
	     reg.put(ATTRIBUTE_LIST_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/attribute_list.png")); //$NON-NLS-1$
	     reg.put(ATTRIBUTE_TREE_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/attribute_tree.png")); //$NON-NLS-1$
	     reg.put(DATA_MODEL_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/smart16.gif")); //$NON-NLS-1$
	     
	     reg.put(SMART_48_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "/images/icons/smart48.gif")); //$NON-NLS-1$
	     reg.put(SMART_EMPLOYEE_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/user_orange.png")); //$NON-NLS-1$
	     reg.put(EMPLOYEE_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/user_green.png")); //$NON-NLS-1$
	     reg.put(STATION_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/station.png")); //$NON-NLS-1$
	}
	
	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static SmartPlugIn getDefault() {
		return plugin;
	}

	public static void log(int status, String message, Throwable t){
        getDefault().getLog().log(new Status(status, PLUGIN_ID, IStatus.OK, message, t));
	}
	
	public static void log(String message, Throwable t){
		int status = t instanceof Exception || message != null ? IStatus.ERROR : IStatus.WARNING;
		log(status, message, t);
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
			if (Display.getCurrent() != null && Display.getDefault().getActiveShell() != null){
				currentShell = Display.getDefault().getActiveShell();
			}
		}
		MessageDialog.openError(currentShell, Messages.SmartPlugIn_Error_Dialog_Title, message);
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
		MessageDialog.openError(Display.getCurrent().getActiveShell(), Messages.SmartPlugIn_Error_Dialog_Title, message);
		System.exit(1);
	}

}
