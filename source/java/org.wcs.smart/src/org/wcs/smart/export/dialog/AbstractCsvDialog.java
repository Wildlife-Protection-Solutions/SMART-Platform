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
package org.wcs.smart.export.dialog;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.export.config.ICsvDialogConfig;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.util.SmartUtils;

/**
 * Dialog for exporting/importing into/from CSV file
 * 
 * @author elitvin
 * @since 1.0.0
 */
public abstract class AbstractCsvDialog extends TitleAreaDialog {

	private ICsvDialogConfig config;
	
	private Text txtFile;
	private Button btnHasHeader;
	
	/**
	 * @param parentShell
	 */
	public AbstractCsvDialog(Shell parentShell, ICsvDialogConfig config) {
		super(parentShell);
		this.config = config;
	}

	/**
	 * Create contents of the dialog.
	 */
	@Override
	public Control createDialogArea(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		main.setLayout(new GridLayout(3, false));
		
		final FileDialog fd = new FileDialog(getShell());
		fd.setFilterExtensions(new String[]{"*.csv", "*.*"}); //$NON-NLS-1$ //$NON-NLS-2$
		fd.setFilterNames(new String[]{Messages.AbstractCsvDialog_FileFilter_Csv, Messages.AbstractCsvDialog_FileFilter_All});
		
		Label lbl = new Label(main, SWT.NONE);
		lbl.setText(Messages.AbstractCsvDialog_File_Label);
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		
		txtFile = new Text(main, SWT.BORDER);
		txtFile.addListener(SWT.Modify, new Listener() {
			@Override
			public void handleEvent(Event event) {
				if (txtFile.getText().length() > 0){
					getButton(IDialogConstants.OK_ID).setEnabled(true);
				}else{
					getButton(IDialogConstants.OK_ID).setEnabled(false);
				}
				
			}
		});
		txtFile.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		Button btnBrowse = new Button(main, SWT.NONE);
		btnBrowse.setText(Messages.AbstractCsvDialog_Browse_Button);
		btnBrowse.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				fd.setFileName(txtFile.getText());
				String file = fd.open();
				if (file != null){
					txtFile.setText(file);
				}
				
			}
		});
		btnBrowse.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		
		btnHasHeader = new Button(main, SWT.CHECK);
		String hasHeaderText = config.getHasHeaderText();
		if (hasHeaderText == null) {
			hasHeaderText = ""; //$NON-NLS-1$
		}
		btnHasHeader.setText(hasHeaderText);
		btnHasHeader.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		btnHasHeader.setVisible(config.includeHasHeader());
		
		Text txtinfo = new Text(main, SWT.WRAP | SWT.READ_ONLY);
		txtinfo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));
		((GridData)txtinfo.getLayoutData()).widthHint = 250;
		txtinfo.setText(config.getInfo());
		
		getShell().setText(config.getTitle());
		setTitle(config.getTitle());
		setMessage(config.getMessage());
		return parent;
		
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		Button btn = createButton(parent, IDialogConstants.OK_ID, config.getActionButtonText(), true);
		btn.setEnabled(false);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}

	@Override
	protected void buttonPressed(int buttonId) {
		String fileName = txtFile.getText();
		if (IDialogConstants.OK_ID == buttonId) {
			if (process(fileName)){
				setReturnCode(OK);
				close();
			}
		} else if (IDialogConstants.CANCEL_ID == buttonId) {
			setReturnCode(CANCEL);
			close();
		}
	}

	@Override
	protected boolean isResizable() {
		return true;
	}

	protected boolean process(final String fileName) {
		ProgressMonitorDialog dialog = new ProgressMonitorDialog(getShell());
		try {
			boolean headers = btnHasHeader != null && btnHasHeader.getSelection();
			ProcessingRunnable processRunnable = new ProcessingRunnable(fileName, headers);
			dialog.run(true, true, processRunnable);
			if (processRunnable.isSuccess()) {
				MessageDialog.openInformation(getShell(), config.getTitle(), config.getSuccessMessage());
				return true;
			}else{
				MessageDialog.openError(getShell(), config.getTitle(), config.getFailMessage());
				return false;
			}
		} catch (Exception ex) {
			SmartPlugIn.displayLog(getShell(), config.getFailMessage() + ex.getLocalizedMessage(), ex);
			return false;
		}
	}

	protected abstract boolean performAction(File file, boolean headers, IProgressMonitor monitor, Session session) throws Exception;

	/**
	 * Inner class responsible for wrapping import/export operation into {@link IRunnableWithProgress}
	 * 
	 * @author elitvin
	 * @since 1.0.0
	 */
	private class ProcessingRunnable implements IRunnableWithProgress {
		private final String file;
		private final boolean headers;
		private boolean success;

		private ProcessingRunnable(String file, boolean headers) {
			this.file = file;
			this.headers = headers;
		}

		@Override
		public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
			Session session = HibernateManager.openSession();
			try {
				boolean ok = performAction(new File(file), headers, monitor, session);
				setSuccess(ok);
			} catch (final Exception ex) {
				setSuccess(false);
				getShell().getDisplay().syncExec(new Runnable() {
					@Override
					public void run() {
						SmartPlugIn.displayLog(getShell(), Messages.AbstractCsvDialog_Fail_Error + SmartUtils.LINE_SEPARATOR + ex.getLocalizedMessage(), ex);
					}						
				});
			} finally {
				session.close();
			}
		}

		public boolean isSuccess() {
			return success;
		}

		private void setSuccess(boolean success) {
			this.success = success;
		}
		
	}
	
}
