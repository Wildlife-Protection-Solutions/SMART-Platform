package org.wcs.smart.i2.birt;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.birt.report.model.api.DesignElementHandle;
import org.eclipse.birt.report.model.api.ModuleHandle;
import org.eclipse.birt.report.model.api.OdaDataSetHandle;
import org.eclipse.birt.report.model.api.PropertyHandle;
import org.eclipse.birt.report.model.api.activity.NotificationEvent;
import org.eclipse.birt.report.model.api.command.ContentEvent;
import org.eclipse.birt.report.model.api.command.NameEvent;
import org.eclipse.birt.report.model.api.core.Listener;
import org.eclipse.birt.report.model.api.elements.structures.ColumnHint;
import org.eclipse.birt.report.model.api.elements.structures.OdaDataSetParameter;
import org.eclipse.birt.report.model.elements.OdaDataSet;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.wcs.smart.birt.ui.IReportEditorManager;
import org.wcs.smart.birt.ui.RCPMultiPageReportEditor;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.birt.datasource.IntelBirtDataSource;
import org.wcs.smart.i2.birt.entity.EntityDataset;
import org.wcs.smart.i2.birt.entity.EntityParameterMetadata.EntityParameter;

public class IntelReportEditorManager implements IReportEditorManager{ 


	private RCPMultiPageReportEditor editor;
	// This listener is a hack to name the
	// smart query datasources with the query name
	// I couldn't figure out how to do this any other way.
	private Listener nameChangeListener = new Listener() {
		@Override
		public void elementChanged(DesignElementHandle focus,
				NotificationEvent ev) {
			if (ev.getTarget() instanceof OdaDataSet && ev.getEventType() == NotificationEvent.NAME_EVENT) {
				NameEvent ne = (NameEvent) ev;
				OdaDataSet ds = (OdaDataSet) ev.getTarget();
				OdaDataSetHandle handle = (OdaDataSetHandle) ds.getHandle(ev.getTarget().getRoot());

				if (handle.getExtensionID().startsWith(EntityDataset.DATASET_TYPE)
						&& ne.getOldName() == null
						&& ne.getNewName().startsWith(
								org.eclipse.birt.report.designer.nls.Messages
										.getString("dataset.new.defaultName")) //$NON-NLS-1$
						&& (ds.getDisplayName() == null || !ds.getDisplayName().equals(ne.getNewName()))) {
						try {
						handle.setName(((OdaDataSet) ev.getTarget())
								.getDisplayName());
					} catch (Exception ex) {
						// eat me - we cant update the name for whatever reason
					}
				}
			} else if (ev.getEventType() == NotificationEvent.CONTENT_EVENT) {
				// auto link start and end data parameters to report parameters
				ContentEvent ce = (ContentEvent) ev;
				if (ce.getAction() == ContentEvent.ADD && ce.getContent() instanceof OdaDataSet) {
					
					OdaDataSet ds = (OdaDataSet) ce.getContent();
					OdaDataSetHandle handle = (OdaDataSetHandle) (ds).getHandle(ev.getTarget().getRoot());
					if (handle.getExtensionID().equals(EntityDataset.DATASET_TYPE)) {
						//link parameters
						PropertyHandle odaDataSetParameterProp = handle.getPropertyHandle(OdaDataSetHandle.PARAMETERS_PROP);
						List<?> items = odaDataSetParameterProp.getItems();
						for (Iterator<?> iterator = items.iterator(); iterator.hasNext();) {
							OdaDataSetParameter parameter = (OdaDataSetParameter) iterator.next();
							if (parameter.getName().equals(EntityParameter.UUID.name)) {
								parameter.setDefaultValue(""); //$NON-NLS-1$
								parameter.setParamName(parameter.getName());
							}
						}
						
						//setup column aliases to make
					//charting UI have "nice" names
						try{
							ArrayList<?> columns = (ArrayList<?>) handle.getProperty("columnHints");  //$NON-NLS-1$
							for (Object col : columns){
								((ColumnHint)col).setProperty("alias", ((ColumnHint)col).getProperty(ds.getRoot(), "displayName"));   //$NON-NLS-1$//$NON-NLS-2$
							}
						}catch (Exception ex){
							Intelligence2PlugIn.log(ex.getMessage(), ex);
						}
					}
				}
			}
		}
	};
		
	public IntelReportEditorManager() {
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		ModuleHandle initialModel = editor.getModel();
		editor.doSaveParent(monitor);
		
		try {
			//on the xml page saving changes the model so we need to reconfigure
			//the listeners
			if (!editor.getModel().equals(initialModel)){
				initialModel.removeListener(nameChangeListener);
				editor.getModel().addListener(nameChangeListener);
			}
			editor.refreshMarkers(editor.getEditorInput());
		} catch (CoreException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void doSaveAs() {
		//not supported
		
	}
//
//
//	/*
//	 * Fires the required notifications.  This is copied from
//	 *  org.eclipse.birt.report.designer.internal.ui.util.UIUtil.doFinishSave
//	 * 
//	 */
//	private void doFinishSave() {
//		ModuleHandle model = editor.getModel();
//		Object[] resolves = ElementAdapterManager.getAdapters(model,
//				IRelatedFileChangeResolve.class);
//		if (resolves == null) {
//			return;
//		}
//
//		for (int i = 0; i < resolves.length; i++) {
//			IRelatedFileChangeResolve find = (IRelatedFileChangeResolve) resolves[i];
//			find.notifySaveFile(model);
//		}
//	}
	
	@Override
	public void dispose() {
		editor.getModel().removeListener(nameChangeListener);
		editor = null;
	}

	@Override
	public void addPages() {	
		editor.getModel().addListener(nameChangeListener);
		if (editor.getEditorInput() instanceof IntelEntityEditorInput){
			IntelEntityEditorInput in = getEditorInputLocal();
			//if we are editing a SMART report update the name; otherwise leave it alone 
			editor.setPartName(in.getEntityType().getName()); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	@Override
	public void init(RCPMultiPageReportEditor editor) {
		this.editor = editor;
	}

	private IntelEntityEditorInput getEditorInputLocal(){
		if (editor == null){
			return null;
		}
		return (IntelEntityEditorInput)editor.getEditorInput();
	}

}
