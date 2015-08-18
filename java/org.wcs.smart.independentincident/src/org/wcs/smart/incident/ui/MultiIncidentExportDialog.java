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
package org.wcs.smart.incident.ui;

import java.io.File;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.List;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.common.control.XmlMultiExportDialog;
import org.wcs.smart.common.filter.DateFilterComposite.DateFilter;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.incident.IncidentPlugIn;
import org.wcs.smart.incident.internal.Messages;
import org.wcs.smart.util.SmartUtils;

/**
 * Dialog to allow users to export multiple incidents at once.
 * 
 * This code performs validation to ensure at least one incident is selected
 * and that the output directory is a valid directory.
 * 
 * @author egouge
 *
 */
public class MultiIncidentExportDialog extends XmlMultiExportDialog implements IIncidentFilteringView {

	private static final String OUTPUT_DIR = "outputDir"; //$NON-NLS-1$
	private static final String INCLUDE_ATTACHMENT = "attachements"; //$NON-NLS-1$
	private static final String EXPORT_DIALOGTITLE = "Export Incidents"; //$NON-NLS-1$

	private static IDialogSettings dialogSettings = new DialogSettings("org.wcs.smart.incident.export.dialog"); //$NON-NLS-1$
	
	static{
		dialogSettings.put(INCLUDE_ATTACHMENT, true);
	}

	private IncidentFilter currentFilter = new IncidentFilter();
	
	/**
	 * Creates a new dialog.
	 * 
	 * @param parentShell parent shell
	 * @param incident incident to export
	 */
	public MultiIncidentExportDialog(Shell parentShell) {
		super(parentShell, Messages.MultiIncidentExportDialog_ChangeFilter);
		this.currentFilter.setDateFilter(DateFilter.LAST_30_DAYS, null, null);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		setTitle(EXPORT_DIALOGTITLE);
		setMessage(Messages.MultiIncidentExportDialog_ExportOk);
		getShell().setText(EXPORT_DIALOGTITLE);
		return super.createDialogArea(parent);
	}
	/**
	 * @see org.eclipse.jface.dialogs.Dialog#buttonPressed(int)
	 */
	protected void buttonPressed(int buttonId) {
		if (buttonId == IDialogConstants.OK_ID){
			if(!validate()){
				return;
			}
		}
		super.buttonPressed(buttonId);
		dialogSettings.put(OUTPUT_DIR, getDirectory());
		dialogSettings.put(INCLUDE_ATTACHMENT, getIncludeAttachments());
	}
	
	private boolean validate(){
		
		if (super.getTableViewer().getCheckedElements().length == 0) {
			MessageDialog.openInformation(getShell(), EXPORT_DIALOGTITLE, Messages.MultiIncidentExportDialog_NothingToExport);
			return false;
		}
		
		File dir = new File(txtFile.getText());
		if (!dir.exists()) {
			if (!MessageDialog.openQuestion(getShell(), EXPORT_DIALOGTITLE, MessageFormat.format(Messages.MultiIncidentExportDialog_DirectoryNotFound, new Object[]{dir.getAbsolutePath()}))) {
				return false;
			}
			if (!SmartUtils.createDirectory(dir)){
				IncidentPlugIn.displayLog(Messages.MultiIncidentExportDialog_CouldNotCreateDirector, null);
				return false;
			}
		}else if (!dir.isDirectory()){
			IncidentPlugIn.displayLog(MessageFormat.format(Messages.MultiIncidentExportDialog_InvalidDirectory, new Object[]{dir.toString()}),null);
			return false;
		}
		return true;
	}

	@Override
	protected boolean getDefaultIncludeAttachments() {
		return dialogSettings.getBoolean(INCLUDE_ATTACHMENT);
	}
	
	@Override
	protected String getDefaultOutputFolder() {
		return dialogSettings.get(OUTPUT_DIR);
	}
	
	@Override
	protected void handleFilterLinkClicked() {
		IncidentFilterDialog pfd = new IncidentFilterDialog(getShell(), MultiIncidentExportDialog.this);
		pfd.open();
	}

	@Override
	protected void loadObjectData() {
		Job loadIncidents = new Job(Messages.MultiIncidentExportDialog_Loading){
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				Session s = HibernateManager.openSession();
				s.beginTransaction();
				try{
					Query q = currentFilter.buildQuery(s);
					List<?> results = q.list();
					final Object[][] data = new Object[results.size()][2];
					int counter = 0;
					for(Object x : results){
						Object[] row = (Object[])x;
						
						String pname = row[1] + " [" + DateFormat.getDateInstance(DateFormat.SHORT).format((Timestamp)row[2]) + "]";   //$NON-NLS-1$ //$NON-NLS-2$
						Object[] thisdata = {pname, (UUID)row[0], row[1]};
						data[counter++] = thisdata;
					}
					
					getShell().getDisplay().asyncExec(new Runnable(){
						@Override
						public void run() {
							getTableViewer().setInput(data);
							getTableViewer().refresh();
						}
					});
					
				}finally{
					if (s.getTransaction().isActive()){
						s.getTransaction().commit();
					}
					s.close();
				}
				return Status.OK_STATUS;
			}
			
		};
		loadIncidents.schedule();
		
	}

	@Override
	public boolean isResizable(){
		return true;
	}

	@Override
	public void updateContent() {
		loadObjectData();
	}

	@Override
	public IncidentFilter getFilter() {
		return currentFilter;
	}

}
