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
package org.wcs.smart.report.internal.ui.designer;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.eclipse.birt.report.designer.internal.ui.editors.IRelatedFileChangeResolve;
import org.eclipse.birt.report.designer.ui.editors.IReportProvider;
import org.eclipse.birt.report.designer.ui.views.ElementAdapterManager;
import org.eclipse.birt.report.model.adapter.oda.model.DesignValues;
import org.eclipse.birt.report.model.adapter.oda.model.util.SerializerImpl;
import org.eclipse.birt.report.model.api.CellHandle;
import org.eclipse.birt.report.model.api.DataItemHandle;
import org.eclipse.birt.report.model.api.DesignElementHandle;
import org.eclipse.birt.report.model.api.ModuleHandle;
import org.eclipse.birt.report.model.api.OdaDataSetHandle;
import org.eclipse.birt.report.model.api.PropertyHandle;
import org.eclipse.birt.report.model.api.ReportDesignHandle;
import org.eclipse.birt.report.model.api.RowHandle;
import org.eclipse.birt.report.model.api.SlotHandle;
import org.eclipse.birt.report.model.api.TableHandle;
import org.eclipse.birt.report.model.api.activity.NotificationEvent;
import org.eclipse.birt.report.model.api.command.ContentEvent;
import org.eclipse.birt.report.model.api.command.NameEvent;
import org.eclipse.birt.report.model.api.core.Listener;
import org.eclipse.birt.report.model.api.elements.DesignChoiceConstants;
import org.eclipse.birt.report.model.api.elements.structures.ColumnHint;
import org.eclipse.birt.report.model.api.elements.structures.NumberFormatValue;
import org.eclipse.birt.report.model.api.elements.structures.OdaDataSetParameter;
import org.eclipse.birt.report.model.elements.OdaDataSet;
import org.eclipse.birt.report.model.elements.TableItem;
import org.eclipse.birt.report.model.elements.interfaces.IDataItemModel;
import org.eclipse.birt.report.model.elements.interfaces.IDataSetModel;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.datatools.connectivity.oda.design.ColumnDefinition;
import org.eclipse.datatools.connectivity.oda.design.ResultSetColumns;
import org.eclipse.datatools.connectivity.oda.design.ResultSetDefinition;
import org.eclipse.datatools.connectivity.oda.design.ResultSets;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ui.IWorkbenchPartConstants;
import org.hibernate.Session;
import org.wcs.smart.birt.BirtSmartUtils;
import org.wcs.smart.birt.ui.IReportEditorManager;
import org.wcs.smart.birt.ui.RCPMultiPageReportEditor;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.report.IReportListener;
import org.wcs.smart.report.ReportEventManager;
import org.wcs.smart.report.ReportEventManager.EventType;
import org.wcs.smart.report.ReportPlugIn;
import org.wcs.smart.report.SmartReportParameters;
import org.wcs.smart.report.internal.Messages;
import org.wcs.smart.report.internal.ui.CreateReportDialog;
import org.wcs.smart.report.manger.ReportManager;
import org.wcs.smart.report.model.Report;
import org.wcs.smart.report.model.ReportFolder;
import org.wcs.smart.report.model.RootReportFolder;
import org.wcs.smart.report.ui.SmartReportEditorInput;

/**
 * Report edit manager for editing of SMART reports
 * 
 * @author Emily
 * @since 2.0.0
 *
 */
public class ReportEditorManager implements IReportEditorManager,IReportListener{ 

