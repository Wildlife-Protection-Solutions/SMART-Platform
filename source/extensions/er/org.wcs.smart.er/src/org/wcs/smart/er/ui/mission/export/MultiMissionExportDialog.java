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


package org.wcs.smart.er.ui.mission.export;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

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
import org.hibernate.Session;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.hibernate.SurveyFilter;
import org.wcs.smart.er.hibernate.SurveyMissionProxy;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.ui.SurveyFilterDialog;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.util.SmartUtils;

/**
 * Dialog to allow users to export multiple patrols at once.
 * 
 * This code performs validation to ensure at least one patrol is selected
 * and that the output directory is a valid directory.
 * 
 * @author Jeff
 *
 */

public class MultiMissionExportDialog extends XmlMultiExportTreeViewerDialog {

	private static final String OUTPUT_DIR = "outputDir"; //$NON-NLS-1$
	private static final String INCLUDE_ATTACHMENT = "attachements"; //$NON-NLS-1$
	private static final String EXPORT_DIALOGTITLE = Messages.MultiMissionExportDialog_DialotTitle;

	private static IDialogSettings dialogSettings = new DialogSettings("org.wcs.smart.patrol.export.dialog"); //$NON-NLS-1$
	static{
		dialogSettings.put(INCLUDE_ATTACHMENT, true);
	}

	private SurveyFilter currentFilter;
	
	
	/**
	 * Creates a new dialog.
	 * 
	 * @param parentShell parent shell
	 */
	public MultiMissionExportDialog(Shell parentShell, SurveyFilter defaultFilter) {
		super(parentShell, Messages.MultiMissionExportDialog_FilterLabel);
		if (defaultFilter != null) {
			currentFilter = defaultFilter;
		}else {
			currentFilter = new SurveyFilter();
		}	
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		setTitle(Messages.MultiMissionExportDialog_Title);
		setMessage(Messages.MultiMissionExportDialog_Message);
		getShell().setText(Messages.MultiMissionExportDialog_Title);
		loadObjectData();
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
		
		if (super.getTreeViewer().getCheckedElements().length == 0) {
			MessageDialog.openInformation(getShell(), EXPORT_DIALOGTITLE, Messages.MultiMissionExportDialog_SelectionRequired);
			return false;
		}
		
		Path dir = Paths.get(txtFile.getText());
		if (!Files.exists(dir)) {
			if (!MessageDialog.openQuestion(getShell(), EXPORT_DIALOGTITLE, MessageFormat.format(Messages.MultiMissionExportDialog_DirectoryDoesNotExist, new Object[]{dir.toAbsolutePath().toString()}))) {
				return false;
			}
			if (!SmartUtils.createDirectory(dir)){
				EcologicalRecordsPlugIn.displayLog(Messages.MultiMissionExportDialog_CouldNotCreate, null);
				return false;
			}
		}else if (!Files.isDirectory(dir)){
			EcologicalRecordsPlugIn.displayLog(MessageFormat.format(Messages.MultiMissionExportDialog_InvalidDirectory, new Object[]{dir.toString()}),null);
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
		SurveyFilterDialog pfd = new SurveyFilterDialog(getShell(), this, currentFilter);
		pfd.open();
	}

	@Override
	protected void loadObjectData() {
		Job loadMissions = new Job(Messages.MultiMissionExportDialog_LoadingListJobName){
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				List<SurveyMissionProxy> dataList = new ArrayList<>();
				
				try(Session s = HibernateManager.openSession()){
					s.beginTransaction();
					try{
						dataList.addAll(currentFilter.executeQuery(s));
					}finally{
						if (s.getTransaction().isActive()){
							s.getTransaction().commit();
						}
					}
				}
		
				if (getShell() == null || getShell().isDisposed()) return Status.OK_STATUS;
				
				getShell().getDisplay().asyncExec(new Runnable(){
					@Override
					public void run() {
						if (getTreeViewer().getTree().isDisposed()) return;
						getTreeViewer().setInput(dataList);
						getTreeViewer().refresh();
					}
				});
				return Status.OK_STATUS;
			}
			
		};
		loadMissions.schedule();	
	}

	@Override
	public boolean isResizable(){
		return true;
	}

	@Override
	public void updateContent() {
		loadObjectData();
	}

	public SurveyFilter getFilter() {
		return currentFilter;
	}

}