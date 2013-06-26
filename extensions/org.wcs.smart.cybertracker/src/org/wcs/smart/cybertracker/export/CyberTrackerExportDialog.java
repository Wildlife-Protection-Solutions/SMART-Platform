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

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
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
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.internal.Messages;

/**
 * Dialog for exporting CyberTracker application data.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class CyberTrackerExportDialog extends TitleAreaDialog {

	private CyberTrackerExporter exporter = new CyberTrackerExporter();

	private Button btnToDevice;
	private Button btnToFile;

	private Text txtFile;
	private Button btnBrowse;	
	
	private File selectedFile;

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

		btnToDevice = new Button(main, SWT.RADIO);
		btnToDevice.setSelection(true);
		btnToDevice.setText("Export to Device (device must be connected to the computer)");
		btnToDevice.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				exportOptionChanged();
			}
		});
		
		btnToFile = new Button(main, SWT.RADIO);
		btnToFile.setSelection(false);
		btnToFile.setText("Export to CTX file");
		btnToFile.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				exportOptionChanged();
			}
		});
		
		Composite fileCmp = new Composite(main, SWT.NONE);
		fileCmp.setLayout(new GridLayout(3, false));
		fileCmp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label lbl = new Label(fileCmp, SWT.NONE);
		lbl.setText(Messages.CyberTrackerExportDialog_Label_File);
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));

		txtFile = new Text(fileCmp, SWT.BORDER);
		txtFile.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		txtFile.setText("c:\\dev\\CyberTracker\\out\\result.ctx");
		
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
				fd.setFilterNames(new String[]{"CyberTracker file (*.ctx)", "All files (*.*)"});
				
				if (txtFile.getText() != null && !txtFile.getText().isEmpty()) {
					fd.setFilterPath(txtFile.getText());
				}
				String f = fd.open();
				if (f != null) {
					txtFile.setText(f);
					getButton(IDialogConstants.OK_ID).setEnabled(true);
				}
			}
		});
		btnBrowse.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
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
	}
	
	/**
	 * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		createButton(parent, IDialogConstants.OK_ID, Messages.CyberTrackerExportDialog_Button_Export, true);
		createButton(parent, IDialogConstants.CANCEL_ID,IDialogConstants.CANCEL_LABEL, false);
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
					MessageDialog.openError(getShell(), Messages.CyberTrackerExportDialog_ErrDialog_Title, Messages.CyberTrackerExportDialog_Err_Dialog_Message);
					return;
				}
			}
			handleExport(btnToDevice.getSelection());
			super.setReturnCode(IDialogConstants.OK_ID);
		}
		close();
	}

	private File getOutputFile() {
		File file = new File(txtFile.getText());
		if (!file.exists()) {
			try {
				FileUtils.forceMkdir(file.getParentFile());
				file.createNewFile();
				return file;
			} catch (IOException ex) {
				return null;	
			}
		}
		return file;
	}
	
	private void handleExport(final boolean toDevice) {
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
		try {
			pmd.run(true, false, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					monitor.beginTask(Messages.CyberTrackerExportHandler_TaskName, 100);
					try {
						File generated = exporter.export(monitor);
						if (toDevice) {
							exporter.uploadPda(generated);
						} else {
							FileUtils.copyFile(generated, selectedFile);
						}
					} catch (Exception e) {
						CyberTrackerPlugIn.displayError(Messages.CyberTrackerExportHandler_ErrDialog_Title, Messages.CyberTrackerExportHandler_ErrDialog_Message);
						e.printStackTrace();
					}
					monitor.done();
					CyberTrackerPlugIn.displayInfo(Messages.CyberTrackerExportHandler_InfoDialog_Title, Messages.CyberTrackerExportHandler_InfoDialog_Message);
				}

			});
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	@Override
	protected boolean isResizable() {
		return true;
	}

}
