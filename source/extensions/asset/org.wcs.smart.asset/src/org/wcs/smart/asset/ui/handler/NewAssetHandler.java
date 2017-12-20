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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.osgi.service.prefs.BackingStoreException;
import org.wcs.smart.asset.AssetEvents;
import org.wcs.smart.asset.AssetPlugIn;
import org.wcs.smart.asset.model.Asset;
import org.wcs.smart.asset.model.AssetType;
import org.wcs.smart.asset.ui.AssetTypeLabelProvider;
import org.wcs.smart.asset.ui.views.asset.AssetEditorInput;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.UuidUtils;

/**
 * New asset handler
 * 
 * @author Emily
 *
 */
public class NewAssetHandler {

	public static final String NEW_TYPE_OPTIONS_NODE = "org.wcs.smart.asset.ui.views.asset.new.assettypes"; //$NON-NLS-1$
	public static final String NEW_TYPE_OPTIONS_KEY = "uuids"; //$NON-NLS-1$
	public static final int NEW_TYPE_MAX_OPTIONS = 5;
	public static final String OPTION_SEP = ","; //$NON-NLS-1$
	
	public void execute( ) {
		execute(null);
	}
	
	public void execute(UUID assetTypeUuid) {
		AssetType type = null;
		
		if (assetTypeUuid != null) {
			try(Session s = HibernateManager.openSession()){
				type = s.get(AssetType.class, assetTypeUuid);
			}
		}
		
		if (type == null) {
			//show select asset type dialog
			SelectAssetTypeDialog dialog = new SelectAssetTypeDialog(Display.getDefault().getActiveShell());
			if (dialog.open() != Window.OK) return;
			type = dialog.getAssetType();
		}
		
		if (type == null) return;
		
		//update last created preference
		String newTypes = ConfigurationScope.INSTANCE.getNode(NewAssetHandler.NEW_TYPE_OPTIONS_NODE).get(NEW_TYPE_OPTIONS_KEY, null);
		if (newTypes == null) newTypes = ""; //$NON-NLS-1$
		String bits[] = newTypes.split(OPTION_SEP);
		String newUuid = UuidUtils.uuidToString(type.getUuid());
		StringBuilder sb = new StringBuilder();
		sb.append(newUuid);
		Set<String> uuids = new HashSet<>();
		uuids.add(newUuid);
		int cnt = 1;
		for (int i = 0; i < bits.length; i ++) {
			if (!bits[i].isEmpty() && !uuids.contains(bits[i])) {
				sb.append(OPTION_SEP);
				sb.append(bits[i]);
				cnt ++;
				if (cnt == NEW_TYPE_MAX_OPTIONS) break;
			}
		}
		ConfigurationScope.INSTANCE.getNode(NewAssetHandler.NEW_TYPE_OPTIONS_NODE).put(NEW_TYPE_OPTIONS_KEY, sb.toString());
		try {
			ConfigurationScope.INSTANCE.getNode(NewAssetHandler.NEW_TYPE_OPTIONS_NODE).flush();
		} catch (BackingStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//open editor
		AssetEditorInput in = new AssetEditorInput(type.getUuid());
		(new OpenAssetHandler()).openAsset(in);
	}
	
	
	public Asset createAndSave(String assetId, IEventBroker broker) {
		SelectAssetTypeDialog dialog = new SelectAssetTypeDialog(Display.getDefault().getActiveShell());
		if (dialog.open() != Window.OK) return null;
		
		AssetType type = dialog.getAssetType();
		Asset newAsset = null;
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				//ensure asset id doesn't already exist
				String query =  "SELECT count(*) FROM Asset WHERE LOWER(id) = :id AND conservationArea = :ca ";
				Query<?> q = session.createQuery(query)
				.setParameter("id", assetId.toLowerCase())
				.setParameter("ca", SmartDB.getCurrentConservationArea());
				Long cnt = (Long)q.uniqueResult();
				if (cnt != 0) {
					MessageDialog.openError(Display.getDefault().getActiveShell(), "Duplicate Asset ID", MessageFormat.format("Asset with id {0} already exists in the system.  Cannot duplicate asset id", assetId));
					return null;
				}
				
				newAsset = new Asset();
				newAsset.setAssetType(type);
				newAsset.setId(assetId);
				newAsset.setConservationArea(SmartDB.getCurrentConservationArea());
				newAsset.setAttributeValues(new ArrayList<>());
				newAsset.setIsRetired(false);
				
				session.save(newAsset);
				session.getTransaction().commit();
			}catch (Exception ex) {
				session.getTransaction().rollback();
				AssetPlugIn.displayLog("Unable to create new asset: " + ex.getMessage(), ex);
				return null;
			}
		}
		
		//todo fire event
		broker.post(AssetEvents.ASSET_NEW, Collections.singleton(newAsset));
		return newAsset;
	}
	
	/**
	 * Dialog for selecting type of asset to create
	 * @author Emily
	 *
	 */
	private class SelectAssetTypeDialog extends TitleAreaDialog {

		private TableViewer lstAssets;
		private AssetType selectedType;
		
		public SelectAssetTypeDialog(Shell parentShell) {
			super(parentShell);
		}
		
		public AssetType getAssetType() {
			return selectedType;
		}
		
		@Override
		public void okPressed() {
			selectedType = null;
			Object x = ((IStructuredSelection)lstAssets.getSelection()).getFirstElement();
			if (x != null && x instanceof AssetType) {
				selectedType = (AssetType) x;
				super.okPressed();
			}
		}
		@Override
		protected Control createDialogArea(Composite parent) {
			Composite c = (Composite) super.createDialogArea(parent);
			
			lstAssets = new TableViewer(c, SWT.READ_ONLY | SWT.BORDER);
			lstAssets.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			lstAssets.setContentProvider(ArrayContentProvider.getInstance());
			lstAssets.setLabelProvider(new AssetTypeLabelProvider());
			lstAssets.setInput(new String[] {DialogConstants.LOADING_TEXT});
			
			lstAssets.addSelectionChangedListener(new ISelectionChangedListener() {
				@Override
				public void selectionChanged(SelectionChangedEvent event) {
					if (!event.getSelection().isEmpty() && ((IStructuredSelection)event.getSelection()).getFirstElement() instanceof AssetType) {
						getButton(IDialogConstants.OK_ID).setEnabled(true);
					}else {
						getButton(IDialogConstants.OK_ID).setEnabled(false);
					}
				}
			});
			lstAssets.addDoubleClickListener(new IDoubleClickListener() {
				@Override
				public void doubleClick(DoubleClickEvent event) {
					okPressed();
				}
			});
			
			setMessage("Select the type of the new asset");
			setTitle("New Asset");
			getShell().setText("New Asset");
			
			loadTypes.setSystem(true);
			loadTypes.schedule();
			
			return c;
		}
		
		@Override
		protected void createButtonsForButtonBar(Composite parent) {
			// create OK and Cancel buttons by default
			Button btnOk = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
			createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
			btnOk.setEnabled(false);
		}
		
		Job loadTypes = new Job("loading asset types") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				List<AssetType> types = new ArrayList<>();
				try(Session session = HibernateManager.openSession()){
					types.addAll(QueryFactory.buildQuery(session, AssetType.class,
							new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).getResultList());
					
				}
				
				Display.getDefault().syncExec(()->{
					lstAssets.setInput(types);
				});
				
				return Status.OK_STATUS;
			}
			
		};
		
		@Override
		public boolean isResizable() {
			return true;
		}

	}
}
