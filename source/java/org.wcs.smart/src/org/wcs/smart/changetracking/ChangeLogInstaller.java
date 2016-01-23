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
import org.wcs.smart.hibernate.HibernateManager;

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
	 * Install change log tracking for all plugins.<br><br>
	 * This checks the database to ensure the plugin tables have been created.  If the plugin
	 * id does not have a entry in the db_versions table the change log tracking will not
	 * be installed for that plugin.
	 * @param session
	 * @throws Exception
	 */
	public void installChangeLogTracking(Session session) throws Exception{
		//if installing multiple plugins at once the system
		//will register all the extension points and then run the install
		//code.  So if installing the plan and connect plugins the plan plugin
		//extension points are registered then the connect install code is run
		//which tries to create the triggers on the plan tables (from the registered 
		//extension) but the plan tables do not exist yet.
		//The current workaround for this issue is to check to ensure the plugin
		//is registered in the database first.
		//
		//this doesn't work because we may be installing all but still have an old
		//version of the plugin
		//Consider the case when a user asks for an update to CT plugin and
		//install the connect plugin at the same time.  If the connect plugin is installed first
		//after it is installed we try to install change tracking for
		//all plugins; however the ct plugin has not yet been updated to
		//this may fail if the ct plugin installs some new tables
		//we cannot just ignore exceptions because this will cause the transaction
		//to fail.
		if (!enabled) return;
		SmartPlugIn.log("Installing ALL change logging. ", null); //$NON-NLS-1$
		for (ChangeTrackerWrapper tracker : getTrackers(null)){
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
