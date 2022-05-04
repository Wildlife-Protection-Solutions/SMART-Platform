/*
 * Copyright (C) 2022 Wildlife Conservation Society
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

import java.text.MessageFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.SmartProperties;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.upgrade.UpgradeEngine.UpgradeFromVersion;
import org.wcs.smart.upgrade.v320.Upgrader310To320;

/**
 * Database upgrader for core tables
 * 
 * @author Emily
 *
 */
public class CoreDatabaseUpgrader implements IDatabaseUpgrader {

	private Set<IDatabaseUpgrader> upgradersRun = new HashSet<IDatabaseUpgrader>();

	@Override
	public boolean isUpdateToDate(Map<String, String> currentVersions) {
		//TODO: this should probably throw an exception as I don't think we can start from nothing
		if (!currentVersions.containsKey(SmartPlugIn.PLUGIN_ID)) return false;
		
		String requiredDbVersion = SmartProperties.getInstance().getProperty(SmartProperties.DB_VERSION_KEY);
		if (currentVersions.get(SmartPlugIn.PLUGIN_ID).equals(requiredDbVersion)) return true;
		return false;
	}
	
	@Override
	public void upgrade(IProgressMonitor monitor) throws Exception {
		monitor.subTask(Messages.CoreDatabaseUpgrader_TaskName);
		
		Map<String, String> versions = null;
		try(Session session = HibernateManager.openSession()){
			versions = UpgradeEngine.getVersions(session);
		}
		
		upgradeInternal(versions.get(SmartPlugIn.PLUGIN_ID), monitor);
		monitor.done();
		
	}

	private void upgradeInternal(String currentVersion, IProgressMonitor monitor) throws Exception {
		SubMonitor sub = SubMonitor.convert(monitor);
		sub.beginTask(Messages.CoreDatabaseUpgrader_TaskName, 1);
		
		UpgradeFromVersion fromVersion = null;
		for (UpgradeFromVersion v : UpgradeFromVersion.values()){
			if (v.fromVersion.equals(currentVersion)){
				fromVersion = v;
			}
		}
		
		if (fromVersion == null) {
			throw new Exception(MessageFormat.format(Messages.UpgradeEngine_Error_Message1, currentVersion, UpgradeFromVersion.V320.toVersion));
		}
		
		int startIndex = 0;
		for (int i = 0; i < UpgradeFromVersion.values().length; i++){
			if (fromVersion == UpgradeFromVersion.values()[i]){
				startIndex = i ;
				break;
			}
		}
		
		SubMonitor sub1 = sub.split(1);
		sub1.setWorkRemaining(UpgradeFromVersion.values().length * 2);
		sub1.subTask(Messages.UpgradeEngine_subprogress2);
		
		upgradersRun.clear();
		
		for (int i = startIndex; i < UpgradeFromVersion.values().length; i ++){
			UpgradeFromVersion v = UpgradeFromVersion.values()[i];
			IDatabaseUpgrader upgrader = v.upgradeEngine.getConstructor().newInstance();
			upgrader.upgrade(sub1.split(1));
			upgradersRun.add(upgrader);
			
			
			List<IDatabaseUpgrader> additionalItems = UpgradeEngine.getCoreExtensions(v.fromVersion, v.toVersion);
			SubMonitor sub2 = sub1.split(1);
			sub2.setWorkRemaining(additionalItems.size());
			for (IDatabaseUpgrader item : additionalItems){
				item.upgrade(sub2.split(1));
			}
			sub2.done();
		}
	}

	@Override
	public void postProcess(IProgressMonitor monitor){
		/* --- post process  --- */
		//this is done here to ensure all plugin tables that are required
		//to check delete are installed
		SubMonitor progress = SubMonitor.convert(monitor);
		progress.setWorkRemaining(upgradersRun.size());
		for (IDatabaseUpgrader up : upgradersRun){
			if (up instanceof Upgrader310To320){
				((Upgrader310To320) up).postProcess(progress.split(1));
			}else {
				progress.worked(1);
			}
		}
	}

	@Override
	public String getPluginId() {
		return SmartPlugIn.PLUGIN_ID;
	}

	@Override
	public String getPluginName() {
		return getName(SmartPlugIn.getDefault().getBundle());
	}

}
