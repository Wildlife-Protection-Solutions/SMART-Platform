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

import org.eclipse.birt.report.engine.api.EmitterInfo;
import org.eclipse.jface.dialogs.IDialogConstants;
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
import org.wcs.smart.report.manger.ReportManager;
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
	
	private String fileName;
	private EmitterInfo emitter;
	
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
	public EmitterInfo getOutputFormat(){
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
			setMessage("Export Report: " + this.report.getName());
		}else{
			setMessage("Export Reports");
		}
		getShell().setText("Export");
		
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
				return ((EmitterInfo)object).getFormat() + " (." + ((EmitterInfo)object).getFormat() + ")";
			}
		});
		cmbEmitters.setInput(ReportManager.getReportEngine().getEmitterInfo());
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
									+ ((EmitterInfo) selection
											.getFirstElement())
											.getFormat());

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

		EmitterInfo defaultEmitter = (EmitterInfo) ((Object[])cmbEmitters.getInput() )[0];
		cmbEmitters.setSelection(new StructuredSelection(defaultEmitter));
		
		if (this.multipleFiles){
			
			addDirectoryListener(btnBrowse);
		}else{
			txtFileName.setText(report.getName().replaceAll("[^a-zA-Z0-9]", "") + "." +  defaultEmitter.getFormat());
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
				dd.setText("Export");
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
				
				EmitterInfo info = (EmitterInfo) ((IStructuredSelection)cmbEmitters.getSelection()).getFirstElement();
				fd.setFilterExtensions(new String[]{"*." + info.getFormat(), "*.*"});
				fd.setFilterNames(new String[]{info.getFormat() + " (*." + info.getFormat() + ")", "All Files (*.*)"});
				
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
			emitter = (EmitterInfo)selection.getFirstElement();
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