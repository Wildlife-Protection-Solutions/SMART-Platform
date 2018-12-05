/*
 * Copyright (C) 2018 Wildlife Conservation Society
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
package org.wcs.smart.imageprocessor;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class ImageProcessingPlugIn extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.wcs.smart.imageprocessor"; //$NON-NLS-1$

	public static final String OK_ICON = "org.wcs.smart.imageprocessor.ok"; //$NON-NLS-1$
	public static final String PROCESSING_ICON = "org.wcs.smart.imageprocessor.processing"; //$NON-NLS-1$
	public static final String WARNING_ICON = "org.wcs.smart.imageprocessor.warn"; //$NON-NLS-1$
	
	// The shared instance
	private static ImageProcessingPlugIn plugin;
	
	/**
	 * The constructor
	 */
	public ImageProcessingPlugIn() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
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
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static ImageProcessingPlugIn getDefault() {
		return plugin;
	}

	
	public static void log(String message, Throwable t){
		int status = t instanceof Exception || message != null ? IStatus.ERROR : IStatus.WARNING;
        getDefault().getLog().log(new Status(status, PLUGIN_ID, IStatus.OK, message, t));
	}

	public static void logInfo(String message){
		getDefault().getLog().log(new Status(IStatus.OK, PLUGIN_ID, IStatus.INFO, message, null));
	}

	
	@Override
	public void initializeImageRegistry(ImageRegistry reg) {
		reg.put(OK_ICON,imageDescriptorFromPlugin(PLUGIN_ID,"images/icons/complete_icon.png"));//$NON-NLS-1$
		reg.put(PROCESSING_ICON,imageDescriptorFromPlugin(PLUGIN_ID,"images/icons/processing_icon.png"));//$NON-NLS-1$
		reg.put(WARNING_ICON,imageDescriptorFromPlugin(PLUGIN_ID,"images/icons/completewarn_icon.png"));//$NON-NLS-1$
		super.initializeImageRegistry(reg);
	}
}
