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
package org.wcs.smart.report.internal.ui;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import javax.inject.Named;

import org.eclipse.birt.report.designer.core.model.SessionHandleAdapter;
import org.eclipse.birt.report.model.api.DataSourceHandle;
import org.eclipse.birt.report.model.api.DesignElementHandle;
import org.eclipse.birt.report.model.api.LibraryHandle;
import org.eclipse.birt.report.model.api.OdaDataSourceHandle;
import org.eclipse.birt.report.model.api.ParameterGroupHandle;
import org.eclipse.birt.report.model.api.ReportDesignHandle;
import org.eclipse.birt.report.model.api.SessionHandle;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.report.ReportEventManager;
import org.wcs.smart.report.ReportPlugIn;
import org.wcs.smart.report.SmartReportParameters;
import org.wcs.smart.report.internal.Messages;
import org.wcs.smart.report.library.SmartBirtLibrary;
import org.wcs.smart.report.manger.ReportManager;
import org.wcs.smart.report.model.Report;
import org.wcs.smart.report.model.ReportFolder;
import org.wcs.smart.report.model.RootReportFolder;

/**
 * Handler for creating a new SMART report.
 * @author egouge
 * @since 1.0.0
 */
public class NewReportHandler {

	@Execute
	public void execute(@Optional @Named(IServiceConstants.ACTIVE_SELECTION) Object thisSelection, final Shell activeShell){
		Object value = null;
		
		if (thisSelection != null 
				&& thisSelection instanceof IStructuredSelection 
				&& !((IStructuredSelection)thisSelection).isEmpty() ){
			Object first = ((IStructuredSelection)thisSelection).getFirstElement();
			if (first instanceof ReportFolder || first instanceof RootReportFolder){
				value = first;
			}else if (first instanceof Report){
				value = ((Report) first).getFolder();
				if (value == null){
					if ( ((Report)first).getShared()){
						value = RootReportFolder.CA_ROOT_FOLDER;
					}else{
						value = RootReportFolder.USER_ROOT_FOLDER;
					}
				}
			}
		}
		
		final File smartLibrary = SmartBirtLibrary.getInstance().getLibraryFile();
		if (!smartLibrary.exists()) {
			throw new IllegalStateException(Messages.NewReportHandler_Error_NoLibrary);
		}

		// display dialog
		CreateReportDialog dialog = new CreateReportDialog(activeShell, value);
		if (dialog.open() != IDialogConstants.OK_ID) return;

		String reportName = dialog.getReportName();

		Object x = dialog.getReportFolder();
		ReportFolder parentFolder = null;
		boolean isShared = false;
		if (x instanceof ReportFolder) {
			parentFolder = (ReportFolder) x;
			isShared = parentFolder.getEmployee() == null;
		} else if (x instanceof RootReportFolder) {
			isShared = ((RootReportFolder) x).isShared();
		}
		//create new report object
		final Report report = new Report();
		report.setConservationArea(SmartDB.getCurrentConservationArea());
		
		report.updateName(SmartDB.getCurrentLanguage(), reportName);
		report.setName(reportName);
		if (!SmartDB.getCurrentLanguage().isDefault()){
			report.updateName(SmartDB.getCurrentConservationArea().getDefaultLanguage(), reportName);	
		}
		
		report.setShared(isShared);
		report.setFolder(parentFolder);
		if (SmartDB.isMultipleAnalysis() && report.getShared()){
			report.setOwner(SmartDB.getSharedEmployee());
		}else{
			report.setOwner(SmartDB.getCurrentEmployee());
		}
		
		//save report
		Job createReportJob = new Job(Messages.NewReportHandler_Progress_CreatingReport) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				File reportFile = null;
				Session hsession = null;
				try{
					hsession = HibernateManager.openSession();
					hsession.beginTransaction();
					
					//save report object o database
					report.setId(ReportManager.generateReportId(SmartDB.getCurrentConservationArea(), hsession));
					report.setFilename(ReportManager.generateFilename(report));
					
					hsession.save(report);
					hsession.getTransaction().commit();
				}catch (Exception ex){
					if (hsession != null){
						hsession.getTransaction().rollback();
					}
					ReportPlugIn.displayLog(Messages.NewReportHandler_Error_CouldNotCreateReport + ex.getLocalizedMessage(), ex);
					return Status.OK_STATUS;
				}finally{
					if (hsession != null){
						hsession.close();
					}
				}

				boolean canEdit = true;
				try{
					//create report file with default library
					reportFile = new File(ReportPlugIn.getReportDirectory(report.getConservationArea()), report.getFilename());

					SessionHandle session = SessionHandleAdapter.getInstance().getSessionHandle();

					//open the library so we can copy stuff from the library to the report
					LibraryHandle library = session.openLibrary(smartLibrary.getPath());
					DesignElementHandle param = null;
					
					List<?> paramGroups = library.getParametersAndParameterGroups();
					for (Iterator<?> iterator = paramGroups.iterator(); iterator.hasNext();) {
						Object type = (Object) iterator.next();
						if (type instanceof ParameterGroupHandle){
							if (((ParameterGroupHandle) type).getName().equals(SmartReportParameters.PARAM_DATEGROUP_NAME)){
								param = (ParameterGroupHandle)type;
								break;
							}
						}	
					}
					
//					List
					DataSourceHandle dataSource = null;
					List<?> dataSources = library.getAllDataSources();
					for (Iterator<?> iterator = dataSources.iterator(); iterator.hasNext();) {
						Object type = (Object) iterator.next();
						if (type instanceof OdaDataSourceHandle){
							if (((OdaDataSourceHandle)type).getExtensionID().equals(ReportManager.SMART_DATASOURCE_ID)){
								dataSource = (DataSourceHandle) type;
								break;
							}
						}
					}
					
					//open the report
					ReportDesignHandle rdh = session.createDesign(reportFile.getAbsolutePath());
					rdh.setTitle(report.getName());
					
					//add default library
					rdh.includeLibrary(SmartBirtLibrary.getInstance().getLibraryFileString(), SmartBirtLibrary.DEFAULT_LIBRARY_NAMESPACE);
					
					//add date parameter automatically
					if (param != null){
						rdh.getParameters().add(rdh.getElementFactory().newElementFrom(param, param.getName()));
					}
					if (dataSource != null){
						rdh.getDataSources().add(rdh.getElementFactory().newElementFrom(dataSource, dataSource.getName()));
					}
					
					try{
						library.close();
					}catch (Exception ex){
						ReportPlugIn.displayLog(
								Messages.NewReportHandler_Error_CouldNotCloseLibrary + ex.getLocalizedMessage(), ex);
					}
					
					rdh.save();
					rdh.close();
				} catch (Exception ex) {
					ReportPlugIn.displayLog(
							Messages.NewReportHandler_Error_CreatingReport + ex.getLocalizedMessage(), ex);
					canEdit = false;
				}

				
				//edit report perspective
				if (canEdit){
					activeShell.getDisplay().asyncExec(new Runnable() {
						@Override
						public void run() {
							ReportManager.editReport(report);

						}
					});
				}
				ReportEventManager.getInstance().fireReportAdded(report);
				
				
				return Status.OK_STATUS;
			}

		};
		createReportJob.schedule();
	}

	
	public static class NewReportHandlerWrapper extends DIHandler<NewReportHandler>{
		public NewReportHandlerWrapper(){
			super(NewReportHandler.class);
		}
	}
}
