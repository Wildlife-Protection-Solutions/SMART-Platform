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
import java.util.List;

import org.eclipse.birt.report.designer.core.model.SessionHandleAdapter;
import org.eclipse.birt.report.designer.data.ui.dataset.DataSetUIUtil;
import org.eclipse.birt.report.model.api.DataSetHandle;
import org.eclipse.birt.report.model.api.ReportDesignHandle;
import org.eclipse.birt.report.model.api.SessionHandle;
import org.eclipse.birt.report.model.api.SlotHandle;
import org.eclipse.birt.report.model.core.DesignElement;
import org.eclipse.birt.report.model.elements.OdaDataSet;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.handlers.HandlerUtil;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.report.ReportEventManager;
import org.wcs.smart.report.ReportPlugIn;
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
public class NewReportHandler extends AbstractHandler implements IHandler {

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {

		ISelection thisSelection = HandlerUtil.getCurrentSelection(event);
		Object selection = null;
		if (thisSelection == null || thisSelection.isEmpty() || !(thisSelection instanceof IStructuredSelection) ){
			selection = null;
		}else{
			selection = ((IStructuredSelection)thisSelection).getFirstElement();
			if (!(selection instanceof ReportFolder || selection instanceof RootReportFolder)){
				selection = null;
			}
		}
		
		File smartLibrary = SmartBirtLibrary.getInstance().getLibraryFile();
		if (!smartLibrary.exists()) {
			// TODO: I don't think we need to throw ane xception here - perhaps
			// just
			// an error dialog instead.
			throw new IllegalStateException("SMART Library does not exist.");
		}

		// display dialog
		CreateReportDialog dialog = new CreateReportDialog(
				HandlerUtil.getActiveShell(event), selection);
		if (dialog.open() != IDialogConstants.OK_ID) {
			return null;
		}

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
		report.setOwner(SmartDB.getCurrentEmployee());
		report.setName(reportName);
		report.setShared(isShared);
		report.setFolder(parentFolder);

		//save report
		Job createReportJob = new Job("Creating Report...") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				File reportFile = null;
				try {
					//save report object o database
					report.setId(ReportManager.generateReportId());
					report.setFilename(ReportManager.generateFilename(report));

					Session hsession = HibernateManager.openSession();
					try{
						hsession.beginTransaction();
						hsession.save(report);
						hsession.getTransaction().commit();
					}catch (Exception ex){
						hsession.getTransaction().rollback();
						throw ex;
					}finally{
						hsession.close();
					}

					//create report file with default library
					reportFile = new File(ReportPlugIn.getReportDirectory(),
							report.getFilename());

					
					SessionHandle session = SessionHandleAdapter.getInstance()
							.getSessionHandle();
					ReportDesignHandle rdh = session.createDesign(reportFile
							.getAbsolutePath());
					rdh.setTitle(report.getName());
					SlotHandle datasets = rdh.getDataSets();
					List<DesignElement> elements = datasets.getSlot()
							.getContents();
					for (DesignElement ds : elements) {
						OdaDataSet dataset = (OdaDataSet) ds;
						// TODO: figure out how to automatically update dataset
						DataSetHandle handle = (DataSetHandle) dataset
								.getHandle(rdh.getModule());
						DataSetUIUtil.updateColumnCache(handle);
					}

					rdh.save();
					rdh.close();
				} catch (Exception ex) {
					ReportPlugIn.displayLog(
							"Could not create report: " + ex.getMessage(), ex);
					return Status.OK_STATUS;
				}

				
				//edit report perspective
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						ReportManager.editReport(report);

					}
				});
				ReportEventManager.getInstance().fireReportAdded(report);
				
				return Status.OK_STATUS;
			}

		};
		createReportJob.schedule();

		return null;
	}

	
	
}
