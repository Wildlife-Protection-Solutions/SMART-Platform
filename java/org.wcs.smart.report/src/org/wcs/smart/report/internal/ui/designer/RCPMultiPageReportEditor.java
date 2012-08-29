/*******************************************************************************
 * Copyright (c) 2004 Actuate Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Actuate Corporation  - initial API and implementation
 *******************************************************************************/

package org.wcs.smart.report.internal.ui.designer;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.List;

import org.eclipse.birt.report.designer.internal.ui.editors.IRelatedFileChangeResolve;
import org.eclipse.birt.report.designer.ui.editors.IReportProvider;
import org.eclipse.birt.report.designer.ui.editors.MultiPageReportEditor;
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
import org.eclipse.birt.report.model.api.elements.structures.OdaDataSetParameter;
import org.eclipse.birt.report.model.elements.OdaDataSet;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchPartConstants;
import org.eclipse.ui.PartInitException;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.report.IReportListener;
import org.wcs.smart.report.ReportEventManager;
import org.wcs.smart.report.ReportEventManager.EventType;
import org.wcs.smart.report.ReportPlugIn;
import org.wcs.smart.report.internal.ui.CreateReportDialog;
import org.wcs.smart.report.internal.ui.viewer.parameter.SmartDateParameterComponent;
import org.wcs.smart.report.manger.ReportManager;
import org.wcs.smart.report.model.Report;
import org.wcs.smart.report.model.ReportFolder;
import org.wcs.smart.report.model.RootReportFolder;
import org.wcs.smart.report.ui.SmartReportEditorInput;


/**
 * RCPMultiPageReportEditor - from BIRT example.
 */
public class RCPMultiPageReportEditor extends MultiPageReportEditor implements IReportListener{

