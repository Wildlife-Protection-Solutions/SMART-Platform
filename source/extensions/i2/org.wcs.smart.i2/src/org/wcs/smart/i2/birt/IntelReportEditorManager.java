/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2.birt;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.birt.report.model.api.DesignElementHandle;
import org.eclipse.birt.report.model.api.ModuleHandle;
import org.eclipse.birt.report.model.api.OdaDataSetHandle;
import org.eclipse.birt.report.model.api.PropertyHandle;
import org.eclipse.birt.report.model.api.activity.NotificationEvent;
import org.eclipse.birt.report.model.api.command.ContentEvent;
import org.eclipse.birt.report.model.api.command.NameEvent;
import org.eclipse.birt.report.model.api.command.NameException;
import org.eclipse.birt.report.model.api.core.Listener;
import org.eclipse.birt.report.model.api.elements.structures.ColumnHint;
import org.eclipse.birt.report.model.api.elements.structures.OdaDataSetParameter;
import org.eclipse.birt.report.model.elements.OdaDataSet;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.wcs.smart.birt.ui.IReportEditorManager;
import org.wcs.smart.birt.ui.RCPMultiPageReportEditor;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.birt.datasource.DataSourceParameter;
import org.wcs.smart.i2.birt.entity.EntityDataset;
import org.wcs.smart.i2.birt.entity.EntityLocationAttributeDataset;
import org.wcs.smart.i2.birt.entity.attachment.EntityAttachmentDataset;
import org.wcs.smart.i2.birt.entity.location.EntityLocationDataset;
import org.wcs.smart.i2.birt.entity.records.EntityRecordDataset;
import org.wcs.smart.i2.birt.entity.relation.EntityRelationDataset;
import org.wcs.smart.i2.birt.record.RecordAttributeDataset;
import org.wcs.smart.i2.birt.record.RecordDataset;
import org.wcs.smart.i2.birt.record.attachment.RecordAttachmentDataset;
import org.wcs.smart.i2.birt.record.entities.RecordEntityDataset;
import org.wcs.smart.i2.birt.record.location.RecordLocationDataset;

/**
 * Manager for entity report editor.
 * 
 * @author Emily
 *
 */
public class IntelReportEditorManager implements IReportEditorManager{ 


	private RCPMultiPageReportEditor editor;
	
	private static final Set<String> SUPPORTED_DATASETS = new HashSet<String>();
	static{
		SUPPORTED_DATASETS.add(EntityLocationAttributeDataset.DATASET_TYPE);
		SUPPORTED_DATASETS.add(EntityDataset.DATASET_TYPE);
		SUPPORTED_DATASETS.add(EntityLocationDataset.DATASET_TYPE);
		SUPPORTED_DATASETS.add(EntityRecordDataset.DATASET_TYPE);
		SUPPORTED_DATASETS.add(EntityAttachmentDataset.DATASET_TYPE);
		SUPPORTED_DATASETS.add(EntityRelationDataset.DATASET_TYPE);
		
		SUPPORTED_DATASETS.add(RecordDataset.DATASET_TYPE);
		SUPPORTED_DATASETS.add(RecordAttributeDataset.DATASET_TYPE);
		SUPPORTED_DATASETS.add(RecordEntityDataset.DATASET_TYPE);
		SUPPORTED_DATASETS.add(RecordAttachmentDataset.DATASET_TYPE);
		SUPPORTED_DATASETS.add(RecordLocationDataset.DATASET_TYPE);
	};
	
	// This listener is a hack to name the
	// smart query datasources with the query name
	// I couldn't figure out how to do this any other way.
	private Listener nameChangeListener = new Listener() {
		@Override
		public void elementChanged(DesignElementHandle focus,
				NotificationEvent ev) {
			if (ev.getTarget() instanceof OdaDataSet && ev.getEventType() == NotificationEvent.NAME_EVENT) {
				//update name
				NameEvent ne = (NameEvent) ev;
				OdaDataSet ds = (OdaDataSet) ev.getTarget();
				OdaDataSetHandle handle = (OdaDataSetHandle) ds.getHandle(ev.getTarget().getRoot());

				String dsId = handle.getExtensionID();
				if ( SUPPORTED_DATASETS.contains(dsId)
						&& ne.getOldName() == null
						&& ne.getNewName().startsWith(org.eclipse.birt.report.designer.nls.Messages.getString("dataset.new.defaultName")) //$NON-NLS-1$
						&& (ds.getDisplayName() == null || !ds.getDisplayName().equals(ne.getNewName()))) {
					
					String newName = ((OdaDataSet) ev.getTarget()).getDisplayName();
					String coreName = newName;
					int cnt = 1;
					while(true){
						try {
							handle.setName(newName);
							break;
						}catch (NameException nex){
							if (!nex.getErrorCode().equals(NameException.DESIGN_EXCEPTION_DUPLICATE)){
								nex.printStackTrace();
								break;
							}
							//duplicate name - lets add a number and try again
						}
						newName = coreName + " - " + cnt;
						cnt++;
					}
				}
			} else if (ev.getEventType() == NotificationEvent.CONTENT_EVENT) {
				// auto link parameters to report parameters
				ContentEvent ce = (ContentEvent) ev;
				if (ce.getAction() == ContentEvent.ADD && ce.getContent() instanceof OdaDataSet) {
					
					OdaDataSet ds = (OdaDataSet) ce.getContent();
					OdaDataSetHandle handle = (OdaDataSetHandle) (ds).getHandle(ev.getTarget().getRoot());
					if (SUPPORTED_DATASETS.contains(handle.getExtensionID())) {
						//link parameters
						PropertyHandle odaDataSetParameterProp = handle.getPropertyHandle(OdaDataSetHandle.PARAMETERS_PROP);
						List<?> items = odaDataSetParameterProp.getItems();
						for (Iterator<?> iterator = items.iterator(); iterator.hasNext();) {
							OdaDataSetParameter parameter = (OdaDataSetParameter) iterator.next();
							if (parameter.getName().equals(DataSourceParameter.ENTITY_UUID.getName())) {
								parameter.setDefaultValue(""); //$NON-NLS-1$
								parameter.setParamName(parameter.getName());
							}else if (parameter.getName().equals(DataSourceParameter.START_DATE.getName())) {
								parameter.setDefaultValue(""); //$NON-NLS-1$
								parameter.setParamName(parameter.getName());
							}else if (parameter.getName().equals(DataSourceParameter.END_DATE.getName())) {
								parameter.setDefaultValue(""); //$NON-NLS-1$
								parameter.setParamName(parameter.getName());
							}else if (parameter.getName().equals(DataSourceParameter.RECORD_UUID.getName())) {
								parameter.setDefaultValue(""); //$NON-NLS-1$
								parameter.setParamName(parameter.getName());
							}
						}

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
		if (editor.getEditorInput() instanceof IntelEntityTypeEditorInput){
			IntelEntityTypeEditorInput in = getEditorInputLocal();
			//if we are editing a SMART report update the name; otherwise leave it alone 
			editor.setPartName(in.getEntityType().getName()); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	@Override
	public void init(RCPMultiPageReportEditor editor) {
		this.editor = editor;
	}

	private IntelEntityTypeEditorInput getEditorInputLocal(){
		if (editor == null){
			return null;
		}
		return (IntelEntityTypeEditorInput)editor.getEditorInput();
	}

}
