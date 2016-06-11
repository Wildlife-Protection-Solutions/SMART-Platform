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
import java.util.Iterator;
import java.util.List;

import org.eclipse.birt.report.designer.internal.ui.editors.IRelatedFileChangeResolve;
import org.eclipse.birt.report.designer.ui.editors.IReportProvider;
import org.eclipse.birt.report.designer.ui.views.ElementAdapterManager;
import org.eclipse.birt.report.model.api.DesignElementHandle;
import org.eclipse.birt.report.model.api.ModuleHandle;
import org.eclipse.birt.report.model.api.OdaDataSetHandle;
import org.eclipse.birt.report.model.api.PropertyHandle;
import org.eclipse.birt.report.model.api.ReportDesignHandle;
import org.eclipse.birt.report.model.api.activity.NotificationEvent;
import org.eclipse.birt.report.model.api.command.ContentEvent;
import org.eclipse.birt.report.model.api.command.NameEvent;
import org.eclipse.birt.report.model.api.core.Listener;
import org.eclipse.birt.report.model.api.elements.structures.ColumnHint;
import org.eclipse.birt.report.model.api.elements.structures.OdaDataSetParameter;
import org.eclipse.birt.report.model.elements.OdaDataSet;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ui.IWorkbenchPartConstants;
import org.hibernate.Session;
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

				if (handle.getExtensionID().startsWith(
						ReportManager.SMART_DATASOURCE_ID)
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
							&& handle.getExtensionID().equals(
									ReportManager.SMART_DATASET_TYPE)) {
						
						//link parameters
						PropertyHandle odaDataSetParameterProp = handle
								.getPropertyHandle(OdaDataSetHandle.PARAMETERS_PROP);
						List<?> items = odaDataSetParameterProp.getItems();
						for (Iterator<?> iterator = items.iterator(); iterator
								.hasNext();) {
							OdaDataSetParameter parameter = (OdaDataSetParameter) iterator
									.next();
							if (parameter.getName().equals(
									SmartReportParameters.PARAM_START_DATE_KEY)
									|| parameter
											.getName()
											.equals(SmartReportParameters.PARAM_END_DATE_KEY)) {
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
							ReportPlugIn.log(ex.getMessage(), ex);
						}
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
		Session s = HibernateManager.openSession();
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
		}finally{
			s.close();
		}
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
					
					Session s = HibernateManager.openSession();
					try {
						s.beginTransaction();
						copy.setId(ReportManager.generateReportId(SmartDB.getCurrentConservationArea(), s));
						copy.setFilename(ReportManager.generateFilename(copy));
						s.save(copy);
						
						ReportManager.updateReportQueries(s, (ReportDesignHandle)editor.getModel(), copy);
						s.getTransaction().commit();
					} catch (Exception ex) {
						s.getTransaction().rollback();
						ReportPlugIn.displayLog(Messages.RCPMultiPageReportEditor_SaveAsError + ex.getLocalizedMessage(), ex);
						return;
					} finally {
						if (s != null) {
							s.close();
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
		
		ReportEventManager.getInstance().removeReportListener(this);
		editor = null;
	}

	@Override
	public void addPages() {
		editor.getModel().addListener(nameChangeListener);		
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
