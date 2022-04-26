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
package org.wcs.smart.ui.ca.datamodel;

import java.io.File;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.internal.ca.datamodel.xml.DataModelToXmlConverter.IconOption;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * An abstract class for new patrol wizard pages.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class ExportDataModelPage2 extends WizardPage {

	public static final String PAGE_NAME = "FILE_PAGE"; //$NON-NLS-1$

	private Text txtFile;
	
	/**
	 * @param pageName the name of the patrol wizard page
	 */
	protected ExportDataModelPage2() {
		super(PAGE_NAME);
	}
	
	private IconOption getIconOption() {
		return ((ExportDataModelWizard)getWizard()).getIconOption();
	}
	 
	@Override
	public void createControl(Composite parent) {
		
		Composite outer = new Composite(parent, SWT.NONE);
		outer.setLayout(new GridLayout());
		((GridLayout)outer.getLayout()).marginWidth = 0;
		((GridLayout)outer.getLayout()).marginHeight = 0;
		
		Composite part = new Composite(outer, SWT.NONE);
		part.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
		part.setLayout(new GridLayout(3, false));
		
		Label l = new Label(part, SWT.NONE);
		l.setText(Messages.ExportDataModelPage2_FileLabel);

		txtFile = new Text(part, SWT.BORDER);
		txtFile.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		txtFile.addListener(SWT.Modify, e->{
			((ExportDataModelWizard)getWizard()).setCanFinish(!txtFile.getText().isBlank());
		});
		Button btnSelect = new Button(part, SWT.PUSH);
		btnSelect.setText("..."); //$NON-NLS-1$
		btnSelect.addListener(SWT.Selection, e->{
			
			IconOption op = getIconOption();
			
			String type = DialogConstants.ZIP_FILES;
			if (op.getFileType().equals("xml")) { //$NON-NLS-1$
				type = DialogConstants.XML_FILES;
			}
			FileDialog fd = new FileDialog(this.getShell(), SWT.SAVE);
			fd.setFilterNames(new String[]{type, DialogConstants.ALL_FILES});
			fd.setFilterExtensions(new String[]{"*." + op.getFileType(), "*.*"}); //$NON-NLS-1$ //$NON-NLS-2$
			
			String file = fd.open();
			if (file == null) return;
			txtFile.setText(file);
			
		});
		
		if (SmartPlugIn.getDefault().getPreferenceStore().contains(ExportDataModelWizard.FILE_KEY)) {
			String filename = SmartPlugIn.getDefault().getPreferenceStore().getString(ExportDataModelWizard.FILE_KEY);
			txtFile.setText( filename );
		}else {
			String filename = System.getProperty("user.home") + File.separator + "datamodel.zip"; //$NON-NLS-1$ //$NON-NLS-2$
			txtFile.setText(filename);
		}

		setTitle(Messages.ExportDataModelPage2_Title);
		setMessage(Messages.ExportDataModelPage2_Message);
		setControl(outer);
	}
	
	public void updateFileName() {
		String filename = txtFile.getText();
		IconOption op = getIconOption();
		if (filename.toLowerCase().endsWith("." + op.getFileType())) return; //$NON-NLS-1$
		
		int index = filename.lastIndexOf('.');
		if (index < 0) {
			filename = filename + "." + op.getFileType(); //$NON-NLS-1$
		}else {
			filename = filename.substring(0, index+1) + op.getFileType();
		}
		txtFile.setText(filename);
		
	}
	public String getFile() {
		return txtFile.getText();
	}
}
