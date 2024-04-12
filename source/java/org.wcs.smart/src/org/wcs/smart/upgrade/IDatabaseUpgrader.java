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
package org.wcs.smart.upgrade;

import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.wcs.smart.internal.Messages;

/**
 * Interface that can be implemented to perform upgrade/install operations
 * while upgrade/restore backup.
 * 
 * @author elitvin
 * @since 3.0.0
 */
public interface IDatabaseUpgrader {

	public static final String PROGRESS_MESSAGE = Messages.IDatabaseUpgrader_upgradeinstallmessage;
	
	/**
	 * The plugin id for the change log manager, used for installing/uninstalling
	 * approriate triggers
	 * @return
	 */
	public String getPluginId();
	
	/**
	 * The plugin name for the UI
	 * @return
	 */
	public String getPluginName();
	
	/**
	 * Returns true if the database is update to date; otherwise false
	 * if upgrade is required
	 *  
	 * @param currentVersion a map from the plugin id to the current version in the database
	 * @return
	 */
	public boolean isUpdateToDate(Map<String,String> currentVersions);
	
	/**
	 * Installs or upgrades the plugin.
	 * 
	 * @param monitor
	 */
	public void upgrade(IProgressMonitor monitor) throws Exception;
	
	/**
	 * Runs any post-processing scripts.  These are to be run
	 * after the upgrade has occurred and the plugin and plugin version validation
	 * has completed.
	 * These should not in any way modify the plugin or plugin version table.
	 * @param monitor
	 */
	public default void postProcess(IProgressMonitor monitor) throws Exception{}
	
	/**
	 *  
	 * @param bundle
	 * @return The bundle-name header from the plugin bundle.
	 */
	public default String getName(Bundle bundle) {
		return bundle.getHeaders().get(Constants.BUNDLE_NAME);
	}
}
