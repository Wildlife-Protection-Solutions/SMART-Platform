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
package org.wcs.smart.intelligence.xml.export;

import java.io.File;
import java.text.MessageFormat;
import java.util.Date;
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
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.intelligence.internal.Messages;
import org.wcs.smart.intelligence.model.Intelligence;
import org.wcs.smart.intelligence.ui.IIntelligenceFilteringView;
import org.wcs.smart.intelligence.ui.IntelligenceFilterDialog;
import org.wcs.smart.intelligence.ui.IntelligenceViewFilter;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.util.SmartUtils;

/**
 * Dialog to allow users to export multiple intelligences at once.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class MultiIntelligenceExportDialog extends XmlMultiExportDialog implements IIntelligenceFilteringView {

	private static final String OUTPUT_DIR = "outputDir"; //$NON-NLS-1$
	private static final String INCLUDE_ATTACHMENT = "attachements"; //$NON-NLS-1$
	
	private static IDialogSettings dialogSettings = new DialogSettings("org.wcs.smart.intelligence.export.dialog"); //$NON-NLS-1$
	static{
		dialogSettings.put(INCLUDE_ATTACHMENT, true);
	}

	private IntelligenceViewFilter currentFilter = new IntelligenceViewFilter();

	/**
	 * Creates a new dialog
	 * @param parentShell parent shell
	 * @param patrol patrol to export
	 */
	public MultiIntelligenceExportDialog(Shell parentShell) {
		super(parentShell, Messages.MultiIntelligenceExportDialog_ChangeFilter);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		setTitle(Messages.MultiIntelligenceExportDialog_PageTitle1);
		setMessage(Messages.MultiIntelligenceExportDialog_Message);
		getShell().setText(Messages.MultiIntelligenceExportDialog_Title1);
		return super.createDialogArea(parent);
	}
	/**
	 * @see org.eclipse.jface.dialogs.Dialog#buttonPressed(int)
	 */
	protected void buttonPressed(int buttonId) {
		if (buttonId == IDialogConstants.OK_ID){
			if (!validate()){
				return;
			}
		}
		super.buttonPressed(buttonId);
		dialogSettings.put(OUTPUT_DIR, getDirectory());
		dialogSettings.put(INCLUDE_ATTACHMENT, getIncludeAttachments());
	}
	
	private boolean validate(){
		
		if (super.getTableViewer().getCheckedElements().length == 0) {
			MessageDialog.openInformation(getShell(),  Messages.MultiIntelligenceExportDialog_Title1, Messages.ExportIntelligenceHandler_NothingSelected);
			return false;
		}
		
		File dir = new File(txtFile.getText());
		if (!dir.exists()) {
			if (!MessageDialog.openQuestion(getShell(), Messages.MultiIntelligenceExportDialog_Title1, MessageFormat.format(Messages.ExportIntelligenceHandler_DirectoryNotExist, new Object[]{dir.getAbsolutePath()}))) {
				return false;
			}
			if (!SmartUtils.createDirectory(dir)){
				SmartPatrolPlugIn.displayLog(Messages.MultiIntelligenceExportDialog_CouldNotCreateDirectoryError, null);
				return false;
			}
		}else if (!dir.isDirectory()){
			SmartPatrolPlugIn.displayLog(MessageFormat.format(Messages.MultiIntelligenceExportDialog_InvalidDirectoryError, new Object[]{dir.toString()}),null);
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
		IntelligenceFilterDialog fd = new IntelligenceFilterDialog(getShell(), this);
		fd.open();
	}

	@Override
	protected void loadObjectData() {
		Job loadItelJob = new Job(Messages.MultiIntelligenceExportDialog_LoadJobName){
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
						String dname = Intelligence.generateLabel((String)row[1], (Date)row[2]);
						Object[] thisdata = {dname, (UUID)row[0], row[1]};
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
		loadItelJob.schedule();
		
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
	public IntelligenceViewFilter getFilter() {
		return currentFilter;
	}
	
}
