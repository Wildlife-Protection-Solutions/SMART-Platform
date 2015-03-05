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
package org.wcs.smart.conversion.ui;

import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * Dialog to display big miltiline massages with ability to copy data to clipboard.
 * Code is base on InputDialog class.
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class ReportDialog extends Dialog {

	private Text text;
	private String title;
	private String message;
	private String value = ""; //$NON-NLS-1$

	public ReportDialog(Shell parentShell, String title, String message, List<String> values) {
		this(parentShell, title, message, join(values, "\n")); //$NON-NLS-1$
	}
	
	public static String join(List<String> values, String delimeter) {
		StringBuilder sb = new StringBuilder();
		for (String v : values) {
			if (sb.length() > 0) {
				sb.append(delimeter);
			}
			sb.append(v);
		}
		return sb.toString();
	}

	public ReportDialog(Shell parentShell, String title, String message, String value) {
		super(parentShell);
		this.title = title;
		this.message = message;
		this.value = value;
	}

	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		if (title != null) {
			shell.setText(title);
		}
	}

	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID,
				IDialogConstants.OK_LABEL, true);
		text.setFocus();
		if (value != null) {
			text.setText(value);
			text.selectAll();
		}
	}

	protected Control createDialogArea(Composite parent) {
		// create composite
		Composite composite = (Composite) super.createDialogArea(parent);
		// create message
        if (message != null) {
    		Label label = new Label(composite, SWT.WRAP);
    		label.setText(message);
    		GridData data = new GridData(GridData.GRAB_HORIZONTAL
    				| GridData.GRAB_VERTICAL | GridData.HORIZONTAL_ALIGN_FILL
    				| GridData.VERTICAL_ALIGN_CENTER);
    		data.widthHint = convertHorizontalDLUsToPixels(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH);
    		data.grabExcessVerticalSpace = false;
    		label.setLayoutData(data);
    		label.setFont(parent.getFont());
        }

		text = new Text(composite, getInputTextStyle());
		GridData gd = new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL);
		gd.widthHint = 500;
		gd.grabExcessVerticalSpace = true;
		gd.verticalAlignment = SWT.FILL;
		text.setLayoutData(gd);
		text.setEditable(false);

		applyDialogFont(composite);

		((GridData)parent.getLayoutData()).heightHint = 400;

		return composite;
	}

	public int getInputTextStyle(){
		return SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL;
	}

}
