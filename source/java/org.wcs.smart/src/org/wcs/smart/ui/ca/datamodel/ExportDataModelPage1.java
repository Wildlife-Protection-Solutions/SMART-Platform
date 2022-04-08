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

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.internal.ca.datamodel.xml.DataModelToXmlConverter.IconOption;

/**
 * An abstract class for new patrol wizard pages.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class ExportDataModelPage1 extends WizardPage {

	public static final String PAGE_NAME = "ICON_OPTIONS"; //$NON-NLS-1$
	
	private Button btnOpAll, btnOpNone; //btnOpCustom, 
	
	/**
	 * @param pageName the name of the patrol wizard page
	 */
	protected ExportDataModelPage1() {
		super(PAGE_NAME);
	}
	
	 
	@Override
	public void createControl(Composite parent) {
		
		Composite outer = new Composite(parent, SWT.NONE);
		outer.setLayout(new GridLayout());
		((GridLayout)outer.getLayout()).marginWidth = 0;
		((GridLayout)outer.getLayout()).marginHeight = 0;
		
		Composite part = new Composite(outer, SWT.NONE);
		part.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
		part.setLayout(new GridLayout());
		
		Label l = new Label(part, SWT.WRAP);
		l.setText(Messages.ExportDataModelPage1_IncludeIcons);
		
		new Label(part, SWT.NONE);
		
		btnOpAll = new Button(part, SWT.RADIO);
		btnOpAll.setText(IDialogConstants.YES_LABEL);
		btnOpAll.setSelection(true);
		
		l = new Label(part, SWT.WRAP);
		l.setText(Messages.ExportDataModelPage1_IncludeIconsInfo);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)l.getLayoutData()).widthHint = 200;
		((GridData)l.getLayoutData()).horizontalIndent = 20;
		
		new Label(part, SWT.NONE);
		
		btnOpNone = new Button(part, SWT.RADIO);
		btnOpNone.setText(IDialogConstants.NO_LABEL);
		
		l = new Label(part, SWT.WRAP);
		l.setText(Messages.ExportDataModelPage1_ExcludeIconsInfo);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)l.getLayoutData()).widthHint = 200;
		((GridData)l.getLayoutData()).horizontalIndent = 20;
		
//		btnOpCustom = new Button(part, SWT.RADIO);
//		btnOpCustom.setText("Custom Icons Only");
//		
//		l = new Label(part, SWT.WRAP);
//		l.setText("Use this option to export the data model to xml along with any custom icons files added to the data model. Use this option when you want to use the export to create a new data model or merge changes into an existing data model.");
//		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
//		((GridData)l.getLayoutData()).widthHint = 200;
//		((GridData)l.getLayoutData()).horizontalIndent = 20;
//		
//		new Label(part, SWT.NONE);

		setTitle(Messages.ExportDataModelPage1_Title);
		setMessage(Messages.ExportDataModelPage1_Message);
		setControl(outer);
	}
	
	public IconOption getOption() {
		if (btnOpNone.getSelection()) return IconOption.NONE;
//		if (btnOpCustom.getSelection()) return IconOption.CUSTOM;
		if (btnOpAll.getSelection()) return IconOption.ALL;
		return IconOption.NONE;
	}
}
