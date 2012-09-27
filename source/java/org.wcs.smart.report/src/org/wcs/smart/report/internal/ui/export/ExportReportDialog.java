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
package org.wcs.smart.report.internal.ui.export;

import java.io.File;

import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.report.export.IExportFormat;
import org.wcs.smart.report.export.internal.ExportReportEngine;
import org.wcs.smart.report.model.Report;

/**
 * Dialog for exporting reports to file.
 * <p>This dialog prompts the user for export format
 * and export location.
 * </p>
 * @author egouge
 * @since 1.0.0
 */
public class ExportReportDialog extends TitleAreaDialog {
	private static final String FORMAT_SETTING = "Format";

	private static final String DIRECTORY_SETTING = "Directory";

	private static IDialogSettings settings = new DialogSettings("org.wcs.smart.report.exportdialog");
	
	private String fileName;
	private IExportFormat emitter;
	
	private Text txtFileName;
	private ComboViewer cmbEmitters;
	private boolean multipleFiles;
	private Report report;
	
	/**
	 * @param parentShell
	 * @param report if not null the dialog prompts the user
	 * for a export file; otherwise it prompts the user for a directory
	 * 
	 */
	public ExportReportDialog(Shell parentShell, Report report) {
		super(parentShell);
		this.multipleFiles = report == null;
		this.report = report;
	}
	
	/**
	 * 
	 * @return ouptut file/directory selected by the user
	 */
	public String getOutputDir(){
		return this.fileName;
	}
	/**
	 * @return the output format selected by the user
	 */
	public IExportFormat getOutputFormat(){
		return this.emitter;
	}
	
