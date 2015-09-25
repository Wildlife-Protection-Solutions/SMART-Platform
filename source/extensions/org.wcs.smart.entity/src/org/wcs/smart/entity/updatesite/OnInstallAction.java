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
package org.wcs.smart.entity.updatesite;

import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.entity.EntityPlugIn;
import org.wcs.smart.p2.common.updatesite.InstallProvisioningAction;

/**
 * Action that is called when Entity plug-in is installed or upgraded using update site
 * 
 * @author elitvin
 * @since 2.0.0
 */
public class OnInstallAction extends InstallProvisioningAction {

	@Override
	public IStatus executeInternal(Map<String, Object> parameters) {
		Job job = new AddEntityJob();
		job.setRule(SmartPlugIn.PLUGIN_START_MUTEX);
		job.schedule();
		try{
			job.join();
		}catch(InterruptedException ex){
			EntityPlugIn.log(ex.getLocalizedMessage(), ex);
		}
		return Status.OK_STATUS;	//always return ok status to plugin is registered; users should uninstall and re-install if error occurs
	}

	@Override
	protected String getPluginId() {
		return EntityPlugIn.PLUGIN_ID;
	}

}
