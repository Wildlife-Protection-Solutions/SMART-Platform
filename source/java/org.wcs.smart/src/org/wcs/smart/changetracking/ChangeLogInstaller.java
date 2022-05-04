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
package org.wcs.smart.changetracking;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;

/**
 * Class that will installs all change log tracking requirements for all
 * plugins.  This class uses the org.wcs.smart.changelog.installer extension
 * point to determine the items to install.  
 * 
 * @author Emily
 *
 */
public enum ChangeLogInstaller {
	
	INSTANCE;
	
	private boolean enabled = false;
	
	/**
	 * Enable or disable the change log installer.  If your plugin requires change log
	 * tracking you should enable this.  This will ensure that plugins installed
	 * after your plugin will automatically install the required change log tracking.
	 * <br>
	 * NOTE: assumes only one plugin is managing this variable.  If multiple plugins
	 * are going to manage it we need to change this implementation.
	 * @param enabled
	 */
	public void setEnabled(boolean enabled){
		this.enabled = enabled;
	}
	/**
	 * The state of the change log.  This determines if plugins install triggers
	 * on database tables for tracking changes or not.  
	 * <br>
	 * As per setEnabled, this assumes only ONE plugin is managing this variable. Changes
	 * are required if multiple plugins are going to manager this variable.
	 * @return 
	 */
	public boolean isChangeLoggingEnabled(){
		return this.enabled;
	}
	/**
	 * Installed change log tracking for a given plugin.
	 * 
	 * @param session current session in active trasaction
	 * @param pluginId plugin id to install 
	 * @param monitor progress monitor
	 * 
	 * @throws Exception
	 */
	public void installChangeLogTracking(Session session, String pluginId) throws Exception{
		if (!enabled) return;
		SmartPlugIn.log("Installing change logging for: " + pluginId, null); //$NON-NLS-1$
		for (ChangeTrackerWrapper tracker : getTrackers(pluginId)){
			tracker.installer.installChangeTracking(session);
		}
	}
	
	/**
	 * Uninstall change log tracking for a given plugin.
	 * 
	 * @param session current session in active trasaction
	 * @param pluginId plugin id to install 
	 * 
	 * @throws Exception
	 */
	public void uninstallChangeLogTracking(Session session, String pluginId) throws Exception{
		
		if (!enabled) return;
		SmartPlugIn.log("Uninstalling change logging for: " + pluginId, null); //$NON-NLS-1$
		for (ChangeTrackerWrapper tracker : getTrackers(pluginId)){
			tracker.installer.removeChangeTracking(session);
		}
	}
	
	/**
	 * Install change log tracking for all plugins.
	 * 
	 * @param session
	 * @throws Exception
	 */
	public void installChangeLogTracking(Session session) throws Exception{
		if (!enabled) return;
		SmartPlugIn.log("Installing ALL change logging. ", null); //$NON-NLS-1$
		for (ChangeTrackerWrapper tracker : getTrackers(null)){
			SmartPlugIn.log("Installing change logging: " + tracker.pluginId, null); //$NON-NLS-1$
			tracker.installer.installChangeTracking(session);
		}
	}
	
	/**
	 * Uninstall change log tracking for all plugins
	 * @param session
	 * @throws Exception
	 */
	public void uninstallChangeLogTracking(Session session) throws Exception{
		uninstallChangeLogTracking(session, null);
	}
	
	/*
	 * find all change log tracking extensions
	 */
	private List<ChangeTrackerWrapper> getTrackers(String pluginId) throws Exception{
		List<ChangeTrackerWrapper> installers = new ArrayList<ChangeTrackerWrapper>();
		
		IConfigurationElement[] config = Platform.getExtensionRegistry()
				.getConfigurationElementsFor(IChangeTrackerInstaller.EXTENSION_ID);
		
		for (IConfigurationElement e : config) {
			if (pluginId == null || e.getContributor().getName().equalsIgnoreCase(pluginId)){
				IChangeTrackerInstaller installer = (IChangeTrackerInstaller) e.createExecutableExtension("clazz"); //$NON-NLS-1$
				installers.add(new ChangeTrackerWrapper(installer, e.getContributor().getName()));	
			}
		}
		return installers;
	}
	
	class ChangeTrackerWrapper{
		public IChangeTrackerInstaller installer;
		public String pluginId;
		
		public ChangeTrackerWrapper(IChangeTrackerInstaller installer, String pluginId){
			this.installer = installer;
			this.pluginId = pluginId;
		}
	}
}