	//listener that listens for
	//table add events and tries to automatically apply
	//a numeric formatting pattern for smart datasets
	//whose metadata supplies a precision values
	private Listener formatListener = new Listener() {
		@SuppressWarnings({ "restriction", "unchecked" })
		@Override
		public void elementChanged(DesignElementHandle focus,
				NotificationEvent ev) {
		
			if (ev instanceof ContentEvent) {
				ContentEvent ce = (ContentEvent)ev;
				
				if (ce.getAction() == ContentEvent.ADD && 
						ce.getContent() instanceof TableItem) {
					
					try {
						org.eclipse.birt.report.model.core.Module root = ev.getTarget().getRoot();
						TableItem ti = (TableItem) ce.getContent();
						OdaDataSet dataset = (OdaDataSet) ti.getDataSetElement(root);
						OdaDataSetHandle handle = (OdaDataSetHandle) dataset.getHandle(root);
	
						if (!handle.getExtensionID().startsWith("org.wcs.smart")) return; //$NON-NLS-1$
						
						//find percision mappings from the designer values metadata					
						HashMap<String, Integer> precisionMap = new HashMap<>();
						HashMap<String, String> nameToKey = new HashMap<>();
						
						OdaDataSetHandle datasethandle = (OdaDataSetHandle)dataset.getHandle(ev.getTarget().getRoot());

						//reads the designer values which are stored as xml 
						String dvaluessml = datasethandle.getDesignerValues();
						DesignValues values = SerializerImpl.instance().read(dvaluessml);
						
						ResultSets imp = values.getResultSets();
						for (ResultSetDefinition d : imp.getResultSetDefinitions()) {
							ResultSetColumns cc = d.getResultSetColumns();
							for (ColumnDefinition cd : cc.getResultColumnDefinitions()) {
								if (cd.getAttributes().getPrecision() != -1) {
									precisionMap.put(cd.getAttributes().getName(), cd.getAttributes().getPrecision());
								}
							}	
						}
					
						//dataset column hints
						ArrayList<ColumnHint> hints = (ArrayList<ColumnHint>) dataset.getProperty(ev.getTarget().getRoot(), IDataSetModel.COLUMN_HINTS_PROP);
						for (ColumnHint h : hints) {
							String key = h.getStringProperty(ev.getTarget().getRoot(), ColumnHint.COLUMN_NAME_MEMBER);
							String name = h.getStringProperty(ev.getTarget().getRoot(), ColumnHint.ALIAS_MEMBER);
							nameToKey.put(name, key);
						}
					
					
						SlotHandle sl = ((TableHandle)ti.getHandle(root)).getDetail();
						RowHandle rh = (RowHandle) sl.get(0);
					
						for (int i = 0; i < ti.getColumnCount(ev.getTarget().getRoot()); i ++) {
							CellHandle cell = (CellHandle)rh.getCells().get(i);
							DataItemHandle di = (DataItemHandle) cell.getSlot(0).get(0);
						
							String columnName = di.getElement().getProperty(ev.getTarget().getRoot(), IDataItemModel.RESULT_SET_COLUMN_PROP).toString();
						
							Integer p = null;
							if (precisionMap.containsKey(columnName)) {
								p = precisionMap.get(columnName);
							}else if (nameToKey.containsKey(columnName) && precisionMap.containsKey(nameToKey.get(columnName))) {
								p = precisionMap.get(nameToKey.get(columnName));
							}
						
							if ( p != null) {
								NumberFormatValue vv = new NumberFormatValue();
							
								String pattern = "0"; //$NON-NLS-1$
								if (p > 0) {
									pattern += "." + "0".repeat(p); //$NON-NLS-1$ //$NON-NLS-2$
								}
								
								vv.setPattern(pattern);
								vv.setCategory(DesignChoiceConstants.NUMBER_FORMAT_TYPE_CUSTOM);
								cell.getSlot(0).get(0).setProperty(DesignChoiceConstants.CHOICE_NUMBER_FORMAT_TYPE, vv);
							}
						}
					}catch (Exception ex) {
						
					}
				}
			}
		}
	};
	
