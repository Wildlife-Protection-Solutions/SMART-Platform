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
import java.text.Collator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

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
import org.eclipse.jface.viewers.TableViewer;
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
import org.wcs.smart.report.internal.Messages;
import org.wcs.smart.report.model.Report;
import org.wcs.smart.report.ui.ReportLabelProvider;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.SmartUtils;

/**
 * Dialog for exporting reports to file.
 * <p>This dialog prompts the user for export format
 * and export location.
 * </p>
 * @author egouge
 * @since 1.0.0
 */
public class ExportReportDialog extends TitleAreaDialog {
	private static final String EXPORT_DIALOGITTLE = Messages.ExportReportDialog_ExportDialogTitle;

	private static final String FORMAT_SETTING = "Format"; //$NON-NLS-1$

	private static final String DIRECTORY_SETTING = "Directory"; //$NON-NLS-1$

	private static IDialogSettings settings = new DialogSettings("org.wcs.smart.report.exportdialog"); //$NON-NLS-1$
	
	private String fileName;
	private IExportFormat emitter;
	
	private Text txtFileName;
	private ComboViewer cmbEmitters;
	private boolean multipleFiles;
	private List<Report> reports;
	private TableViewer lstReports;
	
	/**
	 * @param parentShell
	 * @param reports to export 
	 * 
	 */
	public ExportReportDialog(Shell parentShell, List<Report> reports, boolean isMultiple) {
		super(parentShell);
		this.multipleFiles = isMultiple;
		
		this.reports = new ArrayList<Report>();
		this.reports.addAll(reports);
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
	
	public List<Report> getSelectedReports(){
		return this.reports;
	}
	
	/**
	 * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, Messages.ExportReportDialog_ExportButton,
				true);
		createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, false);
		
	}
	
	/**
	 * @see org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog#createContent(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Composite createDialogArea(Composite parent) {
		parent = (Composite) super.createDialogArea(parent);
		if (!multipleFiles){
			getShell().setText(Messages.ExportReportDialog_DialogTitleA1);
			setTitle(Messages.ExportReportDialog_DialogTitleA1 + ": " + reports.get(0).getName()); //$NON-NLS-1$
			
		}else{
			getShell().setText(Messages.ExportReportDialog_DialogTitleB);
			setTitle(Messages.ExportReportDialog_MultiExportPageTitle);
		}
		setMessage(Messages.ExportReportDialog_DialogMessage);
		
		Composite comp = new Composite(parent, SWT.NONE);
		
		comp.setLayout(new GridLayout(3, false));
		comp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Label lbl = new Label(comp, SWT.NONE);
		lbl.setText(Messages.ExportReportDialog_FormatLabel);
		
		cmbEmitters = new ComboViewer(comp, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbEmitters.getCombo().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		cmbEmitters.setContentProvider(ArrayContentProvider.getInstance());
		cmbEmitters.setLabelProvider(new LabelProvider(){
			@Override
			public String getText(Object object){
				return ((IExportFormat)object).getName() + " (." + ((IExportFormat)object).getFileExtension() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
			}
		});
		IExportFormat[] formats= ExportReportEngine.getSupportedExportFormats();
		Arrays.sort(formats, new Comparator<IExportFormat>() {
			@Override
			public int compare(IExportFormat o1, IExportFormat o2) {
				return Collator.getInstance().compare(o1.getName(), o2.getName());
			}
		});
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
									+ "." //$NON-NLS-1$
									+ ((IExportFormat) selection
											.getFirstElement())
											.getFileExtension());

						}
					});
		}
		
		lbl = new Label(comp, SWT.NONE);
		if (this.multipleFiles){
			lbl.setText(Messages.ExportReportDialog_OutputDirectoryLabel);
		}else{
			lbl.setText(Messages.ExportReportDialog_OutputFileLabel);
		}
		
		txtFileName = new Text(comp, SWT.BORDER);
		txtFileName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		Button btnBrowse = new Button(comp, SWT.NONE);
		btnBrowse.setText(Messages.ExportReportDialog_BrowseButton);

		String x = settings.get(FORMAT_SETTING);
		IExportFormat defaultExport = formats[0];
		if (x != null){
			for (int i = 0; i < formats.length; i ++){
				if (formats[i].getName().equals(x)){
					defaultExport = formats[i];
				}
			}
		}
		
		
		if (this.multipleFiles){
			Label l = new Label(comp, SWT.NONE);
			l.setText(Messages.ExportReportDialog_ReportsLabel);
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));
			
			lstReports = new TableViewer(comp, SWT.BORDER | SWT.MULTI);
			lstReports.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
			lstReports.setContentProvider(ArrayContentProvider.getInstance());
			lstReports.setLabelProvider(new ReportLabelProvider());
			lstReports.setInput(reports);
			((GridData)lstReports.getControl().getLayoutData()).heightHint = 150;
			
			Composite buttonPnl = new Composite(comp, SWT.NONE);
			buttonPnl.setLayout(new GridLayout());
			buttonPnl.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
			
			Button btnAdd = new Button(buttonPnl, SWT.PUSH);
			btnAdd.setText(DialogConstants.ADD_BUTTON_TEXT);
			btnAdd.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			btnAdd.addSelectionListener(new SelectionAdapter(){
				@Override
				public void widgetSelected(SelectionEvent e){
					ReportListDialog d = new ReportListDialog(getShell());
					if (d.open() == ReportListDialog.OK){
						for (Report r : d.getSelectedReports()){
							if (!reports.contains(r)){
								reports.add(r);
							}
						}
					}
					lstReports.refresh();
				}
			});
			
			
			Button btnRemove = new Button(buttonPnl, SWT.PUSH);
			btnRemove.setText(DialogConstants.DELETE_BUTTON_TEXT);
			btnRemove.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			btnRemove.addSelectionListener(new SelectionAdapter() {			
				@Override
				public void widgetSelected(SelectionEvent e) {
					IStructuredSelection selection = (IStructuredSelection) lstReports.getSelection();
					for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
						Object x = (Object) iterator.next();
						reports.remove(x);
					}
					lstReports.refresh();
				}
			});
		}
		
		cmbEmitters.setSelection(new StructuredSelection(defaultExport));
		
		String location = settings.get(DIRECTORY_SETTING);
		if (location == null){
			location = System.getProperty("user.home"); //$NON-NLS-1$
		}
		if (this.multipleFiles){
			txtFileName.setText(location);
			addDirectoryListener(btnBrowse);
		}else{
			location += File.separator + ExportReportEngine.getOutputFileName(reports.get(0), null, defaultExport.getFileExtension()).getName();
			txtFileName.setText(location);
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
				dd.setMessage(Messages.ExportReportDialog_DirectoryDialogMessage);
				dd.setText(Messages.ExportReportDialog_DirectoryDialogTitle);
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
				fd.setFilterExtensions(new String[]{"*." + info.getFileExtension(), "*.*"}); //$NON-NLS-1$ //$NON-NLS-2$
				fd.setFilterNames(new String[]{info.getName() + " (*." + info.getFileExtension() + ")", Messages.ExportReportDialog_AllFilesFilterName}); //$NON-NLS-1$ //$NON-NLS-2$
				
				String dir = fd.open();
				if (dir != null){
					txtFileName.setText(dir);
				}
			}
		});
	}
	
	@Override
	/**
	 * Validate the input before continuing
	 */
	protected void okPressed(){
		File dir = new File(txtFileName.getText());
		
		if (multipleFiles){
			if (!checkDirectory(dir)){
				return;
			}
		}else {
			if (!checkDirectory(dir.getParentFile())){
				return;
			}
		}
		
		updateValues();
		super.okPressed();
	}
	
	private boolean checkDirectory(File dir){
		if (!dir.exists()){
			if (!MessageDialog.openQuestion(getShell(), EXPORT_DIALOGITTLE, 
					MessageFormat.format(Messages.ExportReportDialog_DirDoesNotExist1, new Object[]{dir.toString()}))){
				return false;
			}else{
				if (!SmartUtils.createDirectory(dir)){
					return false;
				}
			}
		}
		if (!dir.isDirectory()){
			MessageDialog.openError(getShell(), Messages.ExportReportDialog_Error_DialogTitle, Messages.ExportReportDialog_InvalidDir);
			return false;
		}
		return true;
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