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

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.export.config.ICsvDialogConfig;
import org.wcs.smart.internal.Messages;

/**
 * Composite containing file and headers option selection for csv export/import
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class CsvFileComposite extends Composite {

	private ICsvDialogConfig config;

	private Label lblFile;
	private Text txtFile;
	private Button btnHasHeader;
	private Text txtInfo;
	private Button btnBrowse;
	private Label lblDelimiter;
	private DelimiterCombo cmbDelimiters;

	public CsvFileComposite(Composite parent, int style, ICsvDialogConfig config) {
		super(parent, style);
		this.config = config;
		createControls();
	}

	private void createControls() {
		setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		setLayout(new GridLayout(3, false));
		
		final FileDialog fd = new FileDialog(getShell(), config.getFileDialogStyle());
		fd.setFilterExtensions(new String[]{"*.csv", "*.*"}); //$NON-NLS-1$ //$NON-NLS-2$
		fd.setFilterNames(new String[]{Messages.AbstractCsvDialog_FileFilter_Csv, Messages.AbstractCsvDialog_FileFilter_All});
		
		lblFile = new Label(this, SWT.NONE);
		lblFile.setText(Messages.AbstractCsvDialog_File_Label);
		lblFile.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		
		txtFile = new Text(this, SWT.BORDER);
		txtFile.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		btnBrowse = new Button(this, SWT.NONE);
		btnBrowse.setText(Messages.AbstractCsvDialog_Browse_Button);
		btnBrowse.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				fd.setFileName(txtFile.getText());
				String file = fd.open();
				if (file != null) {
					if (config.appendFileExtension() && !file.endsWith(".csv")) { //$NON-NLS-1$
						file += ".csv"; //$NON-NLS-1$
					}
					txtFile.setText(file);
				}
			}
		});
		btnBrowse.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		
		
		lblDelimiter = new Label(this, SWT.NONE);
		lblDelimiter.setText(Messages.CsvFileComposite_DelimiterLabel);
		lblDelimiter.setToolTipText(Messages.CsvFileComposite_DelimiterTooltip);
		
		cmbDelimiters = new DelimiterCombo(this, SWT.DROP_DOWN);
		
		if (config.includeHasHeader()){
			btnHasHeader = new Button(this, SWT.CHECK);
			String hasHeaderText = config.getHasHeaderText();
			if (hasHeaderText == null) {
				hasHeaderText = ""; //$NON-NLS-1$
			}
			btnHasHeader.setText(hasHeaderText);
			btnHasHeader.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		}
		if (config.getInfo() != null){
			txtInfo = new Text(this, SWT.WRAP | SWT.READ_ONLY);
			txtInfo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));
			((GridData)txtInfo.getLayoutData()).widthHint = 250;
			txtInfo.setText(config.getInfo());
		}
	}

	public void addFileModifyListener(Listener listener) {
		txtFile.addListener(SWT.Modify, listener);
	}
	
	public String getFileText() {
		return txtFile.getText();
	}
	
	public char getDelimiter() throws Exception{
		return cmbDelimiters.getDelimiter();
	}
	
	/**
	 * Sets the file name text box value
	 * @param fileName
	 */
	public void setFileText(String fileName){
		if (config.appendFileExtension() && !fileName.endsWith(".csv")) { //$NON-NLS-1$
			fileName += ".csv"; //$NON-NLS-1$
		}
		txtFile.setText(fileName);
	}
	
	public boolean getHeadersSelection() {
		return btnHasHeader != null && btnHasHeader.getSelection();
	}
	
	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		lblFile.setEnabled(enabled);
		txtFile.setEnabled(enabled);
		txtInfo.setEnabled(enabled);
		btnBrowse.setEnabled(enabled);
		lblDelimiter.setEnabled(enabled);
		cmbDelimiters.getControl().setEnabled(enabled);
		if (btnHasHeader != null){
			btnHasHeader.setEnabled(enabled);
		}
	}
}