	// This listener is a hack to name the
	// smart query datasources with the query name
	// I couldn't figure out how to do this any other way.
	private Listener nameChangeListener = new Listener() {
		@Override
		public void elementChanged(DesignElementHandle focus,
				NotificationEvent ev) {
			if (ev.getTarget() instanceof OdaDataSet
					&& ev.getEventType() == NotificationEvent.NAME_EVENT) {

				NameEvent ne = (NameEvent) ev;
				OdaDataSet ds = (OdaDataSet) ev.getTarget();
				OdaDataSetHandle handle = (OdaDataSetHandle) ds.getHandle(ev
						.getTarget().getRoot());

				if (handle.getExtensionID().startsWith("org.wcs.smart") //$NON-NLS-1$
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
				if (ce.getAction() == ContentEvent.ADD
						&& ce.getContent() instanceof OdaDataSet) {
					OdaDataSet ds = (OdaDataSet) ce.getContent();
					OdaDataSetHandle handle = (OdaDataSetHandle) (ds)
							.getHandle(ev.getTarget().getRoot());
					if (ce.getAction() == ContentEvent.ADD
							&& handle.getExtensionID().startsWith("org.wcs.smart")) { //$NON-NLS-1$
//									ReportManager.SMART_DATASET_TYPE)) {
						
						//link parameters
						PropertyHandle odaDataSetParameterProp = handle
								.getPropertyHandle(OdaDataSetHandle.PARAMETERS_PROP);
						List<?> items = odaDataSetParameterProp.getItems();
						if (items != null) {
							for (Iterator<?> iterator = items.iterator(); iterator.hasNext();) {
								OdaDataSetParameter parameter = (OdaDataSetParameter) iterator.next();
								if (parameter.getName().equals(SmartReportParameters.PARAM_START_DATE_KEY)
										|| parameter.getName().equals(SmartReportParameters.PARAM_END_DATE_KEY)) {
									parameter.setDefaultValue(""); //$NON-NLS-1$
									parameter.setParamName(parameter.getName());
								}
							}
						}
						
						BirtSmartUtils.updateDatasetConfiguration(handle);
					}
				}
			}
		}

	};
		
	
	private RCPMultiPageReportEditor editor;
	
	public ReportEditorManager() {
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		ModuleHandle initialModel = editor.getModel();
		try(Session s = HibernateManager.openSession()){
			try{
				s.beginTransaction();
				if (editor.getModel() instanceof ReportDesignHandle){
					ReportManager.updateReportQueries(s, (ReportDesignHandle)editor.getModel(), getEditorInputLocal().getReport());
				}
				editor.doSaveParent(monitor);
				s.getTransaction().commit();
			}catch (Exception ex){
				s.getTransaction().rollback();
				ReportPlugIn.displayLog(Messages.RCPMultiPageReportEditor_Error_SavingReport + ex.getLocalizedMessage(), ex);
			}
		}
		try {
			//on the xml page saving changes the model so we need to reconfigure
			//the listeners
			if (!editor.getModel().equals(initialModel)){
				initialModel.removeListener(nameChangeListener);
				editor.getModel().addListener(nameChangeListener);
				
				initialModel.removeListener(formatListener);
				editor.getModel().addListener(formatListener);
			}
			editor.refreshMarkers(editor.getEditorInput());
		} catch (CoreException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void doSaveAs() {
		if (!(editor.getEditorInput() instanceof SmartReportEditorInput)){
			MessageDialog.openError(editor.getSite().getShell(), Messages.RCPMultiPageReportEditor_Error_DialogTitle, Messages.RCPMultiPageReportEditor_SaveAsError_NonSmart);
			return;
		}
		//get save name/location
		final Report report = getEditorInputLocal().getReport();
		final CreateReportDialog dialog = new CreateReportDialog(editor.getSite()
				.getShell(), report.getFolder(), Messages.RCPMultiPageReportEditor_CopyOfLabel + report.getName(), true);
		if (dialog.open() != IDialogConstants.OK_ID) {
			return;
		}

		ProgressMonitorDialog pmd = new ProgressMonitorDialog(editor.getSite().getShell());
		try {
			pmd.run(true, false, new IRunnableWithProgress() {

				@Override
				public void run(final IProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {

					final Report copy = new Report();
					copy.setConservationArea(report.getConservationArea());

					Object x = dialog.getReportFolder();
					ReportFolder parentFolder = null;
					boolean isShared = false;
					if (x instanceof ReportFolder) {
						parentFolder = (ReportFolder) x;
						isShared = parentFolder.getEmployee() == null;
					} else if (x instanceof RootReportFolder) {
						isShared = ((RootReportFolder) x).isShared();
					}

					copy.setFolder(parentFolder);
					copy.setShared(isShared);
					copy.setName(dialog.getReportName());
					copy.updateName(SmartDB.getCurrentLanguage(), dialog.getReportName());
					if (!SmartDB.getCurrentLanguage().isDefault()){
						copy.updateName(SmartDB.getCurrentConservationArea().getDefaultLanguage(), dialog.getReportName());
					}
					
					if (SmartDB.isMultipleAnalysis() && copy.getShared()){
						copy.setOwner(SmartDB.getSharedEmployee());
					}else{
						copy.setOwner(SmartDB.getCurrentEmployee());
					}
					
					try(Session s = HibernateManager.openSession()) {
						s.beginTransaction();
						try{
							copy.setId(ReportManager.generateReportId(SmartDB.getCurrentConservationArea(), s));
						
							copy.setFilename(ReportManager.generateFilename(copy));
							s.persist(copy);
							
							ReportManager.updateReportQueries(s, (ReportDesignHandle)editor.getModel(), copy);
							s.getTransaction().commit();
						} catch (Exception ex) {
							s.getTransaction().rollback();
							ReportPlugIn.displayLog(Messages.RCPMultiPageReportEditor_SaveAsError + ex.getLocalizedMessage(), ex);
							return;
						}
					}

					//update editor input
					final SmartReportEditorInput input = new SmartReportEditorInput(copy);
					try{			
					editor.getSite().getShell().getDisplay().syncExec(new Runnable() {
						@Override
						public void run() {
							//save report file and fire update listeners
							IReportProvider provider =editor.getProvider();
							provider.saveReport(editor.getModel(), input, monitor);
							fireSaveAsUpdate(copy);
						
						}
					});
					}catch (Exception ex){
						ReportPlugIn.displayLog(Messages.RCPMultiPageReportEditor_createFileError + ex.getLocalizedMessage(), ex);
						return;
					}
					
					//open new editor and close me
					editor.getSite().getShell().getDisplay().syncExec(new Runnable(){
						@Override
						public void run() {
							ReportManager.editReport(copy);
							editor.getEditorSite().getPage().closeEditor(editor, false);
						}
						
					});
					
				}
			});
		} catch (Exception ex) {
			ReportPlugIn.displayLog(Messages.RCPMultiPageReportEditor_SaveError + ex.getLocalizedMessage(), ex);
		}
	}

	private void fireSaveAsUpdate(Report report) {
		IReportProvider provider = editor.getProvider();
		if (provider != null) {
			editor.setPartName(provider.getInputPath(getEditorInputLocal()).lastSegment());
			editor.firePropertyChange(IWorkbenchPartConstants.PROP_PART_NAME);
			editor.getProvider().getReportModuleHandle(editor.getEditorInput()).setFileName(
					editor.getProvider().getInputPath(editor.getEditorInput()).toOSString());
		}
		editor.updateRelatedViews();
		doFinishSave();
		ReportEventManager.getInstance().fireReportAdded(report);
	}

	/*
	 * Fires the required notifications.  This is copied from
	 *  org.eclipse.birt.report.designer.internal.ui.util.UIUtil.doFinishSave
	 * 
	 */
	private void doFinishSave() {
		ModuleHandle model = editor.getModel();
		Object[] resolves = ElementAdapterManager.getAdapters(model,
				IRelatedFileChangeResolve.class);
		if (resolves == null) {
			return;
		}

		for (int i = 0; i < resolves.length; i++) {
			IRelatedFileChangeResolve find = (IRelatedFileChangeResolve) resolves[i];
			find.notifySaveFile(model);
		}
	}
	
	@Override
	public void dispose() {
		editor.getModel().removeListener(nameChangeListener);
		editor.getModel().removeListener(formatListener);
		
		ReportEventManager.getInstance().removeReportListener(this);
		editor = null;
	}

	@Override
	public void addPages() {
		editor.getModel().addListener(nameChangeListener);
		editor.getModel().addListener(formatListener);
		if (editor.getEditorInput() instanceof SmartReportEditorInput){
			SmartReportEditorInput in = getEditorInputLocal();
			//if we are editing a SMART report update the name; otherwise leave it alone 
			editor.setPartName(in.getReport().getName() + " [" + in.getReport().getId() + "]"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	@Override
	public void init(RCPMultiPageReportEditor editor) {
		this.editor = editor;
		ReportEventManager.getInstance().addReportListener(this);
	}

	private SmartReportEditorInput getEditorInputLocal(){
		if (editor == null){
			return null;
		}
		return (SmartReportEditorInput)editor.getEditorInput();
	}
	
	/* (non-Javadoc)
	 * @see org.wcs.smart.report.IReportListener#reportEvent(java.lang.Object, org.wcs.smart.report.ReportEventManager.EventType)
	 */
	@Override
	public void reportEvent(Object o, EventType eventType) {
		if (eventType == EventType.REPORT_DELETED){
			if (getEditorInputLocal() == null) return;
			if (getEditorInputLocal().getReport().equals(o)){
				//close me; I have been deleted
				editor.getSite().getShell().getDisplay().asyncExec(new Runnable(){
					@Override
					public void run() {
						editor.getSite().getPage().closeEditor(editor, false);
					}
				});
				
			}
		}else if (eventType == EventType.REPORT_UPDATED){
			if (getEditorInputLocal().getReport().equals(o)){
				editor.setPartName(((Report)o).getName());
			}
		}
		
	}
}
