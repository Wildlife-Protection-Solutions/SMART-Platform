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
package org.wcs.smart.dataentry;

import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.wcs.smart.ca.datamodel.DataModelManager;

/**
 * The activator class controls the plug-in life cycle
 */
public class DataentryPlugIn extends AbstractUIPlugin {

	/**
	 * Image descriptor for group icon
	 */
	public static final String GROUP_ICON = "org.wcs.smart.dataentry.GROUP_ICON"; //$NON-NLS-1$
	public static final String CONFIG_MODEL_ICON = "org.wcs.smart.dataentry.CONFIG_MODEL_ICON"; //$NON-NLS-1$
	
	// The plug-in ID
	public static final String PLUGIN_ID = "org.wcs.smart.dataentry"; //$NON-NLS-1$

	// The shared instance
	private static DataentryPlugIn plugin;
	
	private DataModelItemListener deleteListener = new DataModelItemListener();
	private DataModelListener dmListener = new DataModelListener();
	
	/**
	 * Extension id for Additional Data associated with Configurable Model
	 */
	public static final String CM_EXTRADATA_EXTENSION_ID = "org.wcs.smart.dataentry.extradata"; //$NON-NLS-1$

	
	/**
	 * The constructor
	 */
	public DataentryPlugIn() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		
		DataModelManager.INSTANCE.addItemChangeListener(deleteListener);
		DataModelManager.INSTANCE.addChangeListener(dmListener);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		DataModelManager.INSTANCE.removeItemChangeListener(deleteListener);
		DataModelManager.INSTANCE.removeChangeListener(dmListener);
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static DataentryPlugIn getDefault() {
		return plugin;
	}

	@Override
	 protected void initializeImageRegistry(ImageRegistry reg) {
	     reg.put(GROUP_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons16/group.png")); //$NON-NLS-1$
	     reg.put(CONFIG_MODEL_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons16/configmodel.png")); //$NON-NLS-1$
	}
}
