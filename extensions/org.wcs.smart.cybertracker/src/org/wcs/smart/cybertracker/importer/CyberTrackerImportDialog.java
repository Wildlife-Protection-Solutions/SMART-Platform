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
package org.wcs.smart.cybertracker.importer;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

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
import org.wcs.smart.cybertracker.model.CyberTrackerPatrol;

/**
 * Dialog for importing CyberTracker application data.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class CyberTrackerImportDialog extends TitleAreaDialog {

	private Text txtFile;
	private File selectedFile;
	
	private CTPatrolTableContainer tableContainer;
	private CyberTrackerImporter importer = new CyberTrackerImporter();

	public CyberTrackerImportDialog(Shell parentShell) {
		super(parentShell);
	}
	
	public File getSelectedFile() {
		return selectedFile;
	}
	
	/**
	 * @see org.eclipse.jface.dialogs.TitleAreaDialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createDialogArea(Composite parent){
		Composite composite = (Composite) super.createDialogArea(parent);
		Composite main = new Composite(composite, SWT.NONE);
		main.setLayout(new GridLayout(5, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label lbl = new Label(main, SWT.NONE);
		lbl.setText(Messages.CyberTrackerImportDialog_File);
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));

		txtFile = new Text(main, SWT.BORDER);
		txtFile.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
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
		
		Button btnBrowse = new Button(main, SWT.NONE);
		btnBrowse.setText(Messages.CyberTrackerImportDialog_Button_Browse);
		btnBrowse.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog fd = new FileDialog(getShell(), SWT.MULTI | SWT.OPEN);
				fd.setFilterExtensions(new String[]{"*.xml", "*.*"}); //$NON-NLS-1$ //$NON-NLS-2$
				fd.setFilterNames(new String[]{Messages.CyberTrackerImportDialog_XmlFiles, Messages.CyberTrackerImportDialog_AllFiles});
				
				if (txtFile.getText() != null && !txtFile.getText().isEmpty()) {
					fd.setFilterPath(txtFile.getText());
				}
				String f = fd.open();
				if (f != null) {
					txtFile.setText(f);
				}
			}
		});
		btnBrowse.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));

		Button btnImport = new Button(main, SWT.NONE);
		btnImport.setText(Messages.CyberTrackerImportDialog_Button_Import);
		btnImport.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				performImport(false);
			}
		});
		btnImport.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));

		Button btnImportPda = new Button(main, SWT.NONE);
		btnImportPda.setText(Messages.CyberTrackerImportDialog_Button_ImportFromDevice);
		btnImportPda.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				performImport(true);
			}
		});
		btnImportPda.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		
		tableContainer = new CTPatrolTableContainer(composite, SWT.NONE);
		
		setTitle(Messages.CyberTrackerImportDialog_Title);
		setMessage(Messages.CyberTrackerImportDialog_Message);
		super.getShell().setText(Messages.CyberTrackerImportDialog_Title);
		super.setTitleImage(CyberTrackerPlugIn.getDefault().getImageRegistry().get(CyberTrackerPlugIn.CT_WIZARD_BANNER));
		
		return composite;
	}
	
	/**
	 * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CLOSE_LABEL, false);
		getButton(IDialogConstants.CANCEL_ID).setFocus();
		super.setReturnCode(IDialogConstants.CANCEL_ID);
	}

	
	/**
	 * @see org.eclipse.jface.dialogs.Dialog#buttonPressed(int)
	 */
	@Override
	protected void buttonPressed(int buttonId) {
//		if (IDialogConstants.OK_ID == buttonId) {
//			File file = new File(txtFile.getText());
//			if (!file.exists()) {
//				MessageDialog.openError(getShell(), Messages.CyberTrackerImportDialog_Error_Title, Messages.CyberTrackerImportDialog_Error_Message);
//				return;
//			}
//			selectedFile = file;
//			super.setReturnCode(IDialogConstants.OK_ID);
//		}
		close();
	}
	
	@Override
	protected boolean isResizable() {
		return true;
	}
	
	private void performImport(final boolean fromPda) {
		final File file = new File(txtFile.getText());
		if (!fromPda && !file.exists()) {
			MessageDialog.openError(getShell(), Messages.CyberTrackerImportDialog_Error_Title, Messages.CyberTrackerImportDialog_Error_Message);
			return;
		}
		
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
		try {
			pmd.run(true, false, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					monitor.beginTask(Messages.CyberTrackerImportDialog_Task_RawImport, 100);
					try {
						List<CyberTrackerPatrol> data = fromPda ? importer.importPdaData(monitor) : importer.importData(file, monitor);
						tableContainer.addTableData(data);
					} catch (Exception e) {
//						displayError("Error", "Error occured while importing data from CyberTracker into SMART.");
						e.printStackTrace();
						return;
					}
//					displayInfo("CyberTracker Import", "Import successfully completed.");
				}

			});
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}