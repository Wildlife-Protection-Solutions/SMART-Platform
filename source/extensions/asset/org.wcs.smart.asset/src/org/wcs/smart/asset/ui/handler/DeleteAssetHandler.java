/*
 * Copyright (C) 2017 Wildlife Conservation Society
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
package org.wcs.smart.asset.ui.handler;

import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.asset.AssetEvents;
import org.wcs.smart.asset.AssetManager;
import org.wcs.smart.asset.AssetPlugIn;
import org.wcs.smart.asset.AssetUtils;
import org.wcs.smart.asset.model.Asset;
import org.wcs.smart.ca.advisors.DeleteManager;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * New asset handler
 * 
 * @author Emily
 *
 */
//@SuppressWarnings("restriction")
public class DeleteAssetHandler {

	@Inject
	private IEventBroker eventBroker;
	@Inject
	private Shell activeShell;
	
	public void deleteAsset(List<UUID> assetUuids) {
	
		Shell shell = activeShell;
		List<Asset> toDelete = new ArrayList<>();
		try(Session session = HibernateManager.openSession()){
			for (UUID assetUuid : assetUuids) {
				Asset a = session.get(Asset.class, assetUuid);
			
				if (a == null) continue;
				a.getId();
				try {
					DeleteManager.canDelete(Asset.class, session);
					toDelete.add(a);
				}catch (Exception ex) {
					MessageDialog.openWarning(shell, "Delete Asset", 
							MessageFormat.format("Unable to delete asset ''{0}'': {1}", a.getId(), ex.getMessage()));
				}
				
			}
		}
		if (toDelete.isEmpty()) return;
		
		StringBuilder ids = new StringBuilder();
		for(Asset s : toDelete) ids.append(s.getId() +"\n");
		if (!MessageDialog.openQuestion(shell, "Delete Asset",
				MessageFormat.format("The following assets and all associated data will be removed." + "\n{0}" + "Are you sure you want to delete the following assets?", ids.toString())
				)) {
			return;
		}
		
		if (!AssetUtils.confirmPassword(shell, "Confirm User", "Enter you password to continue with delete operation")) {
			return;
		}
			
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(shell);
		try {
			pmd.run(true,  false, new IRunnableWithProgress() {
				
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					monitor.beginTask("Deleting Assets", toDelete.size());
					for (Asset asset : toDelete) {
						AssetManager.INSTANCE.deleteAsset(asset, eventBroker);
						monitor.worked(1);
					}
				}
			});
		}catch (Exception ex) {
			AssetPlugIn.displayLog("Error deleting assets. Refresh and try again." + ex.getMessage(),  ex);
		}
		eventBroker.post(AssetEvents.ASSETDATA, null);

		
	}
}
