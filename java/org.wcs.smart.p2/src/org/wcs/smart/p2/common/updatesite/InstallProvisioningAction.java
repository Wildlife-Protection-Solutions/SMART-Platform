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
package org.wcs.smart.p2.common.updatesite;

import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.p2.engine.spi.ProvisioningAction;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.changetracking.ChangeLogInstaller;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * Action that is called when some plug-in is being installed. 
 * 
 * All plugins should extend to ensure change log tracking is installed
 * if necessary.
 * 
 * When installing a plugin, this class will uninstall any existing change log
 * tracking for the given plugin (to ensure that any upgrades will not fail due to 
 * triggers), install/upgrade the plugin then reinstall and change log tracking. 
 * 
 * @author elitvin
 * @author egouge
 * @since 3.0.0
 */
public abstract class InstallProvisioningAction extends ProvisioningAction {

	@Override
	public IStatus execute(Map<String, Object> parameters) {
		SmartPlugIn.log("Installing PlugIn: " + getPluginId(), null); //$NON-NLS-1$
		
		try{
			//remove any existing change log tracking
			//this is done so any alter table statements are not
			//prevented from running because of the triggers
			Session s = HibernateManager.openSession();
			try{
				s.beginTransaction();
				ChangeLogInstaller.INSTANCE.uninstallChangeLogTracking(s, getPluginId());
				s.getTransaction().commit();
			}finally{
				s.close();
			}
			
			
			//execute install/upgrade
			IStatus temp = executeInternal(parameters);
			
			//add back all change log tracking if necessary.
			s = HibernateManager.openSession();
			try{
				s.beginTransaction();
				ChangeLogInstaller.INSTANCE.installChangeLogTracking(s, getPluginId());
				s.getTransaction().commit();
			}finally{
				s.close();
			}
			
			return temp;
		}catch (Exception ex){
			return new Status(Status.ERROR, SmartPlugIn.PLUGIN_ID, "Could not install plugin.", ex); //$NON-NLS-1$
		}
	}
	
	
	@Override
	public IStatus undo(Map<String, Object> parameters) {
		return Status.OK_STATUS;
	}
	
	/**
	 * Install or upgrade the plugin
	 * @param parameters
	 * @return
	 */
	protected abstract IStatus executeInternal(Map<String, Object> parameters);

	/**
	 * 
	 * @return the plugin id being installed
	 */
	protected abstract String getPluginId();
}