	/**
	 * The ID of the Report Editor
	 */
	public static final String REPROT_EDITOR_ID = "org.eclipse.birt.report.designer.ui.editors.ReportEditor"; //$NON-NLS-1$
	/**
	 * The ID of the Template Editor
	 */
	public static final String TEMPLATE_EDITOR_ID = "org.eclipse.birt.report.designer.ui.editors.TemplateEditor"; //$NON-NLS-1$
	/**
	 * The ID of the Library Editor
	 */
	public static final String LIBRARY_EDITOR_ID = "org.eclipse.birt.report.designer.ui.editors.LibraryEditor"; //$NON-NLS-1$

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.birt.report.designer.ui.editors.MultiPageReportEditor#init
	 * (org.eclipse.ui.IEditorSite, org.eclipse.ui.IEditorInput)
	 */
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		super.init(site, input);
		getSite().getWorkbenchWindow().getPartService().addPartListener(this);
		ReportEventManager.getInstance().addReportListener(this);
		
	}

	//TODO: this is a hack to name the
	//smart query datasources with the query name
	//I couldn't figure out how to do this any other way.
	private Listener nameChangeListener = new Listener(){
		@Override
		public void elementChanged(DesignElementHandle focus,
				NotificationEvent ev) {
			if (ev.getTarget() instanceof OdaDataSet && ev.getEventType() == NotificationEvent.NAME_EVENT){

				NameEvent ne = (NameEvent)ev;
				OdaDataSet ds = (OdaDataSet)ev.getTarget();
				OdaDataSetHandle handle = (OdaDataSetHandle)ds.getHandle(ev.getTarget().getRoot());
				
				if (handle.getExtensionID().startsWith(ReportManager.SMART_DATASOURCE_ID)
					 && ne.getOldName() == null 
					 && ne.getNewName().startsWith("Data Set")
					&& !ds.getDisplayName().equals(ne.getNewName())){
				
					try{
						handle.setName(((OdaDataSet)ev.getTarget()).getDisplayName());
					}catch (Exception ex){
						//eat me - we cant update the name for whatever reason
					}
				}
			}else if (ev.getEventType() == NotificationEvent.CONTENT_EVENT){
				//auto link start and end data parameters to report parameters
				ContentEvent ce = (ContentEvent)ev;
				if (ce.getAction() == ContentEvent.ADD && ce.getContent() instanceof OdaDataSet){
					OdaDataSet ds = (OdaDataSet)ce.getContent();
					OdaDataSetHandle handle = (OdaDataSetHandle) (ds).getHandle(ev.getTarget().getRoot());
					if (ce.getAction() == ContentEvent.ADD && 
						handle.getExtensionID().equals(ReportManager.SMART_DATASET_TYPE)){
						PropertyHandle odaDataSetParameterProp = handle.getPropertyHandle(OdaDataSetHandle.PARAMETERS_PROP);
						List<?> items = odaDataSetParameterProp.getItems();
						for (Iterator<?> iterator = items.iterator(); iterator.hasNext();) {
							OdaDataSetParameter parameter = (OdaDataSetParameter) iterator.next();
							if (parameter.getName().equals(SmartDateParameterComponent.START_DATE_NAME) || 
									parameter.getName().equals(SmartDateParameterComponent.END_DATE_NAME)){
								parameter.setDefaultValue("");
								parameter.setParamName(parameter.getName());
							}
						}
					}
				}				
			}
		}
		
	};
	
	@Override
	public void addPages(){
		super.addPages();
		super.getModel().addListener(nameChangeListener);
	}
	
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.birt.report.designer.ui.editors.MultiPageReportEditor#dispose
	 * ()
	 */
	public void dispose() {
		super.getModel().removeListener(nameChangeListener);
		super.dispose();
		getSite().getWorkbenchWindow().getPartService()
				.removePartListener(this);
		ReportEventManager.getInstance().removeReportListener(this);
	}

	public void doSave(IProgressMonitor monitor) {
		Session s = HibernateManager.openSession();
		try{
			s.beginTransaction();
			if (getModel() instanceof ReportDesignHandle){
				ReportManager.updateReportQueries(s, (ReportDesignHandle)getModel(), getEditorInputLocal().getReport());
			}
			super.doSave(monitor);
			s.getTransaction().commit();
		}catch (Exception ex){
			s.getTransaction().rollback();
			ReportPlugIn.displayLog("Could not save report: " + ex.getMessage(), ex);
		}finally{
			s.close();
		}
		try {
			refreshMarkers(getEditorInput());
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void refreshMarkers(IEditorInput input) throws CoreException {
		ModuleHandle reportDesignHandle = getModel();
		if (reportDesignHandle != null) {
			reportDesignHandle.checkReport();
		}
	}

	@Override
	/**
	 * Overrides the default save method to save a copy of the report to the 
	 * database and create a new file.
	 */
	public void doSaveAs() {
		
		if (!(super.getEditorInput() instanceof SmartReportEditorInput)){
			MessageDialog.openError(getSite().getShell(), "Error", "Cannot perform save-as on non Smart Reports");
			return;
		}
		//get save name/location
		final Report report = ((SmartReportEditorInput) super.getEditorInput())
				.getReport();
		final CreateReportDialog dialog = new CreateReportDialog(getSite()
				.getShell(), report.getFolder(), "Copy of " + report.getName(), true);
		if (dialog.open() != IDialogConstants.OK_ID) {
			return;
		}

		ProgressMonitorDialog pmd = new ProgressMonitorDialog(getSite()
				.getShell());
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
					copy.setOwner(SmartDB.getCurrentEmployee());
					copy.setShared(isShared);

					Session s = HibernateManager.openSession();
					try {
						s.beginTransaction();
						copy.setId(ReportManager.generateReportId(s));
						copy.setFilename(ReportManager.generateFilename(copy));
						s.save(copy);
						
						ReportManager.updateReportQueries(s, (ReportDesignHandle)getModel(), copy);
						s.getTransaction().commit();
					} catch (Exception ex) {
						s.getTransaction().rollback();
						ReportPlugIn.displayLog("Error Saving copy of report to the database." + ex.getMessage(), ex);
						return;
					} finally {
						if (s != null) {
							s.close();
						}
					}

					//update editor input
					final SmartReportEditorInput input = new SmartReportEditorInput(copy);
					try{			
					getSite().getShell().getDisplay().syncExec(new Runnable() {
						@Override
						public void run() {
								//save report file and fire update listeners
								IReportProvider provider = getProvider();
								provider.saveReport(getModel(), input, monitor);
								fireSaveAsUpdate(copy);
						
						}
					});
					}catch (Exception ex){
						ReportPlugIn.displayLog("Error creating report file. " + ex.getMessage(), ex);
						return;
					}
					
					//open new editor and close me
					Display.getDefault().syncExec(new Runnable(){
						@Override
						public void run() {
							ReportManager.editReport(copy);
							getEditorSite().getPage().closeEditor(RCPMultiPageReportEditor.this, false);
						}
						
					});
					
				}
			});
		} catch (Exception ex) {
			ReportPlugIn.displayLog("Error Saving copy of report." + ex.getMessage(), ex);
		}
	}

	private void fireSaveAsUpdate(Report report) {
		IReportProvider provider = getProvider();
		if (provider != null) {
			setPartName(provider.getInputPath(getEditorInput()).lastSegment());
			firePropertyChange(IWorkbenchPartConstants.PROP_PART_NAME);
			getProvider().getReportModuleHandle(getEditorInput()).setFileName(
					getProvider().getInputPath(getEditorInput()).toOSString());
		}
		updateRelatedViews();
		doFinishSave();
		ReportEventManager.getInstance().fireReportAdded(report);
	}

	/*
	 * Fires the required notifications.  This is copied from
	 *  org.eclipse.birt.report.designer.internal.ui.util.UIUtil.doFinishSave
	 * 
	 */
	private void doFinishSave() {
		ModuleHandle model = getModel();
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

	private SmartReportEditorInput getEditorInputLocal(){
		return (SmartReportEditorInput) super.getEditorInput();
	}
	
	
	/* (non-Javadoc)
	 * @see org.wcs.smart.report.IReportListener#reportEvent(java.lang.Object, org.wcs.smart.report.ReportEventManager.EventType)
	 */
	@Override
	public void reportEvent(Object o, EventType eventType) {
		if (eventType == EventType.REPORT_DELETED){
			if (getEditorInputLocal().getReport().equals(o)){
				//close me; I have been deleted
				Display.getDefault().asyncExec(new Runnable(){
					@Override
					public void run() {
						//TODO figure out how to remove this from the perspective editor tracker
						getSite().getPage().closeEditor(RCPMultiPageReportEditor.this, false);
					}
				});
				
			}
		}
		
	}
}
