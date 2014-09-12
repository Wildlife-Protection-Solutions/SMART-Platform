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
package org.wcs.smart.observation.common.importwp.csv;

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
import org.wcs.smart.export.dialog.DelimiterCombo;
import org.wcs.smart.observation.internal.Messages;

/**
 * Option composite for importing waypoints from csv.  
 * 
 * @author Jeff
 * since 2.0
 *
 */
public class ImportCSVOptionsComposite extends Composite{
	
	private Label lblFile;
	private Text txtFile;
	private Button btnBrowse;

	private DelimiterCombo cmbDelimiter;
	
		
	public ImportCSVOptionsComposite(Composite parent) {
		super(parent, SWT.NONE);
		createControls(parent);
	}
	
	private void createControls(Composite parent) {
		setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		setLayout(new GridLayout(3, false));
		
		final FileDialog fd = new FileDialog(getShell(), parent.getStyle());
		fd.setFilterExtensions(new String[]{"*.csv", "*.*"}); //$NON-NLS-1$ //$NON-NLS-2$
		fd.setFilterNames(new String[]{Messages.ImportCSVOptionsComposite_0,Messages.ImportCSVOptionsComposite_1});
		
		lblFile = new Label(this, SWT.NONE);
		lblFile.setText(Messages.ImportCSVOptionsComposite_2);
		lblFile.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		
		txtFile = new Text(this, SWT.BORDER);
		txtFile.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		btnBrowse = new Button(this, SWT.NONE);
		btnBrowse.setText(Messages.ImportCSVOptionsComposite_3);
		btnBrowse.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				fd.setFileName(txtFile.getText());
				String file = fd.open();
				if (file != null) {
					txtFile.setText(file);
				}
			}
		});
		btnBrowse.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		
		
		Label l = new Label(this, SWT.NONE);
		l.setText(Messages.ImportCSVOptionsComposite_DelimiterLabel);
		l.setToolTipText(Messages.ImportCSVOptionsComposite_DelimiterTooltip);
		
		cmbDelimiter = new DelimiterCombo(this, SWT.NONE);
		cmbDelimiter.getCombo().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
	}

	public void addFileModifyListener(Listener listener) {
		txtFile.addListener(SWT.Modify, listener);
	}
	
	public char getDelimiter() throws Exception{
		return cmbDelimiter.getDelimiter();
	}
	
	public String getFileText() {
		return txtFile.getText();
	}
		
	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		lblFile.setEnabled(enabled);
		txtFile.setEnabled(enabled);
		btnBrowse.setEnabled(enabled);
	}
}

