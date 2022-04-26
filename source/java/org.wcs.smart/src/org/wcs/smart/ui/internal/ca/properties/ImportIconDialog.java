/*
 * Copyright (C) 2022 Wildlife Conservation Society
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
package org.wcs.smart.ui.internal.ca.properties;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.List;
import java.util.function.BiFunction;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.icon.IconSet;
import org.wcs.smart.ca.in.IconImporter;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.ui.SmartStyledTitleDialog;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Dialog for importing custom icons from CSV file.
 * 
 * @author Emily
 *
 */
public class ImportIconDialog extends SmartStyledTitleDialog {

	private static final String IMPORT_TITLE = Messages.ImportIconDialog_Title;
	private Text txtIconFile;
	private Text txtIconDirectory;
	
	private Session session;

	private Path csvFile;
	private Path iconDir;
	
	private BiFunction<Path,Path,Boolean> processor;
	
	public ImportIconDialog(Shell parent, Session session, BiFunction<Path,Path,Boolean> processor) {
		super(parent);
		this.session = session;
		this.processor = processor;
	}

	@Override
	public void okPressed() {
		
		csvFile = Paths.get(txtIconFile.getText());
		if (!Files.exists(csvFile)) {
			MessageDialog.openWarning(getShell(), IMPORT_TITLE, MessageFormat.format(Messages.ImportIconDialog_FileNotFound, csvFile.toString()));
			return;
		}
		
		iconDir = Paths.get(txtIconDirectory.getText());
		if (!Files.exists(iconDir)) {
			MessageDialog.openWarning(getShell(), IMPORT_TITLE, MessageFormat.format(Messages.ImportIconDialog_DirectoryNotFound, iconDir.toString()));
			return;
		}
		if (!processor.apply(csvFile,iconDir)) return;
		super.okPressed();
	}
	
	public Path getCsvFile() {
		return this.csvFile;
	}
	public Path getIconDirectory() {
		return this.iconDir;
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Control main = super.createDialogArea(parent);
		
		Composite comp = new Composite((Composite)main, SWT.NONE);
		comp.setLayout(new GridLayout(3, false));
		comp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Listener validate = e->validate();
		
		Label l = new Label(comp, SWT.NONE);
		l.setText(Messages.ImportIconDialog_IconFile);
		txtIconFile = new Text(comp, SWT.BORDER);
		txtIconFile.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		txtIconFile.addListener(SWT.Modify, validate);
		
		Button btnBrowseFile = new Button(comp, SWT.PUSH);
		btnBrowseFile.setText("..."); //$NON-NLS-1$
		btnBrowseFile.addListener(SWT.Selection, e->{
			FileDialog fd = new FileDialog(getShell(), SWT.OPEN);
			fd.setFilterExtensions(new String[] {"*.csv", "*.*"}); //$NON-NLS-1$ //$NON-NLS-2$
			fd.setFilterNames(new String[] {DialogConstants.CSV_FILES, DialogConstants.ALL_FILES});
			String file = fd.open();
			if (file == null) return;
			txtIconFile.setText(file);
		});
		
		l = new Label(comp, SWT.NONE);
		l.setText(Messages.ImportIconDialog_IconDirectory);
		txtIconDirectory = new Text(comp, SWT.BORDER);
		txtIconDirectory.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		txtIconDirectory.addListener(SWT.Modify, validate);
		
		btnBrowseFile = new Button(comp, SWT.PUSH);
		btnBrowseFile.setText("..."); //$NON-NLS-1$
		btnBrowseFile.addListener(SWT.Selection, e->{
			DirectoryDialog fd = new DirectoryDialog(getShell(), SWT.OPEN);
			String file = fd.open();
			if (file == null) return;
			txtIconDirectory.setText(file);
		});
		
		
		l = new Label(comp, SWT.NONE);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));
		
		l = new Label(comp, SWT.WRAP);
		l.setText(Messages.ImportIconDialog_FileInfo);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));
		((GridData)l.getLayoutData()).widthHint = 200;

		Button btnDownload =new Button(comp, SWT.PUSH);
		btnDownload.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, false, 3, 1));
		btnDownload.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EXPORT_ICON));
		btnDownload.setText(Messages.ImportIconDialog_ExportSample);
		btnDownload.addListener(SWT.Selection, e->exportSample());
		btnDownload.setBackground(comp.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		
		
		getShell().setText(IMPORT_TITLE);
		setTitle(IMPORT_TITLE);
		setMessage(Messages.ImportIconDialog_Message);
		
		return main;
	}
	
	@Override
	public void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		
		getButton(IDialogConstants.OK_ID).setText(DialogConstants.IMPORT_BUTTON_TEXT);
		getButton(IDialogConstants.OK_ID).setEnabled(false);
	}
	
	
	private void validate() {
		boolean ok = true;
		if (txtIconDirectory.getText().isBlank()) {
			ok = false;
		}
		if (txtIconFile.getText().isBlank()) {
			ok = false;
		}
		if (getButton(IDialogConstants.OK_ID) != null) getButton(IDialogConstants.OK_ID).setEnabled(ok);
	}
	
	
	private void exportSample() {
		FileDialog fd = new FileDialog(getShell(), SWT.SAVE);
		fd.setFileName(SmartDB.getCurrentConservationArea().getId() + "_customicons.csv"); //$NON-NLS-1$
		fd.setFilterExtensions(new String[] {"*.csv", "*.*"}); //$NON-NLS-1$ //$NON-NLS-2$
		fd.setFilterNames(new String[] {DialogConstants.CSV_FILES, DialogConstants.ALL_FILES});
		String file = fd.open();
		if (file == null) return;
		
		Path out = Paths.get(file);
		if (Files.exists(out)) {
			if (!MessageDialog.openQuestion(getShell(), IMPORT_TITLE,
					MessageFormat.format(Messages.ImportIconDialog_FileExists, out.toString()))){
				return;
			}
		}
		
		ConservationArea ca = session.get(ConservationArea.class, SmartDB.getCurrentConservationArea().getUuid());
		List<IconSet> sets = QueryFactory.buildQuery(session, IconSet.class, 
				new Object[] {"conservationArea", ca}).list(); //$NON-NLS-1$

		try {
			IconImporter.writeSampleFile(ca, sets, out);
			MessageDialog.openInformation(getShell(), IMPORT_TITLE,  MessageFormat.format(Messages.ImportIconDialog_SampleExportMsg, out.toString()));
		}catch(Exception ex) {
			SmartPlugIn.displayLog(ex.getMessage(), ex);
		}
	}
}
