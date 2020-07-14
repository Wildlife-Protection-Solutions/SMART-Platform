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
package org.wcs.smart.asset;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.ConservationAreaManager;

/**
 * The activator class controls the plug-in life cycle
 */
public class AssetPlugIn extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.wcs.smart.asset"; //$NON-NLS-1$
	
	public static final String DB_VERSION_1 = "1.0"; //$NON-NLS-1$
	public static final String DB_VERSION_2 = "2.0"; //$NON-NLS-1$

	public static final String DB_VERSION = DB_VERSION_2; //current database version
	
	// The shared instance
	private static AssetPlugIn plugin;
	
	public static final String ICON_STATUS_ACTIVE = "org.wcs.smart.asset.status.active"; //$NON-NLS-1$
	public static final String ICON_STATUS_INACTIVE = "org.wcs.smart.asset.status.inactive"; //$NON-NLS-1$
	public static final String ICON_STATUS_RETIRED = "org.wcs.smart.asset.status.retired"; //$NON-NLS-1$
	public static final String ICON_VALIDATE = "org.wcs.smart.asset.validate"; //$NON-NLS-1$
	public static final String ICON_VALIDATE_ALL = "org.wcs.smart.asset.validateall"; //$NON-NLS-1$
	
	public static final String ICON_IMPORT_COMPLETE_WARN = "org.wcs.smart.asset.import.completewarn"; //$NON-NLS-1$
	public static final String ICON_IMPORT_COMPLETE = "org.wcs.smart.asset.import.complete"; //$NON-NLS-1$
	public static final String ICON_IMPORT_INCOMPLETE = "org.wcs.smart.asset.import.incomplete"; //$NON-NLS-1$
	
	public static final String ICON_SETTINGS = "org.wcs.smart.asset.settings"; //$NON-NLS-1$
	
	public static final String ICON_STATION = "org.wcs.smart.asset.station"; //$NON-NLS-1$
	public static final String ICON_STATION_LOCATION = "org.wcs.smart.asset.station.location"; //$NON-NLS-1$
	
	public static final String ICON_MERGE = "org.wcs.smart.asset.merge"; //$NON-NLS-1$
	
	public static final String ICON_STYLE_DEFAULT = "org.wcs.smart.asset.style.default"; //$NON-NLS-1$
	
	public static final String ICON_ASSET = "org.wcs.smart.asset.asset"; //$NON-NLS-1$
	public static final String ICON_ASSET_MAP = "org.wcs.smart.asset.map"; //$NON-NLS-1$
	
	/**
	 * The constructor
	 */
	public AssetPlugIn() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		
		SmartContext.INSTANCE.setClass(IAssetLabelProvider.class, new AssetCoreLabelProvider());
		ConservationAreaManager.getInstance().addDeleteHandler(new DeleteCaHandler(), DeleteCaHandler.EXECUTE_ORDER);
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
	public static AssetPlugIn getDefault() {
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
		
		reg.put(ICON_STATUS_ACTIVE, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/status_active.png")); //$NON-NLS-1$);
		reg.put(ICON_STATUS_INACTIVE, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/status_inactive.png")); //$NON-NLS-1$);
		reg.put(ICON_STATUS_RETIRED, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/status_retired.png")); //$NON-NLS-1$);

		reg.put(ICON_IMPORT_COMPLETE_WARN, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/import_complete_warn.png")); //$NON-NLS-1$);
		reg.put(ICON_IMPORT_COMPLETE, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/import_complete.png")); //$NON-NLS-1$);
		reg.put(ICON_IMPORT_INCOMPLETE, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/import_incomplete.png")); //$NON-NLS-1$);
		reg.put(ICON_SETTINGS, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/settings.png")); //$NON-NLS-1$);
		reg.put(ICON_STATION, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/station.png")); //$NON-NLS-1$);
		reg.put(ICON_STATION_LOCATION, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/location.png")); //$NON-NLS-1$);
		reg.put(ICON_VALIDATE, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/validate.png")); //$NON-NLS-1$);
		reg.put(ICON_VALIDATE_ALL, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/validate_all.png")); //$NON-NLS-1$);
		reg.put(ICON_MERGE, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/merge.png")); //$NON-NLS-1$);
		reg.put(ICON_STYLE_DEFAULT, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/style_default.png")); //$NON-NLS-1$);
		reg.put(ICON_ASSET, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/asset.png")); //$NON-NLS-1$);
		reg.put(ICON_ASSET_MAP, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/asset_map.png")); //$NON-NLS-1$);
	}
}
