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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.swt.widgets.Item;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.report.ReportEventManager;
import org.wcs.smart.report.ReportPlugIn;
import org.wcs.smart.report.internal.Messages;
import org.wcs.smart.report.model.Report;
import org.wcs.smart.report.model.ReportFolder;

/**
 * Cell editor for modifying report folder names or report names.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class ReportItemNameCellEditor implements ICellModifier {

	private boolean folderOnly = false;
	
	/**
	 * Creates a new cell editor
	 */
	public ReportItemNameCellEditor(){
		this(false);
	}
	
	/**
	 * Creates a new cell editor that only allows you to edit folders
	 * not other items
	 */
	public ReportItemNameCellEditor(boolean folderOnly){
		this.folderOnly = folderOnly;
	}
	
	@Override
	public void modify(Object element, String property,
			final Object value) {
		element = ((Item) element).getData();
		if (element instanceof ReportFolder) {
			updateReportFolder((ReportFolder) element, value.toString());
		}else if (!folderOnly && element instanceof Report){
			updateReport((Report) element, value.toString());
		}
	}

	/*
	 * updates the report name in the database
	 */
	private void updateReport(final Report report, final String value){

		if (value.equals(report.getName())) {
			// nothing to update
			return;
		}
		
		Job j = new Job(Messages.ReportItemNameCellEditor_UpdateFolderJobName) {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				Session session = HibernateManager.openSession();
				session.beginTransaction();
				try {
					session.saveOrUpdate(report);
					report.updateName(SmartDB.getCurrentLanguage(),value);
					report.setName(value);
					session.getTransaction().commit();
					ReportEventManager.getInstance().fireReportUpdated(report);
				} catch (Exception ex) {
					session.getTransaction().rollback();
					ReportPlugIn.displayLog(
							Messages.ReportItemNameCellEditor_Error_CouldNotSaveReport
									+ ex.getLocalizedMessage(), ex);
				} finally {
					session.close();
				}

				return Status.OK_STATUS;
			}
		};
		j.schedule();
	}
	/*
	 * updates a report folder name in the database
	 */
	private void updateReportFolder(final ReportFolder folder, final String value){
		
		if (value.equals(folder.getName())) {
			// nothing to update
			return;
		}
		
		Job j = new Job(Messages.ReportItemNameCellEditor_UpdateReportJobName) {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				Session session = HibernateManager.openSession();
				session.beginTransaction();
				try {
					session.saveOrUpdate((ReportFolder) folder);
					folder.updateName(SmartDB.getCurrentLanguage(), value);
					folder.setName(value);
					session.getTransaction().commit();
				} catch (Exception ex) {
					if (session.getTransaction().isActive()) session.getTransaction().rollback();
					ReportPlugIn.displayLog(
							Messages.ReportItemNameCellEditor_Error_CouldNoSaveFolder
									+ ex.getLocalizedMessage(), ex);
				} finally {
					session.close();
				}
				ReportEventManager.getInstance().fireReportFolderModified(folder);
				return Status.OK_STATUS;
			}
		};
		j.schedule();
	}
	@Override
	public Object getValue(Object element, String property) {
		if (element instanceof ReportFolder ){
			return ((ReportFolder) element).getName();
		}else if (element instanceof Report){
			return ((Report) element).getName();
		}
		return ""; //$NON-NLS-1$
	}

	@Override
	public boolean canModify(Object element, String property) {
		return  (element instanceof ReportFolder || (!folderOnly && element instanceof Report) );
	}
}