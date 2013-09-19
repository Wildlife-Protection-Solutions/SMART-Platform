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
package org.wcs.smart.cybertracker.export;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.cybertracker.model.ICyberTrackerConstants;
import org.wcs.smart.cybertracker.util.PdaUtil;
import org.wcs.smart.dataentry.DataentryHibernateManager;
import org.wcs.smart.dataentry.dialog.ConfigurableModelLabelProvider;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.util.SmartUtils;

/**
 * Dialog for exporting CyberTracker application data.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class CyberTrackerExportDialog extends TitleAreaDialog {

	private static final String OUTPUT_FILE = "outputFile"; //$NON-NLS-1$
	private static final String LAUNCH_CT   = "launchCT"; //$NON-NLS-1$
	
	private static final String DEFAULT_CTX_FILENAME = "smart.ctx"; //$NON-NLS-1$
	
	private static IDialogSettings dialogSettings = new DialogSettings("org.wcs.smart.cybertracker.export"); //$NON-NLS-1$
	
//	private CyberTrackerExporter exporter = new CyberTrackerExporter();
	private CyberTrackerConfExporter exporter = new CyberTrackerConfExporter();
	
	private Button btnToDevice;
	private Button btnToFile;

	private Text txtFile;
	private Button btnBrowse;	

	private Button btnLaunchCT;
	
	private File selectedFile;
	private ConfigurableModel selectedModel;

    private ComboViewer modelViewer;
	
	public CyberTrackerExportDialog(Shell parentShell) {
		super(parentShell);
	}
	
	/**
	 * @see org.eclipse.jface.dialogs.TitleAreaDialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createDialogArea(Composite parent){
		Composite composite = (Composite) super.createDialogArea(parent);
		Composite main = new Composite(composite, SWT.NONE);
		main.setLayout(new GridLayout(1, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		Composite modelSelector = new Composite(main, SWT.NONE);
		modelSelector.setLayout(new GridLayout(2, false));
		modelSelector.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		Label modelLabel = new Label(modelSelector, SWT.NONE);
		modelLabel.setText(Messages.CyberTrackerExportDialog_ConfigurableModel);
		modelViewer = new ComboViewer(modelSelector, SWT.READ_ONLY);
		modelViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		modelViewer.setContentProvider(ArrayContentProvider.getInstance());
		modelViewer.setLabelProvider(new ConfigurableModelLabelProvider());
		modelViewer.setInput(getModelsList().toArray());
		modelViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				selectedModel = (ConfigurableModel) ((IStructuredSelection)modelViewer.getSelection()).getFirstElement();
				getButton(IDialogConstants.OK_ID).setEnabled(true);
			}
		});
		
		
		btnToDevice = new Button(main, SWT.RADIO);
		btnToDevice.setSelection(true);
		btnToDevice.setText(Messages.CyberTrackerExportDialog_ExportToDevice);
		btnToDevice.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				exportOptionChanged();
			}
		});
		
		btnToFile = new Button(main, SWT.RADIO);
		btnToFile.setSelection(false);
		btnToFile.setText(Messages.CyberTrackerExportDialog_ExportToFile);
		btnToFile.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				exportOptionChanged();
			}
		});

		Composite toFileCmp = new Composite(main, SWT.NONE);
		GridLayout fileCmpLayout = new GridLayout(1, false);
		fileCmpLayout.marginLeft = 5;
		toFileCmp.setLayout(fileCmpLayout);
		toFileCmp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Composite fileCmp = new Composite(toFileCmp, SWT.NONE);
		fileCmp.setLayout(new GridLayout(3, false));
		fileCmp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label lbl = new Label(fileCmp, SWT.NONE);
		lbl.setText(Messages.CyberTrackerExportDialog_Label_File);
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));

		txtFile = new Text(fileCmp, SWT.BORDER);
		txtFile.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		txtFile.setText(getDefaultFileName());
		
		txtFile.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				if (txtFile.getText() != null && !txtFile.getText().isEmpty()) {
					if (getButton(IDialogConstants.OK_ID) != null) {
						getButton(IDialogConstants.OK_ID).setEnabled(true);
					}
				}
			}
		});
		
		btnBrowse = new Button(fileCmp, SWT.NONE);
		btnBrowse.setText(Messages.CyberTrackerExportDialog_Button_Browse);
		btnBrowse.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog fd = new FileDialog(getShell(), SWT.SAVE);
				fd.setFilterExtensions(new String[]{"*.ctx", "*.*"}); //$NON-NLS-1$ //$NON-NLS-2$
				fd.setFilterNames(new String[]{Messages.CyberTrackerExportDialog_CtxFile, Messages.CyberTrackerExportDialog_AllFiles});
				
				if (txtFile.getText() != null && !txtFile.getText().isEmpty()) {
					fd.setFileName(txtFile.getText());
				}
				String f = fd.open();
				if (f != null) {
					txtFile.setText(f);
					getButton(IDialogConstants.OK_ID).setEnabled(true);
				}
			}
		});
		btnBrowse.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		
		btnLaunchCT = new Button(fileCmp, SWT.CHECK);
		btnLaunchCT.setText(Messages.CyberTrackerExportDialog_LaunchInCyberTracker);
		btnLaunchCT.setSelection(getDefaultLaunchCT());
		btnLaunchCT.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3,1));
		exportOptionChanged();
		
		setTitle(Messages.CyberTrackerExportDialog_Title);
		setMessage(Messages.CyberTrackerExportDialog_Message);
		super.getShell().setText(Messages.CyberTrackerExportDialog_Title);
		super.setTitleImage(CyberTrackerPlugIn.getDefault().getImageRegistry().get(CyberTrackerPlugIn.CT_WIZARD_BANNER));
		return composite;
	}
	
	private void exportOptionChanged() {
		txtFile.setEnabled(btnToFile.getSelection());
		btnBrowse.setEnabled(btnToFile.getSelection());
		btnLaunchCT.setEnabled(btnToFile.getSelection());
	}
	
	/**
	 * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		createButton(parent, IDialogConstants.OK_ID, Messages.CyberTrackerExportDialog_Button_Export, true);
		createButton(parent, IDialogConstants.CANCEL_ID,IDialogConstants.CANCEL_LABEL, false);
		getButton(IDialogConstants.OK_ID).setEnabled(false);
		getButton(IDialogConstants.CANCEL_ID).setFocus();		
		super.setReturnCode(IDialogConstants.CANCEL_ID);
	}
	
	/**
	 * @see org.eclipse.jface.dialogs.Dialog#buttonPressed(int)
	 */
	@Override
	protected void buttonPressed(int buttonId) {
		if (IDialogConstants.OK_ID == buttonId) {
			if (btnToFile.getSelection()) {
				selectedFile = getOutputFile();
				if (selectedFile == null) {
					return;
				}
				dialogSettings.put(OUTPUT_FILE, selectedFile.getAbsolutePath());
				dialogSettings.put(LAUNCH_CT, Boolean.valueOf(btnLaunchCT.getSelection()));
			}
			handleExport(btnToDevice.getSelection());
			super.setReturnCode(IDialogConstants.OK_ID);
		}
		close();
	}

	private File getOutputFile() {
		File file = new File(txtFile.getText());
		if (!file.exists()) {
			if (!file.getParentFile().exists()) {
				if (MessageDialog.openQuestion(getShell(), Messages.CyberTrackerExportDialog_ConfirmOverwrite_Title, MessageFormat.format(Messages.CyberTrackerExportDialog_ConfirmCreateDir_Message, file.getParent()))) {
					if (!SmartUtils.createDirectory(file.getParentFile())) {
						return null;
					}
					return file;
				}
				return null;
			}
		} else {
			if (!MessageDialog.openQuestion(getShell(), Messages.CyberTrackerExportDialog_ConfirmOverwrite_Title, MessageFormat.format(Messages.CyberTrackerExportDialog_ConfirmOverwrite_Message, file.toString()))) {
				return null;
			}
		}
		return file;
	}
	
	private void handleExport(final boolean toDevice) {
		if (!isCyberTrackerInstalled()) {
			CyberTrackerPlugIn.displayError(Messages.CyberTrackerExportHandler_ErrDialog_Title, MessageFormat.format(Messages.CyberTrackerExportDialog_Error_CT_NotFound, ICyberTrackerConstants.MIN_VERSION), null);
			return;
		}
		final boolean launch = !toDevice && btnLaunchCT.getSelection();
		
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
		try {
			pmd.run(true, false, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					monitor.beginTask(Messages.CyberTrackerExportHandler_TaskName, 100);
					File tempDir;
					try {
						tempDir = PdaUtil.createTempDirectory();
					} catch (IOException e) {
						CyberTrackerPlugIn.displayError(Messages.CyberTrackerExportHandler_ErrDialog_Title, Messages.CyberTrackerExportDialog_FailCreateTempFolder, e);
						return;
					}
	
					try {
						File generated = exporter.export(tempDir, selectedModel, monitor);
//						File generated = exporter.export(tempDir, monitor);
						if (generated == null) {
							return; //error is supposed to be tracked inside export call
						}
						if (toDevice) {
							monitor.subTask(Messages.CyberTrackerExportDialog_Task_Upload);
							try {
								final int code = exporter.uploadPda(generated);
								if (code != ICyberTrackerConstants.UPLOAD_CODE_SUCCESS) {
									String codeMeaning = getCyberTrackerCodeMeaning(code);
									CyberTrackerPlugIn.displayError(Messages.CyberTrackerExportHandler_ErrDialog_Title, MessageFormat.format(Messages.CyberTrackerExportDialog_ErrDialog_UploadFailed, code, codeMeaning), null);
									return;
								}
							} catch (Exception e) {
								CyberTrackerPlugIn.displayError(Messages.CyberTrackerExportHandler_ErrDialog_Title, Messages.CyberTrackerExportDialog_Error_UploadFailed, e);
								return;
							}

						} else {
							monitor.subTask(Messages.CyberTrackerExportDialog_Task_Copy);
							try {
								FileUtils.copyFile(generated, selectedFile);
							} catch (IOException e) {
								CyberTrackerPlugIn.displayError(Messages.CyberTrackerExportHandler_ErrDialog_Title, Messages.CyberTrackerExportDialog_Error_CopyFailed, e);
								return;
							}
						}
					} finally {
						PdaUtil.deleteTempDirectory(tempDir);
						monitor.done();
					}
					CyberTrackerPlugIn.displayInfo(Messages.CyberTrackerExportHandler_InfoDialog_Title, Messages.CyberTrackerExportHandler_InfoDialog_Message);
					if (launch) {
						Job job = new LaunchCTJob(selectedFile);
						job.schedule();
					}
				}

			});
		} catch (Exception e) {
			CyberTrackerPlugIn.log(e.getMessage(), e);
		}
	}

	private boolean isCyberTrackerInstalled() {
		try {
			return PdaUtil.getCTAppPath() != null;
		} catch (Exception e) {
			return false;
		}
	}
	
	private String getCyberTrackerCodeMeaning(int code) {
		switch (code) {
		case ICyberTrackerConstants.UPLOAD_CODE_CONNECT_FAIL: return Messages.CyberTrackerExportDialog_CTCode_100;
		case ICyberTrackerConstants.UPLOAD_CODE_IMPORT_FAIL: return Messages.CyberTrackerExportDialog_CTCode_101;
		case ICyberTrackerConstants.UPLOAD_CODE_APP_NOT_FOUND: return Messages.CyberTrackerExportDialog_CTCode_110;
		case ICyberTrackerConstants.UPLOAD_CODE_SYNC_FAIL: return Messages.CyberTrackerExportDialog_CTCode_120;
		case ICyberTrackerConstants.UPLOAD_CODE_CT_NOT_INSTALLED: return Messages.CyberTrackerExportDialog_CTCode_201;
		}
		return ""; //$NON-NLS-1$
	}
	
	@Override
	protected boolean isResizable() {
		return true;
	}

	protected String getDefaultFileName() {
		String name = dialogSettings.get(OUTPUT_FILE);
		if (name != null)
			return name;

		name = System.getProperty("user.home"); //$NON-NLS-1$
		if (name != null)
			return name + "\\" + DEFAULT_CTX_FILENAME; //$NON-NLS-1$
		
		return DEFAULT_CTX_FILENAME;
	}

	protected boolean getDefaultLaunchCT() {
		String name = dialogSettings.get(LAUNCH_CT);
		return "true".equals(name); //$NON-NLS-1$
	}

	private List<ConfigurableModel> getModelsList() {
		List<ConfigurableModel> modelList = new ArrayList<ConfigurableModel>();
		Session s = HibernateManager.openSession();
		s.beginTransaction();
		try {
			modelList = DataentryHibernateManager.getConfigurableModels(s);
		} catch (Exception ex) {
			SmartPlugIn.displayLog(Display.getDefault().getActiveShell(), Messages.CyberTrackerExportDialog_LoadConfModels_Error, ex);
		} finally {
			s.getTransaction().rollback();
			s.close();
		}
		return modelList;
	}
	
	private class LaunchCTJob extends Job {

		private File file;
		
		public LaunchCTJob(File file) {
			super(Messages.CyberTrackerExportDialog_Job_LaunchCT);
			this.file = file;
		}
		
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			try {
				String ctPath = PdaUtil.getCTAppPath();
				String[] launchCommands = {ctPath, ICyberTrackerConstants.COMMAND_DATAFILE, file.getAbsolutePath()};
				Runtime.getRuntime().exec(launchCommands);
			} catch (Exception e) {
				CyberTrackerPlugIn.displayError(Messages.CyberTrackerExportHandler_ErrDialog_Title, Messages.CyberTrackerExportDialog_Error_LaunchCT, e);
			}
			return Status.OK_STATUS;
		}
	}

}
