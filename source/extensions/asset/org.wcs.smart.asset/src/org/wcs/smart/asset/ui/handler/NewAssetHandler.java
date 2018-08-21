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
import org.eclipse.e4.core.contexts.IEclipseContext;
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
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.osgi.service.prefs.BackingStoreException;
import org.wcs.smart.asset.AssetEvents;
import org.wcs.smart.asset.AssetPlugIn;
import org.wcs.smart.asset.internal.Messages;
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
	
	public void execute( IEclipseContext context ) {
		execute(null, context);
	}
	
	public void execute(UUID assetTypeUuid, IEclipseContext context) {
		AssetType type = null;
		
		if (assetTypeUuid != null) {
			try(Session s = HibernateManager.openSession()){
				type = s.get(AssetType.class, assetTypeUuid);
			}
		}
		
		SelectAssetTypeDialog dialog = null;
		String assetId = null;
		if (type == null) {
			//show select asset type dialog
			dialog = new SelectAssetTypeDialog(Display.getDefault().getActiveShell());
		}else {
			dialog = new SelectAssetTypeDialog(Display.getDefault().getActiveShell(), type);
		}
		
		if (dialog.open() != Window.OK) return;
		type = dialog.getAssetType();
		assetId = dialog.getAssetId();
		
		if (assetId == null || type == null) return;
		
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
			AssetPlugIn.log(e.getMessage(), e);
		}
		
		Asset newAsset = createAsset(assetId, type);
		if (newAsset != null) {
			//open editor
			AssetEditorInput in = new AssetEditorInput(newAsset.getUuid(), newAsset.getId(), true);
			(new OpenAssetHandler()).openAsset(in);
			context.get(IEventBroker.class).post(AssetEvents.ASSET_NEW, Collections.singleton(newAsset));
		}
	}
	
	
	public Asset createAndSave(String assetId, IEventBroker broker) {
		SelectAssetTypeDialog dialog = new SelectAssetTypeDialog(Display.getDefault().getActiveShell(), assetId);
		if (dialog.open() != Window.OK) return null;
		
		AssetType type = dialog.getAssetType();
		assetId = dialog.getAssetId();
		Asset newAsset = createAsset(assetId, type);
		if (newAsset != null) {
			broker.post(AssetEvents.ASSET_NEW, Collections.singleton(newAsset));
		}
		return newAsset;
	}
	
	private Asset createAsset(String assetId, AssetType type) {
		Asset newAsset = null;
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				//ensure asset id doesn't already exist
				if (checkDuplicate(session, assetId)) {
					MessageDialog.openError(Display.getDefault().getActiveShell(), Messages.NewAssetHandler_DuplicateIdTitle, MessageFormat.format(Messages.NewAssetHandler_DuplicateIdMsg, assetId));
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
				AssetPlugIn.displayLog(Messages.NewAssetHandler_CreateError + ex.getMessage(), ex);
				return null;
			}
		}
		return newAsset;
	}
	
	private boolean checkDuplicate(Session session, String assetId) {
		String query =  "SELECT count(*) FROM Asset WHERE LOWER(id) = :id AND conservationArea = :ca "; //$NON-NLS-1$
		Query<?> q = session.createQuery(query)
		.setParameter("id", assetId.toLowerCase()) //$NON-NLS-1$
		.setParameter("ca", SmartDB.getCurrentConservationArea()); //$NON-NLS-1$
		Long cnt = (Long)q.uniqueResult();
		if (cnt == 0) return false;
		return true;
	}
	/**
	 * Dialog for selecting type of asset to create
	 * @author Emily
	 *
	 */
	private class SelectAssetTypeDialog extends TitleAreaDialog {

		private TableViewer lstAssets;
		private AssetType selectedType;
		private String assetId;
		private Text txtId;
		
		private AssetType defaultType = null;
		private String defaultId = null;
		
		public SelectAssetTypeDialog(Shell parentShell) {
			super(parentShell);
		}
		
		public SelectAssetTypeDialog(Shell parentShell, AssetType defaultType) {
			super(parentShell);
			this.defaultType = defaultType;
		}
		
		public SelectAssetTypeDialog(Shell parentShell, String defaultId) {
			super(parentShell);
			this.defaultId = defaultId;
		}
		
		public AssetType getAssetType() {
			return selectedType;
		}
		
		public String getAssetId() {
			return this.assetId;
		}
		
		@Override
		public void okPressed() {
			selectedType = null;
			Object x = ((IStructuredSelection)lstAssets.getSelection()).getFirstElement();
			if (x == null || !(x instanceof AssetType)) {
				return;
			}
			selectedType = (AssetType) x;
			
			assetId = txtId.getText().trim();
			if (assetId.isEmpty()) {
				MessageDialog.openError(getShell(), Messages.NewAssetHandler_ErrorTitle, Messages.NewAssetHandler_IdRequired);
				return;
			}
			boolean dup = false;
			try(Session s = HibernateManager.openSession()){
				dup = checkDuplicate(s, assetId);
			}
			if (dup) {
				MessageDialog.openError(getShell(), Messages.NewAssetHandler_ErrorTitle, MessageFormat.format(Messages.NewAssetHandler_DuplicateAssetId, assetId));
				return;
			}
			super.okPressed();
		}
		
		private void validate() {
			if (txtId.getText().trim().isEmpty()) {
				setErrorMessage(Messages.NewAssetHandler_AssetRequired);
			}else {
				setErrorMessage(null);
			}
		}
		
		@Override
		protected Control createDialogArea(Composite parent) {
			Composite c = (Composite) super.createDialogArea(parent);
			
			Composite main = new Composite(c, SWT.NONE);
			main.setLayout(new GridLayout(2, false));
			main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			
			Label l = new Label(main, SWT.NONE);
			l.setText(Messages.NewAssetHandler_AssetIdLabel);
			
			txtId = new Text(main, SWT.BORDER);
			txtId.setText(""); //$NON-NLS-1$
			txtId.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			if (defaultId != null) {
				txtId.setText(defaultId);
			}
			txtId.addListener(SWT.Modify, e->{
				validate();
			});
			
			l = new Label(main, SWT.NONE);
			l.setText(Messages.NewAssetHandler_TypeLabel);
			l.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
			
			lstAssets = new TableViewer(main, SWT.READ_ONLY | SWT.BORDER);
			lstAssets.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			((GridData)lstAssets.getControl().getLayoutData()).heightHint = 150;
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
						
			setMessage(Messages.NewAssetHandler_Message);
			setTitle(Messages.NewAssetHandler_Title);
			getShell().setText(Messages.NewAssetHandler_Title);
			
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
		
		Job loadTypes = new Job(Messages.NewAssetHandler_loadingTypeJobName) {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				List<AssetType> types = new ArrayList<>();
				try(Session session = HibernateManager.openSession()){
					types.addAll(QueryFactory.buildQuery(session, AssetType.class,
							new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).getResultList()); //$NON-NLS-1$
					
				}
				
				Display.getDefault().syncExec(()->{
					lstAssets.setInput(types);
					if (defaultType != null) {
						lstAssets.setSelection(new StructuredSelection(defaultType));
					}
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
