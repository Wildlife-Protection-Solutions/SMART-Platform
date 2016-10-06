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
package org.wcs.smart.i2;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.ConservationAreaManager;
import org.wcs.smart.i2.handlers.DeleteCaHandler;
import org.wcs.smart.i2.internal.IntelligenceLabelProviderImpl;


/**
 * The activator class controls the plug-in life cycle
 */
public class Intelligence2PlugIn extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.wcs.smart.i2"; //$NON-NLS-1$

	//Version of Data Structures required for current implementation
	public static final String DB_VERSION_31 = "3.1"; //$NON-NLS-1$
	public static final String DB_VERSION_32 = "3.2"; //$NON-NLS-1$
	public static final String DB_VERSION_40 = "4.0"; //$NON-NLS-1$
	public static final String DB_VERSION = DB_VERSION_40; //current version
	
	public static final String ICON_ENTITY = "org.wcs.smart.i2.icon.entity"; //$NON-NLS-1$
	public static final String ICON_RECORD = "org.wcs.smart.i2.icon.record"; //$NON-NLS-1$
	public static final String ICON_EDIT = "org.wcs.smart.i2.icon.edit"; //$NON-NLS-1$
	public static final String ICON_REFRESH = "org.wcs.smart.i2.icon.refresh"; //$NON-NLS-1$
	public static final String ICON_RECORD_NEW = "org.wcs.smart.i2.icon.record.new"; //$NON-NLS-1$
	public static final String ICON_WORKINGSET_NEW = "org.wcs.smart.i2.icon.workingset.new"; //$NON-NLS-1$
	public static final String ICON_WORKINGSET_SELECT = "org.wcs.smart.i2.icon.workingset.select"; //$NON-NLS-1$
	public static final String ICON_WORKINGSET_COPY = "org.wcs.smart.i2.icon.workingset.copy"; //$NON-NLS-1$
	public static final String ICON_PDF = "org.wcs.smart.i2.icon.print.pdf"; //$NON-NLS-1$
	
	// The shared instance
	private static Intelligence2PlugIn plugin;

	
	/**
	 * The constructor
	 */
	public Intelligence2PlugIn() {
	}

	@Override
	protected void initializeImageRegistry(ImageRegistry reg) {
		super.initializeImageRegistry(reg);
		
		reg.put(ICON_ENTITY, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/entity.png")); //$NON-NLS-1$);
		reg.put(ICON_RECORD, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/script.png")); //$NON-NLS-1$);
		reg.put(ICON_RECORD_NEW, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/script_add.png")); //$NON-NLS-1$);
		reg.put(ICON_EDIT, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/edit.png")); //$NON-NLS-1$);
		reg.put(ICON_REFRESH, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/refresh.png")); //$NON-NLS-1$);
		reg.put(ICON_WORKINGSET_NEW, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/working_set_add.png")); //$NON-NLS-1$);
		reg.put(ICON_WORKINGSET_SELECT, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/working_set_select.png")); //$NON-NLS-1$);
		reg.put(ICON_WORKINGSET_COPY, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/working_set_copy.png")); //$NON-NLS-1$);
		reg.put(ICON_PDF, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/printpdf.png")); //$NON-NLS-1$);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	
		SmartContext.INSTANCE.setClass(IIntelligenceLabelProvider.class, new IntelligenceLabelProviderImpl());
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
	public static Intelligence2PlugIn getDefault() {
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
}