	/**
	 * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, "Export",
				true);
		createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, false);
		
	}
	
	/**
	 * @see org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog#createContent(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Composite createDialogArea(Composite parent) {
		if (!multipleFiles){
			getShell().setText("Export Report: " + this.report.getName());
		}else{
			getShell().setText("Export Reports");
		}
		setMessage("Select the export location and format.");
		
		Composite comp = new Composite(parent, SWT.NONE);
		
		comp.setLayout(new GridLayout(3, false));
		comp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		Label lbl = new Label(comp, SWT.NONE);
		lbl.setText("Export Format:");
		
		cmbEmitters = new ComboViewer(comp, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbEmitters.getCombo().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		cmbEmitters.setContentProvider(ArrayContentProvider.getInstance());
		cmbEmitters.setLabelProvider(new LabelProvider(){
			@Override
			public String getText(Object object){
				return ((IExportFormat)object).getName() + " (." + ((IExportFormat)object).getFileExtension() + ")";
			}
		});
		IExportFormat[] formats= ExportReportEngine.getSupportedExportFormats();
		cmbEmitters.setInput(formats);
		
		if (!this.multipleFiles) {
			cmbEmitters
					.addSelectionChangedListener(new ISelectionChangedListener() {
						@Override
						public void selectionChanged(SelectionChangedEvent event) {
							IStructuredSelection selection = ((IStructuredSelection) cmbEmitters
									.getSelection());
							if (selection == null || selection.isEmpty()) {
								return;
							}
							String filename = txtFileName.getText();
							if (filename.lastIndexOf('.') <= 0)
								return;
							txtFileName.setText(filename.substring(0,
									filename.lastIndexOf('.'))
									+ "."
									+ ((IExportFormat) selection
											.getFirstElement())
											.getFileExtension());

						}
					});
		}
		
		lbl = new Label(comp, SWT.NONE);
		if (this.multipleFiles){
			lbl.setText("Output Directory:");
		}else{
			lbl.setText("Output File:");
		}
		
		txtFileName = new Text(comp, SWT.BORDER);
		txtFileName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		Button btnBrowse = new Button(comp, SWT.NONE);
		btnBrowse.setText("Browse...");

		String x = settings.get(FORMAT_SETTING);
		IExportFormat defaultExport = formats[0];
		if (x != null){
			for (int i = 0; i < formats.length; i ++){
				if (formats[i].getName().equals(x)){
					defaultExport = formats[i];
				}
			}
		}
		
		cmbEmitters.setSelection(new StructuredSelection(defaultExport));
		
		if (this.multipleFiles){
			
			x = settings.get(DIRECTORY_SETTING);
			if (x != null){
				txtFileName.setText(x);	
			}
			addDirectoryListener(btnBrowse);
		}else{
			x = settings.get(DIRECTORY_SETTING);
			if (x == null){
				x = "";
			}else{
				x += File.separator;
			}
			x += report.getName().replaceAll("[^a-zA-Z0-9]", "") + "." +  defaultExport.getFileExtension(); 
			txtFileName.setText(x);
			addFileListner(btnBrowse);
		}
		return comp;
	}
	
	/*
	 * Adds a listener to select a directory
	 */
	private void addDirectoryListener(Button btnBrowse){
		btnBrowse.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog dd = new DirectoryDialog(getShell());
				dd.setMessage("Select directory to place exported report(s).");
				dd.setText("Export Directory");
				dd.setFilterPath(txtFileName.getText());
				String dir = dd.open();
				if (dir != null){
					txtFileName.setText(dir);
				}
			}
		});
	}
	/*
	 * Adds a listener to selected a file
	 */
	private void addFileListner(Button btnBrowse){
		btnBrowse.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog fd = new FileDialog(getShell(), SWT.SAVE);
				fd.setFileName(txtFileName.getText());
				
				IExportFormat info = (IExportFormat) ((IStructuredSelection)cmbEmitters.getSelection()).getFirstElement();
				fd.setFilterExtensions(new String[]{"*." + info.getFileExtension(), "*.*"});
				fd.setFilterNames(new String[]{info.getName() + " (*." + info.getFileExtension() + ")", "All Files (*.*)"});
				
				String dir = fd.open();
				if (dir != null){
					txtFileName.setText(dir);
				}
			}
		});
	}
	
	@Override
	/**
	 * Validsate the input before continuing
	 */
	protected void okPressed(){
		File dir = new File(txtFileName.getText());
		if (multipleFiles){
			if (!dir.isDirectory()){
				MessageDialog.openError(getShell(), "Error", "Invalid directory.");
			}
			if (!dir.exists()){
				if (!MessageDialog.openConfirm(getShell(), "Export", "The directory " + txtFileName.getText() + " does not exist and will be created.  Are you sure you want to continue?")){
					return;
				}
			}else{
				if (!MessageDialog.openConfirm(getShell(), "Export", "The files in the directory " + txtFileName.getText() + " may be overwritten.  Are you sure you want to continue?")){
					return;
				}
			}
		}else {
			if (dir.exists()){
				if (!MessageDialog.openConfirm(getShell(), "Export", "The file " + txtFileName.getText() + " exists and will be overwritten.  Are you sure you want to continue?")){
					return;
				}
			}
		}
		
		updateValues();
		super.okPressed();
	}
	
	private void updateValues(){
		fileName = txtFileName.getText();
		IStructuredSelection selection = ((IStructuredSelection)cmbEmitters.getSelection());
		if (selection != null && !selection.isEmpty()){
			emitter = (IExportFormat)selection.getFirstElement();
		}
		try{
			if (this.multipleFiles){
				settings.put(DIRECTORY_SETTING, (new File(fileName)).toString());
			}else{
				settings.put(DIRECTORY_SETTING, (new File(fileName)).getParent()  );
			}
			settings.put(FORMAT_SETTING, emitter.getName());
		}catch (Exception ex){
			//eatme
		}
	}
	
	/**
	 * @see org.eclipse.jface.dialogs.Dialog#isResizable()
	 * @return <code>true</code>
	 */
	@Override
	public boolean isResizable() {
		return true;
	}

}