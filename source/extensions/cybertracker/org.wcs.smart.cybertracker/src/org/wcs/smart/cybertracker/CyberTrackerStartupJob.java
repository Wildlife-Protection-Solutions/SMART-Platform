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
package org.wcs.smart.cybertracker;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.cybertracker.model.CyberTrackerProperties;
import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesOption;
import org.wcs.smart.cybertracker.util.PdaUtil;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * Performs activities that are required on startup:
 * - ensure that database tables exist (create then if the do not exist)
 * - clean up storage according to specified settings
 * - ensure all storage locations exist
 * - record all required registry keys
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class CyberTrackerStartupJob extends Job {

	public CyberTrackerStartupJob() {
		super(Messages.CyberTrackerPlugIn_InitJob_Title);
	}


	@Override
	protected IStatus run(IProgressMonitor monitor) {
	
		List<ConservationArea> caList = null;
		List<CyberTrackerPropertiesOption> propList = null;
		Session session = HibernateManager.openSession();
		try {
			caList = HibernateManager.getConservationAreas(session);
			propList = CyberTrackerHibernateManager.getAllStorageOptions(session);
		} catch (Exception e) {
			CyberTrackerPlugIn.getDefault().getLog().log(new Status(IStatus.ERROR, CyberTrackerPlugIn.PLUGIN_ID, IStatus.OK, "Failed to select CA list and CyberTracker properties.", e)); //$NON-NLS-1$
		} finally {
			session.close();
		}
		checkFolderAndRegistry(caList);
		cleanStorage(caList, propList);
		return Status.OK_STATUS;
	}

	
	
	private void checkFolderAndRegistry(List<ConservationArea> caList) {
		if (caList == null)
			return;
		for (ConservationArea ca : caList) {
			try {
				PdaUtil.updateRegistryKey(ca);
			} catch (Exception e) {
				CyberTrackerPlugIn.getDefault().getLog().log(new Status(IStatus.ERROR, CyberTrackerPlugIn.PLUGIN_ID, IStatus.OK, "Failed to create folder or update registry for CA "+ca.getName(), e)); //$NON-NLS-1$
			}
		}
	}

	private void cleanStorage(List<ConservationArea> caList, List<CyberTrackerPropertiesOption> storageOptionList) {
		if (caList == null || storageOptionList == null)
			return;
		Map<UUID, CyberTrackerPropertiesOption> propMap = new HashMap<UUID, CyberTrackerPropertiesOption>();
		for (CyberTrackerPropertiesOption ctp : storageOptionList) {
			propMap.put(ctp.getConservationArea().getUuid(), ctp);
		}
		
		for (ConservationArea ca : caList) {
			CyberTrackerPropertiesOption ctp = propMap.get(ca.getUuid());
			File storageDir = PdaUtil.getStorageFolder(ca);
			int dayLimit = (ctp != null) ? ctp.getIntegerValue() : CyberTrackerProperties.STORAGE_TIME_DEFAULT_VALUE;
			cleanStorage(storageDir, dayLimit);
		}
	}

	private void cleanStorage(File folder, long dayLimit) {
		if (!folder.exists())
			return;
		long current = new Date().getTime();
		long bound = dayLimit * 24 * 60 * 60 * 1000;
		for (File file : folder.listFiles()) {
			if (current - file.lastModified() > bound) {
				FileUtils.deleteQuietly(file);
			}
		}
	}
	
}
