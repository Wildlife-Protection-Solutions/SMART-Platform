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
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.cybertracker.model.ICyberTrackerConstants;
import org.wcs.smart.cybertracker.util.PdaUtil;
import org.wcs.smart.cybertracker.util.WinRegistry;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.properties.LanguageViewer;
import org.wcs.smart.util.SmartUtils;

/**
 * Dialog for exporting CyberTracker application data.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public abstract class CyberTrackerExportDialog extends TitleAreaDialog {

	private static final String OUTPUT_FILE = "outputFile"; //$NON-NLS-1$
	private static final String LAUNCH_CT   = "launchCT"; //$NON-NLS-1$
	
	private static final String DEFAULT_CTX_FILENAME = "smart.ctx"; //$NON-NLS-1$
	
	private static IDialogSettings dialogSettings = new DialogSettings("org.wcs.smart.cybertracker.export"); //$NON-NLS-1$
	
	private Button btnToDevice;
	private Button btnToFile;

	private Text txtFile;
	private Button btnBrowse;	

	private Button btnLaunchCT;
	
	private Label lblFile;
	private File selectedFile;

    private LanguageViewer languageViewer;
	
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

		String warnMessage = validateCyberTrackerVersion();
		if (warnMessage != null) {
			Label warnLabel = new Label(main, SWT.NONE);
			GridData layoutData = new GridData(SWT.FILL, SWT.FILL, true, false);
			layoutData.horizontalIndent = 15;
			warnLabel.setLayoutData(layoutData);
			warnLabel.setText(warnMessage);

			FontData fd = warnLabel.getFont().getFontData()[0];
			fd.setStyle(SWT.BOLD);
			final Font boldFont = new Font(warnLabel.getDisplay(), fd);
			warnLabel.setFont(boldFont);
			warnLabel.setForeground(warnLabel.getDisplay().getSystemColor(SWT.COLOR_DARK_RED));
			warnLabel.addDisposeListener(new DisposeListener() {
				
				@Override
				public void widgetDisposed(DisposeEvent e) {
					boldFont.dispose();
				}
			});
			
			ControlDecoration warnDecoration = new ControlDecoration(warnLabel, SWT.LEFT);
			warnDecoration.setImage(FieldDecorationRegistry.getDefault()
					.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
			warnDecoration.setMarginWidth(4);
			warnDecoration.show();
			
			Label lbl = new Label(main, SWT.SEPARATOR | SWT.HORIZONTAL);
			lbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			
		}
		
		
		Composite modelSelector = new Composite(main, SWT.NONE);
		modelSelector.setLayout(new GridLayout(2, false));
		modelSelector.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		addModelSourceControl(modelSelector);

		Label languageLabel = new Label(modelSelector, SWT.NONE);
		languageLabel.setText(Messages.CyberTrackerExportDialog_Language);
		languageViewer = new LanguageViewer(modelSelector, SWT.DROP_DOWN | SWT.READ_ONLY, SmartDB.getCurrentConservationArea());
		languageViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label lblOp = new Label(modelSelector, SWT.NONE);
		lblOp.setText(Messages.CyberTrackerExportDialog_ExportOptionsLabel);
		lblOp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2,1));
		
		btnToDevice = new Button(modelSelector, SWT.RADIO);
		GridData gd = new GridData(SWT.FILL, SWT.CENTER,true, false, 2, 1);
		gd.horizontalIndent = 10;
		btnToDevice.setLayoutData(gd);
		
		btnToDevice.setSelection(true);
		btnToDevice.setText(Messages.CyberTrackerExportDialog_ExportToDevice);
		btnToDevice.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				exportOptionChanged();
			}
		});
		
		btnToFile = new Button(modelSelector, SWT.RADIO);
		btnToFile.setSelection(false);
		gd = new GridData(SWT.FILL, SWT.CENTER,true, false, 2, 1);
		gd.horizontalIndent = 10;
		btnToFile.setLayoutData(gd);
		btnToFile.setText(Messages.CyberTrackerExportDialog_ExportToFile);
		btnToFile.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				exportOptionChanged();
			}
		});

		Composite toFileCmp = new Composite(modelSelector, SWT.NONE);
		GridLayout fileCmpLayout = new GridLayout(1, false);
		fileCmpLayout.marginLeft = 15;
		toFileCmp.setLayout(fileCmpLayout);
		toFileCmp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		Composite fileCmp = new Composite(toFileCmp, SWT.NONE);
		fileCmp.setLayout(new GridLayout(3, false));
		fileCmp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		lblFile = new Label(fileCmp, SWT.NONE);
		lblFile.setText(Messages.CyberTrackerExportDialog_Label_File);
		lblFile.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));

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

	protected abstract void addModelSourceControl(Composite parent);
	
    protected abstract IConfigurableModelProvider getConfigurableModelProvider();

	protected abstract CyberTrackerConfExporter getExporter();
	
	private void exportOptionChanged() {
		lblFile.setEnabled(btnToFile.getSelection());
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
			CyberTrackerPlugIn.displayError(Messages.CyberTrackerExportHandler_ErrDialog_Title, MessageFormat.format(Messages.CyberTrackerExportDialog_Error_CT_NotFound, ICyberTrackerConstants.DISPLAY_MIN_VERSION), null);
			return;
		}
		final boolean launch = !toDevice && btnLaunchCT.getSelection();
		final CyberTrackerConfExporter exporter = getExporter();
		exporter.setCurrentLanguage(languageViewer.getCurrentSelection());
		
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
		try {
			pmd.run(true, false, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					File tempDir;
					try {
						tempDir = PdaUtil.createTempDirectory();
					} catch (IOException e) {
						CyberTrackerPlugIn.displayError(Messages.CyberTrackerExportHandler_ErrDialog_Title, Messages.CyberTrackerExportDialog_FailCreateTempFolder, e);
						return;
					}
	
					try {
						File generated = exporter.export(tempDir, getConfigurableModelProvider(), monitor);
						if (generated == null) {
							return; //error is supposed to be tracked inside export call
						}
						if (toDevice) {
							monitor.subTask(Messages.CyberTrackerExportDialog_Task_Upload);
							try {
								final int code = PdaUtil.uploadPda(generated);
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
					}catch (Exception ex){
						CyberTrackerPlugIn.displayError(Messages.CyberTrackerExportHandler_ErrDialog_Title, 
								Messages.CyberTrackerExportDialog_Error_ExportError + "\n\n" + ex.getMessage(), ex); //$NON-NLS-1$
						return;
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
			CyberTrackerPlugIn.displayError(Messages.CyberTrackerExportHandler_ErrDialog_Title, 
					Messages.CyberTrackerExportDialog_Error_ExportError , e); 
		}
	}

	private boolean isCyberTrackerInstalled() {
		try {
			return PdaUtil.getCTAppPath() != null;
		} catch (Exception e) {
			return false;
		}
	}

	protected String validateCyberTrackerVersion() {
		String version = null;
		try {
			version = WinRegistry.readString(WinRegistry.HKEY_CURRENT_USER,
					ICyberTrackerConstants.REG_KEY_PATH, ICyberTrackerConstants.REG_KEY_VERSION);
		} catch (Exception e) {
			String message = MessageFormat.format(Messages.CyberTrackerExportDialog_Error_CT_NotFound, ICyberTrackerConstants.DISPLAY_MIN_VERSION);
			CyberTrackerPlugIn.displayError(Messages.CyberTrackerExportHandler_ErrDialog_Title, message, e);
			return message;
		}
		if (version == null){
			String message = MessageFormat.format(Messages.CyberTrackerExportDialog_Error_CT_NotFound, ICyberTrackerConstants.DISPLAY_MIN_VERSION);
			CyberTrackerPlugIn.displayError(Messages.CyberTrackerExportHandler_ErrDialog_Title, message, null);
			return message;
		}
		String[] parts = version.split("\\."); //$NON-NLS-1$
		String[] reqParts = ICyberTrackerConstants.INSTALL_MIN_VERSION.split("\\."); //$NON-NLS-1$
		//create a display version from the current version
		//this removes the second part and the first 0 of the third part
		//IE 3.00.0345 becomes 3.345
		//this is an artifact of the CT installer.
		String displayVersion = parts[0] + "." + (parts[2].startsWith("0") ? parts[2].substring(1) : parts[2]); //$NON-NLS-1$ //$NON-NLS-2$
		if (parts.length > reqParts.length)
			return MessageFormat.format(Messages.CyberTrackerExportDialog_Warn_CT_Version, displayVersion, ICyberTrackerConstants.DISPLAY_MIN_VERSION);
		for (int i = 0; i < reqParts.length; i++) {
			Integer val = Integer.valueOf(parts[i]);
			Integer reqVal = Integer.valueOf(reqParts[i]);
			if (val < reqVal) {
				return MessageFormat.format(Messages.CyberTrackerExportDialog_Warn_CT_Version, displayVersion, ICyberTrackerConstants.DISPLAY_MIN_VERSION);
			}
		}
		return null;
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
