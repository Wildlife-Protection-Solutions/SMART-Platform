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
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.common.control.WarningDialog;
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

	private static final String LAST_DIR_KEY = "ABSTRACT_CSV_LAST_DIR_KEY"; //$NON-NLS-1$
	
	private ICsvDialogConfig config;
	protected CsvFileComposite csvComposite;
	
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
		Composite comp = (Composite) super.createDialogArea(parent);
		initDialogLabels();
		return comp;
	}
	
	/**
	 * Creates the file import option
	 * @param parent parent composite
	 * @param updateButtons <code>true</code> if dialog buttons should be updated when
	 * file changed <code>false</code> otherwise
	 * @return the csv file composite created
	 */
	protected CsvFileComposite createFileComposite(Composite parent, boolean updateButtons){
		csvComposite = new CsvFileComposite(parent, SWT.NONE, config);
		if (updateButtons){
			csvComposite.addFileModifyListener(new Listener() {
				@Override
				public void handleEvent(Event event) {
					updateButtons();
				}
			});
		}
		
		String initLocation = SmartPlugIn.getDefault().getDialogSettings().get(LAST_DIR_KEY);
		if (initLocation == null){
			initLocation = System.getProperty("user.home"); //$NON-NLS-1$
		}
		csvComposite.setFileText(initLocation + File.separator + config.getDefaultFileName());
		
		return csvComposite;
	}
	
	private void updateButtons(){
		if (getButton(IDialogConstants.OK_ID) == null) return;
		if (csvComposite.getFileText().length() > 0){
			getButton(IDialogConstants.OK_ID).setEnabled(true);
		}else{
			getButton(IDialogConstants.OK_ID).setEnabled(false);
		}
	}
	
	protected void initDialogLabels() {
		getShell().setText(config.getTitle());
		setTitle(config.getTitle());
		setMessage(config.getMessage());
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		Button btn = createButton(parent, IDialogConstants.OK_ID, config.getActionButtonText(), true);
		btn.setEnabled(false);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
		
		updateButtons();
	}

	@Override
	protected void buttonPressed(int buttonId) {
		try{
			if (IDialogConstants.OK_ID == buttonId) {
				String fileName = csvComposite.getFileText();
				char delimiter = csvComposite.getDelimiter();
			
				if (validateFilename(fileName)){
					SmartPlugIn.getDefault().getDialogSettings().put(SmartPlugIn.DEFAULT_DELIMITER_KEY, String.valueOf(delimiter));	
					SmartPlugIn.getDefault().getDialogSettings().put(LAST_DIR_KEY, (new File(fileName)).getParent());
				
					if (process(fileName, delimiter)){
						setReturnCode(OK);
						close();
					}
				}
			} else if (IDialogConstants.CANCEL_ID == buttonId) {
				setReturnCode(CANCEL);
				close();
			}
		}catch (Exception ex){
			MessageDialog.openError(getShell(), Messages.AbstractCsvDialog_ErrorTitle, ex.getMessage());
		}
	}

	/**
	 * Validates the filename before completing the process.
	 * @param fileName 
	 * @return <code>true</code> if file is okay, <code>false</code>if process should not continue
	 */
	protected boolean validateFilename(String fileName){
		return true;
	}
	
	@Override
	protected boolean isResizable() {
		return true;
	}

	protected boolean process(final String fileName, final char delimiter) {
		ProgressMonitorDialog dialog = new ProgressMonitorDialog(getShell());
		try {
			boolean headers = csvComposite.getHeadersSelection();
			ProcessingRunnable processRunnable = new ProcessingRunnable(fileName, delimiter, headers);
			dialog.run(true, true, processRunnable);
			
			
			if (processRunnable.isSuccess()) {
				//display warnings
				if (getWarnings() != null && getWarnings().size() > 0){
					WarningDialog wdialog = new WarningDialog(getShell(), config.getTitle(),Messages.AbstractCsvDialog_ImportWarning,getWarnings());
					wdialog.open();
				}
				MessageDialog.openInformation(getShell(), config.getTitle(), config.getSuccessMessage());
				return true;
			}else{
				MessageDialog.openError(getShell(), config.getTitle(), config.getFailMessage());
				return false;
			}
		} catch (Exception ex) {
			SmartPlugIn.displayLog(config.getFailMessage() + ex.getLocalizedMessage(), ex);
			return false;
		}
	}

	/**
	 * perform the appropriate actions
	 * 
	 * @param file the file to process
	 * @param delimiter the field delimiter 
	 * @param headers if headers are to be included
	 * @param monitor progress monitor
	 * @param session database session
	 * @return
	 * @throws Exception
	 */
	protected abstract boolean performAction(File file, char delimiter, 
			boolean headers, IProgressMonitor monitor, Session session) throws Exception;

	/**
	 * 
	 * @return set of warnings generated during action
	 */
	protected abstract List<String> getWarnings();
	
	/**
	 * Inner class responsible for wrapping import/export operation into {@link IRunnableWithProgress}
	 * 
	 * @author elitvin
	 * @since 1.0.0
	 */
	private class ProcessingRunnable implements IRunnableWithProgress {
		private final String file;
		private final boolean headers;
		private final char delimiter;
		private boolean success;

		private ProcessingRunnable(String file, char delimiter, boolean headers) {
			this.file = file;
			this.headers = headers;
			this.delimiter = delimiter;
		}

		@Override
		public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
			Session session = HibernateManager.openSession();
			try {
				boolean ok = performAction(new File(file), delimiter, headers, monitor, session);
				setSuccess(ok);
			} catch (final Exception ex) {
				setSuccess(false);
				getShell().getDisplay().syncExec(new Runnable() {
					@Override
					public void run() {
						SmartPlugIn.displayLog(Messages.AbstractCsvDialog_Fail_Error + SmartUtils.LINE_SEPARATOR + ex.getLocalizedMessage(), ex);
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
