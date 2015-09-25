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
import org.eclipse.equinox.internal.p2.engine.InstallableUnitOperand;
import org.eclipse.equinox.p2.engine.spi.ProvisioningAction;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.changetracking.ChangeLogInstaller;
import org.wcs.smart.hibernate.HibernateManager;


/**
 * Action that is called when some plug-in is being uninstalled.
 * Class contains logic that allows to distinguish if this is plug-in removal or upgrade.
 * All plugins should extend this class to ensure change log tracking is
 * also removed if required.
 * 
 * @author elitvin
 * @since 3.0.0
 */
@SuppressWarnings("restriction")
public abstract class UninstallProvisioningAction extends ProvisioningAction {

	@Override
	public IStatus execute(Map<String, Object> parameters) {
		IInstallableUnit upgradeTo = null;
		Object operand = parameters.get("operand"); //$NON-NLS-1$
		try {
			if (operand instanceof InstallableUnitOperand)
				upgradeTo = ((InstallableUnitOperand) operand).second();
		}
		catch (Throwable e) {
			// Ignore class not found in case InstallableUnitOperand is missing
		}
		if (upgradeTo == null) {
			// We have an unistallation, not an upgrade
			try{
				//remove all change log tracking 
				Session s = HibernateManager.openSession();
				try{
					s.beginTransaction();
					ChangeLogInstaller.INSTANCE.uninstallChangeLogTracking(s, getPluginId());
					s.getTransaction().commit();
				}finally{
					s.close();
				}
				performRemove();
			}catch (Exception ex){
				SmartPlugIn.log("Error uninstalled plugin.", ex); //$NON-NLS-1$
			}
		} else {
			performUpgrade();
		}
		return Status.OK_STATUS;
	}

	@Override
	public IStatus undo(Map<String, Object> parameters) {
		return Status.OK_STATUS;
	}

	/**
	 * Remove all database tables/requirements 
	 */
	protected abstract void performRemove();
	
	/**
	 * 
	 * @return the plugin id being processed
	 */
	protected abstract String getPluginId();

	/**
	 * We do nothing here by default as this will be dealt with on the install
	 * action.
	 */
	protected void performUpgrade() {
		//nothing by default
	}
}
