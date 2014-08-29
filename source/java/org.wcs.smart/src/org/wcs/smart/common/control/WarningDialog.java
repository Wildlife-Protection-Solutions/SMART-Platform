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
package org.wcs.smart.common.control;

import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * Extension of a message dialog that includes
 * a list of strings displayed in a text box below
 * the message.
 * 
 * @author Emily
 *
 */
public class WarningDialog extends MessageDialog {

	private List<String> warnings;
	
	public WarningDialog(Shell parentShell, String dialogTitle,
			String dialogMessage, List<String> warnings) {

		super(parentShell, dialogTitle, null, dialogMessage,
				MessageDialog.WARNING, new String[]{IDialogConstants.OK_LABEL}, 0);
		
		this.warnings = warnings;
	}

	public WarningDialog(Shell parentShell, String dialogTitle,
			String dialogMessage, List<String> warnings, String[] buttonLabels, int index) {

		super(parentShell, dialogTitle, null, dialogMessage,
				MessageDialog.WARNING, buttonLabels, index);
		
		this.warnings = warnings;
	}
	
    protected Control createCustomArea(Composite parent) {
    	Text txtWarnings = new Text(parent, SWT.MULTI | SWT.V_SCROLL | SWT.BORDER);
    	txtWarnings.setEditable(false);
    	txtWarnings.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
    	GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
    	gd.heightHint = 200;
    	txtWarnings.setLayoutData(gd);
    	
    	StringBuilder sb = new StringBuilder();
    	for (String warn : warnings){
    		sb.append(warn);
    		sb.append("\n"); //$NON-NLS-1$
    	}
    	txtWarnings.setText(sb.toString());
        return txtWarnings;
    }

}
