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
package org.wcs.smart.ui.internal.ca;

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
import org.wcs.smart.ca.in.EmployeeCsvImporter;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.internal.Messages;


/**
 * Dialog for selecting a CSV file to import.
 * Created off the ImportEmployeeDialog.java
 * @author Garrett
 * @since 1.0.0
 */
public class ExportEmployeeDialog extends TitleAreaDialog {

	
	private Text txtFile;
	private String fileName = null;
	private boolean skipHeader = false;
	private Button btnSkipHeader;
	
	/**
	 * Create the dialog.
	 * 
	 * @param parent
	 * 
	 */
	public ExportEmployeeDialog(Shell parent) {
		super(parent);
	
	}

	@Override
	protected boolean isResizable() {
		return true;
	}

	
	
	/**
	 * Create contents of the dialog.
	 */
	@Override
	public Control createDialogArea(Composite parent){
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		main.setLayout(new GridLayout(3, false));
		final FileDialog fd = new FileDialog(getShell());
		fd.setFilterExtensions(new String[]{"*.csv", "*.*"}); //$NON-NLS-1$ //$NON-NLS-2$
		fd.setFileName(Messages.ExportEmployeeDialog_CSVFileName);
		fd.setFilterNames(new String[]{Messages.ExportEmployeeDialog_CSVFilterName, Messages.ExportEmployeeDialog_AllFilesFilterName});
		Label lbl = new Label(main, SWT.NONE);
		lbl.setText(Messages.ExportEmployeeDialog_FileLabe);
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
		btnBrowse.setText(Messages.ExportEmployeeDialog_BrowseButton);
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
		btnSkipHeader = new Button(main, SWT.CHECK);
		btnSkipHeader.setText(Messages.ExportEmployeeDialog_IncludeHaderOp);
		btnSkipHeader.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		Text txtinfo = new Text(main, SWT.NONE | SWT.READ_ONLY);
		txtinfo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));
		txtinfo.setText(Messages.ExportEmployeeDialog_CSVFormat_1
				+ Messages.ExportEmployeeDialog_CSVFormat_2
				+ Messages.ExportEmployeeDialog_CSVFormat_3);
		getShell().setText(Messages.ExportEmployeeDialog_DialogTitle);
		setMessage(Messages.ExportEmployeeDialog_DialogMessage);
		return parent;
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		Button btn = createButton(parent, IDialogConstants.OK_ID, Messages.ExportEmployeeDialog_ExportButton, true);
		btn.setEnabled(false);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}
	
	/**
	 * @return the selected filename
	 */
	private String getFileName(){
		return this.fileName;
	}
	/**
	 * @return if the first line is to be skipped or not
	 */
	public boolean getSkipHeader(){
		return this.skipHeader;
	}
	
	@Override
	protected void buttonPressed(int buttonId) {
		fileName = txtFile.getText();
		
		if (IDialogConstants.OK_ID == buttonId) {
			setReturnCode(OK);
			loadData();
			close();
		} else if (IDialogConstants.CANCEL_ID == buttonId) {
			setReturnCode(CANCEL);
			close();
		}
	}
	
	/**
	 * Loads employee data into the database.
	 */
	private void loadData(){
		ProgressMonitorDialog dialog = new ProgressMonitorDialog(getShell());
		try{
		dialog.run(true, true, new IRunnableWithProgress() {
			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException,
					InterruptedException {
				Session session = HibernateManager.openSession();
				try{
					EmployeeCsvImporter importer = new EmployeeCsvImporter();
					final boolean ok =importer.importCsvFile(new File(getFileName()), getSkipHeader(), monitor, session);
					getShell().getDisplay().syncExec(new Runnable() {
						@Override
						public void run() {
							if(ok){
								MessageDialog.openInformation(getShell(),Messages.ExportEmployeeDialog_DialogTitle,Messages.ExportEmployeeDialog_SuccessMessage); 	
							}else{
								MessageDialog.openError(getShell(), Messages.ExportEmployeeDialog_DialogTitle, Messages.ExportEmployeeDialog_FailureMessage);
							}
							
						}
					});
				}catch (final Exception ex){
					getShell().getDisplay().syncExec(new Runnable(){

						@Override
						public void run() {
							SmartPlugIn.displayLog(getShell(), Messages.ExportEmployeeDialog_Error_FailedMessage + ex.getLocalizedMessage(), ex);
						}						
					});	
				}finally{
					session.close();
				}
			}
		});
		}catch (Exception ex){
			SmartPlugIn.displayLog(getShell(), Messages.ExportEmployeeDialog_Error_FailedMessage + ex.getLocalizedMessage(), ex);
		}
	}
	

}